package com.ishaanraja.decentchat.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

import com.dosse.upnp.UPnP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

/**
 * Manages DecentChat constants and values in the config.json file.
 *
 */
public final class DecentConfig {
	
	//Hardcoded constants
	public static final short PORT = 10862;
	public static final int MAX_DIFFICULTY = 2048;
	public static final int MIN_DIFFICULTY = 9;
	public static final String VERSION = "1.0";
	//Maximum length of a message, in characters
	public static final int MAX_MESSAGE_LENGTH = 256;
	//Maximum length of a username, in characters
	public static final int MAX_USERNAME_LENGTH = 16;
	//Number of historical messages that we want from other peers
	public static final int MESSAGE_HISTORY_LENGTH = 32;
	//The length of public key identifiers
	public static final int IDENTIFIER_LENGTH = 10;
	
	/**
	 * Timestamp Tolerance is how far off a timestamp can be from the current time and 
	 * still be recognized and (for chat messages) propagated across the network.
	 * Value is in seconds, currently set at 2 minutes.
	 */
	public static final long TIMESTAMP_TOLERANCE = 60 * 2;
	/**
	 * Historical message timestamp tolerance in seconds (currently set to one hour).
	 * This is to ensure that the client is receiving fresh historical messages.
	 */
	public static final long HISTORICAL_TIMESTAMP_TOLERANCE = 60*60;

	// Defaults
	
	private static File configFile;
	private static int maximumConnections = 256;
	//Setting difficulty at 10 because that is the most likely difficulty if the network has received between
	//1-1024 messages in the past hour (see section 6 of the whitepaper)
	private static int difficulty = 10;
	//On first start, client has not been on the network long enough to share a difficulty value
	private static int peeringDifficulty = 0;
	private static String username = "Newbie";
	private static boolean upnpEnabled = true;
	private static boolean headlessMode = false;
	
	private DecentConfig() {}
	
