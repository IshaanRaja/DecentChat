package com.ishaanraja.decentchat.p2p;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ishaanraja.decentchat.client.DifficultyAdjustmentThread;
import com.ishaanraja.decentchat.client.HistoryManager;
import com.ishaanraja.decentchat.config.DecentConfig;
import com.ishaanraja.decentchat.config.DecentLogger;
import com.ishaanraja.decentchat.io.DecentCallback;
import com.ishaanraja.decentchat.io.DecentListener;
import com.ishaanraja.decentchat.io.DecentPeerChecker;
import com.ishaanraja.decentchat.io.DecentSocket;
import com.ishaanraja.decentchat.message.ChatMessage;
import com.ishaanraja.decentchat.message.HistoryAskMessage;
import com.ishaanraja.decentchat.message.HistoryMessage;
import com.ishaanraja.decentchat.message.Message;
import com.ishaanraja.decentchat.message.PeerAskMessage;
import com.ishaanraja.decentchat.message.PeersMessage;
import com.ishaanraja.decentchat.message.PingMessage;
import com.ishaanraja.decentchat.message.PongMessage;

/** 
 * The NodeManager class manages all incoming/outgoing messages from the client
 * and finds and communicates with other peers.
 */

public class NodeManager {
	
	private static final String[] DNS_SEEDS = {"decentseeds.ishaanraja.com"};
	private static final String EXTERNAL_IP_PROVIDER = "http://checkip.amazonaws.com";
	
	private class NodeManagerCallback extends DecentCallback {

		@Override
		public Message onSocketMessageReceived(String message, DecentSocket origin) {
			return onMessageReceived(message, origin);
		}

		@Override
		public synchronized void removeSocketPeer(InetAddress address) {
			removePeer(address);
		}

		@Override
		public synchronized void addSocketPeer(DecentSocket socket) {
			addPeer(socket);
		}

		@Override
		public synchronized boolean canAddSocketPeer(InetAddress address) {
			return canAddPeer(address);
		}
		
	}
	
	private File peersFile;
	
	private Map<InetAddress, DecentSocket> peers;
	
	private DecentListener listener;
	private DecentPeerChecker checker;
	private DecentCallback callback;
	
	private BiFunction<ChatMessage, DecentSocket, Void> chatMessageCallback;
	
	private int maximumConnections;
	
	private InetAddress externalIP;
	private InetAddress internalIP;
	private DifficultyAdjustmentThread difficultyAdjuster;
	private HistoryManager historyManager;
	/**
	 * This is ONLY used to store messages sent while the client has no peers. Once the client gets peers,
	 * the messages contained in this queue are sent out.
	 */
	private Queue<Message> noPeersMessageQueue;
	
	/**
	 * Creates a new NodeManager instance. 
	 * 
	 * @param chatMessageCallback The Function to call when a chat message is received
	 * @param chatClient The DecentChatClient instance that this NodeManager will be managing.
	 * @param difficultyAdjuster The DecentChatClient's DifficultyAdjustmentThread
	 */
	
	public NodeManager(BiFunction<ChatMessage, DecentSocket, Void> chatMessageCallback, DifficultyAdjustmentThread difficultyAdjuster, HistoryManager historyManager) {
		this.peers = new HashMap<InetAddress, DecentSocket>();
		this.peersFile = new File("peers.txt");
		this.callback = new NodeManagerCallback();
		this.chatMessageCallback = chatMessageCallback;
		this.maximumConnections = DecentConfig.getMaximumConnections();
		this.checker = new DecentPeerChecker(callback, peers);
		this.listener = new DecentListener(callback, peers);
		this.difficultyAdjuster = difficultyAdjuster;
		this.historyManager = historyManager;
		noPeersMessageQueue = new LinkedBlockingQueue<Message>();
		findInternalExternalIP();
		findPeers();
	}
	
