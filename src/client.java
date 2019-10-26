import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.LinkedList;

public class client {
	
	private static final int MAXBUFFERLEN = 3000;
	private static final int MAXREADSIZE = 30;
	private static final int EXTRAPACKETS = 0;
	private static final int WINDOWSIZE = 7;
	private static final int TIMER = 500;
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

	private static void talkOn(DatagramSocket clientSocket, String serverName, int serverPort, String fileName) throws IOException, ClassNotFoundException {
		
		byte[][] serializedPackets = commons.serializePacketizedFile(fileName,MAXREADSIZE,EXTRAPACKETS,WINDOWSIZE);
		InetAddress serverHost = InetAddress.getByName(serverName);
		
		gbnTransfer(serializedPackets,clientSocket,serverHost,serverPort);
	}

	private static void loadWindow(DatagramSocket clientSocket, LinkedList<DatagramPacket> window,InetAddress serverName, int serverPort, byte[][] serializedPackets, int base) throws IOException, ClassNotFoundException {
		
		int n = WINDOWSIZE-window.size();
		int s = window.size();
		for(int i=0;i<n;i++)
		{
			try 
			{
				DatagramPacket item = new DatagramPacket(serializedPackets[base+s+i],serializedPackets[base+s+i].length,serverName,serverPort);
				window.add(item);
				commons.sendAndLog(clientSocket,item,seqlogbuff,(base+s+i)%(WINDOWSIZE+1));
			}catch(IndexOutOfBoundsException e)
			{
				break;
			}
			
		}
		
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
					
					ack = commons.deserializePacket(dataReceived);
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
				seqlogbuff.append("timeout here \n");
				for(int i=0;i<window.size();i++)
				{
					DatagramPacket item = window.get(i);
					commons.sendAndLog(clientSocket,item,seqlogbuff,(base+i)%(WINDOWSIZE+1));
				}
			}
			
		}
		
		int last = base%(WINDOWSIZE+1);
		byte[] EOTSerialized = commons.serializePacket(new packet(3,last,0,null));
		DatagramPacket packetToSend = new DatagramPacket(EOTSerialized,EOTSerialized.length,serverName,serverPort);
		commons.sendAndLog(clientSocket,packetToSend,seqlogbuff,last);
		
		byte[] dataToDeserialize = new byte[MAXBUFFERLEN];
		DatagramPacket packetToReceive = new DatagramPacket(dataToDeserialize,dataToDeserialize.length);
		clientSocket.setSoTimeout(0);
		clientSocket.receive(packetToReceive);
		
		packet ack = commons.deserializePacket(dataToDeserialize);
		int ackno = ack.getSeqNum();
		acklogbuff.append(ackno + "\n");
		
		commons.writeFile(seqlog,seqlogbuff.toString());
		commons.writeFile(acklog,acklogbuff.toString());
		
	}
}
