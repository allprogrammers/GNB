/*
 * Muhammad Hamza Ali
 * ma1973
 * Programming Assignment 2 client.java
 * 
 * help taken from lecture slides.
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.LinkedList;

public class client {
	
	private static final int MAXBUFFERLEN = 3000;
	private static final int MAXREADSIZE = 30;
	private static final int WINDOWSIZE = 7;
	private static final int TIMER = 2000;// 2 seconds
	private static final String seqlog = "clientseqnum.log";
	private static final String acklog = "clientack.log";
	
	private static StringBuilder seqlogbuff= new StringBuilder();
	private static StringBuilder acklogbuff = new StringBuilder();
	
	/*
	 * deals with the arguments creates socket and communicates using gbn on the socket
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		String emulatorName = args[0];
		int emulatorPort = Integer.parseInt(args[1]);
		int clientPort = Integer.parseInt(args[2]);
		String fileName = args[3];
		
		DatagramSocket clientSocket = new DatagramSocket(clientPort);
		
		talkOn(clientSocket,emulatorName,emulatorPort,fileName);
		
		clientSocket.close();
	}
	
	/*
	 * takes the input file and creates packets
	 * serializes those packets
	 * transfers those packets using gbn to ther server
	 */
	private static void talkOn(DatagramSocket clientSocket, String serverName, int serverPort, String fileName) throws IOException, ClassNotFoundException {
		
		byte[][] serializedPackets = commons.serializePacketizedFile(fileName,MAXREADSIZE,WINDOWSIZE);
		InetAddress serverHost = InetAddress.getByName(serverName);
		
		gbnTransfer(serializedPackets,clientSocket,serverHost,serverPort);
	}
	
	/*
	 * fills up the window with more packets and send them if there are more packets available and there is space in the window otherwise does nothing
	 */
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
	
	/*
	 * implements the gbn protocol
	 */
	private static void gbnTransfer(byte[][] serializedPackets, DatagramSocket clientSocket, InetAddress serverName, int serverPort) throws IOException, ClassNotFoundException {
		
		LinkedList<DatagramPacket> window = new LinkedList<DatagramPacket>();
		int base = 0;
		int nextSeq = WINDOWSIZE;
		long timeStarted=System.currentTimeMillis();
		int remainingTime = 0;
		
		while(base<serializedPackets.length)
		{
			//while there are more packets it keeps window full
			loadWindow(clientSocket,window,serverName,serverPort,serializedPackets,base);
			
			byte[] dataReceived = new byte[MAXBUFFERLEN];
			DatagramPacket ackPack = new DatagramPacket(dataReceived,dataReceived.length);
			
			clientSocket.setSoTimeout(TIMER);
			
			//(re)starts the timer
			if(remainingTime<=0)
			{
				timeStarted = System.currentTimeMillis();
			}
			
			//tries to get an ack before the timeout
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
					
					//if duplicate ack prevents timer from being changed
					if(ackno==nextSeq)
					{
						remainingTime = TIMER-(int)(System.currentTimeMillis()-timeStarted);
						
						//timeout management
						if(remainingTime<=0)
							throw new SocketTimeoutException();
						else
							clientSocket.setSoTimeout(remainingTime); 
						continue;
					}
					break;
				}
				
				//updates the base if ack is not duplicate
				nextSeq = ackno;
				while(base%(WINDOWSIZE+1)!=ackno)
				{
					base +=1;
					window.removeFirst();
				}
				base +=1;
				window.removeFirst();
				
			}catch(SocketTimeoutException e)//resends the window on timeout
			{
				for(int i=0;i<window.size();i++)
				{
					DatagramPacket item = window.get(i);
					commons.sendAndLog(clientSocket,item,seqlogbuff,(base+i)%(WINDOWSIZE+1));
				}
			}
			
		}
		
		//creates and sends the final EOT packet
		//waits for the EOT from the server
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
		
		//writes the logs to the file
		acklogbuff.append(ackno + "\n");
		commons.writeFile(seqlog,seqlogbuff.toString());
		commons.writeFile(acklog,acklogbuff.toString());
		
	}
}
