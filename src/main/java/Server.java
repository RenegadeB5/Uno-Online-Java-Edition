import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import org.java_websocket.WebSocket;
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
		String id = ""+ws;
		System.out.println(id + " has connected!");
		this.users.add(new User(id, ws));
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
		List<String> gameIDs = (this.games.size() == 0) ? Arrays.asList("") : this.games.stream()
			.map(game -> game.getID())
			.collect(Collectors.toList());
		Game game = (user.getGameID() != null) ? this.games.get(gameIDs.indexOf(user.getGameID())) : null;
		if (type != 1 && user.getName() == null) {
			return;
		}
		switch (type) {
			case 1:
				user.setName(decoder.getString());
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
						user.setGameID(gameID);
						user.sendMessage("Game successfully created");
					}
				}
				else if (action == 1) {
					if (gameIDs.contains(gameID)) {
						Game gameToJoin = this.games.stream()
							.filter(gme -> gme.getID().equals(gameID))
							.collect(Collectors.toList())
							.get(0);
						gameToJoin.addUser(user);
						user.setGameID(gameID);
						gameToJoin.broadcastUsers();
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
					user.setReady(true);
					game.broadcastUsers();
					game.start();
				}
				break;
			case 6:
				if (game != null) {
					game.remove(ID);
					user.setGameID(null);
					user.setReady(false);
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
		List<String> ids = this.users.stream()
			.map(usr -> usr.getID())
			.collect(Collectors.toList());
		User user = this.users.stream()
			.filter(usr -> usr.getID().equals(id))
			.collect(Collectors.toList())
			.get(0);
		if (user.getGameID() != null) {
			Game game = this.games.stream()
				.filter(gme -> gme.getID().equals(user.getGameID()))
				.collect(Collectors.toList())
				.get(0);
			List<String> gameIDs = this.games.stream()
				.map(gme -> gme.getID())
				.collect(Collectors.toList());
			game.remove(user.getID());
			game.broadcastMessage(user.getName() + " has left!");
			if (game.playerCount() == 1) {

			}
			else if (game.playerCount() == 0) {
				int index = gameIDs.indexOf(user.getGameID());
				this.games.remove(index);
			}
			this.ongoingGames -= 1;
		}
		int index = ids.indexOf(id);
		this.users.remove(index);
		this.connections -= 1;
	}
}
