package packages;
import packages.*;
import java.util.*;
import java.nio.*;
import java.lang.*;
import java.io.*;
import java.util.stream.*;
import java.util.ArrayList;

public abstract class Game {
    private String id;
    protected ArrayList<User> players;
    protected boolean ongoing;
    protected int turn;
    public abstract int getPosition(String id);
    protected abstract int nextTurn(int n);
    public abstract void updateTurn();
    public abstract void start();

    public Game(String id, User user) {
        this.id = id;
        this.players = new ArrayList<User>();
        this.players.add(user);
        this.ongoing = false;
    }

    public boolean ongoing() {
        return this.ongoing;
    }
	
	public String getID() {
		return this.id;
	}

    public int playerCount() {
        return this.players.size();
    }

    public void addUser(User user) {
        this.players.add(user);
        this.broadcastUsers();
        user.setGame(this);
        user.sendMessage("You have successfully joined the game!");
    }

    public void remove(String id) {
        for (int i = 0; i < this.players.size(); i++) {
            User player = this.players.get(i);
            if (player.getID().equals(id)) {
                this.players.remove(i);
                this.broadcastMessage(player.getName() + " has left the game.");
                if (this.players.size() == 1 || this.players.size() == 0) {
                    this.end();
                }
            }
        }
        this.broadcastUsers();
    }

    public void broadcastUsers() {
        Encoder enc = new Encoder();
        enc.addInt(6);
        StringBuilder str = new StringBuilder();
        str.append("Current users:\n");
        for (User user: this.players) {
            str.append(user.getName() + ": " + (user.isReady() ? "ready" : "not ready") + "\n");
        }
        enc.addString(str.toString());
        ByteBuffer buff = enc.finish();
        for (User user: this.players) {
            user.send(buff);
        }
    }

    public void broadcastMessage(String message) {
        for (User user: this.players) {
            user.sendMessage(message);
        }
    }

    public void broadcastMessage(String exclude, String sender, String message) {
        for (User user: this.players) {
            if (user.getID().equals(exclude)) {
                
            }
            else {
                user.sendMessage(sender + ": " + message);
            }
        }
    }

    public void end() {
        this.broadcastMessage("The game has ended!");
        for (User user: this.players) {
			user.setGame(null);
            user.setReady(false);
        }
    }
}
