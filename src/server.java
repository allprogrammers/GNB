import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class server {
	
	private static final int MAXBUFFERLEN = 3000;
	private static StringBuilder downloadedContent = new StringBuilder();
	private static final int WINDOWSIZE = 7;
	
	public static void main(String args[]) throws ClassNotFoundException, IOException {
		
		String emulatorName = args[0];
		int serverPort = Integer.parseInt(args[1]);
		int emulatorPort = Integer.parseInt(args[2]);
		String fileName = args[3];
		
		DatagramSocket serverSocket = new DatagramSocket(serverPort);
		
		talkOn(serverSocket,emulatorName,emulatorPort,fileName);
		
		serverSocket.close();
		
	}
	
	private static byte[] serializePacket(packet data) throws IOException {
		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		ObjectOutputStream oostream = new ObjectOutputStream(ostream);
		
		oostream.writeObject(data);
		oostream.close();
		
		byte[] toReturn = ostream.toByteArray();
		ostream.close();
		
		return toReturn;
	}
	
	private static packet deserializePacket(byte[] dataToDeserialize) throws IOException, ClassNotFoundException {
		ByteArrayInputStream iStream = new ByteArrayInputStream(dataToDeserialize);
		ObjectInputStream oiStream = new ObjectInputStream(iStream);
		
		packet toReturn = (packet) oiStream.readObject();
		
		iStream.close();
		oiStream.close();
		
		return toReturn;
	}
	
	private static void talkOn(DatagramSocket serverSocket, String clientName, int clientPort, String fileName) throws IOException, ClassNotFoundException {
		
		int expectedSeq = 0;
		int toAck = WINDOWSIZE;
		
		packet dataToSend;
		packet dataReceived;
		
		while(true)
		{
			byte[] dataToDeserialize = new byte[MAXBUFFERLEN];
			DatagramPacket packetToReceive = new DatagramPacket(dataToDeserialize,dataToDeserialize.length);
			
			serverSocket.receive(packetToReceive);
			
			dataReceived = deserializePacket(dataToDeserialize);
			System.out.println(dataReceived.getSeqNum()+" "+expectedSeq); 
			if(dataReceived.getSeqNum()==expectedSeq)
			{
				toAck = expectedSeq;
				expectedSeq = (expectedSeq+1)%(WINDOWSIZE+1);
				if(dataReceived.getType()==3)
				{
					System.out.println("done");
					break;
				}else
				{
					downloadedContent.append(dataReceived.getData());
					//dataReceived.printContents();
				}
			}

			dataToSend = new packet(0,toAck,0,null);
			byte[] serializedData = serializePacket(dataToSend);
			
			DatagramPacket packetToSend = new DatagramPacket(serializedData,serializedData.length,InetAddress.getByName(clientName),clientPort);
			serverSocket.send(packetToSend);
			
		}
		dataToSend = new packet(2,dataReceived.getSeqNum(),0,null);
		byte[] serializedData = serializePacket(dataToSend);
		
		DatagramPacket packetToSend = new DatagramPacket(serializedData,serializedData.length,InetAddress.getByName(clientName),clientPort);
		serverSocket.send(packetToSend);
		writeFile(fileName,downloadedContent.toString());
		
	}

	private static void writeFile(String filename,String filecontent) throws IOException {
		FileWriter fw = new FileWriter(filename,false);//can also buffer the output to the writing buffer so that all of the file is not held in the memory
		fw.write(filecontent);
		fw.close();
	}
	
}
