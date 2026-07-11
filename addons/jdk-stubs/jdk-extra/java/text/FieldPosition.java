package java.text;

public class FieldPosition {
    private int fieldIndex;
    private int beginIndex;
    private int endIndex;
    private Format.Field attribute;

    public FieldPosition(int field) {
        this.fieldIndex = field;
        this.beginIndex = 0;
        this.endIndex = 0;
    }

    public FieldPosition(Format.Field attribute) {
        this.attribute = attribute;
        this.fieldIndex = 0;
        this.beginIndex = 0;
        this.endIndex = 0;
    }

    public FieldPosition(Format.Field attribute, int fieldID) {
        this.attribute = attribute;
        this.fieldIndex = fieldID;
        this.beginIndex = 0;
        this.endIndex = 0;
    }

    public int getField() {
        return fieldIndex;
    }

    public void setField(int field) {
        this.fieldIndex = field;
    }

    public Format.Field getFieldAttribute() {
        return attribute;
    }

    public void setField(Format.Field attribute) {
        this.attribute = attribute;
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setBeginIndex(int beginIndex) {
        this.beginIndex = beginIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FieldPosition)) return false;
        FieldPosition other = (FieldPosition) obj;
        return fieldIndex == other.fieldIndex &&
               beginIndex == other.beginIndex &&
               endIndex == other.endIndex;
    }

    @Override
    public int hashCode() {
        return fieldIndex ^ beginIndex ^ endIndex;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[field=" + fieldIndex + ", beginIndex=" + beginIndex + ", endIndex=" + endIndex + "]";
    }
}