	private void findPeers() {
		readPeers();
		//If we've found no peers at all, last resort find peer
		if(checker.getQueueLength() == 0 && peers.size() == 0) {
			lastResortFindPeer();
		}
	}
	/** 
	 * Adds a DecentSocket to the peers list. 
	 * 
	 * Before it does that, it checks that the peers list isn't full and that the socket address is valid by calling
	 * the canAddPeer().
	 * 
	 * If the peer is the first peer on the list, any pending messages in the message queue are flushed.
	 * 
	 * @param newPeer The DecentSocket to add to the peers list
	 */
	private void addPeer(DecentSocket socket) {
		InetAddress address = socket.getInetAddress();
		if(canAddPeer(address)) {
			peers.put(address, socket);
			//If this is the client's first peer, flush the message queue
			if(peers.size() == 1) {
				flushMessageQueue();
				DecentLogger.write("Flushed message queue");
			}
			DecentLogger.write("Added new peer: "+address.getHostAddress());
			writePeers();
			if(peers.size() < maximumConnections) {
				socket.send((new PeerAskMessage()));
			}
		}
	}
	/**
	 * Does the following checks on a given InetAddress:
	 * <ul>
	 * <li>The client's peer list is not full
	 * <li>The client is not already peered with the candidate InetAddress.
	 * <li>The candidate is not an external or internal IP.
	 * <li>The candidate is not a loopback address.
	 * </ul>
	 * 
	 * This does not check if a given peer is online or not.
	 * 
	 * @param candidate The InetAddress to check if can be a valid peer
	 * @return true/false if this InetAddress can be added as a peer
	 */
	private boolean canAddPeer(InetAddress candidate) {
		return peers.size() < maximumConnections && !peers.containsKey(candidate) && !candidate.equals(externalIP) && !candidate.equals(internalIP) && !candidate.isLoopbackAddress();
	}
	/**
	 * Removes a given peer from the peer list. Does nothing if the peer is not in the list.
	 * 
	 * @param peerToRemove The InetAddress to remove from the peers list
	 */
	private void removePeer(InetAddress peerToRemove) {
		if(peers.containsKey(peerToRemove)) {
			peers.remove(peerToRemove);
			writePeers();
		}
	}
	/**
	 * Callback method for when messages are received.
	 * 
	 * Verifies the message has a timestamp, checks the message type, and deserializes it 
	 * into its respective object. 
	 * 
	 * @param message String message that was received
	 * @param origin The DecentSocket it was received from
	 * @return The message to send back or null if no response necessary 
	 */
	private Message onMessageReceived(String message, DecentSocket origin) {
		Gson gson = new Gson();
		JsonObject messageObj = JsonParser.parseString(message).getAsJsonObject();
		//Timestamp member required for all messages
		if(messageObj.has("timestamp")) {
			String type = messageObj.get("type").getAsString();
			switch(type) {
				case "chat":
					chatMessageCallback.apply(gson.fromJson(message, ChatMessage.class), origin);
					break;
				case "peerAsk":
					return onPeerAskMessageReceived(gson.fromJson(message, PeerAskMessage.class), origin);
				case "peers":
					return onPeersMessageReceived(gson.fromJson(message, PeersMessage.class), origin);
				case "ping":
					return onPingMessageReceived(gson.fromJson(message, PingMessage.class), origin);
				case "pong":
					return onPongMessageReceived(gson.fromJson(message, PongMessage.class), origin);
				case "historyAsk":
					return onHistoryAskMessageReceived(gson.fromJson(message, HistoryAskMessage.class), origin);
				case "history":
					return onHistoryMessageReceived(gson.fromJson(message, HistoryMessage.class), origin);
			}
		}
		return null;
	}
	
