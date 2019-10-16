import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class client {
	
	private static final int MAXBUFFERLEN = 3000;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		int clientPort = Integer.parseInt(args[0]);
		String serverName = args[1];
		int serverPort = Integer.parseInt(args[2]);
		
		DatagramSocket clientSocket = new DatagramSocket(clientPort);
		
		packet data = new packet(1,2,3,"4");
		
		talkOn(clientSocket,serverName,serverPort,data);
		
		clientSocket.close();
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
	
	private static void talkOn(DatagramSocket clientSocket, String serverName, int serverPort, packet data) throws IOException, ClassNotFoundException {
		byte[] serializedData = serializePacket(data);
		
		DatagramPacket packetToSend = new DatagramPacket(serializedData,serializedData.length, InetAddress.getByName(serverName),serverPort);
		clientSocket.send(packetToSend);
		
		byte[] dataToDeserialize = new byte[MAXBUFFERLEN];
		
		DatagramPacket packetToReceive = new DatagramPacket(dataToDeserialize,dataToDeserialize.length);
		clientSocket.receive(packetToReceive);
		
		packet ack = deserializePacket(dataToDeserialize);
		
		ack.printContents();
	}
}
