package com.funniray.minimap.folia.impl;

import com.funniray.minimap.common.api.MinimapLocation;
import com.funniray.minimap.common.api.MinimapPlayer;
import com.funniray.minimap.common.version.Version;
import com.funniray.minimap.folia.FoliaMinimap;
import com.funniray.minimap.folia.service.FoliaSchedulerService;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FoliaPlayer implements MinimapPlayer {
    private final Player nativePlayer;

    public FoliaPlayer(Player player) {
        nativePlayer = player;
    }

    @Override
    public void sendPluginMessage(byte[] message, String channel) {
        schedulerService().runForPlayer(nativePlayer, () -> nativePlayer.sendPluginMessage(FoliaMinimap.getInstance(), channel, message));
    }

    @Override
    public void sendMessage(Component message) {
        schedulerService().runForPlayer(nativePlayer, () -> FoliaMinimap.getInstance().adventure().player(nativePlayer).sendMessage(message));
    }

    @Override
    public void teleport(MinimapLocation location) {
        Location nativeLocation = ((FoliaLocation) location).getNativeLocation();
        schedulerService().runForPlayer(nativePlayer, () -> nativePlayer.teleportAsync(nativeLocation, PlayerTeleportEvent.TeleportCause.COMMAND));
    }

    @Override
    public MinimapLocation getLocation() {
        ensureOnPlayerThread();
        return new FoliaLocation(nativePlayer.getLocation());
    }

    @Override
    public void disconnect(Component reason) {
        schedulerService().runForPlayer(nativePlayer, () -> nativePlayer.kick(reason));
    }

    @Override
    public CompletableFuture<MinimapLocation> getLocationAsync() {
        return schedulerService().supplyForPlayer(nativePlayer, this::getLocation);
    }

    @Override
    public CompletableFuture<Boolean> hasPermissionAsync(String string) {
        return schedulerService().supplyForPlayer(nativePlayer, () -> hasPermission(string));
    }

    @Override
    public CompletableFuture<Version> getVersionAsync() {
        return schedulerService().supplyForPlayer(nativePlayer, this::getVersion);
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
        ensureOnPlayerThread();
        return nativePlayer.hasPermission(string);
    }

    @Override
    public Version getVersion() {
        ensureOnPlayerThread();
        FoliaMinimap plugin = FoliaMinimap.getInstance();
        if (plugin.viaHooked) {
            return plugin.viaHook.getPlayerVersion(this);
        } else {
            return new FoliaServer().getMinecraftVersion();
        }
    }

    public Player getNativePlayer() {
        ensureOnPlayerThread();
        return nativePlayer;
    }

    private void ensureOnPlayerThread() {
        schedulerService().ensureOnPlayerThread(nativePlayer);
    }

    private FoliaSchedulerService schedulerService() {
        return FoliaMinimap.getInstance().getSchedulerService();
    }
}
