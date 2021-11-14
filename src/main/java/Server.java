import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.util.*;
import java.io.*;
import java.lang.*;
import java.util.stream.*;
import packages.*;

public class Server extends WebSocketServer {
	private int connections;
	private int ongoingGames;
	private ArrayList<User> users;
	//private ArrayList<Game> games;

	public static void main(String[] args) throws InterruptedException, IOException {
		Server server = new Server(Integer.parseInt(System.getenv("PORT")));
		server.start();
		System.out.println("Server started on port: " + server.getPort());
	}

	public Server(int port) throws UnknownHostException {
		super(new InetSocketAddress(Integer.parseInt(System.getenv("PORT"))));
		this.connections = 0;
		this.ongoingGames = 0;
		this.users = new ArrayList<User>();
		//this.games = new ArrayList<Game>();
	}
	
	public void gameBroadcast(String gameID, ByteBuffer packet) {
		List<User> users = this.users.stream()
			.filter(usr -> usr.getGameID().equals(gameID))
			.collect(Collectors.toList());
	}

	@Override
	public void onOpen(WebSocket ws, ClientHandshake hs) {
		System.out.println(ws + " has connected!");
		Encoder encoder = new Encoder();
		// 1 for sending name for now
		encoder.addInt(1);
		encoder.addString(""+ws);
		ws.send(encoder.finish());
		this.connections += 1;
	}
	
	@Override
	public void onMessage(WebSocket ws, ByteBuffer packet) {
		Decoder decoder = new Decoder(packet);
		int type = decoder.getInt();
		switch (type) {
			case 1: {
				String ID = ""+ws;
				this.users.add(new User(ID, decoder.getString(), ws));
				break;
			}
		}
	}
	
	@Override
	public void onMessage(WebSocket ws, String message) {
		System.out.println(message);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
	}

	@Override
	public void onStart() {
		setConnectionLostTimeout(100);
	}

	@Override
	public void onClose(WebSocket ws, int code, String reason, boolean remote) {
		String id = ""+ws;
		System.out.println(id + " has left!");
		List<User> users = this.users.stream()
			.filter(usr -> usr.getID().equals(id))
			.collect(Collectors.toList());
		if (users.size() != 0) {
			List<String> ids = this.users.stream()
				.map(usr -> usr.getID())
				.collect(Collectors.toList());
			int index = ids.indexOf(id);
			User user = this.users.get(index);
			if (user.getGameID() != null) {
				Game game = this.games.stream()
				.filter(game -> game.getID().equals(user.getGameID()))
				.collect(Collectors.toList())
				.get(0);
				game.remove(user.getID());
				game.broadcastMessage(user.getName() + " has left!");
				this.ongoingGames -= 1;
			}
			this.users.remove(index);
		}
		this.connections -= 1;
	}
}