	static {
		// Load them in from config file
		configFile = new File("config.json");
		if (!configFile.exists()) {
			try {
				configFile.createNewFile();
				writeToFile();
			} catch (IOException e) {
				DecentLogger.write("Unable to create config.json");
			}
		}
		else {
			try {
				Reader reader = new FileReader(configFile);
				JsonObject jsonObj = JsonParser.parseReader(new JsonReader(reader)).getAsJsonObject();
				maximumConnections = jsonObj.get("maximumConnections").getAsInt();
				username = jsonObj.get("username").getAsString();
				headlessMode = jsonObj.get("headlessMode").getAsBoolean();
				if(username.length() < 1 || username.length() > 16) {
					DecentLogger.write("Could not set username to "+username+" because it is not between 1 and 16 characters");
					username = "Newbie";
					
				}
				setUPNPEnabled(jsonObj.get("upnpEnabled").getAsBoolean());
			} catch (Exception e) {
				DecentLogger.write("Unable to read config.json");
			}
		}
		
	}
	/**
	 * Difficulty is a measure of how difficult the proof of work required is. 
	 * It is a value that specifies the required number of leading zero bits in the message signature to be accepted by the network. 
	 * Since the message signature is always 256 bytes with the current RSA implementation, 
	 * the theoretical maximum difficulty is 2048 and due to the current difficulty determination function, the minimum difficulty is 9. 
	 * 
	 * @return An int representing the difficulty
	 */
	public static int getDifficulty() {
		return difficulty;
	}
	/**
	 * Checks if the new difficulty is within minimum and maximum difficulty values, and if it is
	 * sets the client's difficulty.
	 * 
	 * This method also updates the peeringDifficulty to be equal to the new difficulty as well.
	 * 
	 * @param difficulty The new difficulty to set
	 */
	public static void setDifficulty(int difficulty) {
		if(difficulty >= MIN_DIFFICULTY && difficulty <= MAX_DIFFICULTY) {
			DecentConfig.difficulty = difficulty;
			DecentConfig.peeringDifficulty = difficulty;
			DecentLogger.write("Difficulty set to: "+difficulty);
		}
		//Client should NEVER be in such a situation, but just in case difficulty is set at max in case of parameter > max
		else if(difficulty > MAX_DIFFICULTY) {
			DecentConfig.difficulty = DecentConfig.MAX_DIFFICULTY;
			DecentConfig.peeringDifficulty = DecentConfig.MAX_DIFFICULTY;
			DecentLogger.write("Difficulty set to: "+DecentConfig.MAX_DIFFICULTY+" which is nearly impossible to calculate.");
		}
	}
	/**
	 * Peering difficulty is very similar to the regular difficulty value, except it is the one
	 * that the client shares with other peers, but is not enforcing yet. This is determined after
	 * difficulty determination time, but before difficulty set time, and allows other clients to ask for
	 * and set their difficulties relative to others (if they have not been online for an hour).
	 * 
	 * Most of the time both peering and regular difficulty are the same. But between difficulty
	 * determination time and difficulty set time they may differ. 
	 * 
	 * Additionally, if the client has not been online for at least an hour continuously, the peering difficulty
	 * is set to 1 to indicate to other clients that it has not been on the network long enough to produce
	 * a proper difficulty calculation.
	 * 
	 * @return The peering difficulty
	 */
	public static int getPeeringDifficulty() {
		return peeringDifficulty;
	}
	/**
	 * Checks if the new peering difficulty is between minimum and maximum values, and if it is 
	 * sets the new peering difficulty.
	 * 
	 * @param peeringDifficulty The new peering difficulty
	 */
	public static void setPeeringDifficulty(int peeringDifficulty) {
		if(difficulty >= MIN_DIFFICULTY && difficulty <= MAX_DIFFICULTY) {
			DecentConfig.peeringDifficulty = peeringDifficulty;
		}
	}
	/**
	 * By default, the client will impose a maximum of 256 peers. 
	 * This value can be changed in the config and determines the maximum size of the peer list.
	 * 
	 * @return The maximum number of peers this client can have
	 */
	public static int getMaximumConnections() {
		return maximumConnections;
	}
	/**
	 * Returns whether UPNP is enabled in the config. 
	 * 
	 * Note: This does not return whether UPNP is available on the network. All it does is return
	 * whether upnpEnabled is set to true in the config or not.
	 * 
	 * @return true/false whether upnp is enabled
	 */
	public static boolean getUPNPEnabled() {
		return upnpEnabled;
	}
	/**
	 * This client has two possible UIs: the GUI and the headless command line UI.
	 * 
	 * By default, this client will render a Swing GUI, but the config can be changed
	 * to "headlessMode", which will put this client into using the command line UI.
	 * 
	 * @see com.ishaanraja.decentchat.ui.TerminalUI
	 * 
	 * @return true/false for command line UI or not
	 */
	public static boolean getHeadlessMode() {
		return headlessMode;
	}
	/**
	 * Sets whether the client should use UPNP. 
	 * 
	 * Note: The client will only enable/disable UPNP if it is available for use on the network.
	 * Otherwise, this method will do nothing.
	 * 
	 * @param enabled true/false whether UPNP should be enabled.
	 */
	public static void setUPNPEnabled(boolean enabled) {
		upnpEnabled = enabled;
		boolean isAvailable = UPnP.isUPnPAvailable() && !UPnP.isMappedTCP(PORT);
		if(UPnP.isMappedTCP(PORT)) {
			DecentLogger.write("UPNP cannot be enabled because port 10862 is already mapped");
		}
		else if(upnpEnabled && !isAvailable) {
			DecentLogger.write("UPNP cannot be enabled because UPNP is not available on this network");
		}
		else if(upnpEnabled && isAvailable) {
			UPnP.openPortTCP(PORT);
		}
		else if(isAvailable) {
			UPnP.closePortTCP(PORT);
		}
		writeToFile();
	}
	/**
	 * Gets the client's current human readable username.
	 * 
	 * Usernames can be between 1 and 16 characters.
	 * 
	 * Default username is "Newbie".
	 * 
	 * @return The client's current username
	 */
	public static String getUsername() {
		return username;
	}
	/**
	 * Sets the human readable username. If the username is not within 1-16 characters, this method
	 * will do nothing.
	 * 
	 * @param username Human readable username between 1 and 16 characters.
	 */
	public static void setUsername(String username) {
		if(username.length() >= 1 && username.length() <= MAX_USERNAME_LENGTH) {
			DecentConfig.username = username;
			writeToFile();
		}
	}
	/**
	 * Writes all values to the config.json. Normally called after a config change has been made.
	 */
	private static void writeToFile() {
		try(FileWriter filewriter = new FileWriter(configFile)) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonObject jsonObj = new JsonObject();
			jsonObj.addProperty("maximumConnections", DecentConfig.maximumConnections);
			jsonObj.addProperty("username", username);
			jsonObj.addProperty("upnpEnabled", upnpEnabled);
			jsonObj.addProperty("headlessMode", headlessMode);
			filewriter.write(gson.toJson(jsonObj));
		} catch (IOException e) {
			DecentLogger.write("Unable to write config to file");
		}
	}

}
