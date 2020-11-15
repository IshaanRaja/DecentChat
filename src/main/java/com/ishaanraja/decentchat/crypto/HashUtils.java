package com.ishaanraja.decentchat.crypto;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.ishaanraja.decentchat.config.DecentConfig;
import com.ishaanraja.decentchat.config.DecentLogger;

/**
 * Helper methods for calculating SHA-256 hashes and 
 * public key identifiers.
 */
public class HashUtils {
	
	private HashUtils() {}
	
	/**
	 * Calculates a SHA-256 hash of a given string.
	 * 
	 * @param s The String to hash
	 * @return A hexadecimal representation of the SHA-256 hash
	 * @throws NoSuchAlgorithmException
	 */
	
	public static String sha256(String s) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
	    md.update(s.getBytes());
	    byte[] digest = md.digest();
	    String hex = String.format("%064x", new BigInteger(1, digest));
	    return hex;
	}
	
	/**
	 * Returns the first 10 characters (80 bits) of the hexadecimal representation
	 * of SHA-256 hash of the public key. 
	 * 
	 * @param pubKey A String containing base 64 encoded RSA public key
	 * @return The first 10 characters of the SHA-256 hash of the public key
	 */
	public static String getIdentifier(String pubKey) {
		try {
			String hashRepresentation = sha256(pubKey);
			return hashRepresentation.substring(0, DecentConfig.IDENTIFIER_LENGTH);
		} catch (NoSuchAlgorithmException e) {
			DecentLogger.write("Could not find algorithm SHA-256");
		}
		return null;
	}
	 

}
