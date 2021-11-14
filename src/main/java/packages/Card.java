package packages;

public class Card {
    private String c_color;
    private String c_number;
    private int position;
    private boolean top;

    public Card(String color, String number) {
        this.c_color = color;
        this.c_number = number;
        this.position = 0;
        this.top = false;
    }

    public String color() {
        return this.c_color;
    }

    public void color(String color) {
        this.c_color = color;
    }

    public String number() {
        return this.c_number;
    }

    public void number(String number) {
        this.c_number = number;
    }

    public int position() {
        return this.position;
    }

    public void position(int pos) {
        this.position = pos;
    }

    public String stringify() {
        return this.c_color + "-" + this.c_number;
    }
}
