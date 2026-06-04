package com.funniray.minimap.folia.impl;

import com.funniray.minimap.common.api.MinimapLocation;
import com.funniray.minimap.common.api.MinimapPlayer;
import com.funniray.minimap.common.version.Version;
import com.funniray.minimap.folia.FoliaMinimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

public class FoliaPlayer implements MinimapPlayer {
    private final Player nativePlayer;

    public FoliaPlayer(Player player) {
        nativePlayer = player;
    }

    @Override
    public void sendPluginMessage(byte[] message, String channel) {
        nativePlayer.getScheduler().run(FoliaMinimap.getInstance(), task -> nativePlayer.sendPluginMessage(FoliaMinimap.getInstance(), channel, message), null);
    }

    @Override
    public void sendMessage(Component message) {
        FoliaMinimap.getInstance().adventure().player(nativePlayer).sendMessage(message);
    }

    @Override
    public void teleport(MinimapLocation location) {
        nativePlayer.teleportAsync(((FoliaLocation) location).getNativeLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
    }

    @Override
    public MinimapLocation getLocation() {
        return new FoliaLocation(nativePlayer.getLocation());
    }

    @Override
    public void disconnect(Component reason) {
        nativePlayer.kickPlayer(LegacyComponentSerializer.legacy('\u00a7').serialize(reason));
    }

    @Override
    public UUID getUniqueId() {
        return nativePlayer.getUniqueId();
    }

    @Override
    public String getUsername() {
        return nativePlayer.getName();
    }

    @Override
    public boolean hasPermission(String string) {
        return nativePlayer.hasPermission(string);
    }

    @Override
    public Version getVersion() {
        FoliaMinimap plugin = FoliaMinimap.getInstance();
        if (plugin.viaHooked) {
            return plugin.viaHook.getPlayerVersion(this);
        } else {
            return new FoliaServer().getMinecraftVersion();
        }
    }

    public Player getNativePlayer() {
        return nativePlayer;
    }
}