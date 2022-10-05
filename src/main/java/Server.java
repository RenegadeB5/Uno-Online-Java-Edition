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

interface Worker {
	void execute(Decoder decoder, String ID, User user, List<String> gameIDs, Game game, ArrayList<Game> games);
}

public class Server extends WebSocketServer {
	private int connections;
	private int ongoingGames;
	private ArrayList<User> users;
	private ArrayList<Game> games;
	private Map<Integer, Worker> functions;

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
		this.functions = new HashMap<Integer, Worker>();
		this.gameIDs = new ArrayList<String>;


		Worker func1 = new Worker() {
			public void execute(Decoder decoder, String ID, User user, List<String> gameIDs, Game game, ArrayList<Game> games) {
				user.setName(decoder.getString());
			}
		};
		Worker func2 = new Worker() {
			public void execute(Decoder decoder, String ID, User user, List<String> gameIDs, Game game, ArrayList<Game> games) {
				int action = decoder.getInt();
				String gameID = decoder.getString();
				if (action == 0) {
					if (gameIDs.contains(gameID)) {
						user.sendMessage("That ID isn't available!");
					}
					else if (user.getGame() != null) {
						user.sendMessage("You are already in a game!");
					}
					else {
						Game new_game = new Game(gameID, user);
						games.add(new_game);
						user.setGame(new_game);
						user.sendMessage("Game successfully created");
						this.updateGameIDs();
					}
				}
				else if (action == 1) {
					if (gameIDs.contains(gameID) && user.getGame() == null) {
						Game gameToJoin = games.stream()
							.filter(gme -> gme.getID().equals(gameID))
							.collect(Collectors.toList())
							.get(0);
						gameToJoin.addUser(user);
						user.setGame(gameToJoin);
						gameToJoin.broadcastUsers();
					}
					else {
						user.sendMessage("That game ID doesn\'t exist!");
					}
				}
				else {
					System.out.println("The server got a message it doesn't know about: header 2");
				}
			}
		};
		Worker func3 = new Worker() {
			public void execute(Decoder decoder, String ID, User user, List<String> gameIDs, Game game, ArrayList<Game> games) {
				if (game != null) {
					String message = decoder.getString();
					game.broadcastMessage(ID, user.getName(), message);
				}
			}
		};
		Worker func4 = new Worker() {
			public void execute(Decoder decoder, String ID, User user, List<String> gameIDs, Game game, ArrayList<Game> games) {
				if (game != null) {
					List<String> cards = new ArrayList<String>();
					int amount = decoder.getInt();
					for (int i = 0; i < amount; i++) {
						String card = decoder.getString();
						if (card.equals("draw")) {
							game.deal(user.getID(), 1);
							game.updateTurn();
							return;
						} 
						else {
							cards.add(card);
						}
					}
					game.play(ID, cards);
				}
			}
		};
		Worker func5 = new Worker() {
			public void execute(Decoder decoder, String ID, User user, List<String> gameIDs, Game game, ArrayList<Game> games) {
				if (game != null) {
					user.setReady(true);
					game.broadcastUsers();
					game.start();
				}
			}
		};
		Worker func6 = new Worker() {
			public void execute(Decoder decoder, String ID, User user, List<String> gameIDs, Game game, ArrayList<Game> games) {
				if (game != null) {
					game.remove(ID);
					user.setGameID(null);
					user.setReady(false);
				}
			}
		};
		this.functions.put(1, func1);
		this.functions.put(2, func2);
		this.functions.put(3, func3);
		this.functions.put(4, func4);
		this.functions.put(5, func5);
		this.functions.put(6, func6);
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
		String id = ""+ws;
		User user = this.getUser(id);
		List<String> gameIDs = this.gameIDs;
		Game game = user.getGame();
		if (type != 1 && user.getName() == null) {
			return;
		}
		if (type != 0) this.functions.get(type).execute(decoder, id, user, gameIDs, game, this.games);
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
		User user = this.getUser(id);
		if (user.getGame() != null) {
			Game game = user.getGame();
			List<String> gameIDs = this.gameIDs;
			game.remove(user.getID());
			game.broadcastMessage(user.getName() + " has left!");
			if (game.playerCount() == 0) {
				int index = gameIDs.indexOf(user.getGameID());
				this.games.remove(index);
				this.ongoingGames -= 1;
				this.updateGameIDs();
			}
		}
		int index = ids.indexOf(id);
		this.users.remove(index);
		this.connections -= 1;
	}

	public User getUser(String id) {
		User user = this.users.stream()
			.filter(usr -> usr.getID().equals(id))
			.collect(Collectors.toList())
			.get(0)
			.orElse(null);
		return user;
	}

	public void updateGameIDs() {
		this.gameIDs = this.games.stream()
			.map(gme -> gme.getID())
			.collect(Collectors.toList())
			.orElse(null);
	}
}
