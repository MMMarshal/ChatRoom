import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;


/**
 * This class represents a chatroom and manages all actions
 * Taken within the chatroom. As this server is based on a thread per room
 * model this single thread will manages the requested from multiple SocketChannles,
 * relaying on synchronization to avoid race conditions.
 * @author marshal foster
 *
 */
public class ChatRoom {
	private String roomName = new String();
	private Selector selector = Selector.open();
	ArrayList<SocketChannel> clientsInRoom = new ArrayList<SocketChannel>();
	private ArrayList<SocketChannel> newClients = new ArrayList<SocketChannel>();
	private ArrayList<Message> messageHistory = new ArrayList<Message>();

	/**
	 * @param chatRoomName - The name of the chatroom
	 * @throws IOException
	 */
	public ChatRoom(String chatRoomName) throws IOException {
		roomName = chatRoomName;
	}
	
	/**
	 * Adds a to the newClients array.
	 * This array will be checked every iteration of listen()
	 * and any socketChannels within newClients will be registered
	 * within the chatroom.
	 * @param clientSC - the client's socket channel to be added to to the arraylist of clients.
	 * @throws IOException
	 */
	public synchronized void addClient(SocketChannel clientSC) throws IOException {
		newClients.add(clientSC);
		//used to synchronize the thread when a client is added.
		selector.wakeup();
	}

	/**
	 * Listens for client's messages and broadcasts that message to the room.
	 * This is the driving method for the room.
	 */
	public void listen(){
		while(newClients.size() > 0 || clientsInRoom.size() > 0) {
			try {
				registerNewClients();
				int readyChannels = selector.select();
				if(readyChannels == 0) 
					continue;
			} catch (IOException e1) {
				System.out.println("Register client or selector error.");
				if(checkToCloseRoom()) return;
				e1.printStackTrace();
			}
			Set<SelectionKey> selectionKeySet = selector.selectedKeys();
			Iterator<SelectionKey> keyIterator = selectionKeySet.iterator();		
			while(keyIterator.hasNext()) { 
				SelectionKey selKey = keyIterator.next();
				if (selKey.isReadable()) {
					try {
						keyIterator.remove();
            SocketChannel sockChnl = (SocketChannel) selKey.channel();
            selKey.cancel();
            sockChnl.configureBlocking(true);
            Message message = ReadRequest.readMessage(sockChnl);
            if(checkClientClosed(message, selKey)) {
            	if(checkToCloseRoom()) 
            		return;
            	continue;
            } 
            sockChnl.configureBlocking(false);
            selector.selectNow();
            selKey.channel().register(selector, SelectionKey.OP_READ);
            messageHistory.add(message);
            broadCast(message);
					} catch (IOException e) {
						System.out.println("Client output stream creation error.");
            clientsInRoom.remove(selKey.channel());
            selKey.cancel();
            System.out.println("Client reomved. Clients in room: " + clientsInRoom.size());
            if(checkToCloseRoom()) {
            	System.out.println("No clients in room. Closing room.");
            	return;
            }
            e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Broadcasts a messages received from any client to
	 * everyone within the chatroom.
	 * @param m - The message to be broadcast.
	 * @throws IOException
	 */
	private void broadCast(Message m) throws IOException {
		for(SocketChannel c:clientsInRoom){
			System.out.println("Broadcasting");
      SelectionKey key = c.keyFor(selector);
      key.cancel();
      c.configureBlocking(true);
      OutputStream out = c.socket().getOutputStream();
      //magic number for first byte 10000001;
			out.write(0x81);
			out.write(m.length);
		 	out.write(m.message.getBytes());
		 	out.flush();
      c.configureBlocking(false);
      selector.selectNow();
      key.channel().register(selector, SelectionKey.OP_READ);
    }	
	}

	/**
	 * Configures all socket channels within the newClients array,
	 * sends the new clients past chat history, and clears the
	 * newClients array.
	 * @throws IOException
	 */
	public synchronized void registerNewClients() throws IOException {
		for(SocketChannel newSC : newClients) {
			sendMessageHistory(newSC);
			newSC.configureBlocking(false);
			newSC.register(selector, SelectionKey.OP_READ);
			clientsInRoom.add(newSC);
			System.out.println("New client added");
			System.out.println();
		}
		newClients.clear();	
	}
	
	/**
	 * Checks a client message for an empty payload.
	 * If detected the client has left the room.
	 * @param message - The client's message
	 * @param selKey - The selection key used to associate a Socket channel to a client.
	 * @return True if the message had a length of zero, else false.
	 */
	private boolean checkClientClosed(Message message, SelectionKey selKey) {
		if (message.length == 0) {
			clientsInRoom.remove(selKey.channel());
  		selKey.cancel();
  		System.out.println("Client reomved. Clients in room: " + clientsInRoom.size());
  		return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Checks if the clients if there are 0 clients in the room.
	 * @return - True if the size of clientsInRoom is zero, else false
	 */
	private boolean checkToCloseRoom() {
		return clientsInRoom.size() == 0;
	}
	
	/**
	 * Sends all previous chatroom message history to a client.
	 * @param client - The SocketChannel of the client to be send the
	 * chat history to.
	 * @throws IOException
	 */
	private void sendMessageHistory(SocketChannel client) throws IOException {
		OutputStream out = client.socket().getOutputStream();
		for(int i = 0; i < messageHistory.size(); i++) {
			out.write(0x81);
			out.write(messageHistory.get(i).length);
		 	out.write(messageHistory.get(i).message.getBytes());
		 	out.flush();
		 	System.out.println("Sent history message");
		}
	}
}