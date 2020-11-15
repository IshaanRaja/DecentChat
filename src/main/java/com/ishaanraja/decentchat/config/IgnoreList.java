package com.ishaanraja.decentchat.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Manages the list of ignored identifiers.
 */
public final class IgnoreList {
	
	private static File ignoredIdentifiers;
	private static ArrayList<String> ignoreList;
	
	private IgnoreList() {}

	static {
		ignoredIdentifiers = new File("ignorelist.txt");
		ignoreList = new ArrayList<String>();
		try {
			if(ignoredIdentifiers.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(ignoredIdentifiers));
				String line = reader.readLine();
				while(line != null) {
					if(line.trim().length() == DecentConfig.IDENTIFIER_LENGTH) {
						ignoreList.add(line.trim());
					}
					line = reader.readLine();
				}
				reader.close();
			}
			else {
				ignoredIdentifiers.createNewFile();
			}
		}
		catch(Exception e) {
			DecentLogger.write("Could not load ignorelist.txt");
		}
	}
	
	/**
	 * Adds a 10 character identifier to the ignore list. 
	 * @param identifier The 10 character identifier to start ignoring
	 */
	public static void addIgnored(String identifier) {
		ignoreList.add(identifier);
		writeToFile();
	}
	/**
	 * Removes a 10 character identifier to the ignore list. If the identifier does not exist, this does nothing.
	 * @param identifier The 10 character identifier to stop ignoring
	 */
	public static void removeIgnored(String identifier) {
		ignoreList.remove(identifier);
		writeToFile();
	}
	
	private static synchronized void writeToFile() {
		try(FileWriter filewriter = new FileWriter(ignoredIdentifiers)) {
			for(String i: ignoreList) {
				filewriter.write(i+"\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	/**
	 * Checks if a given 10 character identifier is present on the ignore list. 
	 * 
	 * Note: An identifier is derived from the first 10 characters of the SHA-256 hash of the 
	 * sender's public key.An identifier can be obtained from a ChatMessage by calling getIdentifier().
	 * 
	 * @param identifier The 10 character identifier to check
	 * @return true/false whether the given identifier is on the ignore list
	 */
	public static boolean isIgnored(String identifier) {
		return ignoreList.contains(identifier);
	}
	/**
	 * Returns the current list of ignored identifiers.
	 * @return An ArrayList of ignored identifiers
	 */
	public static ArrayList<String> getIgnored() {
		return ignoreList;
	}
	

}
