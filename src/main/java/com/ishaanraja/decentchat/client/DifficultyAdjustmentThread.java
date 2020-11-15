package com.ishaanraja.decentchat.client;

import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.ishaanraja.decentchat.config.DecentConfig;
import com.ishaanraja.decentchat.message.PongMessage;

/**
 * DifficultyAdjustmentThread manages the client's difficulty and adjusts it in accordance to the network.
 * 
 * The inherent goal of the difficulty is for it to be modified to provide a steady flow of 1024 messages per hour (or ~17 messages per minute). 
 * The client’s default difficulty is set at 9, which was found to be calculable by the average computer in about 5 seconds. 
 *
 * This thread manages setting the client's difficulty and client peer pruning.
 * 
 * See section 6 of the DecentChat whitepaper for more information.
 */

public class DifficultyAdjustmentThread implements Runnable {
	
	private DecentChatClient client;
	private Map<String, Long> signatureTimestampMap;
	private long startTime;
	private boolean online;
	private int difficulty;
	private int[] difficultyFrequencies;
	private ArrayList<InetAddress> difficultyPeers;
	
	/**
	 * Creates a new DifficultyAdjustmentThread object
	 *  
	 * @param client The DecentChatClient this thread will be managing
	 * @param signatureTimestampMap The DecentChatClient's Map of ChatMessage signatures to to their timestamps
	 */
	
	public DifficultyAdjustmentThread(DecentChatClient client, Map<String, Long> signatureTimestampMap) {
		//The parameters are references, so when DecentChatClient updates them, we can see the updates from this thread too
		this.client = client;
		this.signatureTimestampMap = signatureTimestampMap;
		startTime = Instant.now().getEpochSecond();
		online = true;	
		difficultyFrequencies = new int[DecentConfig.MAX_DIFFICULTY+1];
		difficultyPeers = new ArrayList<InetAddress>();
		new Thread(this).start();
	}
	
	/**
	 * Checks if it is currently difficulty determination time, pruning time, or difficulty set time, then executes methods in accordance
	 * with those times and then sleeps for 60 seconds. 
	 */
	@Override
	public void run() {
		setInitialDifficulty();
		//Set the peering difficulty to 1 since client is still new to network
		DecentConfig.setPeeringDifficulty(1);
		while(online) {
			if(isDifficultyDeterminationTime()) {
				if(hasBeenOnlineForOneHour()) {
					difficulty = calculateDifficulty();
					DecentConfig.setPeeringDifficulty(difficulty);
				}
				else {
					//Client has not been online for long enough, so we share a peering difficulty of 1
					DecentConfig.setPeeringDifficulty(1);
				}
			}
			else if(isPruningTime()) {
				//Theoretical Minimum difficulty is 0, theoretical maximum difficulty is 2048
				difficultyFrequencies = new int[DecentConfig.MAX_DIFFICULTY+1]; 
				difficultyPeers = new ArrayList<InetAddress>();
				//Normal pruning
				client.askForPeers();
				client.prunePeers();
			}
			else if(isDifficultySetTime()) {
				if(hasBeenOnlineForOneHour()) {
					DecentConfig.setDifficulty(difficulty);
				}
				else if(getUptime() > 60) {
					//Set difficulty based on other peers if we have been connected for more than 60 seconds
					//The 60 seconds requirement is to prevent anyone who joins during difficulty set time from experiencing any problems
					DecentConfig.setDifficulty(findDifficultyWithHighestFrequency());
				}
			}
			//Sleep until second 0 of the next minute to prevent doing any of these twice
			//This is to prevent having peers set their difficulties at different times during the minute
			int currentSecond = LocalTime.now().getSecond();
			try {
				Thread.sleep((60-currentSecond)*1000);
			} catch (InterruptedException e) {
				
			}
		}
	}
	/**
	 * Stops this thread's execution
	 */
	public void stop() {
		online = false;
	}
	/**
	 * Waits for a total of 10 seconds and then sets the client's initial difficulty based on 
	 * the difficulty values of other peers on the network.
	 */
	public void setInitialDifficulty() {
		try {
			Thread.sleep(1000);
			client.display("Please wait, determining network difficulty...");
			//Give the client time to find multiple peers
			Thread.sleep(9000);
		} catch (InterruptedException e1) {
			
		}
		int networkDifficulty = findDifficultyWithHighestFrequency();
		DecentConfig.setDifficulty(networkDifficulty);
		client.display("Network difficulty determined to be: "+networkDifficulty);
	}
	
