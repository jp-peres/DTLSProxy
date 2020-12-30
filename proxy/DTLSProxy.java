package proxy;
/* 
 * Created by
 * Joao Peres n 48320, Luis Silva 54449
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

class DTLSProxy {
	public static void main(String[] args) throws Exception {
		
		if (args.length != 4) {
			System.out.println("Erro, usar: DTLSProxy <keystore-name> <keystore-pass> <truststore-name> <truststore-pass>");
			System.exit(-1);
		}
		InputStream inputStream = null;
		InputStream dtlsconf = null;
		try	{
			inputStream = new FileInputStream("config.properties");
		}	
		catch(Exception ex) {
			System.err.println("config.properties file not found!");
			System.exit(1);
		}
		try {
			dtlsconf = new FileInputStream("dtls.conf");
		} 
		catch(Exception ex) {
			System.err.println("dtls.conf file not found!");
			System.exit(1);
		}
		String ksName = args[0];
		String ksPass = args[1];
		String tsName = args[2];
		String tsPass = args[3];
		
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
			imp = new DTLSSocket(protocol,peerType, authType, listCiphers, ksName, ksPass, tsName,tsPass, inSocket, serverSocketAddress);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
        
		DatagramSocket outSocket = new DatagramSocket();
		byte[] buffer = new byte[4 * 1024];
		int received = 0;
		
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
}
