package com.ishaanraja.decentchat.commands;

import com.ishaanraja.decentchat.client.DecentChatClient;

public class HelpCommand extends Command {

	@Override
	protected String execute(DecentChatClient client, String[] arguments) {
		return  "/help - Provides a list of available commands\n" + 
				"/addpeer [address] - Allows a user to manually add a peer by specifying its IP Address\n" + 
				"/peerinfo - Shows a list of connected peers\n" + 
				"/difficultyinfo - Shows the client’s current difficulty\n" +
				"/ignore [identifier] - Ignores a specified identifier\n" + 
				"/ignorelist - Shows the list of currently ignored identifiers\n" + 
				"/info - Shows information about the current client\n" +
				"/unignore [identifier] - Removes a specified identifier from the ignore list\n" + 
				"/changeusername [new_username] - Changes the client’s human readable username for all following\n messages.";
	}

	@Override
	public String getName() {
		return "help";
	}

}
