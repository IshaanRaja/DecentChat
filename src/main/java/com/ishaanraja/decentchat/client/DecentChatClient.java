package com.ishaanraja.decentchat.client;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.ishaanraja.decentchat.commands.CommandParser;
import com.ishaanraja.decentchat.config.DecentConfig;
import com.ishaanraja.decentchat.config.DecentLogger;
import com.ishaanraja.decentchat.config.IgnoreList;
import com.ishaanraja.decentchat.crypto.HashUtils;
import com.ishaanraja.decentchat.crypto.KeyUtils;
import com.ishaanraja.decentchat.io.DecentSocket;
import com.ishaanraja.decentchat.message.ChatMessage;
import com.ishaanraja.decentchat.message.Message;
import com.ishaanraja.decentchat.p2p.NodeManager;
import com.ishaanraja.decentchat.ui.Display;

/**
 * DecentChatClient manages loading keys, command execution, sending/displaying messages, 
 * and showing information to the user.
 */

public class DecentChatClient {
	
	private ArrayList<ChatMessage> messages;
	
	/**
	 * This map serves two purposes; it can identify duplicate messages and it can identify
	 * how many messages have been received during some given period of time.
	 */
	private Map<String, Long> signatureTimestampMap;
	
	private NodeManager nodeManager;
	
	private PublicKey pubKey;
	private PrivateKey privKey;
	private String identifier;
	
	private Display displayObj;
	private CommandParser commandParser;
	private DifficultyAdjustmentThread difficultyAdjuster;
	private HistoryManager historyManager;
	
	/**
	 * Constructs a new DecentChatClient instance. 
	 * 
	 * @param displayObj The object that will be displaying text to the user
	 */
	
