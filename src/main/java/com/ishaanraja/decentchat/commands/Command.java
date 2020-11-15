package com.ishaanraja.decentchat.commands;

import com.ishaanraja.decentchat.client.DecentChatClient;

public abstract class Command {
	
	/**
	 * Executes the command with the specified arguments.
	 * 
	 * @param client The DecentChatClient that this command will reference or modify 
	 * @param arguments The command's specified arguments or null if no arguments given
	 * @return The text to display to the user
	 */
	
	protected abstract String execute(DecentChatClient client, String[] arguments);
	
	/**
	 * Returns the Command object's name. No use currently, but may be used in the future.
	 * @return A string of the command's name
	 */
	
	public abstract String getName();
}
