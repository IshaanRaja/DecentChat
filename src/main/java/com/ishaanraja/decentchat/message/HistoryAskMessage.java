package com.ishaanraja.decentchat.message;

import java.time.Instant;

import com.ishaanraja.decentchat.config.DecentConfig;

public class HistoryAskMessage extends Message {
	
	private int numMessages;
	
	public HistoryAskMessage() {
		super("historyAsk");
		numMessages = DecentConfig.MESSAGE_HISTORY_LENGTH;
	}

	@Override
	public boolean isValid() {
		try {
			long now = Instant.now().getEpochSecond();
			long timestampTolerance = DecentConfig.TIMESTAMP_TOLERANCE;
			boolean valid = (type.equals("historyAsk"));
			valid &= timestamp >= now-timestampTolerance && timestamp <= now+timestampTolerance;
			return valid;
		}
		catch(Exception e) {
			return false;
		}
	}
	/**
	 * Returns the number of historical messages that the sender is requesting.
	 * 
	 * @return requested number of historical messages
	 */
	public int getNumMessages() {
		return numMessages;
	}

}
