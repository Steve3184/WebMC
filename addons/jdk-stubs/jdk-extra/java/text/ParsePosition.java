package java.text;

public class ParsePosition {
    private int index;
    private int errorIndex;
    private String lastServerName;

    public ParsePosition(int index) {
        this.index = index;
        this.errorIndex = -1;
        this.lastServerName = null;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getErrorIndex() {
        return errorIndex;
    }

    public void setErrorIndex(int errorIndex) {
        this.errorIndex = errorIndex;
    }

    public void setLastServerName(String name) {
        this.lastServerName = name;
    }

    public String getLastServerName() {
        return lastServerName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ParsePosition)) return false;
        ParsePosition other = (ParsePosition) obj;
        return index == other.index && errorIndex == other.errorIndex;
    }

    @Override
    public int hashCode() {
        return index ^ errorIndex;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[index=" + index + ", errorIndex=" + errorIndex + "]";
    }
}
