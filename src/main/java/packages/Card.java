package packages;

public class Card {
    private String color;
    private String number;
    private int position;
    private boolean top;

    public Card(String color, String number) {
        this.color = color;
        this.number = number;
        this.position = 0;
        this.top = false;
    }

    public String color() {
        return this.color;
    }

    public String number() {
        return this.number;
    }

    public int position() {
        return this.position;
    }

    public void position(int pos) {
        this.position = pos;
    }

    public String stringify() {
        return this.color + "-" + this.number;
    }

    public void setTop(boolean b) {
        this.top = b;
    }

    public boolean isTop() {
        return this.top;
    }
}
