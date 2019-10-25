import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class common {
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
}
