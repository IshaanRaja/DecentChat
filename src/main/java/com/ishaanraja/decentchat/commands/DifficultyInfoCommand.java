package com.ishaanraja.decentchat.commands;

import com.ishaanraja.decentchat.client.DecentChatClient;
import com.ishaanraja.decentchat.config.DecentConfig;

public class DifficultyInfoCommand extends Command {

	@Override
	protected String execute(DecentChatClient client, String[] arguments) {
		return "Current Difficulty is: "+DecentConfig.getDifficulty();
	}

	@Override
	public String getName() {
		return "difficultyinfo";
	}

}
