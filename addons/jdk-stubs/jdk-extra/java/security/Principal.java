package java.security;
public interface Principal {
    String getName();
    String getLocalizedName();
    boolean implies(Principal p);
}
