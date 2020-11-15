package com.ishaanraja.decentchat.io;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import com.ishaanraja.decentchat.config.DecentConfig;
import com.ishaanraja.decentchat.config.DecentLogger;
import com.ishaanraja.decentchat.message.PingMessage;

public class DecentListener implements Runnable {
	
	//Timeout in milliseconds
	private static final int PONG_TIMEOUT = 500;
	
	private DecentCallback callback;
	private Map<InetAddress, DecentSocket> peers;
	private boolean online;
	
	/**
	 * Constructs a new DecentListener object. This is a ServerSocket and accepts/delegates
	 * any incoming connections.
	 * 
	 * @param callback The DecentCallback object that will be called on certain events
	 * @param peers A Map of the client's peers
	 */
	public DecentListener(DecentCallback callback, Map<InetAddress, DecentSocket> peers) {
		this.callback = callback;
		this.peers = peers;
		online = true;
		new Thread(this).start();
	}

	@Override
	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(DecentConfig.PORT);
			while(online) {
				Socket socket = serverSocket.accept();
				//If someone already peered with reconnects, the client checks to see if the existing socket is dead
				//If it is, it removes it and creates a new one
				if(peers.containsKey(socket.getInetAddress())) {
					DecentSocket s = peers.get(socket.getInetAddress());
					if(!s.testPing(new PingMessage(), PONG_TIMEOUT)) {
						s.stop();
					}
				}
				new DecentSocket(callback, socket);
			}
			serverSocket.close();
		}
		catch(BindException e) {
			DecentLogger.write("Unable to bind to port (are there two instances running on this machine?)");
			e.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
	}
	/**
	 * Stops this thread from running.
	 */
	public void stop() {
		online = false;
	}

}
