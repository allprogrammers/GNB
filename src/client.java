import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;

public class client {
	
	private static final int MAXBUFFERLEN = 3000;
	private static final int MAXREADSIZE = 30;
	private static final int EXTRAPACKETS = 0;
	private static final int WINDOWSIZE = 7;
	private static final int TIMER = 50;
	private static final String seqlog = "clientseqnum.log";
	private static final String acklog = "clientack.log";
	
	private static StringBuilder seqlogbuff= new StringBuilder();
	private static StringBuilder acklogbuff = new StringBuilder();
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		String emulatorName = args[0];
		int emulatorPort = Integer.parseInt(args[1]);
		int clientPort = Integer.parseInt(args[2]);
		String fileName = args[3];
		
		DatagramSocket clientSocket = new DatagramSocket(clientPort);
		
		talkOn(clientSocket,emulatorName,emulatorPort,fileName);
		
		clientSocket.close();
	}
	
	private static void writeFile(String filename, String filecontent) throws IOException
	{
		FileWriter fw = new FileWriter(filename,false);//can also buffer the output to the writing buffer so that all of the file is not held in the memory
		fw.write(filecontent);
		fw.close();
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
	
	private static byte[][] serializePacketizedFile(String fileName,int maxReadSize,int extraPackets,int windowsize) throws IOException
	{
		packet[] packets = packetizeFile(fileName,maxReadSize,extraPackets,windowsize);
		byte[][] toReturn = new byte[packets.length][];
		
		for(int i=0;i<packets.length;i++)
		{
			toReturn[i] = serializePacket(packets[i]);
		}
		
		return toReturn;
	}
	
	private static packet[] packetizeFile(String fileName,int maxReadSize,int extraPackets,int windowsize) throws IOException{
		
		String fileContent = readFile(fileName);//can also read file in chunks of 30 characters if the file is too big
		packet[] packets = new packet[fileContent.length()/maxReadSize + 1 + extraPackets];
		int ind=0;
		packets[ind] = new packet(1,0,0,null);
		for(int i=0;i<fileContent.length();i+=MAXREADSIZE)
		{
			ind = i/MAXREADSIZE;
			try
			{
				packets[ind] = new packet(1,ind%(WINDOWSIZE+1),MAXREADSIZE,fileContent.substring(i, i+MAXREADSIZE));
			}catch(IndexOutOfBoundsException e)
			{
				packets[ind] = new packet(1,ind%(WINDOWSIZE+1),fileContent.length()-i, fileContent.substring(i, fileContent.length()));
				break;
			}
			System.out.println(packets[ind].getData());
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
		InetAddress serverHost = InetAddress.getByName(serverName);
		
		gbnTransfer(serializedPackets,clientSocket,serverHost,serverPort);
	}

	private static void loadWindow(DatagramSocket clientSocket, LinkedList<DatagramPacket> window,InetAddress serverName, int serverPort, byte[][] serializedPackets, int base) throws IOException, ClassNotFoundException {
		
		
		int n = WINDOWSIZE-window.size();
		int s = window.size();
		//System.out.println("start "+ base + " " + n + " " + min + " " + serializedPackets.length);
		for(int i=0;i<n;i++)
		{
			try 
			{
				DatagramPacket item = new DatagramPacket(serializedPackets[base+s+i],serializedPackets[base+s+i].length,serverName,serverPort);
				window.push(item);
				sendAndLog(clientSocket,item,(base+s+i)%(WINDOWSIZE+1));
			}catch(IndexOutOfBoundsException e)
			{
				break;
			}
			
		}
		
	}
	
	private static void sendAndLog(DatagramSocket clientSocket,DatagramPacket item,int seqno) throws IOException
	{
		clientSocket.send(item);
		seqlogbuff.append(seqno + "\n");
	}
	
	private static void gbnTransfer(byte[][] serializedPackets, DatagramSocket clientSocket, InetAddress serverName, int serverPort) throws IOException, ClassNotFoundException {
		
		LinkedList<DatagramPacket> window = new LinkedList<DatagramPacket>();
		int base = 0;
		int nextSeq = WINDOWSIZE;
		
		while(base<serializedPackets.length)
		{
			loadWindow(clientSocket,window,serverName,serverPort,serializedPackets,base);
			
			byte[] dataReceived = new byte[MAXBUFFERLEN];
			DatagramPacket ackPack = new DatagramPacket(dataReceived,dataReceived.length);
			
			clientSocket.setSoTimeout(TIMER);
			long timeStarted = System.currentTimeMillis();
			
			try
			{
				packet ack;
				int ackno;
				while(true)
				{
					
					clientSocket.receive(ackPack);
					
					ack = deserializePacket(dataReceived);
					
					ackno = ack.getSeqNum();
					acklogbuff.append(ackno + "\n");
					if(ackno==nextSeq)
					{
						int temp = TIMER-(int)(System.currentTimeMillis()-timeStarted);
						clientSocket.setSoTimeout(temp>0?temp:0);
						continue;
					}
					break;
				}
					
				nextSeq = ackno;
				while(base%(WINDOWSIZE+1)!=ackno)
				{
					base +=1;
					window.removeLast();
				}
				base +=1;
				window.removeLast();
				
			}catch(SocketTimeoutException e)
			{
				for(int i=0;i<window.size();i++)
				{
					DatagramPacket item = window.get(i);
					sendAndLog(clientSocket,item,(base+i)%(WINDOWSIZE+1));
				}
			}
			System.out.println(base+" "+serializedPackets.length);
			
		}

		
		byte[] EOTSerialized = serializePacket(new packet(3,nextSeq,0,null));
		DatagramPacket packetToSend = new DatagramPacket(EOTSerialized,EOTSerialized.length,serverName,serverPort);
		sendAndLog(clientSocket,packetToSend,nextSeq);
		
		byte[] dataToDeserialize = new byte[MAXBUFFERLEN];
		DatagramPacket packetToReceive = new DatagramPacket(dataToDeserialize,dataToDeserialize.length);
		clientSocket.setSoTimeout(0);
		clientSocket.receive(packetToReceive);
		
		packet ack = deserializePacket(dataToDeserialize);
		int ackno = ack.getSeqNum();
		acklogbuff.append(ackno + "\n");
		
		writeFile(seqlog,seqlogbuff.toString());
		writeFile(acklog,acklogbuff.toString());
		
	}
}