	/**
	 * Adds a new received pong message to the thread's difficulty determination calculation.
	 * 
	 * At minute 1 of every hour, the client prunes its peers. It sends "ping" messages to all of them
	 * to verify that they are online and removes the ones that do not respond in time. 
	 * 
	 * Every time this client receives a "pong" response (which contain the sender's difficulty), it is fed
	 * into this method. 
	 * 
	 * This method also checks if a pong message has already been received the address it was received from.
	 * This is to prevent one peer from trying to game the difficulty calculation by sending out multiple pong messages.
	 * 
	 * Note: Difficulty is only calculate based on other peers when the client has not been online for an hour.
	 * 
	 * @param m The received pong message
	 * @param origin The InetAddress the pong message was received from
	 */
	public void receivedPong(PongMessage m, InetAddress origin) {
		if(!difficultyPeers.contains(origin)) {
			difficultyPeers.add(origin);
			addDifficulty(m.getDifficulty());
		}
	}
	/**
	 * Adds a difficulty value to the difficulty determination calculation.
	 * 
	 * @param difficulty The difficulty value to add to the calculation
	 */
	private void addDifficulty(int difficulty) {
		if(difficulty >= DecentConfig.MIN_DIFFICULTY && difficulty <= DecentConfig.MAX_DIFFICULTY) {
			difficultyFrequencies[difficulty]++;
		}
	}
	/**
	 * At minute 5 of every hour ("difficulty set time") or one first connect to the network, using the "pong" responses it has received,
	 * this method is called calculate the difficulty value with the highest frequency on the network. 
	 * 
	 * Note: Difficulty is only determined in relation to other clients on first connect or if the client has not been connected for an hour
	 * at difficulty determination time. Otherwise, difficulty is determined by the calculateDifficulty() method.
	 * 
	 * @return the most prevalent difficulty value of this client's peers
	 */
	private int findDifficultyWithHighestFrequency() {
		int highestFrequencyIndex = DecentConfig.MIN_DIFFICULTY;
		int highestFrequency = 0;
		for(int i=0;i<difficultyFrequencies.length;i++) {
			if(difficultyFrequencies[i] > highestFrequency) {
				highestFrequencyIndex = i;
				highestFrequency = difficultyFrequencies[i];
			}
		}
		return highestFrequencyIndex;
	}
	/**
	 * Determines if this particular client has been connected to the network for an hour uninterrupted.
	 * 
	 * @return true/false if the client has been connected to the network for an hour uninterrupted
	 */
	private boolean hasBeenOnlineForOneHour() {
		long currentUptime = getUptime();
		//There are 3600 seconds in an hour
		return currentUptime >= 3600;
	}
	/**
	 * Returns this thread's uptime in seconds.
	 * @return Thread uptime in seconds
	 */
	private long getUptime() {
		return Instant.now().getEpochSecond()-startTime;
	}
	/**
	 * Difficulty Determination Time is at minute 0 of every hour.
	 * 
	 * @return true/false if it is currently difficulty determination time
	 */
	private static boolean isDifficultyDeterminationTime() {
		int currentMinute = LocalTime.now().getMinute();
		//Determination time happens at minute 0 of each hour
		return currentMinute == 0;
	}
	/**
	 * Pruning Time is at minute 1 of every hour.
	 * 
	 * @return true/false if it is currently pruning time
	 */
	private static boolean isPruningTime() {
		int currentMinute = LocalTime.now().getMinute();
		//Pruning time happens at minute 1 of each hour
		return currentMinute == 1;
	}
	/**
	 * Difficulty Set Time is at minute 5 of every hour.
	 * 
	 * @return true/false if it is currently difficulty set time
	 */
	private static boolean isDifficultySetTime() {
		int currentMinute = LocalTime.now().getMinute();
		//Difficulty SET time happens at minute 5 of each hour AFTER determination time
		return currentMinute == 5;
	}
	/**
	 * Calculates difficulty based on the number of messages received in the past hour.
	 * 
	 * The formula is: f(d,n) = ((d-9)/1024)*n + 9 where d is the current network difficulty and
	 * n is the number of valid messages received in the past hour.
	 * 
	 * This method is only used if the client has been connected to the network for the past hour, uninterrupted.
	 * 
	 * See section 6 of the DecentChat whitepaper for more information
	 * 
	 * @return the new calculated difficulty value
	 */
	private int calculateDifficulty() {
		//Getting all messages from the past hour
		long cutoffTime = Instant.now().getEpochSecond()-3600;
		int n = signatureTimestampMap.entrySet().stream()
	    .filter(entry -> entry.getValue() >= cutoffTime)
	    .map(Entry::getKey)
	    .collect(Collectors.toList()).size();
		int d = DecentConfig.getDifficulty();
		//Special case for d == minimum diff.
		if(d == DecentConfig.MIN_DIFFICULTY && n > 0) {
			return DecentConfig.MIN_DIFFICULTY+1;
		}
		else {
			double difficultyDouble = ((((double)d-9)/1024)*n)+9;
			return (int) Math.ceil(difficultyDouble);
		}
		
	}
	

}
