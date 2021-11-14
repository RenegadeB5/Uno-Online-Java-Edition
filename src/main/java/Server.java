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
	private ArrayList<Game> games;

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
		this.games = new ArrayList<Game>();
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
		String ID = ""+ws;
		User user = this.users.stream()
			.filter(usr -> usr.getID().equals(ID))
			.collect(Collectors.toList())
			.get(0);
		List<String> gameIDs = this.games.stream()
			.map(game -> game.getID())
			.collect(Collectors.toList());
		Game game = null;
		if (user.getGameID() != null) {
			game = this.games.get(gameIDs.indexOf(user.getGameID()));
		}
		switch (type) {
			case 1:
				this.users.add(new User(ID, decoder.getString(), ws));
				break;
			case 2:
				int action = decoder.getInt();
				String gameID = decoder.getString();
				if (action == 0) {
					if (gameIDs.contains(gameID)) {
						user.sendMessage("That ID isn't available!");
					}
					else {
						Game new_game = new Game(gameID, user);
						this.games.add(new_game);
						user.sendMessage("Game successfully created");
					}
				}
				else if (action == 1) {
					if (gameIDs.contains(gameID)) {
						game.addUser(user);
						game.broadcastUsers();
					}
					else {
						user.sendMessage("That game ID doesn\'t exist!");
					}
				}
				else {
					System.out.println("The server got a message it doesn't know about: header 2");
				}
				break;
			case 3:
				if (game != null) {
					String message = decoder.getString();
					game.broadcastMessage(ID, message);
				}
				break;
			case 4:
				List<String> cards = new ArrayList<String>();
				int amount = decoder.getInt();
				for (int i = 0; i < amount; i++) {
					cards.add(decoder.getString());
				}
				if (game != null) {
					game.play(ID, cards);
				}
				break;
			case 5:
				if (game != null) {
					game.remove(ID);
					user.setGameID(null);
					user.setReady(false);
				}
				break;
			case 6:
				if (game != null) {
					user.setReady(true);
					game.broadcastUsers();
					game.start();
				}
				break;
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
				.filter(gme -> gme.getID().equals(user.getGameID()))
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
