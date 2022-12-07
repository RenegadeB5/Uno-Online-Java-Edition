package packages;
import packages.*;
import java.util.*;
import java.nio.*;
import java.lang.*;
import java.io.*;
import java.util.stream.*;
import java.util.ArrayList;

public class TicTacToeGame extends Game {

    public int[] board;

    public TicTacToeGame(String id, User user) {
        super(id, user);
        this.board = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
    }

    public int getPosition(String id) {
        List<String> userIds = super.players.stream()
            .map(user -> user.getID())
			.collect(Collectors.toList());
        return (userIds.indexOf(id));
    }

    protected int nextTurn(int n) {
        int turn = super.turn;
        turn += n;
        if (turn == 2) {
            turn = 0;
        }
        return turn;
    }

    public void updateTurn() {
        super.turn = this.nextTurn(1);
        this.broadcastBoard();
        super.players.get(super.turn).sendMessage("It's your turn!", 0);
    }

    public void play(String id, int position) {
        User player = super.players.stream()
            .filter(user -> user.getID().equals(id))
			.collect(Collectors.toList())
            .get(0);
        if (!super.players.get(super.turn).getID().equals(id)) {
            player.sendMessage("It isn't your turn!", 1);
            return;
        }
        if (!super.ongoing) {
            player.sendMessage("The game hasn\'t started yet!", 1);
            return;
        }
        if (this.board[position] == 0) {
            this.board[position] = super.turn + 1;
        }
        else {
            player.sendMessage("The space is already taken!", 1);
        }
        int one = board[0] + board[1] + board[2];
        int two = board[3] + board[4] + board[5];
        int three = board[6] + board[7] + board[8];
        int four = board[0] + board[3] + board[6];
        int five = board[1] + board[4] + board[7];
        int six = board[2] + board[5] + board[8];
        int seven = board[0] + board[4] + board[8];
        int eight = board[2] + board[4] + board[6];
        boolean tieConditions[] = new boolean[] {
            one >= 3,
            two >= 3,
            three >= 3,
            four >= 3,
            five >= 3,
            six >= 3,
            seven >= 3,
            eight >= 3
        };
        boolean winConditions[] = new boolean[] {
            board[0] == board[1] && board[1] == board[2] && one >= 3,
            board[3] == board[4] && board[4] == board[5] && two >= 3,
            board[6] == board[7] && board[7] == board[8] && three >= 3,
            board[0] == board[3] && board[3] == board[6] && four >= 3,
            board[1] == board[4] && board[4] == board[7] && five >= 3,
            board[2] == board[5] && board[5] == board[8] && six >= 3,
            board[0] == board[4] && board[4] == board[8] && seven >= 3,
            board[2] == board[4] && board[4] == board[6] && eight >= 3
        };
        int tie = 0;
        for (boolean check: tieConditions) {
            if (check) tie += 1;
        }
        for (boolean check: winConditions) {
            if (check) {
                this.broadcastBoard();
                this.broadcastMessage(player.getName() + " has won!!!!");
                this.end();
                return;
            }
        }
        if (tie == 8) {
            this.broadcastBoard();
            this.broadcastMessage("The game has tied!");
            this.end();
            return;
        }
        this.updateTurn();
    }

    public void start() {
        boolean ready = true;
        for (User user: super.players) {
            if (!user.isReady()) {
                ready = false;
            }
        }
        if (ready) {
            this.broadcastBoard();
            super.ongoing = true;
        }
    }

    private void broadcastBoard() {
        for (int i = 0; i < super.players.size(); i++) {
            User user = super.players.get(i);
            int index = i;
            Encoder encoder = new Encoder();
            encoder.addInt(4);
            encoder.addInt(this.turn);
            for (int pos: this.board) {
                encoder.addInt(pos);
            }
            user.send(encoder.finish());
        }
    }

    public void reset() {
        this.ongoing = false;
        this.board = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
    }
}
