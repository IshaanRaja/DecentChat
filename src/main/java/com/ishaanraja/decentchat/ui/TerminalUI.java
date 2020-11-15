package com.ishaanraja.decentchat.ui;

import java.util.Scanner;

import com.ishaanraja.decentchat.client.DecentChatClient;

public class TerminalUI implements Display {
	
	private DecentChatClient client;
	
	public TerminalUI() {
		client = new DecentChatClient(this);
		startMainThread();
	}
	
	public void display(String text) {
		System.out.println(text);
	}
	
	private void startMainThread() {
		Scanner scanner = new Scanner(System.in);
		boolean online = true;
		while(online) {
			String text = scanner.nextLine();
			if(text.startsWith("/")) {
				client.executeCommand(text);
			}
			else if(text.length() > 0) {
				client.sendChatMessage(text);
			}
		}
		scanner.close();
	}

}
