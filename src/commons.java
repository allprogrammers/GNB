/*
 * Muhammad Hamza Ali
 * ma1973
 * Programming Assignment 2 commons.java
 * 
 * pretty self explanatory
 * 
 * help taken from JAVA docs
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class commons {
	
	public static void writeFile(String filename, String filecontent) throws IOException
	{
		FileWriter fw = new FileWriter(filename,false);//can also buffer the output to the writing buffer so that all of the file is not held in the memory
		fw.write(filecontent);
		fw.close();
	}

	public static byte[] serializePacket(packet data) throws IOException {
		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		ObjectOutputStream oostream = new ObjectOutputStream(ostream);
		
		oostream.writeObject(data);
		oostream.close();
		
		byte[] toReturn = ostream.toByteArray();
		ostream.close();
		
		return toReturn;
	}
	
	public static packet deserializePacket(byte[] dataToDeserialize) throws IOException, ClassNotFoundException {
		ByteArrayInputStream iStream = new ByteArrayInputStream(dataToDeserialize);
		ObjectInputStream oiStream = new ObjectInputStream(iStream);
		
		packet toReturn = (packet) oiStream.readObject();
		
		iStream.close();
		oiStream.close();
		
		return toReturn;
	}
	
	public static byte[][] serializePacketizedFile(String fileName, int maxreadsize,int windowsize) throws IOException
	{
		packet[] packets = packetizeFile(fileName,maxreadsize,windowsize);
		byte[][] toReturn = new byte[packets.length][];
		
		for(int i=0;i<packets.length;i++)
		{
			toReturn[i] = serializePacket(packets[i]);
		}
		
		return toReturn;
	}
	
	public static packet[] packetizeFile(String fileName, int maxreadsize,int windowsize) throws IOException{
		
		String fileContent = readFile(fileName);//can also read file in chunks of 30 characters if the file is too big
		packet[] packets = new packet[(int)((fileContent.length()*1.0)/maxreadsize + 0.5)];
		int ind=0;
		packets[ind] = new packet(1,0,0,null);
		for(int i=0;i<fileContent.length();i+=maxreadsize)
		{
			ind = i/maxreadsize;
			try
			{
				packets[ind] = new packet(1,ind%(windowsize+1),maxreadsize,fileContent.substring(i, i+maxreadsize));
			}catch(IndexOutOfBoundsException e)
			{
				packets[ind] = new packet(1,ind%(windowsize+1),fileContent.length()-i, fileContent.substring(i, fileContent.length()));
				break;
			}
		}
		
		return packets;
	}
	
	public static String readFile(String fileName) throws IOException {
		String content = new String(Files.readAllBytes(Paths.get(fileName)),StandardCharsets.US_ASCII);
		return content;
	}
	
	public static void sendAndLog(DatagramSocket clientSocket,DatagramPacket item,StringBuilder seqlogbuff,int seqno) throws IOException, ClassNotFoundException
	{
		clientSocket.send(item);
		seqlogbuff.append(seqno + "\n");
	}
}
