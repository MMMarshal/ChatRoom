import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

/**
 * This class drives the server with its main responsibility of
 * managing clients and creating and removing rooms.
 * @author marshal foster
 *
 */
public class Management {
	// All chat-rooms being hosted.
	private static HashMap<String, ChatRoom> rooms = new HashMap<String, ChatRoom>();
	private static ServerSocketChannel serverSocket = null;

	/**
	 * Starts the serve socket.
	 * Must be called before runServerSocket().
	 */
	public static void openServerSocket() {
		try {
			serverSocket = ServerSocketChannel.open();
      serverSocket.bind(new InetSocketAddress(8080));
		} catch(IOException e){
      System.out.println("Server socket creation error.");
      e.printStackTrace();
      System.exit(1);
		}
	}
	
	/**
	 * The main driving function for the server.
	 */
	public static void runServerSocket() {
		while(serverSocket.isOpen()) {
			try {
				// Creating client socket & gathering client input.
        SocketChannel clientSocketChnl = serverSocket.accept();
        Scanner input = new Scanner(clientSocketChnl.socket().getInputStream());
        File file = ReadRequest.readRequest(input);
        HashMap<String, String> header = ReadRequest.readHeader(input);
        OutputStream output = clientSocketChnl.socket().getOutputStream();
            	
        if (header.containsKey("Upgrade") && header.get("Upgrade").equals("websocket"))
        	placeClient(header, output, clientSocketChnl);
        else {
        	WriteResponse.writeResponse(file, output);
          output.flush();
          clientSocketChnl.socket().close();
        }
			} catch (IOException e) {
				System.out.println("Client socket creation error.");
        e.printStackTrace();
        System.exit(1);
      }
		}
	}
	
	/**
	 * Starts the process of adding a client to a room.
	 * Will read parsed client request and then start a new thread
	 * which will add the client into the requested chatroom.
	 * @param header - A parsed version of the client's request header containing 
	 * the room they want to join.
	 * @param output - The output stream from the server socket to the client socket.
	 * @param clientSocketChnl - The client's socket channel.
	 * @throws IOException
	 */
	private static void placeClient(final HashMap<String, String> header, final OutputStream output, 
			final SocketChannel clientSocketChnl) throws IOException {
		WriteResponse.webSocketResponse(header.get("Sec-WebSocket-Key"), output);	
		Thread newThread = new Thread(() -> {addClientToRoom(clientSocketChnl);});
		newThread.start();
		System.out.println("New addToRoom thread started");   		
	}
	
	/**
	 * Will remove any empty rooms from the rooms hashmap.
	 */
	@SuppressWarnings("rawtypes")
	private static void removeEmptyRooms() {
		Iterator<Entry<String, ChatRoom>> it = rooms.entrySet().iterator();
    	while (it.hasNext()) {
    		Map.Entry pair = (Map.Entry)it.next();
    		if(rooms.get(pair.getKey()).clientsInRoom.size() == 0) {
    			System.out.println("Room Name: " + pair.getKey() + " sucessfully closed");
    			rooms.remove(pair.getKey());
    		}
    	}	
	}
	
	/**
	 * Called when a client has requested to be placed within a new room which
	 * is not being hosted.
	 * This will create the new room and place the client inside of that room.
	 * @param roomName - The room name the client requested to join.
	 * @param initalClient - The client who wises to start and join the new room.
	 * @throws IOException
	 */
	private static void createNewRoom(final String roomName, final SocketChannel initalClient ) throws IOException {
		ChatRoom newRoom = new ChatRoom(roomName);
		newRoom.addClient(initalClient);
		Thread roomThread = new Thread(() -> {newRoom.listen();});
		roomThread.start();
		rooms.put(roomName, newRoom);
		System.out.println(roomName + " thread started");
	}
	
	/**
	 * Finalizes the process of adding a client to a room.
	 * @param clientSocketChnl - The client's socket channel.
	 */
	private static void addClientToRoom(final SocketChannel clientSocketChnl){ 
		removeEmptyRooms();
    	try {
    		Message requestedRoom = ReadRequest.readMessage(clientSocketChnl);
    		String[] splitMessage = requestedRoom.message.split(" ");
    		if (!splitMessage[0].equals("join")) {
    			System.out.println(splitMessage[0] + splitMessage[1]);
    			return;
    		}
    		if(rooms.containsKey(splitMessage[1]))
    			// Add too existing room
    			rooms.get(splitMessage[1]).addClient(clientSocketChnl);
    		else
    			// Create new room & add client to it
    			createNewRoom(splitMessage[1],clientSocketChnl);
		} catch (IOException e) {
			System.out.println("Error adding client to room");
			e.printStackTrace();
		}
  }
}
