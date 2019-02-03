/**
 * Represents a message sent from a client.
 * @author marshal foster
 *
 */
public class Message {
	public byte length;
	public String message;
	public Message(String s, int payloadLen) {
		message = new String(s);
		length = (byte) payloadLen;
	}
}
