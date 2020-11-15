package com.ishaanraja.decentchat.ui;

/**
 * All objects that display information from DecentChatClient must implement this
 * interface.
 */
public interface Display {
	
	/**
	 * Displays a String of text to the user.
	 * 
	 * @param text The text to display.
	 */
	public void display(String text);

}
