import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class client {
	
	private static final int MAXBUFFERLEN = 3000;
	private static final int MAXREADSIZE = 30;
	private static final int EXTRAPACKETS = 0;
	private static final int WINDOWSIZE = 7;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		String emulatorName = args[0];
		int emulatorPort = Integer.parseInt(args[1]);
		int clientPort = Integer.parseInt(args[2]);
		String fileName = args[3];
		
		DatagramSocket clientSocket = new DatagramSocket(clientPort);
		
		talkOn(clientSocket,emulatorName,emulatorPort,fileName);
		
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
	
	private static byte[][] serializePacketizedFile(String fileName) throws IOException
	{
		packet[] packets = packetizeFile(fileName);
		byte[][] toReturn = new byte[packets.length][];
		
		for(int i=0;i<packets.length;i++)
		{
			toReturn[i] = serializePacket(packets[i]);
		}
		
		return toReturn;
	}
	
	private static packet[] packetizeFile(String fileName) throws IOException{
		
		String fileContent = readFile(fileName);//can also read file in chunks of 30 characters if the file is too big
		packet[] packets = new packet[fileContent.length()/MAXREADSIZE + 1 + EXTRAPACKETS];
		int ind=0;
		packets[ind] = new packet(1,0,0,null);
		for(int i=0;i<fileContent.length();i+=MAXREADSIZE)
		{
			ind = i/MAXREADSIZE;
			try
			{
				packets[ind] = new packet(1,ind%7,MAXREADSIZE,fileContent.substring(i, i+MAXREADSIZE));
			}catch(IndexOutOfBoundsException e)
			{
				packets[ind] = new packet(1,ind%7,fileContent.length()-i, fileContent.substring(i, fileContent.length()));
				break;
			}
		}
		/*ind += 1;
		
		packets[ind] = new packet(3,ind%7,0,null);*/
		
		return packets;
	}
	
	private static String readFile(String fileName) throws IOException {
		String content = new String(Files.readAllBytes(Paths.get(fileName)),StandardCharsets.US_ASCII);
		return content;
	}

	private static void talkOn(DatagramSocket clientSocket, String serverName, int serverPort, String fileName) throws IOException, ClassNotFoundException {
		//byte[] serializedData = serializePacket(data);
		
		byte[][] serializedPackets = serializePacketizedFile(fileName);
		byte[] dataToDeserialize;
		
		DatagramPacket packetToSend;
		DatagramPacket packetToReceive;
		packet ack;
		InetAddress host = InetAddress.getByName(serverName);
		for(int i=0;i<serializedPackets.length;i+=7)
		{
			byte[] serializedData = serializedPackets[i];
			packetToSend = new DatagramPacket(serializedData,serializedData.length,host,serverPort);
			clientSocket.send(packetToSend);
			
			dataToDeserialize = new byte[MAXBUFFERLEN];
			
			packetToReceive = new DatagramPacket(dataToDeserialize,dataToDeserialize.length);
			clientSocket.receive(packetToReceive);
			
			ack = deserializePacket(dataToDeserialize);
			
			ack.printContents();
		}
		//GBN 
		System.out.println("sent everything. now sending EOT");
		byte[] EOTSerialized = serializePacket(new packet(3,(serializedPackets.length+1)%7,0,null));
		packetToSend = new DatagramPacket(EOTSerialized,EOTSerialized.length,host,serverPort);
		clientSocket.send(packetToSend);
		
		dataToDeserialize = new byte[MAXBUFFERLEN];
		packetToReceive = new DatagramPacket(dataToDeserialize,dataToDeserialize.length);
		clientSocket.receive(packetToReceive);
		ack = deserializePacket(dataToDeserialize);
		
		ack.printContents();
		
	}
}
