package com.funniray.minimap.common.api;

import com.funniray.minimap.common.version.Version;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Platform-neutral player facade.
 *
 * <p>Synchronous player-bound methods ({@link #getLocation()}, {@link #hasPermission(String)},
 * {@link #getVersion()} and player actions) must be called from the platform's owning player
 * context. On Folia this means the player's entity scheduler thread. Common handlers are expected
 * to be entered through the platform adapter, which is responsible for switching to that context.
 * Code that starts from a global/async context should use the async accessors instead so Folia can
 * schedule the read on the owning entity thread without blocking.</p>
 */
public interface MinimapPlayer {
    void sendPluginMessage(byte[] message, String channel);
    void sendMessage(Component message);
    void teleport(MinimapLocation location);
    MinimapLocation getLocation();
    void disconnect(Component reason);

    UUID getUniqueId();
    String getUsername();
    boolean hasPermission(String string);
    Version getVersion();

    default CompletableFuture<MinimapLocation> getLocationAsync() {
        return CompletableFuture.completedFuture(getLocation());
    }

    default CompletableFuture<Boolean> hasPermissionAsync(String string) {
        return CompletableFuture.completedFuture(hasPermission(string));
    }

    default CompletableFuture<Version> getVersionAsync() {
        return CompletableFuture.completedFuture(getVersion());
    }
}
