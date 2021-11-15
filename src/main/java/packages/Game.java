package packages;
import packages.*;
import java.util.*;
import java.nio.*;
import java.lang.*;
import java.io.*;
import java.util.stream.*;

public class Game {
    private String id;
    private ArrayList<User> players;
    private ArrayList<Card> deck;
    private int turn;
    private int direction;
    private int draw;
    private int skip;
    private boolean ongoing;
    private Card top;

    public Game(String id, User user) {
        this.id = id;
        this.players = new ArrayList<User>();
        this.deck = new ArrayList<Card>();
        this.players.add(user);
        this.turn = 1;
        this.direction = 1;
        this.draw = 0;
        this.ongoing = false;
        try {
            System.out.println(new File(".").getAbsoluteFile());
            File file = new File("/app/cards.dat");
            Scanner input = new Scanner(file);
            while (input.hasNext()) {
	            String line = input.nextLine();
                String[] c = line.split("-");
                Card card = new Card(c[0], c[1]);
                this.deck.add(card);
            }
        }
        catch (IOException err) {
            System.out.println(err.getMessage());
            err.printStackTrace();
        }
        for (int i = 0; i < 10; i++) {
            this.shuffle();
        }
        for (Card card: this.deck) {
            if (card.number().equals("wild") || card.number().equals("+4")) {
                continue;
            }
            else {
                this.top = new Card(card.color(), card.number());
                break;
            }
        }
    }

    public boolean ongoing() {
        return this.ongoing;
    }
	
	public String getID() {
		return this.id;
	}

    public void addUser(User user) {
        this.players.add(user);
        this.broadcastUsers();
        user.sendMessage("You have successfully joined the game!");
    }

    public int getPosition(String id) {
        List<String> userIds = this.players.stream()
            .map(user -> user.getID())
			.collect(Collectors.toList());
        return userIds.indexOf(id) + 1;
    }

    private int nextTurn(int n) {
        int turn = this.turn;
        for (int i = 0; i < n; i++) {
            if (turn == this.players.size() && this.direction == 1) {
                turn = 1;
            }
            else if (turn == 0 && this.direction == -1) {
                turn = this.players.size();
            }
            else {
                turn += this.direction;
            }
        }
        return turn;
    }

    private void updateTurn() {
        if (this.skip != 0) {
            this.turn = this.nextTurn(1 + this.skip);
            for (int i = this.turn; i < this.skip; i=this.nextTurn(1)) {
                this.players.get(i).sendMessage("You were skipped!");
            }
            this.skip = 0;
        }
        else if (this.draw != 0) {
            List<Card> cards = this.deck.stream()
                .filter(card -> card.position() == this.nextTurn(1))
			    .collect(Collectors.toList());
            List<String> numbers = cards.stream()
			    .map(card -> card.number())
			    .collect(Collectors.toList());
            boolean contains = numbers.contains("+2") || numbers.contains("+4");
            if (!contains) {
                this.deal(this.players.get(this.turn-1).getID(), this.draw);
                this.players.get(this.nextTurn(1)).sendMessage("You picked up " + this.draw + " cards!");
                this.turn = this.nextTurn(2);
                this.draw = 0;
            }
        }
        else {
            this.turn = this.nextTurn(1);
        }
        this.deal(this.players.get(this.turn-1).getID(), this.draw);
        this.broadcastCards();
        this.players.get(this.turn-1).sendMessage("It's your turn!");
    }

    private void switchDirection() {
        this.direction *= -1;
    }

    public void shuffle() {
        Collections.shuffle(this.deck);
    }

