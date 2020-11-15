package com.ishaanraja.decentchat.commands;

import java.util.ArrayList;

import com.ishaanraja.decentchat.client.DecentChatClient;
import com.ishaanraja.decentchat.config.IgnoreList;

public class IgnoreListCommand extends Command {

	@Override
	protected String execute(DecentChatClient client, String[] arguments) {
		ArrayList<String> ignoredIdentifiers = IgnoreList.getIgnored();
		String response = "";
		if(ignoredIdentifiers.size() > 0) {
			response = "Ignored Identifiers:\n";
			for(int i=0;i<ignoredIdentifiers.size()-1;i++) {
				response+=ignoredIdentifiers.get(i)+"\n";
			}
			//Get last outside of loop to prevent extra newline at end of string
			response+=ignoredIdentifiers.get(ignoredIdentifiers.size()-1);
		}
		else {
			response = "No identifiers currently ignored";
		}
		return response;
	}

	@Override
	public String getName() {
		return "ignorelist";
	}

}
