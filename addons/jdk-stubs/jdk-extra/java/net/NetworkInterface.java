package java.net;

import java.util.Collections;
import java.util.Enumeration;

public final class NetworkInterface {
    private NetworkInterface() {}
    public String getName() { return "lo"; }
    public Enumeration<InetAddress> getInetAddresses() { return Collections.enumeration(Collections.singletonList(InetAddress.getLoopbackAddress())); }
    public java.util.List<InterfaceAddress> getInterfaceAddresses() { return Collections.emptyList(); }
    public Enumeration<NetworkInterface> getSubInterfaces() { return Collections.emptyEnumeration(); }
    public NetworkInterface getParent() { return null; }
    public int getIndex() { return 0; }
    public String getDisplayName() { return "loopback"; }
    public boolean isUp() { return true; }
    public boolean isLoopback() { return true; }
    public boolean isPointToPoint() { return false; }
    public boolean supportsMulticast() { return false; }
    public byte[] getHardwareAddress() { return new byte[0]; }
    public int getMTU() { return 65536; }
    public boolean isVirtual() { return false; }
    public static NetworkInterface getByName(String name) { return null; }
    public static NetworkInterface getByIndex(int index) { return null; }
    public static NetworkInterface getByInetAddress(InetAddress addr) { return null; }
    public static Enumeration<NetworkInterface> getNetworkInterfaces() { return Collections.emptyEnumeration(); }
}
