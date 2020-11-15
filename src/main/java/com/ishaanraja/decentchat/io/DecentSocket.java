package com.ishaanraja.decentchat.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ishaanraja.decentchat.config.DecentConfig;
import com.ishaanraja.decentchat.config.DecentLogger;
import com.ishaanraja.decentchat.message.Message;
import com.ishaanraja.decentchat.message.PingMessage;
import com.ishaanraja.decentchat.message.PongMessage;

/**
 * This class is in charge of reading from and writing to a specific Socket 
 * corresponding to a certain peer. 
 * 
 */
public class DecentSocket implements Runnable {
	
	private static final int NORMAL_TIMEOUT = 200;
	/**
	 * EOT (end of transmission) is an ASCII character with decimal representation of 4. This character
	 * is sent by DecentSocket to indicate that the socket is being closed. 
	 * When received, this indicates that the remote end of the socket is being closed.
	 */
	private static final char EOT = 4;
	
	private Socket socket;
	private Queue<Message> messageQueue;
	private DecentCallback callback;
	private boolean online;
	private boolean testingPong;
	private boolean pongReceived;
	private boolean isPeer;
	
	private BufferedReader socketReader;
	private BufferedWriter socketWriter;
	
	/**
	 * Constructs a new DecentSocket object.
	 * 
	 * @param callback The DecentCallback object that will be called on certain events
	 * @param socket The socket object that will be communicated with
	 */
	protected DecentSocket(DecentCallback callback, Socket socket) {
		this.callback = callback;
		this.socket = socket;
		messageQueue = new LinkedBlockingQueue<Message>();
		online = true;
		isPeer = callback.canAddSocketPeer(socket.getInetAddress());
		if(isPeer) {
			callback.addSocketPeer(this);
		}
		try {
			socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			new Thread(this).start();
		}
		catch(IOException e) {
			DecentLogger.write("Could not open socket to "+getInetAddress()+"because of "+e.getMessage());
			stop();
			
		}
	}

	@Override
	public void run() {
		try {
			socket.setSoTimeout(NORMAL_TIMEOUT);
			socket.setKeepAlive(true);
			flushMessageQueue();
			readSocket();
			flushMessageQueue();
			if(isPeer) {
				sustainSocket();
			}
			else {
				stop();
			}
		}
		catch(SocketException e) {
			stop();
		}
		catch(Exception e) {
			
		}
		try {
			socket.close();
		} catch (IOException e1) {
			DecentLogger.write("Failed to close socket for "+socket.getInetAddress().getHostAddress());
		}
	}
	/**
	 * Reads from the Socket and replies with messages as needed.
	 */
	private void readSocket() {
		try {
			String message = socketReader.readLine();
			while(!socket.isInputShutdown() && message != null) {
				if(message.charAt(0) == EOT) {
					stop();
				}
				JsonObject jsonObj = JsonParser.parseString(message).getAsJsonObject();
				//If getting by "type" throws an exception, that means we have an invalid message and we throw the message away
				String type = jsonObj.get("type").getAsString();
				if(type.equals("ping")) {
					sendString(new PongMessage(DecentConfig.getPeeringDifficulty()).toJson());
				}
				else if(type.equals("pong")) {
					//Don't execute message callback for pong, unless we asked for it
					if(testingPong && !pongReceived) {
						pongReceived = true;
						callback.onSocketMessageReceived(message, this);
					}
				}
				else {
					Message m = callback.onSocketMessageReceived(message, this);	
					if(m != null) {
						send(m);
					}
				}
				message = socketReader.readLine();
			}
		}
		catch(Exception e) {
			//Do nothing - sometimes client will receive invalid messages
		}
		catch(OutOfMemoryError e) {
			DecentLogger.write("A DecentSocket thread has run out of memory. This can be caused by insufficient memory allocation or a malicious peer sending long messages.");
			stop();
		}
	}
	/**
	 * Creates a loop of reading and writing to the socket. If this DecentSocket is a peer 
	 * (and not just a one-time reply), this method is used.
	 */
	private void sustainSocket() {
		while(online) {
			readSocket();
			flushMessageQueue();
		}
	}
	private void flushMessageQueue() {
		while(!socket.isOutputShutdown() && !messageQueue.isEmpty()) {
			sendString(messageQueue.poll().toJson());
		}
	}
	/**
	 * Adds a message to the sending queue. 
	 * 
	 * @param m The message that will be sent
	 * @param destination The destination it is bound for
	 */
	public void send(Message m) {
		messageQueue.add(m);
	}
	/**
	 * Sends a given String to the Socket
	 * @param text
	 */
	private void sendString(String text) {
		try {
			socketWriter.write(text);
			socketWriter.newLine();
			socketWriter.flush();
		} 
		catch (IOException e) {
			DecentLogger.write("Could not send message to "+getInetAddress()+" because of "+e.getMessage());
		}
	}
	/**
	 * Sends a ping message and waits to see if a pong message is received back.
	 * 
	 * Note: This method is intended to be called from a thread not within this DecentSocket,
	 * particularly because this method blocks and that might prevent the thread from reading/writing
	 * from the remote socket.
	 * 
	 * @param m The ping message to send
	 * @param timeout The amount of time (in milliseconds) to wait for a pong response
	 * @return true/false whether a pong message was received back
	 */
	public boolean testPing(PingMessage m, int timeout) {
		try {
			if(online) {
				testingPong = true;
				pongReceived = false;
				sendString(m.toJson());
				long timeWaited = 0;
				while(pongReceived == false && timeWaited < timeout) {
					Thread.sleep(10);
					timeWaited+=10;
				}
				testingPong = false;
				return pongReceived;
			}
		}
		catch (Exception e) {
			DecentLogger.write("Could not test ping because of "+e.getMessage());
		}
		return false;
	}
	/**
	 * Returns the InetAddress of the remote end of the socket.
	 * 
	 * @return InetAddress of other end of the socket.
	 */
	public InetAddress getInetAddress() {
		return socket.getInetAddress();
	}
	/**
	 * Closes the socket and shuts down input/output streams.
	 */
	private void closeSocket() {
		try {
			if(socket.isConnected()) {
				sendString(String.valueOf(EOT));
			}
			socket.shutdownInput();
			socket.shutdownOutput();
			socket.close();
		} catch (IOException e) {

		}
	}
	/**
	 * Closes the socket, removes it from the peers list, and stops the sustainSocket() 
	 * loop (if running).
	 */
	public void stop() {
		closeSocket();
		if(isPeer) {
			callback.removeSocketPeer(getInetAddress());
		}
		online = false;
	}
}
