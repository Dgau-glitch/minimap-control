package com.funniray.minimap.common.api;

import com.funniray.minimap.common.version.Version;

import java.util.List;
import java.util.function.Consumer;

public interface MinimapServer {
    Version getMinecraftVersion();
    String getLoaderVersion();
    String getLoaderName();

    /**
     * Returns a platform-defined snapshot of currently online players.
     * Folia callers must not use this for arbitrary player-bound work from a region thread;
     * prefer {@link #forEachPlayer(Consumer)} so the platform can switch to the right context.
     */
    List<MinimapPlayer> getPlayers();

    /**
     * Runs {@code action} for every online player in the platform-appropriate context.
     */
    default void forEachPlayer(Consumer<MinimapPlayer> action) {
        getPlayers().forEach(action);
    }

    /**
     * Returns a platform-defined snapshot of loaded worlds.
     */
    List<MinimapWorld> getWorlds();
}
