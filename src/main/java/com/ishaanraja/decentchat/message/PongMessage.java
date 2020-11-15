package com.ishaanraja.decentchat.message;

import java.time.Instant;

import com.ishaanraja.decentchat.config.DecentConfig;

public class PongMessage extends Message {
	
	private int difficulty;
	
	public PongMessage(int difficulty) {
		super("pong");
		this.difficulty = difficulty;
	}

	@Override
	public boolean isValid() {
		try {
			long now = Instant.now().getEpochSecond();
			long timestampTolerance = DecentConfig.TIMESTAMP_TOLERANCE;
			boolean valid = (type.equals("pong"));
			valid &= timestamp >= now-timestampTolerance && timestamp <= now+timestampTolerance;
			//Pong messages with a difficulty of 0 are from peers that have not been online
			//for long enough to determine a difficulty
			valid &= difficulty >= 1 && difficulty <= DecentConfig.MAX_DIFFICULTY;
			return valid;
		}
		catch(Exception e) {
			return false;
		}
	}
	
	public int getDifficulty() {
		return difficulty;
	}

}
