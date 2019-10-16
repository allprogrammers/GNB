import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class server {
	
	private static final int MAXBUFFERLEN = 3000;
	
	public static void main(String args[]) throws ClassNotFoundException, IOException {
		
		int serverPort = Integer.parseInt(args[0]);
		String clientName = args[1];
		int clientPort = Integer.parseInt(args[2]);
		
		DatagramSocket serverSocket = new DatagramSocket(serverPort);
		
		packet data = new packet(4,3,2,"1");
		talkOn(serverSocket,clientName,clientPort,data);
		
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
	
	private static void talkOn(DatagramSocket serverSocket, String clientName, int clientPort, packet data) throws IOException, ClassNotFoundException {
		byte[] dataToDeserialize = new byte[MAXBUFFERLEN];
		DatagramPacket packetToReceive = new DatagramPacket(dataToDeserialize,dataToDeserialize.length);
		
		serverSocket.receive(packetToReceive);
		
		packet dataReceived = deserializePacket(dataToDeserialize);
		dataReceived.printContents();
		
		packet dataToSend = data;
		byte[] serializedData = serializePacket(dataToSend);
		
		DatagramPacket packetToSend = new DatagramPacket(serializedData,serializedData.length,InetAddress.getByName(clientName),clientPort);
		serverSocket.send(packetToSend);
	}
	
}
