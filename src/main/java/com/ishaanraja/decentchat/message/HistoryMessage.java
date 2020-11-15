package com.ishaanraja.decentchat.message;

import java.time.Instant;

import com.ishaanraja.decentchat.config.DecentConfig;

public class HistoryMessage extends Message {
	
	private ChatMessage[] messageHistory;
	
	public HistoryMessage(ChatMessage[] messageHistory) {
		super("history");
		this.messageHistory = messageHistory;
	}

	@Override
	public boolean isValid() {
		long now = Instant.now().getEpochSecond();
		long timestampTolerance = DecentConfig.TIMESTAMP_TOLERANCE;
		boolean valid = (type.equals("history"));
		valid &= timestamp >= now-timestampTolerance && timestamp <= now+timestampTolerance;
		valid &= messageHistory.length > 0 && messageHistory.length <= DecentConfig.MESSAGE_HISTORY_LENGTH;
		for(int i=0;messageHistory != null && i<messageHistory.length && valid;i++) {
			valid &= messageHistory[i].isValidHistoricalMessage();
		}
		return valid;
	}
	
	public ChatMessage[] getMessageHistory() {
		return messageHistory;
	}
	/**
	 * Returns the total amount of leading zeros in each message in this history message's array.
	 * 
	 * This is primarily used to compare which history message has the highest proof of work.
	 * @return an int of the total number of zeros 
	 */
	public int getProofOfWork() {
		int proofOfWork = 0;
		for(ChatMessage m: messageHistory) {
			proofOfWork+=m.getNumberOfLeadingZeros();
		}
		return proofOfWork;
	}

}
