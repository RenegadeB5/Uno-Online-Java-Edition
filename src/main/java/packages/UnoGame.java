package packages;
import packages.*;
import java.util.*;
import java.nio.*;
import java.lang.*;
import java.io.*;
import java.util.stream.*;
import java.util.ArrayList;

public class UnoGame extends Game {
    private ArrayList<Card> deck;
    private int direction;
    private int draw;
    private int skip;
    private Card top;

    public UnoGame(String id, User user) {
        super(id, user);
        this.deck = new ArrayList<Card>();
        super.turn = 2;
        this.direction = 1;
        this.draw = 0;
        this.skip = 0;
        try {
            File file = new File("./src/main/java/packages/cards.dat");
            Scanner input = new Scanner(file);
            while (input.hasNext()) {
	            String line = input.nextLine();
                String[] c = line.split("-");
                Card card = new Card(c[0], c[1]);
                this.deck.add(card);
            }
            input.close();
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
                card.position(1);
                break;
            }
        }
    }

    public int getPosition(String id) {
        List<String> userIds = super.players.stream()
            .map(user -> user.getID())
			.collect(Collectors.toList());
        return (userIds.indexOf(id) + 2);
    }

    protected int nextTurn(int n) {
        int turn = super.turn;
        for (int i = 0; i < n; i++) {
            if (turn == super.players.size()+1 && this.direction == 1) {
                turn = 2;
            }
            else if (turn == 2 && this.direction == -1) {
                turn = super.players.size()+1;
            }
            else {
                turn += this.direction;
            }
        }
        return turn;
    }

    private void switchDirection() {
        this.direction *= -1;
    }

    public void updateTurn() {
        if (this.skip != 0) {
            for (int i = 0, j = super.turn; i < this.skip; i++) {
                super.players.get(j-2).sendMessage("You were skipped!", 1);
                j = this.nextTurn(1);
            }
            super.turn = this.nextTurn(1 + this.skip);
            this.skip = 0;
        }
        else if (this.draw != 0) {
            List<Card> cards = this.deck.stream()
                .filter(card -> card.position() == this.nextTurn(1))
			    .collect(Collectors.toList());
            List<String> numbers = cards.stream()
			    .map(card -> card.number())
			    .collect(Collectors.toList());
            boolean contains = numbers.contains(this.top.number());
            if (!contains) {
                int draw = this.draw;
                this.draw = 0;
                super.players.get(this.nextTurn(1)-2).sendMessage("You picked up " + draw + " cards!", 1);
                this.deal(super.players.get(this.nextTurn(1)-2).getID(), draw);
                super.turn = this.nextTurn(2);
            }
        }
        else {
            super.turn = this.nextTurn(1);
        }
        this.broadcastCards();
        super.players.get(super.turn-2).sendMessage("It's your turn!", 0);
    }

    public void shuffle() {
        Collections.shuffle(this.deck);
    }

    public void play(String id, List<String> cardStrings) {
        User player = super.players.stream()
            .filter(user -> user.getID().equals(id))
			.collect(Collectors.toList())
            .get(0);
        if (!super.players.get(super.turn-2).getID().equals(id)) {
            player.sendMessage("It isn't your turn!", 1);
            return;
        }
        if (!super.ongoing) {
            player.sendMessage("The game hasn\'t started yet!", 1);
            return;
        }
        List<Card> playerCardsToClone = this.deck.stream()
            .filter(card -> card.position() == this.getPosition(id))
			.collect(Collectors.toList());
        List<Card> playerCards = new ArrayList<Card>();
        for (Card crd: playerCardsToClone) playerCards.add(crd);
        Card top = new Card(this.top.color(), this.top.number());
        for (int i = 0; i < cardStrings.size(); i++) {
            String[] card = cardStrings.get(i).split("-");
            boolean go = false;
            boolean wild = false;
            if ((top.color().equals(card[0]) && i == 0)) {
                go = true;
            }
            if (top.number().equals(card[1])) {
                go = true;
            }
            List<String> colors = playerCards.stream()
                .map(c -> c.color())
			    .collect(Collectors.toList());
            List<String> numbers = playerCards.stream()
                .map(c -> c.number())
			    .collect(Collectors.toList());
            List<String> combo = playerCards.stream()
                .map(c -> c.color() + "-" + c.number())
			    .collect(Collectors.toList());
            if (!combo.contains(card[0] + "-" + card[1])) {
                go = false;
            }
            if ((card[1].equals("wild") || card[1].equals("+4")) && (numbers.contains(card[0]) || numbers.contains(card[1]))) {
                go = true;
                wild = true;
            }
            if (!go) {
                player.sendMessage("You can't put that down!", 1);
                return;
            }
            top.color(card[0]);
            top.number(card[1]);
            if (wild) {
                playerCards.remove(combo.indexOf("wild" + "-" + card[1]));
            }
            else {
                playerCards.remove(combo.indexOf(card[0] + "-" + card[1]));
            }
        }
        String color = top.color();
        String number = top.number();
        this.top.color(color);
        this.top.number(number);
        this.deck.stream()
            .filter(c -> c.position() == 1)
		    .findFirst()
            .ifPresent(crd -> crd.position(0));
        this.deck.stream()
            .filter(c -> c.position() == super.turn && c.color().equals(color) && c.number().equals(number))
            .findFirst()
            .ifPresent(crd -> crd.position(1));
        for (String cd: cardStrings) {
            String[] card = cd.split("-");
            this.deck.stream()
                .filter(c -> c.position() == this.getPosition(id) && c.color().equals(card[0]) && c.number().equals(card[1]))
			    .findFirst()
                .ifPresent(crd -> crd.position(0));
            if (card[1].equals("wild") || card[1].equals("+4")) {
                this.deck.stream()
                    .filter(c -> c.position() == super.turn && c.number().equals(card[1]))
			        .findFirst()
                    .ifPresent(crd -> crd.position(0));
            }
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
                case "reverse":
                    this.switchDirection();
                    break;
            }
        }
        if (playerCards.size() == 0) {
            super.broadcastMessage(player.getName() + " has won!!!!!!\nThe room will close soon!");
            this.broadcastCards();
            this.end();
            return;
        }
        this.shuffle();
        this.updateTurn();
    }

    private void deal(int amount) {
        for (int i = 0, j = 0; i < super.players.size(); i++) {
            for (int k = 0; k < amount;) {
                Card card = this.deck.get(j);
                if ((!card.color().equals("wild") && (card.number().equals("wild") || card.number().equals("+4"))) || card.position() != 0) {
                    j++;
                    continue;
                }
                this.deck.get(j).position(i+2);
                j++;
                k++;
            }
        }
    }

    public void deal(String id, int amount) {
        for (int i = 0, j = 0; i < amount + this.draw;) {
            Card card = this.deck.get(j);
            if ((!card.color().equals("wild") && (card.number().equals("wild") || card.number().equals("+4"))) || card.position() != 0) {
                j++;
                continue;
            }
            this.deck.get(j).position(this.getPosition(id));
            j++;
            i++;
        }
        if (this.draw != 0) {
            this.draw = 0;
        }
        this.broadcastCards();
    }

    public void start() {
        boolean ready = true;
        for (User user: super.players) {
            if (!user.isReady()) {
                ready = false;
            }
        }
        if (ready) {
            this.deal(7);
            this.broadcastCards();
            super.ongoing = true;
        }
    }

    private void broadcastCards() {
        for (int i = 0; i < super.players.size(); i++) {
            User user = super.players.get(i);
            int index = i;
            Encoder encoder = new Encoder();
            encoder.addInt(4);
            encoder.addInt(this.direction);
            encoder.addString(super.players.get(super.turn-2).getID());
            encoder.addString(super.players.get(this.nextTurn(1) - 2).getID());
            encoder.addString(this.top.color() + "-" + this.top.number());
            List<Card> playerCards = this.deck.stream()
                .filter(card -> card.position() == index+2)
                .collect(Collectors.toList());
            Collections.sort(playerCards, (Card c1, Card c2) -> (c1.stringify()).compareTo(c2.stringify()));
            encoder.addInt(playerCards.size());
            for (Card c: playerCards) {
                encoder.addString(c.color() + "-" + c.number());
            }
            encoder.addInt(super.players.size());
            for (int j = 0; j < super.players.size(); j++) {
                User player = super.players.get(j);
                int idx = j;
                encoder.addString(player.getID());
                encoder.addString(player.getName());
                int count = this.deck.stream()
                    .filter(card -> card.position() == idx+2)
                    .collect(Collectors.toList())
                    .size();
                encoder.addInt(count);
            }
            user.send(encoder.finish());
        }
    }
}
