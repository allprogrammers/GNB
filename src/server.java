/*
 * Muhammad Hamza Ali
 * ma1973
 * Programming Assignment 2 server.java
 * 
 * help taken from lecture slides
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class server {
	
	private static final int MAXBUFFERLEN = 3000;
	private static final int WINDOWSIZE = 7;
	private static final String arrivallogfile = "arrival.log";
	
	private static StringBuilder downloadedContent = new StringBuilder();
	private static StringBuilder arrivallog = new StringBuilder();
	
	/*
	 * deals with arguments
	 * creates and talks on the socket using gbn specified by the arguments
	 */
	public static void main(String args[]) throws ClassNotFoundException, IOException {
		
		String emulatorName = args[0];
		int serverPort = Integer.parseInt(args[1]);
		int emulatorPort = Integer.parseInt(args[2]);
		String fileName = args[3];
		
		DatagramSocket serverSocket = new DatagramSocket(serverPort);
		
		talkOn(serverSocket,emulatorName,emulatorPort,fileName);
		
		serverSocket.close();
		
	}
	
	/*
	 * talks to the client on the socket specifies by the arguments using gbn
	 */
	private static void talkOn(DatagramSocket serverSocket, String clientName, int clientPort, String fileName) throws IOException, ClassNotFoundException {
		
		int expectedSeq = 0;
		int toAck = WINDOWSIZE;
		
		packet dataToSend;
		packet dataReceived;
		
		//waits for a packet and if the packet was with an expected sequence number deals with the data
		while(true)
		{
			byte[] dataToDeserialize = new byte[MAXBUFFERLEN];
			DatagramPacket packetToReceive = new DatagramPacket(dataToDeserialize,dataToDeserialize.length);

			serverSocket.receive(packetToReceive);
			
			dataReceived = commons.deserializePacket(dataToDeserialize);
			arrivallog.append(dataReceived.getSeqNum()+"\n");
			
			
			if(dataReceived.getSeqNum()==expectedSeq)
			{
				toAck = expectedSeq;
				expectedSeq = (expectedSeq+1)%(WINDOWSIZE+1);
				//deals with EOT packet
				if(dataReceived.getType()==3)
				{
					break;
				}else
				{
					downloadedContent.append(dataReceived.getData());
				}
			}

			dataToSend = new packet(0,toAck,0,null);
			byte[] serializedData = commons.serializePacket(dataToSend);
			
			DatagramPacket packetToSend = new DatagramPacket(serializedData,serializedData.length,InetAddress.getByName(clientName),clientPort);
			serverSocket.send(packetToSend);
			
		}
		
		//by coming here it has received an EOT packet so it prepares and sends a return EOT to the client
		dataToSend = new packet(2,dataReceived.getSeqNum(),0,null);
		byte[] serializedData = commons.serializePacket(dataToSend);
		
		DatagramPacket packetToSend = new DatagramPacket(serializedData,serializedData.length,InetAddress.getByName(clientName),clientPort);
		serverSocket.send(packetToSend);

		//writes the downloaded file and the arrival log file
		commons.writeFile(fileName,downloadedContent.toString());
		commons.writeFile(arrivallogfile, arrivallog.toString());
		
	}
	
}