	public DecentChatClient(Display displayObj) {
		this.messages = new ArrayList<ChatMessage>();
		this.signatureTimestampMap = new HashMap<String, Long>();
		this.commandParser = new CommandParser(this);
		this.displayObj = displayObj;
		getKeys();
		this.difficultyAdjuster = new DifficultyAdjustmentThread(this, signatureTimestampMap);
		this.historyManager = new HistoryManager(this);
		DecentLogger.write("Client started, network identifier is "+getIdentifier());
		this.nodeManager = new NodeManager(this::onChatMessageReceived, difficultyAdjuster, historyManager);
		display(getWelcomeMessage());
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				DecentLogger.write("Client shutting down");
				shutdown();
			}
		});
	}
	private String getWelcomeMessage() {
		String welcomeMessage = "Welcome to DecentChat!\n\n";
		welcomeMessage+="Refer to https://github.com/IshaanRaja/DecentChat for documentation.\n";
		welcomeMessage+="To send a chat message, type it in the box below and press enter.\n";
		welcomeMessage+="To access the command list, type \"/help\" and press enter.\n";
		return welcomeMessage;
	}
	/**
	 * Loads the private/public keypair from the private.pem and public.pem files, respectively.
	 * Sets the privKey/pubKey fields, respectively.
	 */
	private void getKeys() {
		File privateKeyFile = new File("private.pem");
		File publicKeyFile = new File("public.pem");
		try {
			if(!privateKeyFile.exists()) {
				KeyPair p = KeyUtils.generateKeyPair();
				DecentLogger.write("Generating new private key...");
				privKey = p.getPrivate();
				DecentLogger.write("Generating new public key...");
				pubKey = p.getPublic();
				KeyUtils.writePrivateKeyToFile(privKey, privateKeyFile);			
				KeyUtils.writePublicKeyToFile(pubKey, publicKeyFile);
			}
			else if(!publicKeyFile.exists()) {
				privKey = KeyUtils.getPrivateKeyFromFile(privateKeyFile);
				DecentLogger.write("Generating new public key...");
				pubKey = KeyUtils.getPublicKeyFromPrivateKey(privKey);
				KeyUtils.writePublicKeyToFile(pubKey, publicKeyFile);
			}
			else {
				privKey = KeyUtils.getPrivateKeyFromFile(privateKeyFile);
				pubKey = KeyUtils.getPublicKeyFromFile(publicKeyFile);
			}
		}
		catch(Exception e) {
			DecentLogger.write("Unable to read keys from files due to "+e.getMessage());
		}
	}
	/**
	 * Executes a given command.
	 * 
	 * @param commandStr The command string with the "/" still in front of it
	 */
	public void executeCommand(String commandStr) {
			display(commandParser.executeCommand(commandStr)+"\n");
	}
	/**
	 * Callback method for the receipt of a chat message. 
	 * 
	 * First, it verifies that this message is not a duplicate (and has not been received before).
	 * It then checks if the message is valid.
	 * 
	 * If it passes all the above checks, the message is forwarded to all peers. 
	 * As long as the message sender's identifier is not ignore, the message is displayed to user. 
	 * If it fails any of the above checks, the message is ignored.
	 * 
	 * See section 7 of the DecentChat whitepaper for more information.
	 * 
	 * @param m The chat message
	 * @param origin The origin of the chat message. This is not necessarily the original sender, as the message could have been forwarded. 
	 * @return a null Void object
	 */
	private synchronized Void onChatMessageReceived(ChatMessage m, DecentSocket origin) {
		if(!signatureTimestampMap.containsKey(m.getSignature()) && m.isValid()) {
			if(!IgnoreList.isIgnored(m.getIdentifier())) {
				display(m.toString());
			}
			//We still must forward ignored messages to ensure that all peers can come to 
			//a difficulty consensus
			propagateToAllPeers(m, origin);
			messages.add(m);
			signatureTimestampMap.put(m.getSignature(), m.getTimestamp());
			DecentLogger.write(m);
		}
		return null;
	}
	/**
	 * Loads a historical ChatMessage object into the UI. This method will checks if the chat message is valid, and then loads
	 * it into the GUI/UI.
	 * 
	 * This will not forward the ChatMessage.
	 * 
	 * @param m The historical ChatMessage object.
	 */
	protected synchronized void loadIntoChatHistory(ChatMessage m) {
		if(!IgnoreList.isIgnored(m.getIdentifier()) && !signatureTimestampMap.containsKey(m.getSignature()) && m.isValidHistoricalMessage()) {
			display(m.toString());
			messages.add(m);
			signatureTimestampMap.put(m.getSignature(), m.getTimestamp());
		}
	}
	/**
	 * Sends a chat message to the network and verifies that it meets min/max length requirements.
	 * 
	 * Note: This cannot process commands. To process commands, use the executeCommand() method.
	 * @see com.ishaanraja.decentchat.client.DecentChatClient#executeCommand(String)
	 * 
	 * @param message The message to send to the network
	 */
	public void sendChatMessage(String message) {
		//Trim of leading and trailing spaces so that people aren't sending a bunch of spaces for no good reason
		if(message.trim().length() <= DecentConfig.MAX_MESSAGE_LENGTH) {
			ChatMessage m = new ChatMessage(message.trim(), pubKey, privKey);
			messages.add(m);
			signatureTimestampMap.put(m.getSignature(), m.getTimestamp());
			display(m.toString());
			nodeManager.propagateToAllPeers(m);
			if(nodeManager.getPeers().size() == 0) {
				display("No peers found, queueing message for later send.");
			}
			DecentLogger.write(m);
		}
		else {
			display("Messages must be between 1 and 256 characters.");
		}
	}
	/**
	 * Displays text to the user by calling the displayCallback method.
	 * @param text Text to display to the user
	 */
	protected synchronized void display(String text) {
		displayObj.display(text);
	}
	/**
	 * Adds a peer to the client's checking queue. If the address is online, reachable, and if there is enough space
	 * then the client will add it as a peer. 
	 * 
	 * @param address The IP Address of the peer to add
	 */
	public void addPeer(String address) {
		try {
			nodeManager.checkPeer(InetAddress.getByName(address));
		} catch (UnknownHostException e) {
			DecentLogger.write("Unable to add peer "+address);
		}
	}
	/**
	 * Verifies whether a given IP address (or domain) is formatted correctly
	 * 
	 * @param address Address to check validity
	 * @return true/false if the address is valid or not
	 */
	public static boolean isValidHost(String address) {
		try {
			InetAddress.getByName(address);
			return true;
		} catch (UnknownHostException e) {
			return false;
		}
	}
	/**
	 * Returns a list of this client's current peers.
	 * @return An ArrayList of this client's peers
	 */
	public ArrayList<InetAddress> getPeers() {
		return nodeManager.getPeers();
	}
	/**
	 * Gets a certain amount of messages that were received most recently, sorted by time of receipt
	 * in ascending order. Only responds with messages that meet the historical timestamp tolerance. 
	 * 
	 * @param amount number of most recently received messages to get
	 * @return an array of ChatMessage objects of length amount or messages.size(), depending
	 * on if the requested amount is greater than the total number of messages received
	 */
	public ChatMessage[] getLastMessages(int amount) {
		int numOfHistoricalMessages = 0;
		for(int i=messages.size()-1; i>=0 && messages.get(i).isValidHistoricalMessage();i--) {
			numOfHistoricalMessages++;
		}
		ChatMessage[] lastMessages;
		if(amount > numOfHistoricalMessages) {
			lastMessages = new ChatMessage[numOfHistoricalMessages];
		}
		else {
			lastMessages = new ChatMessage[amount];
		}
		int index = lastMessages.length-1;
		for(int i=messages.size()-1; i>=0 && index >=0;i--) {
			if(messages.get(i).isValidHistoricalMessage()) {
				lastMessages[index] = messages.get(i);
				index--;
			}
		}
		return lastMessages;
	}
	/**
	 * Propagates a message to all peers EXCEPT for those specified. 
	 * This is primarily used to forward chat messages to other peers, exempting the peer it was received from. 
	 * 
	 * @param message The message to send.
	 * @param exempt The peers that will not receive this message.
	 */
	protected void propagateToAllPeers(Message m, DecentSocket... exempt) {
		nodeManager.propagateToAllPeers(m, exempt);
	}
	/** 
	 * Sends a "ping" to every current peer, and removes those who do not respond 
	 * promptly. This method is executed hourly by DifficultyDeterminationThread to clean up and
	 * remove any peers that may have gone offline since they were added to the peer list. 
	 */
	protected void prunePeers() {
		nodeManager.prunePeers();
	}
	/** 
	 * Broadcasts an askPeers message to all currently connected peers.
	 */
	protected void askForPeers() {
		nodeManager.askForPeers();
	}
	/**
	 * Returns this client's identifier, which is the first 10 characters of the SHA-256 hash of the public key. 
	 * 
	 * @return a String of the client's public key identifier. 
	 */
	public String getIdentifier() {
		if(identifier == null) {
			identifier = HashUtils.getIdentifier(Base64.getEncoder().encodeToString(pubKey.getEncoded()));
		}
		return identifier;
	}
	/**
	 * Stops the client and NodeManager.
	 */
	public void shutdown() {
		nodeManager.shutdown();
	}

}
