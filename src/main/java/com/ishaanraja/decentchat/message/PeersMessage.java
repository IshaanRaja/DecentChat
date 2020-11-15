package com.ishaanraja.decentchat.message;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ishaanraja.decentchat.config.DecentConfig;

public class PeersMessage extends Message {
	
	private List<String> peers;
	
	public PeersMessage(List<InetAddress> peers) {
		super("peers");
		this.peers = new ArrayList<String>();
		for(InetAddress p: peers) {
			this.peers.add(p.getHostAddress());
		}
	}

	@Override
	public boolean isValid() {
		try {
			long now = Instant.now().getEpochSecond();
			long timestampTolerance = DecentConfig.TIMESTAMP_TOLERANCE;
			boolean valid = (type.equals("peers"));
			valid &= timestamp >= now-timestampTolerance && timestamp <= now+timestampTolerance;
			return valid;
		}
		catch(Exception e) {
			return false;
		}
	}
	/**
	 * Returns the peers stored in this PeersMessage.
	 * 
	 * @return A list of peers in this PeersMessage
	 */
	public ArrayList<InetAddress> getPeers() {
		ArrayList<InetAddress> peersList = new ArrayList<InetAddress>();
		for(String ip:peers) {
			try {
				peersList.add(InetAddress.getByName(ip));
			} catch (UnknownHostException e) {
				//Do nothing, just means IP address is invalid
			}
		}
		return peersList;
	}

}
