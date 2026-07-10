package java.text;

public interface StringCharacterIterator extends Cloneable {
    char FIRST = 65535;
    char DONE = 65535;
    char current();
    char next();
    char previous();
    char setIndex(int pos);
    int getBeginIndex();
    int getEndIndex();
    int getIndex();
    Object clone();
}
