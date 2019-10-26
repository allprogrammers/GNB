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
	
	public static void main(String args[]) throws ClassNotFoundException, IOException {
		
		String emulatorName = args[0];
		int serverPort = Integer.parseInt(args[1]);
		int emulatorPort = Integer.parseInt(args[2]);
		String fileName = args[3];
		
		DatagramSocket serverSocket = new DatagramSocket(serverPort);
		
		talkOn(serverSocket,emulatorName,emulatorPort,fileName);
		
		serverSocket.close();
		
	}
	
	private static void talkOn(DatagramSocket serverSocket, String clientName, int clientPort, String fileName) throws IOException, ClassNotFoundException {
		
		int expectedSeq = 0;
		int toAck = WINDOWSIZE;
		
		packet dataToSend;
		packet dataReceived;
		
		while(true)
		{
			System.out.println(expectedSeq);
			byte[] dataToDeserialize = new byte[MAXBUFFERLEN];
			DatagramPacket packetToReceive = new DatagramPacket(dataToDeserialize,dataToDeserialize.length);
			
			serverSocket.receive(packetToReceive);
			
			dataReceived = commons.deserializePacket(dataToDeserialize);
			arrivallog.append(dataReceived.getSeqNum()+"\n");
			if(dataReceived.getSeqNum()==expectedSeq)
			{
				toAck = expectedSeq;
				expectedSeq = (expectedSeq+1)%(WINDOWSIZE+1);
				if(dataReceived.getType()==3)
				{
					break;
				}else
				{
					downloadedContent.append(dataReceived.getData());
				}
			}else
			{
				System.out.println("weird "+expectedSeq+" "+ dataReceived.getSeqNum());
			}

			dataToSend = new packet(0,toAck,0,null);
			byte[] serializedData = commons.serializePacket(dataToSend);
			
			DatagramPacket packetToSend = new DatagramPacket(serializedData,serializedData.length,InetAddress.getByName(clientName),clientPort);
			serverSocket.send(packetToSend);
			
		}
		dataToSend = new packet(2,dataReceived.getSeqNum(),0,null);
		byte[] serializedData = commons.serializePacket(dataToSend);
		
		DatagramPacket packetToSend = new DatagramPacket(serializedData,serializedData.length,InetAddress.getByName(clientName),clientPort);
		serverSocket.send(packetToSend);
		commons.writeFile(fileName,downloadedContent.toString());
		commons.writeFile(arrivallogfile, arrivallog.toString());
		
	}
	
}
