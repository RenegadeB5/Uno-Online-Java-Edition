package packages;
import java.nio.ByteBuffer;
import org.java_websocket.WebSocket;

public class User {
	private String id;
	private String name;
	private Game game;
	private int gameNum;
	private boolean ready;
	private WebSocket ws;

	public User(String id, WebSocket ws) {
		this.id = id;
		this.name = null;
		this.game = null;
		this.gameNum = 0;
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

	public Game getGame() {
		return this.game;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public int getGameNum() {
		return this.gameNum;
	}

	public void setGameNum(int gameNum) {
		this.gameNum = gameNum;
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
	
	public void sendMessage(String m, int code) {
		Encoder encoder = new Encoder();
		encoder.addInt(3); 
		// 0 for normal, 1 for error (dialog box)
		encoder.addInt(code);
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
