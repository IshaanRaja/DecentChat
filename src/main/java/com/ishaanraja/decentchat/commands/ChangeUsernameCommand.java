package com.ishaanraja.decentchat.commands;

import com.ishaanraja.decentchat.client.DecentChatClient;
import com.ishaanraja.decentchat.config.DecentConfig;

public class ChangeUsernameCommand extends Command {

	@Override
	protected String execute(DecentChatClient client, String[] arguments) {
		if(arguments != null && arguments.length == 1 && arguments[0] != null) {
			String newUsername = arguments[0].trim();
			if(newUsername.length() >= 1&& newUsername.length() <= 16) {
				DecentConfig.setUsername(newUsername);
				return "Username has been set to \""+newUsername+"\"";
			}
		}
		return "Invalid username. Usernames must be between 1 and 16 characters.";
	}

	@Override
	public String getName() {
		return "changeusername";
	}

}
