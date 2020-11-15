package com.ishaanraja.decentchat.io;

import java.net.InetAddress;

import com.ishaanraja.decentchat.message.Message;

/**
 * Abstract callback class used by DecentSocket.
 */

public abstract class DecentCallback {
	
	/**
	 * The callback method for when a message is received from a DecentSocket. 
	 * 
	 * @param message The message that was received
	 * @param origin The DecentSocket it came from
	 * @return A response Message or null if sending no response
	 */
	public abstract Message onSocketMessageReceived(String message, DecentSocket origin);
	/**
	 * Removes an InetAddress from the client's peers.
	 * 
	 * @param address
	 */
	public abstract void removeSocketPeer(InetAddress address);
	
	/**
	 * Adds a DecentSocket to client's peers.
	 * 
	 * @param socket The DecentSocket to add
	 */
	
	public abstract void addSocketPeer(DecentSocket socket);
	
	/**
	 * Returns whether the peer is valid and can be added to the client's peers. 
	 * 
	 * @param address The InetAddress to check for
	 * @return true/false whether peer can be added
	 */
	
	public abstract boolean canAddSocketPeer(InetAddress address);

}
