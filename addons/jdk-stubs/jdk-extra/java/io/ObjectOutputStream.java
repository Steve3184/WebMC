package java.io;
import java.io.IOException;
import java.io.OutputStream;
public class ObjectOutputStream extends OutputStream implements ObjectOutput {
    public ObjectOutputStream(OutputStream out) throws IOException { super(); }
    public void writeObject(Object obj) throws IOException {}
    public void writeInt(int v) throws IOException {}
    public void writeUTF(String s) throws IOException {}
    public void writeBytes(String s) throws IOException {}
    public void writeChars(String s) throws IOException {}
    public void writeChar(int v) throws IOException {}
    public void writeBoolean(boolean v) throws IOException {}
    public void writeByte(int v) throws IOException {}
    public void writeShort(int v) throws IOException {}
    public void writeLong(long v) throws IOException {}
    public void writeFloat(float v) throws IOException {}
    public void writeDouble(double v) throws IOException {}
    public void write(int b) throws IOException {}
    public void close() throws IOException {}
    public void flush() throws IOException {}
}
