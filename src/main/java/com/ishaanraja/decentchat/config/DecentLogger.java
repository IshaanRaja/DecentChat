package com.ishaanraja.decentchat.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import com.ishaanraja.decentchat.message.ChatMessage;

/**
 * Handles writing to debug.log file.
 */
public final class DecentLogger {
	
	private static File logFile;
	private static File chatLogFile;
	private static FileWriter logWriter;
	private static FileWriter chatLogWriter;
	
	private DecentLogger() {}
	
	static {
		try {
			logFile = new File("debug.log");
			chatLogFile = new File("chat.log");
			if(!logFile.exists()) {
				logFile.createNewFile();
			}
			if(!chatLogFile.exists()) {
				chatLogFile.createNewFile();
			}
			logWriter = new FileWriter(logFile);
			chatLogWriter = new FileWriter(chatLogFile, true);
		}
		catch(Exception e) {
			System.out.println("Failed to instantiate logger");
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes text with a corresponding date and time to the log file.
	 * @param text Text to write to the log file
	 */
	public synchronized static void write(String text) {
		try {
			logWriter.write(new Date()+": "+text+"\n");
			logWriter.flush();
		} catch (IOException e) {
			System.out.println("Failed to write to log file");
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes a ChatMessage with a corresponding date and time to the chat.log file.
	 * @param m ChatMessage to write to the chat.log file
	 */
	public synchronized static void write(ChatMessage m) {
		try {
			chatLogWriter.write(new Date()+" - "+m.toString()+"\n");
			chatLogWriter.flush();
		} catch (IOException e) {
			System.out.println("Failed to write to log file");
			e.printStackTrace();
		}
	}


}
