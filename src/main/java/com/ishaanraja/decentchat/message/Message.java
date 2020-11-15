package com.ishaanraja.decentchat.message;

import java.net.InetAddress;
import java.time.Instant;

import com.google.gson.Gson;
import com.ishaanraja.decentchat.config.DecentConfig;

public abstract class Message {
	
	protected InetAddress sender;
	protected String type;
	protected String version;
	protected long timestamp;
	
	protected Message(String messageType) {
		type = messageType;
		timestamp = Instant.now().getEpochSecond();
		version = DecentConfig.VERSION;
	}
	/**
	 * Each subclass will return whether that particular Message instance is valid.
	 * 
	 * @return true/false if the message is valid
	 */
	public abstract boolean isValid();
	
	public InetAddress getSender() {
		return sender;
	}
	/**
	 * Type describes to the network how a particular message should be handled and processed.
	 * 
	 * @return The message's type
	 */
	public String getType() {
		return type;
	}
	/**
	 * Currently unused as of now, but returns the sender of this message's DecentChat version.
	 * 
	 * @return The sender's DecentChat version
	 */
	public String getVersion() {
		return version;
	}
	/**
	 * POSIX standard epoch timestamp representing the number of seconds from January 1, 1970 UTC.
	 * 
	 * @return The timestamp of this message
	 */
	public long getTimestamp() {
		return timestamp;
	}
	/**
	 * Returns the JSON representation of this Message object. 
	 * 
	 * @return A String of the JSON representation of this message
	 */
	public String toJson() {
		return new Gson().toJson(this);
	}

}
