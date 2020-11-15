package com.ishaanraja.decentchat.main;

import java.awt.EventQueue;

import com.ishaanraja.decentchat.config.DecentConfig;
import com.ishaanraja.decentchat.ui.DecentChatGUI;
import com.ishaanraja.decentchat.ui.TerminalUI;

public class Main {

	public static void main(String[] args) {
		if(DecentConfig.getHeadlessMode()) {
			startTerminalUI();
		}
		else {
			startGUI();
		}
	}
	
	public static void startGUI() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					new DecentChatGUI();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public static void startTerminalUI() {
		new TerminalUI();
	}

}
