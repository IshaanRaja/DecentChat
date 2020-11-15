package com.ishaanraja.decentchat.commands;

import java.net.InetAddress;
import java.util.ArrayList;

import com.ishaanraja.decentchat.client.DecentChatClient;
 
public class PeerInfoCommand extends Command {
	
	@Override
	protected String execute(DecentChatClient client, String[] arguments) {
		String peers = "Peers:\n";
		ArrayList<InetAddress> peersArr = client.getPeers();
		for(int i=0;i<peersArr.size()-1;i++) {
			peers+=peersArr.get(i).getHostAddress()+"\n";
		}
		if(peersArr.size() > 0) {
			peers+=peersArr.get(peersArr.size()-1).getHostAddress();
		}
		else {
			peers+="WARNING: No peers found, please check firewall that outbound connections to port 10862 are permitted";
		}
		return peers;
	}

	@Override
	public String getName() {
		return "peerinfo";
	}

}
