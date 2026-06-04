package com.funniray.minimap.folia.impl;

import com.funniray.minimap.common.api.MinimapPlayer;
import com.funniray.minimap.common.api.MinimapServer;
import com.funniray.minimap.common.api.MinimapWorld;
import com.funniray.minimap.common.version.Version;
import com.funniray.minimap.folia.FoliaMinimap;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

public class FoliaServer implements MinimapServer {
    private static volatile List<MinimapWorld> worldSnapshot = Collections.emptyList();

    public static void refreshWorldSnapshot() {
        worldSnapshot = Bukkit.getWorlds().stream()
                .map(FoliaWorld::new)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Version getMinecraftVersion() {
        String[] ver = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
        if (ver.length < 3) {
            return new Version(parseInt(ver[0]), parseInt(ver[1]), 0);
        } else {
            return new Version(parseInt(ver[0]), parseInt(ver[1]), parseInt(ver[2]));
        }
    }

    @Override
    public String getLoaderVersion() {
        return Bukkit.getVersion();
    }

    @Override
    public String getLoaderName() {
        return Bukkit.getName();
    }

    @Override
    public List<MinimapPlayer> getPlayers() {
        if (!FoliaMinimap.getInstance().getSchedulerService().isOnGlobalThread()) {
            throw new IllegalStateException("FoliaServer#getPlayers() must only be called from the global region thread. Use MinimapServer#forEachPlayer for player-bound work.");
        }

        return Bukkit.getServer().getOnlinePlayers().stream()
                .map(FoliaPlayer::new)
                .collect(Collectors.toList());
    }

    @Override
    public void forEachPlayer(Consumer<MinimapPlayer> action) {
        FoliaMinimap.getInstance().getSchedulerService().runForEachOnlinePlayer(player -> action.accept(new FoliaPlayer(player)));
    }

    @Override
    public List<MinimapWorld> getWorlds() {
        return worldSnapshot;
    }
}
