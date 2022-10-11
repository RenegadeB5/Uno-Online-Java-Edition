package packages;
import java.nio.ByteBuffer;
import org.java_websocket.WebSocket;

interface GameType <T> {
	T gameObj = null;
}

public class User {
	private String id;
	private String name;
	private GameType game;
	private boolean ready;
	private WebSocket ws;

	public User(String id, WebSocket ws) {
		this.id = id;
		this.name = null;
		this.game = null;
		this.ready = false;
		this.ws = ws;
	}

	public String getID() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void setGame(GameType game) {
		this.game = game.gameObj;
	}
	
	public GameType getGame() {
		return this.game.gameObj;
	}

	public void send(ByteBuffer s) {
		try {
			this.ws.send(s);
		}
		catch (Exception err) {
			System.out.println(err.getMessage());
			System.out.println(err.getStackTrace());
		}
	}
	
	public void sendMessage(String m) {
		Encoder encoder = new Encoder();
		encoder.addInt(3);
		encoder.addString(m);
		this.ws.send(encoder.finish());
	}
	
	public boolean isReady() {
		return this.ready;
	}
	
	public void setReady(boolean r) {
		this.ready = r;
	}
}
