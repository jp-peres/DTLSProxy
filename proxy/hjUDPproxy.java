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

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import dtls.DTLSSocket;

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
		String server = properties.getProperty("server");
		String proxy = properties.getProperty("proxy");
		String destinations = properties.getProperty("localdelivery");
		
		properties.load(dtlsconf);
		String protocol = properties.getProperty("TLS-PROT-ENF");
		String authType = properties.getProperty("TLS-AUTH");
		String ciphersuites = properties.getProperty("CIPHERSUITES");
		
		String[] listCiphers = ciphersuites.split(","); 
		
		InetSocketAddress proxySocketAddress = parseSocketAddress(proxy);
		InetSocketAddress serverSocketAddress = parseSocketAddress(server);
		Set<SocketAddress> outSocketAddressSet = Arrays.stream(destinations.split(",")).map(s -> parseSocketAddress(s))
				.collect(Collectors.toSet());

		DatagramSocket inSocket = new DatagramSocket(proxySocketAddress);
		
		DTLSSocket imp = null;
		try {
			imp = new DTLSSocket(protocol,peerType, authType, listCiphers, ksName, ksPass, inSocket, serverSocketAddress);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
        
		DatagramSocket outSocket = new DatagramSocket();
		byte[] buffer = new byte[4 * 1024];
		int received = 0;
		// While there is something to read
		
		
		while (true) {
			DatagramPacket inPacket = new DatagramPacket(buffer, received);
			
			imp.receive(inPacket);
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
