package com.ishaanraja.decentchat.message;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;

import com.ishaanraja.decentchat.config.DecentConfig;
import com.ishaanraja.decentchat.config.DecentLogger;
import com.ishaanraja.decentchat.crypto.HashUtils;
import com.ishaanraja.decentchat.crypto.KeyUtils;

public class ChatMessage extends Message {
	
	private String username;
	private String pubKey;
	private String message;
	private String signature;
	private int nonce;
	
	private transient String identifier;
	
	/** 
	 * Creates a ChatMessage object and calculates the required nonce/signature.
	 * 
	 * This constructor does NOT check if the input message meets bounds or is within the correct amount of characters. 
	 * That is left to the DecentChatClient to implement.
	 * 
	 * @param message The message that should be sent.
	 * @param pubKey The sender's public key.
	 * @param privKey The sender's private key
	 */
	
	public ChatMessage(String message, PublicKey pubKey, PrivateKey privKey) {
		super("chat");
		this.username = DecentConfig.getUsername();
		this.pubKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());
		this.message = message;
		String digest = type+message+timestamp;
		//Find nonce and signature
		try {
			this.nonce = getNonce(privKey, digest);
			//getNonce() returns 0 if no nonce matching the difficulty is found, in that case we increment timestamp and try again
			while(this.nonce == 0) {
				timestamp++;
				digest = type+message+timestamp;
				this.nonce = getNonce(privKey, digest);
			}
			this.signature = KeyUtils.sign(digest+nonce, privKey);
		}
		catch(Exception e) {
			String logMsg = String.format("Unable to create message \"%s\" because %s", message, e.getCause());
			DecentLogger.write(logMsg);
		}
	}
	/** 
	 * Iterates through testNonce until the digest+testNonce equals or exceeds the current 
	 * difficulty.
	 * 
	 * @param privateKey
	 * @param text The digest of the message (type+message+timestamp)
	 * @return The nonce that can be added to the message so that the message meets the difficulty
	 * @throws Exception
	 */
	private int getNonce(PrivateKey privateKey, String text) throws Exception {
		int testNonce = 0;
		Signature privateSignature = Signature.getInstance("SHA256withRSA");
		privateSignature.initSign(privateKey);
		byte[] sigBytes = privateSignature.sign();
		while(!isProofOfWorkValid(sigBytes) && testNonce < Integer.MAX_VALUE) {
			testNonce++;
			privateSignature.update((text+testNonce).getBytes("UTF-8"));
			sigBytes = privateSignature.sign();
		}
		return testNonce;
	}
	
	/**
	 * Does the following to verify if a ChatMessage object is valid:
	 * <ul>
	 * <li>Timestamp - The client verifies that the timestamp is within the config's timestamp tolerance of the current UTC epoch time.
	 * <li>Bounds - The client verifies whether the message is between 1-256 characters and that the human readable username is between 1-16 characters.
	 * <li>Proof of Work - The client verifies that the message signature meets the current difficulty by having the proper number of zero bits at the front. 
	 * <li>Signature - The client assembles the message’s digest and using the message’s public key, verifies that message is signed properly.
	 * </ul>
	 * Note: Checking if if the message is a duplicate or if the sender is on the ignore list should be done beforehand by the DecentChatClient.
	 * 
	 * See section 7 of the DecentChat whitepaper for more information.
	 * 
	 * @return true/false if the message has met the above requirements
	 */

	@Override
	public boolean isValid() {
		try {
			long now = Instant.now().getEpochSecond();
			long timestampTolerance = DecentConfig.TIMESTAMP_TOLERANCE;
			boolean valid = (type.equals("chat"));
			//Checking timestamp within bounds
			valid &= timestamp >= now-timestampTolerance && timestamp <= now+timestampTolerance;
			//Checking message within bounds
			valid &= username.length() >= 1 && username.length() <= DecentConfig.MAX_MESSAGE_LENGTH;
			valid &= message.trim().length() >= 1 && message.length() <= DecentConfig.MAX_MESSAGE_LENGTH;
			//Validate proof of work
			byte[] sigBytes = Base64.getDecoder().decode(signature);
			if(valid && isProofOfWorkValid(sigBytes)) {
				//Validate signature
				String digest = type+message+timestamp+nonce;
				PublicKey pkey = KeyUtils.getPublicKeyFromString(pubKey);
				try {
					return KeyUtils.verify(digest, signature, pkey);
				} catch (Exception e) {
					DecentLogger.write("Failed to verify message signature");
				}
			}	
		}
		catch(Exception e) {
			DecentLogger.write("Failed to check message validity due to "+e.getMessage());
		}
		return false;
	}
	/**
	 * Used exclusively for if a historical ChatMessage meets the absolute bare minimum requirements to be a valid
	 * ChatMessage. 
	 * 
	 * Checks the following:
	 * <ul>
	 * <li>Timestamp - The client verifies that the timestamp is within the config's historical timestamp tolerance of the current UTC epoch time.
	 * <li>Bounds - The client verifies whether the message is between 1-256 characters and that the human readable username is between 1-16 characters.
	 * <li>Signature Validity - Verifies that the message signature is valid. Does not check proof of work.
	 * </ul>
	 * This should ONLY be used for historical messages, normal message validity checks should use the isValid() method.
	 * @see com.ishaanraja.decentchat.message.ChatMessage#isValid()
	 * 
	 * @return true/false whether this is a valid historical message
	 */
	public boolean isValidHistoricalMessage() {
		try {
			long now = Instant.now().getEpochSecond();
			long timestampTolerance = DecentConfig.HISTORICAL_TIMESTAMP_TOLERANCE;
			boolean valid = (type.equals("chat"));
			valid &= timestamp >= now-timestampTolerance && timestamp <= now+timestampTolerance;
			//Checking message within bounds
			valid &= username.length() >= 1 && username.length() <= DecentConfig.MAX_MESSAGE_LENGTH;
			valid &= message.trim().length() >= 1 && message.length() <= DecentConfig.MAX_MESSAGE_LENGTH;
			if(valid) {
				//Validate signature
				String digest = type+message+timestamp+nonce;
				PublicKey pkey = KeyUtils.getPublicKeyFromString(pubKey);
				try {
					return KeyUtils.verify(digest, signature, pkey);
				} catch (Exception e) {
					DecentLogger.write("Failed to verify message signature");
				}
			}	
		}
		catch(Exception e) {
			DecentLogger.write("Failed to check message validity due to "+e.getMessage());
		}
		return false;
	}
	/** 
	 * Verifies that the message's signature has the correct number of leading zero bits 
	 * to meet the current network difficulty.
	 * 
	 * @param sigBytes byte array of the RSA signature to check
	 * @return true/false whether the given sigBytes meets the current network difficulty
	 */
	private static boolean isProofOfWorkValid(byte[] sigBytes) {
		//Check if signature meets our difficulty
		int difficultyStandard = DecentConfig.getDifficulty();
		//Check leading bytes
		int leadingBytesToCheck = difficultyStandard / 8;
		for(int i=0;i<leadingBytesToCheck;i++) {
			//All leading bytes must have 8 0s and thus must be equal to 0
			if(sigBytes[i] != 0) {
				return false;
			}
		}
		//Check last byte
		//To get the actual number of zeros on a particular byte, we must subtract 24 from Integer.numberOfLeadingZeros()
		return Integer.numberOfLeadingZeros(sigBytes[leadingBytesToCheck])-24 >= difficultyStandard % 8;
	}
	/**
	 * Returns the number of leading zeros on this message's signature.
	 * 
	 * @return the number of leading zeros on the signature
	 */
	public int getNumberOfLeadingZeros() {
		byte[] sigBytes = Base64.getDecoder().decode(signature);
		int numberOfLeadingZeroBits = 0;
		//If the numberOfLeadingZeroBits is no longer divisible by zero, that means we have hit a byte
		//that is not completely zeros, in that case we can stop incrementing
		for(int i=0;i<sigBytes.length && numberOfLeadingZeroBits % 8 == 0;i++) {
			//To get the actual number of zeros on a particular byte, we must subtract 24 from Integer.numberOfLeadingZeros()
			numberOfLeadingZeroBits+=Integer.numberOfLeadingZeros(sigBytes[i])-24;
		}
		return numberOfLeadingZeroBits;
	}
	/**
	 * Returns the sender of this message's human readable username.
	 * 
	 * @return a String between 1 and 16 characters of the sender's username
	 */
	public String getUsername() {
		return username;
	}
	public String getPubKey() {
		return pubKey;
	}
	public String getSignature() {
		return signature;
	}
	/**
	 * Returns the 10 character identifier of the public key. 
	 * A message's identifier is the first 10 characters of the SHA-256 hash of the sender's public key. 
	 * @return The sender's 10 character identifier
	 */
	public String getIdentifier() {
		if(identifier == null) {
			identifier = HashUtils.getIdentifier(pubKey);
		}
		return identifier;
	}
	
	/**
	 * Puts everything together into one string that can be displayed to the user. 
	 * 
	 * See section 3 of the DecentChat whitepaper for more information. 
	 * 
	 * General format: [username]@[identifier]: [chat message]
	 */
	public String toString() {
		String identifier = getIdentifier();
		String formattedMessage = username+"@"+identifier+": "+message.trim();
		return cleanString(formattedMessage);
	}
	
	/**
	 * Replaces whitespace (carriage returns, tabs, etc.) with a singular space, removes ISO Control 
	 * characters, and returns a "cleaned" string.
	 * 
	 * @param s The String to clean
	 * @return The "cleaned" String
	 */
	private static String cleanString(String s) {
		String cleanedStr = "";
		char[] stringArr = s.toCharArray();
		for(char c: stringArr) {
			if(Character.isWhitespace(c)) {
				cleanedStr+=" ";
			}
			else if(!Character.isISOControl(c)) {
				cleanedStr+=c;
			}
		}
		return cleanedStr;
	}

}
