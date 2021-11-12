package packages;
import java.nio.*;
import java.util.*;

public class Decoder {
    private ByteBuffer buffer;

    public Decoder(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public int getInt() {
        return this.buffer.getInt();
    }

    public String getString() {
        int length = this.buffer.getInt();
        String s = "";
        for (int i = 0; i < length; i++) {
            s += this.buffer.getChar();
        }
        return s;
    }
}
