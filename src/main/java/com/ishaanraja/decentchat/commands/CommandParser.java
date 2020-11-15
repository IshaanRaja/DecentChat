package com.ishaanraja.decentchat.commands;

import com.ishaanraja.decentchat.client.DecentChatClient;

public class CommandParser {
	
	private DecentChatClient client;
	
	public CommandParser(DecentChatClient client) {
		this.client = client;
	}
	/**
	 * Executes a given command string. 
	 * 
	 * @param commandStr The command to be executed with the "/" included. This does not necessarily need to be a valid command. It 
	 * only needs to be desired command to be executed. If the command does not exist, CommandParser will reply as such.
	 * 
	 * @return Text to display to the user
	 */
	public String executeCommand(String commandStr) {
		//We split at index 1 because index 0 is the substring. 
		if(commandStr.startsWith("/")) {
			String command = commandStr.substring(1).split(" ")[0].trim();
			int indexOfArgStart = commandStr.trim().indexOf(" ");
			String[] arguments = null;
			if(indexOfArgStart > 0) {
				arguments = commandStr.substring(commandStr.indexOf(" ")+1).split(" ");
			}
			Command commandObj = getCommandObject(command);
			if(commandObj != null) {
				return commandObj.execute(client, arguments);
			}
		}
		return "Invalid command. Use /help for a list of commands";
	}
	/**
	 * Translates a command into its corresponding Command object that can be executed.
	 * 
	 * @param command String of the command String (without the "/" at the beginning)
	 * @return Command object that can be executed
	 */
	private Command getCommandObject(String commandStr) {
		String command = commandStr.toLowerCase();
		switch(command) {
			case "help":
				return new HelpCommand();
			case "peerinfo":
				return new PeerInfoCommand();
			case "difficultyinfo":
				return new DifficultyInfoCommand();
			case "addpeer":
				return new AddPeerCommand();
			case "ignore":
				return new IgnoreCommand();
			case "ignorelist":
				return new IgnoreListCommand();
			case "info":
				return new InfoCommand();
			case "unignore":
				return new UnignoreCommand();
			case "changeusername":
				return new ChangeUsernameCommand();
			default:
				return null;
		}
	}

}
