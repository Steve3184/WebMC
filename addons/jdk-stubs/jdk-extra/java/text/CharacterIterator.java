package java.text;

public interface CharacterIterator {
    char DONE = '￿';
    
    char current();
    char first();
    char last();
    char next();
    char previous();
    void setIndex(int pos);
    int getIndex();
    int getBeginIndex();
    int getEndIndex();
}
