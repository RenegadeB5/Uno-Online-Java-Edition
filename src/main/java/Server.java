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
	void execute(Decoder decoder, String ID, User user, List<String> gameIDs, ArrayList<Game> games, Server server);
}

public class Server extends WebSocketServer {
	private int connections;
	private int ongoingGames;
	private ArrayList<User> users;
	private ArrayList<Game> games;
	private Map<Integer, Worker> functions;
	private List<String> gameIDs;

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
		this.gameIDs = new ArrayList<String>();


		Worker func1 = new Worker() {
			public void execute(Decoder decoder, String ID, User user, List<String> gameIDs, ArrayList<Game> games, Server server) {
				user.setName(decoder.getString());
			}
		};
		Worker func2 = new Worker() {
			public void execute(Decoder decoder, String ID, User user, List<String> gameIDs, ArrayList<Game> games, Server server) {
				int action = decoder.getInt();
				int gameNum = decoder.getInt();
				String gameID = decoder.getString();
				if (action == 0) {
					if (gameIDs.contains(gameID)) {
						user.sendMessage("That ID isn't available!", 2);
					}
					else if (user.getGame() != null) {
						user.sendMessage("You are already in a game!", 2);
					}
					else {
						if (gameNum == 1) {
							UnoGame new_game = new UnoGame(gameID, user);
							new_game.setGameNumber(1);
							games.add(new_game);
							server.updateGameIDs();
							user.setGame(new_game);
							user.setGameNum(1);
							user.sendMessage("Game successfully created", 1);
							Encoder encoder = new Encoder();
							encoder.addInt(2);
							encoder.addInt(1);
							user.send(encoder.finish());
						}
						else if (gameNum == 2) {
							TicTacToeGame new_game = new TicTacToeGame(gameID, user);
							new_game.setGameNumber(2);
							games.add(new_game);
							server.updateGameIDs();
							user.setGame(new_game);
							user.setGameNum(2);
							user.sendMessage("Game successfully created", 1);
							Encoder encoder = new Encoder();
							encoder.addInt(2);
							encoder.addInt(2);
							user.send(encoder.finish());
						}
					}
				}
				else if (action == 1) {
					if (gameIDs.contains(gameID) && user.getGame() == null) {
						Game game = server.games.get(gameIDs.indexOf(gameID));
						Encoder encoder = new Encoder();
						encoder.addInt(2);
						encoder.addInt(game.getGameNumber());
						user.send(encoder.finish());
						game.addUser(user);
						user.setGame(game);
						user.setGameNum(gameNum);
						user.sendMessage("Sucessfully joined game!", 1);
					}
					else {
						user.sendMessage("That game ID doesn\'t exist!", 3);
					}
				}
				else {
					System.out.println("The server got a message it doesn't know about: header 2");
				}
			}
		};
		Worker func3 = new Worker() {
			public void execute(Decoder decoder, String ID, User user, List<String> gameIDs, ArrayList<Game> games, Server server) {
				if (user.getGame() != null) {
					String message = decoder.getString();
					user.getGame().broadcastMessage(ID, user.getName(), message);
				}
			}
		};
		Worker func4 = new Worker() {
			public void execute(Decoder decoder, String ID, User user, List<String> gameIDs, ArrayList<Game> games, Server server) {
				if (user.getGame() != null) {
					List<String> cards = new ArrayList<String>();
					int gameNum = decoder.getInt();
					if (gameNum == 1) {
						UnoGame game = (UnoGame)user.getGame();
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
					else if (gameNum == 2) {
						TicTacToeGame game = (TicTacToeGame)user.getGame();
						game.play(user.getID(), decoder.getInt());
					}
				}
			}
		};
		Worker func5 = new Worker() {
			public void execute(Decoder decoder, String ID, User user, List<String> gameIDs, ArrayList<Game> games, Server server) {
				if (user.getGame() != null) {
					if (user.getGameNum() == 1) {
						UnoGame game = (UnoGame)user.getGame();
						user.setReady(true);
						game.broadcastUsers();
						game.start();
					}
					else if (user.getGameNum() == 2) {
						TicTacToeGame game = (TicTacToeGame)user.getGame();
						user.setReady(true);
						game.broadcastUsers();
						game.start();
					}
				}
			}
		};
		Worker func6 = new Worker() {
			public void execute(Decoder decoder, String ID, User user, List<String> gameIDs, ArrayList<Game> games, Server server) {
				if (user.getGame() != null) {
					user.getGame().remove(ID);
					user.setGame(null);
					user.setGameNum(0);
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
		if (type != 1 && user.getName() == null) {
			return;
		}
		if (type != 0) this.functions.get(type).execute(decoder, id, user, gameIDs, this.games, this);
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
		int index = ids.indexOf(id);
		System.out.println(index);
		User user = this.getUser(id);
		if (user.getGame() != null) {
			Game game = user.getGame();
			List<String> gameIDs = this.gameIDs;
			game.remove(user.getID());
			game.broadcastMessage(user.getName() + " has left!");
			if (game.playerCount() == 0) {
				int idx = gameIDs.indexOf(user.getGameID());
				this.games.remove(idx);
				this.ongoingGames -= 1;
				this.updateGameIDs();
			}
		}
		this.users.remove(index);
		this.connections -= 1;
	}

	public User getUser(String id) {
		User user = this.users.stream()
			.filter(usr -> usr.getID().equals(id))
			.findFirst()
			.orElse(null);
		return user;
	}

	public void updateGameIDs() {
		this.gameIDs = this.games.stream()
			.map(gme -> gme.getID())
			.collect(Collectors.toList());
	}
}
