package top.steve3184.webmc.net;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.net.InetAddress;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Offline / browser-friendly stand-in for {@code YggdrasilMinecraftSessionService}.
 *
 * <p>The real Yggdrasil service issues blocking HTTPS calls to Mojang on
 * construction (public keyset fetch) and per-method (join-server, fetch
 * profile, fetch textures). In the browser we can't do any of that
 * synchronously without the user signing in — and for the path-to-mainmenu
 * MVP we don't need to. Returning sensible "no online identity" values
 * keeps the client constructor and the splash screen happy.</p>
 *
 * <p>This is the foundation for {@link top.steve3184.webmc.net.WebHttp}-backed
 * online mode later: when we wire up Microsoft auth and an offscreen-iframe
 * authentication flow, we'll replace specific methods here with real fetch
 * calls.</p>
 */
public final class OfflineSessionService implements MinecraftSessionService {

    public static final OfflineSessionService INSTANCE = new OfflineSessionService();

    private OfflineSessionService() {}

    @Override
    public void joinServer(UUID profileId, String authenticationToken, String serverId) throws AuthenticationException {
        // Offline mode — pretend the join request succeeded. Real online
        // multiplayer would talk to sessionserver.mojang.com here.
    }

    @Nullable
    @Override
    public ProfileResult hasJoinedServer(String profileName, String serverId, @Nullable InetAddress address)
            throws AuthenticationUnavailableException {
        // We're never the multiplayer server target, so this is dead code in
        // practice. Returning null = "not joined" is the safe default.
        return null;
    }

    @Override
    public Property getPackedTextures(GameProfile profile) {
        // No skin/cape data available offline.
        return null;
    }

    @Override
    public MinecraftProfileTextures unpackTextures(Property packedTextures) {
        return MinecraftProfileTextures.EMPTY;
    }

    @Override
    public ProfileResult fetchProfile(UUID profileId, boolean requireSecure) {
        // Construct a profile from just the UUID — name will be filled in
        // from the session's user record by callers that care.
        return new ProfileResult(new GameProfile(profileId, "Player"));
    }

    @Override
    public String getSecurePropertyValue(Property property) throws InsecurePublicKeyException {
        return property.value();
    }
}
