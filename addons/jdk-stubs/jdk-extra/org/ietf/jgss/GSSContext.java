package org.ietf.jgss;

public class GSSContext {
    public void dispose() throws GSSException {}

    public void requestMutualAuth(boolean state) throws GSSException {}

    public byte[] initSecContext(byte[] inputBuf, int offset, int len) throws GSSException {
        return new byte[0];
    }

    public byte[] initSecContext(byte[] inputBuf, int offset, int len, Object[] channelBindings) throws GSSException {
        return new byte[0];
    }

    public boolean initSecContext(java.io.InputStream in, java.io.OutputStream out) throws GSSException {
        return false;
    }

    public int initSecContext(byte[] inToken, int offset, int len, byte[] outToken, int outOffset) throws GSSException {
        return 0;
    }
}
