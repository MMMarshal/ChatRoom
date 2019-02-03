import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Scanner;


/**
 * A static class that handles all reading and decoding of client messages.
 * @author marshal foster
 *
 */
public class ReadRequest {
	/**
	 * Interprets the client's HTTP page request and handles any requests of unknown pages.
	 * @param readIn - The client's request.
	 * @return a File containing the requested page or the bad request page if the requested
	 * page was not found.
	 */
	public static File readRequest(Scanner readIn) {
			// Reads client request &  splitting request line to isolate the requested file.
			String requestLine = readIn.nextLine();	
			String[] splitRequestLine = requestLine.split(" ");	
			if (splitRequestLine.length < 3) {
				File badRequest = new File("client/badRequest.html");
				return badRequest;
			}	
			// Isolating the requested file.
			String fileRequest = splitRequestLine[1];
			System.out.println("The client is requesting: " + fileRequest);
			File file = new File("client" + fileRequest);
			// Checking if client requested root directory.
			if (fileRequest.equals("/")){
				File homeFile = new File("client/webClient.html");
				return homeFile;
			} else {
				return file;	
			}	
	}
		
	/**
	 * Parses the client's HTTP header after the request line is read.
	 * Is used in the Management class to determine
	 * if the client has requested to upgrade to the Websocket protocol.
	 * Should be called with the same Scanner after calling readRequest().
	 * @param readIn - The client's request.
	 * @return A hashmap with the field names as the keys and the field values as the values.
	 * @throws IOException
	 */
	public static HashMap<String, String> readHeader(Scanner readIn) throws IOException {	
		HashMap<String, String> header = new HashMap<String, String>();
			while(true) {
				String line = readIn.nextLine();
				if(line.equals("")){
					break;	
				} else {
					String[] splitHeader = line.split(": ");
					header.put(splitHeader[0], splitHeader[1]);
				}
			}
			return header;
	}
	
	/**
	 * Interprets the client's based on WebSocket protocol.
	 * @param clientSC - The client's socket channel.
	 * @return A Message object contain the plain text message.
	 * @throws IOException
	 */
	public static Message readMessage(SocketChannel clientSC) throws IOException{
		DataInputStream dataIn = new DataInputStream(clientSC.socket().getInputStream());
		byte[] msg = new byte[2];
			dataIn.read(msg,0,2);	
		int secondByte = msg[1]&0xff;
		//System.out.println("first byte unsign " + (msg[0]&0xff));
		//System.out.println("second byte unsign " + secondByte);	
		int payloadLen = 0;	
		if((secondByte - 128) <= 125) {
			payloadLen = secondByte - 128;
		} else if ((secondByte - 128) == 126 ) {
			//In progress
				//byte[] msgLng = new byte[2];
				//dataIn.read(msgLng,0,2);
				//payloadLen = ((msgLng[0] << 8) | (msgLng[1] & 0xFF));	
		} else if ((secondByte - 128) == 127){
			//In progress
		} else {
			System.out.println("Error reading client message");
			return null;
		}
		System.out.println("length is "+payloadLen);	
		byte[] key = new byte[4];
		dataIn.read(key,0,4);	
		
		String message = new String();
		if(payloadLen > 0) {
			byte[] decoded = new byte[payloadLen];
			byte[] encoded = new byte[payloadLen];
			dataIn.read(encoded);
			for(int i = 0; i < payloadLen; i++) {
				decoded[i] = (byte)(encoded[i] ^ key[i % 4]);
			}
		message = new String(decoded);
		System.out.println("message is " + message);
		if (payloadLen == 0)
			message = "null";
		}
		Message send = new Message(message, payloadLen);
		return send;
	}
}