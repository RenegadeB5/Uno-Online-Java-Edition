package packages;
import java.nio.ByteBuffer;
import org.java_websocket.WebSocket;

public class User {
	private String id;
	private String name;
	private String gameID;
	private boolean ready;
	private WebSocket ws;

	public User(String id, WebSocket ws) {
		this.id = id;
		this.name = null;
		this.gameID = null;
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
	
	public void setGameID(String gameID) {
		this.gameID = gameID;
	}
	
	public String getGameID() {
		return this.gameID;
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
