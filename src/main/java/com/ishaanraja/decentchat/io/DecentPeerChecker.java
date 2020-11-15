package com.ishaanraja.decentchat.io;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.ishaanraja.decentchat.config.DecentConfig;
import com.ishaanraja.decentchat.config.DecentLogger;
import com.ishaanraja.decentchat.message.PingMessage;

/**
 * DecentPeerChecker is used to verify at one time
 * if a candidate peer is online or not. 
 * 
 * This is a thread that contains a queue of peers to check. 
 * For every given candidate peer, it sends a ping message and waits a
 * prescribed number of seconds for a pong message in response.
 * 
 * If the client is online (and responds promptly), a new DecentSocket object is constructed
 * if one does not exist already.
 */
public class DecentPeerChecker implements Runnable {
	
	//Timeout in milliseconds
	private static final int PONG_TIMEOUT = 2000;
	
	private Map<InetAddress, DecentSocket> peersMap;
	private Queue<InetAddress> checkQueue;
	private DecentCallback callback;
	private boolean online;
	
	public DecentPeerChecker(DecentCallback callback, Map<InetAddress, DecentSocket> peersMap) {
		this.peersMap = peersMap;
		checkQueue = new LinkedBlockingQueue<InetAddress>();
		this.callback = callback;
		online = true;
		new Thread(this).start();
	}

	@Override
	public void run() {
		while(online) {
			while(!checkQueue.isEmpty()) {
				InetAddress address = checkQueue.poll();
				if(peersMap.containsKey(address)) {
					DecentSocket s = peersMap.get(address);
					boolean isPeerOnline = s.testPing(new PingMessage(), PONG_TIMEOUT);
					if(!isPeerOnline) {
						s.stop();
					}
				}
				else {
					isOnline(address);
				}
			}
		}
	}
	/**
	 * Contacts a given InetAddress, sends it a ping message and waits for a response.
	 * If it responds, a new DecentSocket object is created
	 * If it doesn't respond in time (see the PONG_TIMEOUT value), nothing happens.
	 * 
	 * @param host The host to check
	 * @return true/false if the host is online or not
	 */
	private boolean isOnline(InetAddress host) {
		try {
			Socket socket = new Socket();
			//.connect() for connection timeout
			socket.connect(new InetSocketAddress(host, DecentConfig.PORT), PONG_TIMEOUT);
			DecentSocket s = new DecentSocket(callback, socket);
			if(s.testPing(new PingMessage(), PONG_TIMEOUT)) {
				return true;
			}
			else {
				s.stop();
			}
		} catch (Exception e) {
			//Just allow it to silently fail - sometimes peers don't accept messages
			if(e.getMessage() != null && e.getMessage().contains("Network is unreachable (connect failed)")) {
				DecentLogger.write("Network is unreachable (connect failed)");
				return true;
			}
		}
		return false;
	}
	/**
	 * Adds a list of InetAddresses to the checking queue.
	 * 
	 * @param list A list of InetAddresses to check if online or not
	 */
	public void checkList(ArrayList<InetAddress> list) {
		for(InetAddress p: list) {
			checkQueue.add(p);
		}
	}
	/**
	 * Adds a given InetAddress to the checking queue.
	 * 
	 * @param address the InetAddress to check if is online or not
	 */
	public void checkAddress(InetAddress address) {
		checkQueue.add(address);
	}
	/**
	 * Returns the length of the checking queue.
	 * 
	 * @return the length of the checking queue
	 */
	public int getQueueLength() {
		return checkQueue.size();
	}
	
	public void stop() {
		online = false;
	}

}
