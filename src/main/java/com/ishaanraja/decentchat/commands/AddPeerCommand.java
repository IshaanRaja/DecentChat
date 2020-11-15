package com.ishaanraja.decentchat.commands;

import com.ishaanraja.decentchat.client.DecentChatClient;
import com.ishaanraja.decentchat.config.DecentConfig;

public class AddPeerCommand extends Command {

	@Override
	protected String execute(DecentChatClient client, String[] arguments) {
		if(arguments != null && arguments.length == 1 && arguments[0] != null) {
			String newPeer = arguments[0].trim();
			if(client.getPeers().size() >= DecentConfig.getMaximumConnections()) {
				return "Peers list has "+DecentConfig.getMaximumConnections()+" connections and is full.";
			}
			else if(DecentChatClient.isValidHost(newPeer)) {
				client.addPeer(newPeer);
				return "Successfully added "+newPeer+" as a peer";
			}
		}
		return "Invalid peer";
	}

	@Override
	public String getName() {
		return "addpeer";
	}

}
