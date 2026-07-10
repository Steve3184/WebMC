package org.ietf.jgss;
public class GSSException extends Exception {
    public static final int BAD_NAME = 1;
    public static final int BAD_NAMETYPE = 2;
    public static final int BAD_MECH = 3;
    public static final int BAD_STATUS = 4;
    public static final int FAILURE = 5;
    public static final int NO_CONTEXT = 6;
    public static final int NO_CREDENTIAL = 7;
    public static final int DEFECTIVE_TOKEN = 8;
    public static final int DEFECTIVE_CREDENTIAL = 9;
    public static final int CREDENTIAL_EXPIRED = 10;
    public static final int CONTEXT_EXPIRED = 11;
    public static final int BAD_QOP = 12;
    public static final int UNAUTHORIZED = 13;
    public static final int UNAVAILABLE = 14;
    public static final int DUPLICATE_ELEMENT = 15;
    public static final int NAME_NOT_FOUND = 16;
    private final int majorCode;
    public GSSException(int majorCode) { super(); this.majorCode = majorCode; }
    public GSSException(int majorCode, String message) { super(message); this.majorCode = majorCode; }
    public int getMajor() { return majorCode; }
    public String getMessage() { return super.getMessage(); }
    public int getMinor() { return 0; }
    public void setMinor(int minor, String message) {}
}
