package com.ishaanraja.decentchat.message;

import java.time.Instant;

import com.ishaanraja.decentchat.config.DecentConfig;

public class PingMessage extends Message {
	
	public PingMessage() {
		super("ping");
	}

	@Override
	public boolean isValid() {
		try {
			long now = Instant.now().getEpochSecond();
			long timestampTolerance = DecentConfig.TIMESTAMP_TOLERANCE;
			boolean valid = (type.equals("ping"));
			valid &= timestamp >= now-timestampTolerance && timestamp <= now+timestampTolerance;
			return valid;
		}
		catch(Exception e) {
			return false;
		}
	}

}
