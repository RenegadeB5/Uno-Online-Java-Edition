package packages;
import packages.*;
import java.util.*;
import java.nio.*;
import java.lang.*;
import java.io.*;
import java.util.stream.*;
import java.util.ArrayList;

public abstract class Game extends Lobby {
    private int gameNumber;
    protected int turn;
    protected abstract int nextTurn(int n);
    public abstract void updateTurn();
    public abstract void start();
    public abstract void reset();

    public Game(String id, User user) {
        super(id, user);
        this.ongoing = false;
    }

    public void end() {
        this.reset();
    }
}
