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
	private static StringBuilder writingBuffer;
	
	public static void main(String args[]) throws ClassNotFoundException, IOException {
		
		String emulatorName = args[0];
		int serverPort = Integer.parseInt(args[1]);
		int emulatorPort = Integer.parseInt(args[2]);
		String fileName = args[3];
		
		writingBuffer = new StringBuilder("");
		
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
		
		while(true)
		{
			byte[] dataToDeserialize = new byte[MAXBUFFERLEN];
			DatagramPacket packetToReceive = new DatagramPacket(dataToDeserialize,dataToDeserialize.length);
			
			serverSocket.receive(packetToReceive);
			
			packet dataReceived = deserializePacket(dataToDeserialize);
			dataReceived.printContents();
			
			packet dataToSend;
			if(dataReceived.getType()!=3)
			{
				writingBuffer.append(dataReceived.getData());
				dataToSend = new packet(0,dataReceived.getSeqNum(),0,null);
			}else
			{
				dataToSend = new packet(2,dataReceived.getSeqNum(),0,null);
				byte[] serializedData = serializePacket(dataToSend);
				
				DatagramPacket packetToSend = new DatagramPacket(serializedData,serializedData.length,InetAddress.getByName(clientName),clientPort);
				serverSocket.send(packetToSend);
				writeToFile(fileName);
				return;
			}
			byte[] serializedData = serializePacket(dataToSend);
			
			DatagramPacket packetToSend = new DatagramPacket(serializedData,serializedData.length,InetAddress.getByName(clientName),clientPort);
			serverSocket.send(packetToSend);
			
		}
		
	}

	private static void writeToFile(String fileName) throws IOException {
		FileWriter fw = new FileWriter(fileName,false);//can also buffer the output to the writing buffer so that all of the file is not held in the memory
		fw.write(writingBuffer.toString());
		fw.close();
		
	}
	
}
