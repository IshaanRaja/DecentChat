package com.ishaanraja.decentchat.commands;

import com.ishaanraja.decentchat.client.DecentChatClient;
import com.ishaanraja.decentchat.config.DecentConfig;

public class InfoCommand extends Command {

	@Override
	protected String execute(DecentChatClient client, String[] arguments) {
		String response = "Client Information:\n";
		response+="Identifier: "+client.getIdentifier()+"\n";
		response+="Version: "+DecentConfig.VERSION;
		return response;
	}

	@Override
	public String getName() {
		return "info";
	}

}
