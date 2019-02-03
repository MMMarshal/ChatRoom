import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * A static class that handles all writing responses to client requests.
 * @author marshal foster
 *
 */
public class WriteResponse {
	/**
	 * Composes and writes the response to the a client's HTTP request 
	 * @param file - The file the client requested
	 * @param out - The output stream within the client's socket channel.
	 */
	public static void writeResponse(File file, OutputStream out) {
		try {
			File badRequest = new File("client/badRequest.html");
			// Handling a bad request from a client.
			if(!file.isFile()){
				File file404 = new File("client/error404.html");
				byte[] byteArray = new byte[(int)file404.length()];
				BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file404));
				bufferedInputStream.read(byteArray, 0, byteArray.length);
				bufferedInputStream.close();
				String responce = "HTTP/1.1 404 Not Found\r\n" + "Content-Length: " + byteArray.length + "\r\n";
				out.write(responce.getBytes());
				out.write("\r\n".getBytes());
				out.write(byteArray, 0, byteArray.length);
			} else if (file.equals(badRequest)){
				String responce = "HTTP/1.1 400 Bad Request\r\n" + "Content-Length: " + 0 + "\r\n";
				out.write(responce.getBytes());
				out.write("\r\n".getBytes());
				throw new BadRequestException("Bad request");
				// Handling a valid request from a client.
			} else {
				byte[] byteArray = new byte[(int)file.length()];
				BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
				bufferedInputStream.read(byteArray, 0, byteArray.length);
				bufferedInputStream.close();
				String responce = "HTTP/1.1 200 Ok\r\n" + "Content-Length: " + byteArray.length + "\r\n";
				out.write(responce.getBytes());
				out.write("\r\n".getBytes());
				out.write(byteArray, 0, byteArray.length);
			}
		} catch(IOException e){
			System.out.println("Write responce error.");
			e.printStackTrace();
			return;
		} catch(BadRequestException e){
			System.out.println("Bad request error.");
			e.printStackTrace();
			return;		
		}
	}

	/**
	 * 
	 * @param secWebSocketKey - The Client's specific Websocket key used to encode websocket messages.
	 * @param out - The output stream within the client's socket channel.
	 * @throws IOException
	 */
	public static void webSocketResponse (String secWebSocketKey, OutputStream out) throws IOException{
		// The mess is concatenated with a key provided in the Websocket protocol.
		String concatKey = secWebSocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		try {
			MessageDigest sha1Digest;
			sha1Digest = MessageDigest.getInstance("SHA-1");
			byte[] hashedBytes = sha1Digest.digest(concatKey.getBytes());
			String encoded = Base64.getEncoder().encodeToString(hashedBytes);
			String responce = "HTTP/1.1 101 Switching Protocols\r\n" + "Upgrade: websocket\r\n" + 
			"Connection: Upgrade\r\n" + "Sec-WebSocket-Accept:" + encoded + "\r\n";
			out.write(responce.getBytes());
			out.write("\r\n".getBytes());
			System.out.println("Web socket handshake sent");	
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}