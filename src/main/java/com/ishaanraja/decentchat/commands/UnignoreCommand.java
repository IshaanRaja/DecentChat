package com.ishaanraja.decentchat.commands;

import com.ishaanraja.decentchat.client.DecentChatClient;
import com.ishaanraja.decentchat.config.DecentConfig;
import com.ishaanraja.decentchat.config.IgnoreList;

public class UnignoreCommand extends Command {

	@Override
	protected String execute(DecentChatClient client, String[] arguments) {
		if(arguments != null && arguments.length == 1 && arguments[0] != null) {
			String identifier = arguments[0].trim();
			if(identifier.length() == DecentConfig.IDENTIFIER_LENGTH) {
				if(IgnoreList.isIgnored(identifier)) {
					IgnoreList.removeIgnored(identifier);
					return "Successfully removed "+identifier+" from the ignore list";
				}
				else {
					return "Identifier "+identifier+" is not currently ignored";
				}
				
			}
		}
		return "Invalid identifier, please specify the "+DecentConfig.IDENTIFIER_LENGTH+" character identifier to stop ignoring";
	}

	@Override
	public String getName() {
		return "unignore";
	}

}
