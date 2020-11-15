package com.ishaanraja.decentchat.client;

import com.ishaanraja.decentchat.config.DecentLogger;
import com.ishaanraja.decentchat.message.ChatMessage;
import com.ishaanraja.decentchat.message.HistoryAskMessage;
import com.ishaanraja.decentchat.message.HistoryMessage;

/**
 * HistoryManager handles getting history from other peers, determining which peer's history has the 
 * longest proof of work, and loading the history into the DecentChatClient;
 */

public class HistoryManager implements Runnable {
	
	private DecentChatClient client;
	private ChatMessage[] historyWithHighestProofOfWork;
	private int highestProofOfWork;
	
	/**
	 * Creates a HistoryManager thread.
	 * 
	 * @param client The DecentChatClient that will be managed by this HistoryManager
	 */
	public HistoryManager(DecentChatClient client) {
		this.client = client;
		new Thread(this).start();
	}
	/**
	 * This waits for 10 seconds (to allow the client to locate peers), then it sends out "historyAsk" messages
	 * to all peers.
	 * 
	 * Then it waits another 10 seconds to allow responses to come in.
	 * 
	 * After that, it takes the HistoryMessage with the highest proof of work and loads it into the 
	 * client.
	 */
	@Override
	public void run() {
		try {
			Thread.sleep(10100);
		} catch (InterruptedException e) {
			
		}
		client.display("\nFetching chat history...");
		askPeersForHistory();
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			
		}
		if(historyWithHighestProofOfWork != null && historyWithHighestProofOfWork.length > 0) {
			DecentLogger.write(String.format("Found history with a total proof of work of %d and %d messages.", highestProofOfWork, historyWithHighestProofOfWork.length));
			for(ChatMessage m: historyWithHighestProofOfWork) {
				client.loadIntoChatHistory(m);
			}
		}
		else {
			client.display("\nNo chat history found.");
			DecentLogger.write("No chat history found. Either there are no recent messages, or unable to connect to network (check firewall or router).");
		}
		
	}
	/**
	 * Sends out a "historyAsk" method to all peers.
	 */
	private void askPeersForHistory() {
		HistoryAskMessage m = new HistoryAskMessage();
		client.propagateToAllPeers(m);
	}
	/**
	 * Adds a HistoryMessage for consideration.
	 * 
	 * This will calculate the HistoryMessage's total proof of work, and if it is the current highest,
	 * it will save it.
	 * 
	 * @param m The HistoryMessage to add
	 */
	public void receivedHistory(HistoryMessage m) {
		int messageProofOfWork = m.getProofOfWork();
		if(messageProofOfWork > highestProofOfWork) {
			historyWithHighestProofOfWork = m.getMessageHistory();
			highestProofOfWork = messageProofOfWork;
		}
	}
	/**
	 * Returns the most recent messages from the DecentChatClient.
	 * 
	 * @param numMessages The number of historical messages to return
	 * @return An array with the most recent ChatMessages received, sorted in ascending order by time of receipt
	 */
	public ChatMessage[] getMessageHistory(int numMessages) {
		return client.getLastMessages(numMessages);
	}

}
