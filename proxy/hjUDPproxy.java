package proxy;
/* hjUDPproxy, 20/Mar/18
 *
 * This is a very simple (transparent) UDP proxy
 * The proxy can listening on a remote source (server) UDP sender
 * and transparently forward received datagram packets in the
 * delivering endpoint
 *
 * Possible Remote listening endpoints:
 *    Unicast IP address and port: configurable in the file config.properties
 *    Multicast IP address and port: configurable in the code
 *  
 * Possible local listening endpoints:
 *    Unicast IP address and port
 *    Multicast IP address and port
 *       Both configurable in the file config.properties
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.net.ssl.SSLSocket;

import dtls.DTLSImpl;

class hjUDPproxy {
	public static void main(String[] args) throws Exception {
		InputStream inputStream = new FileInputStream("src/main/java/config.properties");
		if (inputStream == null) {
			System.err.println("config.properties file not found!");
			System.exit(1);
		}
		InputStream dtlsconf = new FileInputStream("src/main/java/dtls.conf");
		if (dtlsconf == null) {
			System.err.println("dtls.conf file not found!");
			System.exit(1);
		}
		String ksName = null;
		String ksPass = null;
		
		if (args.length == 2) {
			ksName = args[0];
			ksPass = args[1];
		}
		
		String peerType = "PROXY";
		
		Properties properties = new Properties();
		properties.load(inputStream);
		String remote = properties.getProperty("remote");
		String destinations = properties.getProperty("localdelivery");
		
		properties.load(dtlsconf);
		String protocol = properties.getProperty("TLS-PROT-ENF");
		String authType = properties.getProperty("TLS-AUTH");
		String ciphersuites = properties.getProperty("CIPHERSUITES");
		
		String[] listCiphers = ciphersuites.split(","); 
		
		int port = parsePort(remote);
		InetSocketAddress inSocketAddress = parseSocketAddress(remote);
		Set<SocketAddress> outSocketAddressSet = Arrays.stream(destinations.split(",")).map(s -> parseSocketAddress(s))
				.collect(Collectors.toSet());

		
		DTLSImpl imp = null;
		try {
			imp = new DTLSImpl(protocol,peerType, authType, listCiphers, ksName, ksPass, inSocketAddress);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		// has to be done internally
		//DatagramSocket inSocket = new DatagramSocket(inSocketAddress);
		SSLSocket inSocket = imp.getSocket();
		inSocket.startHandshake();
		
		InputStream sockStream = inSocket.getInputStream();
        
		DatagramSocket outSocket = new DatagramSocket();
		byte[] buffer = new byte[4 * 1024];
		int received = 0;
		// While there is something to read
		while ( (received = sockStream.read(buffer)) != -1) {
			DatagramPacket inPacket = new DatagramPacket(buffer, received);
			System.out.print("*");
			for (SocketAddress outSocketAddress : outSocketAddressSet) {
				outSocket.send(new DatagramPacket(inPacket.getData(), inPacket.getLength(), outSocketAddress));
			}
		}
	}

	private static InetSocketAddress parseSocketAddress(String socketAddress) {
		String[] split = socketAddress.split(":");
		String host = split[0];
		int port = Integer.parseInt(split[1]);
		return new InetSocketAddress(host, port);
	}
	
	private static int parsePort(String socketAddress) {
		String[] split = socketAddress.split(":");
		return Integer.parseInt(split[1]);
	}
}
