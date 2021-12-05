package packages;
import java.nio.*;

public class Encoder {
    private ByteBuffer buffer;

    public Encoder() {
        this.buffer = ByteBuffer.allocate(1500);
    }

    public int getPosition() {
        return this.buffer.position();
    }

    public void addInt(int i) {
        this.buffer.putInt(i);
    }

    public void addString(String s) {
        this.buffer.putInt(s.length());
        for (int i = 0; i < s.length(); i++) {
            this.buffer.putChar(s.charAt(i));
        }
    }

    public ByteBuffer finish() {
        int length = this.buffer.position();
        ByteBuffer buffer = ByteBuffer.allocate(length);
        this.buffer.flip();
        for (int i = 0; i < length; i++) {
            buffer.put(this.buffer.get(i));
        }
        buffer.flip();
        return buffer;
    }
}