	private Message onPeersMessageReceived(PeersMessage m, DecentSocket origin) {
		if(m.isValid()) {
			 ArrayList<InetAddress> checkPeers = m.getPeers();
			 int uniquePeers = 0;
			 //Don't get more than half of maximum connections from same peer
			 for(int i=0;i<checkPeers.size() && peers.size() < maximumConnections && uniquePeers<DecentConfig.getMaximumConnections()/2;i++) {
				 InetAddress p = checkPeers.get(i);
				 if(canAddPeer(p)) {
					 uniquePeers++;
					 checkPeer(p);
				 }
			 }
		}
		return null;
	}
	private Message onPingMessageReceived(PingMessage m, DecentSocket origin) {
		//No real need to do anything with ping messages right now, as DecentSocket will auto return those
		return null;
	}
	private Message onPongMessageReceived(PongMessage m, DecentSocket origin) {
		if(m.isValid()) {
			difficultyAdjuster.receivedPong(m, origin.getInetAddress());
		}
		return null;
	}
	private Message onPeerAskMessageReceived(PeerAskMessage m, DecentSocket origin) {
		if(m.isValid()) {
			return new PeersMessage(this.getPeers());
		}
		return null;
	}
	private Message onHistoryAskMessageReceived(HistoryAskMessage m, DecentSocket origin) {
		if(m.isValid()) {
			ChatMessage[] history = historyManager.getMessageHistory(m.getNumMessages());
			return new HistoryMessage(history);
		}
		return null;
	}
	private Message onHistoryMessageReceived(HistoryMessage m, DecentSocket origin) {
		if(m.isValid()) {
			historyManager.receivedHistory(m);
		}
		return null;
	}
	/**
	 * Uses DNS resolution to locate a peer. Only used as a last resort if unable to find peers
	 * any other way. 
	 */
	private void lastResortFindPeer() {
		DecentLogger.write("Resolving peers from DNS (last resort)");
		for(String seed: DNS_SEEDS) {
			ArrayList<InetAddress> hosts = DNSResolver.getARecords(seed);
			if(hosts != null) {
				checkList(hosts);
			}
		}
	}
	/** 
	 * Broadcasts an askPeers message to all currently connected peers.
	 */
	public void askForPeers() {
		if(peers.size() < maximumConnections) {
			propagateToAllPeers(new PeerAskMessage());
		}
	}
	/** 
	 * Sends a "ping" to every current peer, and removes those who do not respond 
	 * promptly. This method is executed hourly by DifficultyDeterminationThread to clean up and
	 * remove any peers that may have gone offline since they were added to the peer list. 
	 */
	public synchronized void prunePeers() {
		checker.checkList(this.getPeers());
	}
	/**
	 * @param newPeer The candidate peer that the client should ping to see if it is online
	 */
	public void checkPeer(InetAddress newPeer) {
		if(canAddPeer(newPeer)) {
			checker.checkAddress(newPeer);
		}
	}
	public void checkList(ArrayList<InetAddress> listToCheck) {
		for(InetAddress p: listToCheck) {
			checkPeer(p);
		}
	}
	/**
	 * Flushes noPeersMessageQueue. Used once the client finds its first peer.
	 */
	private synchronized void flushMessageQueue() {
		if(peers.size() > 0) {
			while(!noPeersMessageQueue.isEmpty()) {
				propagateToAllPeers(noPeersMessageQueue.poll());
			}
		}
	}
	/**
	 * Propagates a message to all peers EXCEPT for those specified. 
	 * This is primarily used to forward chat messages to other peers, exempting the peer it was received from. 
	 * 
	 * @param message The message to send
	 * @param exempt The peers that will not receive this message
	 */
	public void propagateToAllPeers(Message message, DecentSocket... exempt) {
		if(peers.size() == 0) {
			noPeersMessageQueue.add(message);
			lastResortFindPeer();
		}
		ArrayList<DecentSocket> connections = new ArrayList<DecentSocket>(peers.values());
		for(DecentSocket socket: connections) {
			boolean isExempt = false;
			for(int i=0;i<exempt.length && !isExempt;i++) {
				if(socket.equals(exempt[i])) {
					isExempt = true;
				}
			}
			if(!isExempt) {
				socket.send(message);
			}
		}
	}
	/**
	 * Reads the peers file with each line being a different peer's IP address. 
	 * It then adds every IP in the peers file to the DecentPingChecker's queue to check
	 * if that peer is online or not. 
	 */
	private void readPeers() {
		if(peersFile.exists()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(peersFile));
				String line = reader.readLine();
				int lineNumber = 1;
				while(line != null && peers.size() < maximumConnections) {
					try {
						InetAddress candidate = InetAddress.getByName(line);
						checkPeer(candidate);
					} catch (UnknownHostException e) {
						String logMsg = String.format("Invalid host address \"%s\" in file %s at line number %d", line, peersFile.getName(), lineNumber);
						DecentLogger.write(logMsg);
					}
					line = reader.readLine();
					lineNumber++;
				}
				reader.close();
			}
			catch (FileNotFoundException e1) {
				String logMsg = String.format("Peer file %s not found", peersFile.getName());
				DecentLogger.write(logMsg);
			} 
			catch (IOException e1) {
				DecentLogger.write("Unable to read file due to "+e1.getMessage());
			}
		}
		else {
			try {
				peersFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/** 
	 * Writes all peers into the peers file.
	 */
	public void writePeers() {
		try(FileWriter filewriter = new FileWriter(peersFile)) {
			Iterator<InetAddress> itr = peers.keySet().iterator();
			while(itr.hasNext()) {
				InetAddress p = itr.next();
				filewriter.write(p.getHostAddress()+"\n");
			}
		}
		catch(Exception e) {
			DecentLogger.write("Unable to write peers to file");
		}
	}
	/**
	 * Returns a list of all peers currently in the peer list.
	 * 
	 * @return A list of all current peers
	 */
	public ArrayList<InetAddress> getPeers() {
		return new ArrayList<InetAddress>(peers.keySet());
	}
	/** 
	 * Finds the internal and external IPs of the current client. This is used to ensure
	 * that a client does not add itself as a peer.
	 */
	private void findInternalExternalIP() {
		try {
			externalIP = InetAddress.getByName(getExternalIP());
		} catch (UnknownHostException e) {
			DecentLogger.write("Unable to to resolve external IP to InetAddress (problem with ip provider?)");
		}
		try {
			internalIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			DecentLogger.write("Unable to to resolve internal IP to InetAddress");
		}
	}
	/**
	 * Uses a given website to find the client's external IP address.
	 * 
	 * @return A String representation of the external IP
	 */
	private static String getExternalIP() {
		try {
	        URL checkURL = new URL(EXTERNAL_IP_PROVIDER);
	        BufferedReader in = new BufferedReader(new InputStreamReader(checkURL.openStream()));;
	        String ip = in.readLine();
	        return ip;
	     } catch (IOException e) {
	    	 DecentLogger.write("Unable to get external IP Address");
	     }
		return null;
	}
	/**
	 * Closes listener and executes stop() on all DecentSockets currently in peers list. 
	 * This closes their connections and allows them to send an EOT notifying remote end
	 * of socket close.
	 */
	public void shutdown() {
		//We call peers.values() and put into Array to create an unchanging copy, one that
		//does not shrink even as the sockets remove themselves from the peers list
		//this prevents a ConcurrentModificationException
		ArrayList<DecentSocket> connections = new ArrayList<DecentSocket>(peers.values());
		for(DecentSocket socket: connections) {
			socket.stop();
		}
		listener.stop();
		checker.stop();
	}
}