    public void play(String id, List<String> cardStrings) {
        boolean error = false;
        User player = this.players.stream()
            .filter(user -> user.getID().equals(id))
			.collect(Collectors.toList())
            .get(0);
        if (!this.players.get(this.turn-1).getID().equals(id)) {
            player.sendMessage("It isn't your turn!");
            return;
        }
        if (!this.ongoing) {
            player.sendMessage("The game hasn\'t started yet!");
            return;
        }
        List<Card> playerCards = this.deck.stream()
            .filter(card -> card.position() == this.getPosition(id))
			.collect(Collectors.toList());
        Card top = new Card(this.top.color(), this.top.number());
        for (int i = 0; i < cardStrings.size(); i++) {
            String[] card = cardStrings.get(i).split("-");
            boolean go = false;
            if ((top.color().equals(card[0]) && i == 0)) {
                go = true;
            }
            if ((card[1].equals("wild") || card[1].equals("+4")) && i == 0) {
                go = true;
            }
            if (this.top.number().equals(card[1])) {
                go = true;
            }
            List<String> colors = playerCards.stream()
                .map(c -> c.color())
			    .collect(Collectors.toList());
            List<String> numbers = playerCards.stream()
                .map(c -> c.number())
			    .collect(Collectors.toList());
            List<String> four = Arrays.asList("blue", "green", "yellow", "red");
            if ((!colors.contains(card[0]) && !numbers.contains(card[1])) || !four.contains(card[0])) {
                go = false;
            }
            if (!go) {
                player.sendMessage("You can't put that down!");
                return;
            }
            top.color(card[0]);
            top.number(card[1]);
        }
        String color = top.color();
        String number = top.number();
        this.top.color(color);
        this.top.number(number);
        for (String cd: cardStrings) {
            String[] card = cd.split("-");
            Card cardToSwitch = this.deck.stream()
                .filter(c -> c.position() == 0 && c.color().equals(card[0]) && c.number().equals(card[1]))
			    .collect(Collectors.toList())
                .get(0);
            cardToSwitch.position(this.turn);
            switch (card[1]) {
                case "+2":
                    this.draw += 2;
                    break;
                case "+4":
                    this.draw += 4;
                    break;
                case "skip":
                    this.skip += 1;
                    break;
            }
        }
        this.updateTurn();
    }

    public void remove(String id) {
        for (int i = 0; i < this.players.size(); i++) {
            User player = this.players.get(i);
            if (player.getID().equals(id)) {
                this.players.remove(i);
                if (this.players.size() == 1) {
                    this.end();
                }
            }
        }
    }

    private void deal(int amount) {
        for (int i = 0, j = 0; i < this.players.size(); i++) {
            for (int k = 0; k < 7; k++) {
                Card card = this.deck.get(j);
                if ((!card.color().equals("wild") || !card.color().equals("+4")) && (card.number().equals("wild") || card.number().equals("+4"))) {
                    continue;
                }
                this.deck.get(j).position(i+1);
                j++;
            }
        }
    }

    private void deal(String id, int amount) {
        for (int i = 0, j = 0; i < this.players.size(); i++) {
            for (int k = 0; k < 7; k++) {
                Card card = this.deck.get(j);
                if (card.position() != 0 || ((!card.color().equals("wild") || !card.color().equals("+4")) && (card.number().equals("wild") || card.number().equals("+4")))) {
                    continue;
                }
                this.deck.get(j).position(this.getPosition(id));
                j++;
            }
        }
    }

    public void start() {
        boolean ready = true;
        for (User user: this.players) {
            if (!user.isReady()) {
                ready = false;
            }
        }
        if (ready) {
            this.deal(7);
            this.broadcastCards();
            this.ongoing = true;
        }
    }

    public void end() {
        this.broadcastMessage("The game has ended!");
        for (User user: this.players) {
			user.setGameID(null);
            user.setReady(false);
        }
    }

    private void broadcastCards() {
        Encoder encoder = new Encoder();
        encoder.addInt(4);
        encoder.addInt(this.players.size());
        for (int i = 0; i < this.players.size(); i++) {
            User user = this.players.get(i);
            encoder.addString(user.getID());
            encoder.addString(user.getName());
            int pos = i+1;
            List<Card> cards = this.deck.stream()
                .filter(card -> card.position() == pos)
			    .collect(Collectors.toList());
            encoder.addInt(cards.size());
            for (Card card: cards) {
                String s = card.color() + "-" + card.number();
                encoder.addString(s);
            }
        }
        ByteBuffer buffer = encoder.finish();
        for (User user: this.players) {
            user.send(buffer);
        }
    }

    public void broadcastUsers() {
        StringBuilder str = new StringBuilder();
        str.append("Current users:\n");
        for (User user: this.players) {
            str.append(user.getName() + ": " + (user.isReady() ? "ready" : "not ready") + "\n");
        }
        this.broadcastMessage(str.toString());
    }

    public void broadcastMessage(String message) {
        for (User user: this.players) {
            user.sendMessage(message);
        }
    }

    public void broadcastMessage(String exclude, String message) {
        for (User user: this.players) {
            if (!user.getID().equals(id)) {
                user.sendMessage(message);
            }
        }
    }
}
