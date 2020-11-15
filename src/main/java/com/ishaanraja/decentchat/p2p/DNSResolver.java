package com.ishaanraja.decentchat.p2p;

import java.net.InetAddress;
import java.util.ArrayList;

import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;

import com.ishaanraja.decentchat.config.DecentLogger;

/** 
 * This class is used by NodeManager as a last resort to find DecentChat peers. 
 * It resolves A records of a given domain and returns a list of InetAddresses.
 */

public class DNSResolver {
	
	/**
	 * Fetches a domain's A records from DNS
	 * @param domain The domain to resolve A records from
	 * @return A list of InetAddresses that were resolved from the domain
	 */
	public static ArrayList<InetAddress> getARecords(String domain) {
		ArrayList<InetAddress> hosts = new ArrayList<InetAddress>();
		try {
			Attribute attr = new InitialDirContext().getAttributes("dns:"+domain, new String[] {"A"}).get("A");
			for(int i=0;i<attr.size();i++) {
				String address = (String)attr.get(i);
				hosts.add(InetAddress.getByName(address));
			}
		}
		catch(Exception e) {
			DecentLogger.write("Unable to resolve DNS seeds");
		}
		return hosts;
	}

}
