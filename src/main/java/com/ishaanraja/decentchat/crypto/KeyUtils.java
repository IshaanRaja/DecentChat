package com.ishaanraja.decentchat.crypto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import com.ishaanraja.decentchat.config.DecentLogger;

/**
 * Helper methods for various tasks relating to handling PKCS#8 RSA 
 * private/public keys.
 */
public class KeyUtils {
	
	private KeyUtils() {}
	
	/**
	 * Generates a new private/public keypair.
	 * 
	 * @return KeyPair
	 * @throws Exception
	 */
	public static KeyPair generateKeyPair() throws Exception {
		  KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		  kpg.initialize(2048, new SecureRandom());
		  KeyPair kpair = kpg.genKeyPair();
		  return kpair;
	}
	/**
	 * Uses a given private key to sign a message. 
	 * 
	 * @param plainText The text to sign. 
	 * @param privateKey The privateKey to sign with
	 * @return A Base 64 String representation of the message's signature
	 * @throws Exception
	 */
	public static String sign(String plainText, PrivateKey privateKey) throws Exception {
	    Signature privateSignature = Signature.getInstance("SHA256withRSA");
	    privateSignature.initSign(privateKey);
	    privateSignature.update(plainText.getBytes("UTF-8"));
	    byte[] signature = privateSignature.sign();
	    return Base64.getEncoder().encodeToString(signature);
	}
	/**
	 * Returns whether a given public key signed a given message
	 * 
	 * @param plainText The text that was purportedly signed. 
	 * @param signature The Base 64 String representation of the message's signature.
	 * @param publicKey The sender's public key
	 * @return true/false if the given public key (and its corresponding private key) signed that message.
	 * @throws Exception
	 */
	public static boolean verify(String plainText, String signature, PublicKey publicKey) throws Exception {
	    Signature publicSignature = Signature.getInstance("SHA256withRSA");
	    publicSignature.initVerify(publicKey);
	    publicSignature.update(plainText.getBytes("UTF-8"));
	    byte[] signatureBytes = Base64.getDecoder().decode(signature);
	    return publicSignature.verify(signatureBytes);
	}
	/**
	 * Returns a PublicKey object from a Base 64 encoded String representation of a public key.
	 * 
	 * @param asString The Base 64 encoded representation of the public key
	 * @return PublicKey The PublicKey object of that string parameter
	 */
	public static PublicKey getPublicKeyFromString(String asString) {
		try {
			byte[] byteKey = Base64.getDecoder().decode(asString.getBytes());
			X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			return kf.generatePublic(X509publicKey);
		} 
		catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			DecentLogger.write("Could not parse public key string");
			return null;
		}
	}
	/**
	 * Used when the public.pem file does not exist or cannot be found, but the private.pem file is found.
	 * It generates the corresponding PublicKey object from the private key. 
	 * 
	 * @param privKey PrivateKey to generate PublicKey from
	 * @return The PublicKey companion to the PrivateKey parameter.
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	public static PublicKey getPublicKeyFromPrivateKey(PrivateKey privKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
	    RSAPrivateCrtKey privk = (RSAPrivateCrtKey)privKey;
	    RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privk.getModulus(), privk.getPublicExponent());;
	    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
	    return publicKey;
	}
	/**
	 * Reads a Base 64 encoded PKCS#8 public key from a given File object and returns its corresponding
	 * PublicKey object.
	 * 
	 * @param pubkeyFile The file containing the public key to read from
	 * @return a PublicKey object
	 * @throws FileNotFoundException
	 */
	public static PublicKey getPublicKeyFromFile(File pubkeyFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(pubkeyFile));
		String pubKeyStr = "";
		String line = reader.readLine();
		while(line != null) {
			pubKeyStr+=line.trim();
			line = reader.readLine();
		}
		reader.close();
		pubKeyStr = pubKeyStr.replace("-----BEGIN PUBLIC KEY-----", "");
		pubKeyStr = pubKeyStr.replace("-----END PUBLIC KEY-----", "");
		return getPublicKeyFromString(pubKeyStr);
	}
	/**
	 * Reads a Base 64 encoded PKCS#8 private key from a given File object and returns its corresponding
	 * PrivateKey object.
	 * 
	 * @param privkeyFile The file containing the private key to read from
	 * @return a PrivateKey object
	 * @throws IOException 
	 */
	public static PrivateKey getPrivateKeyFromFile(File privkeyFile) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(privkeyFile));
		String privKeyStr = "";
		String line = reader.readLine();
		while(line != null) {
			privKeyStr+=line.trim();
			line = reader.readLine();
		}
		reader.close();
		privKeyStr = privKeyStr.replace("-----BEGIN PRIVATE KEY-----", "");
		privKeyStr = privKeyStr.replace("-----END PRIVATE KEY-----", "");
		byte[] byteKey = Base64.getDecoder().decode(privKeyStr.getBytes());
		PKCS8EncodedKeySpec pkcs8 = new PKCS8EncodedKeySpec(byteKey);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(pkcs8);
	}
	/**
	 * Encodes a PrivateKey object into Base 64 and writes it to a file.
	 * 
	 * Note: If the file exists already, its content is overwritten.
	 * 
	 * @param privkey The PrivateKey object to write
	 * @param privateKeyFile The file to write the key to
	 */
	public static void writePrivateKeyToFile(PrivateKey privkey, File privateKeyFile) {
		try {;
			privateKeyFile.createNewFile();
			FileWriter privwriter = new FileWriter(privateKeyFile);
			privwriter.write("-----BEGIN PRIVATE KEY-----\n");
			privwriter.write(Base64.getMimeEncoder().encodeToString(privkey.getEncoded()));
			privwriter.write("\n-----END PRIVATE KEY-----");
			privwriter.close();
		}
		catch(Exception e) {
			DecentLogger.write("Unable to create "+privateKeyFile.getName());
		}
	}
	/**
	 * Encodes a PublicKey object into Base 64 and writes it to a file.
	 * 
	 * Note: If the file exists already, its content is overwritten.
	 * 
	 * @param pubKey The PublicKey object to write
	 * @param publicKeyFile The file to write the key to
	 */
	public static void writePublicKeyToFile(PublicKey pubKey, File publicKeyFile) {
		try {
			publicKeyFile.createNewFile();
			FileWriter pubwriter = new FileWriter(publicKeyFile);
			pubwriter.write("-----BEGIN PUBLIC KEY-----\n");
			pubwriter.write(Base64.getMimeEncoder().encodeToString(pubKey.getEncoded()));
			pubwriter.write("\n-----END PUBLIC KEY-----");
			pubwriter.close();
		}
		catch(Exception e) {
			DecentLogger.write("Unable to create "+publicKeyFile.getName());
		}
	}

}
