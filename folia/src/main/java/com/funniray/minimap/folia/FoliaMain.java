package com.funniray.minimap.folia;

import com.funniray.minimap.common.JavaMinimapPlugin;
import com.funniray.minimap.common.MinimapConfig;
import com.funniray.minimap.common.api.MinimapServer;
import com.funniray.minimap.folia.impl.FoliaPlayer;
import com.funniray.minimap.folia.impl.FoliaServer;
import com.funniray.minimap.folia.service.FoliaSchedulerService;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FoliaMain extends JavaMinimapPlugin implements PluginMessageListener, Listener {
    private final FoliaMinimap plugin;
    private final FoliaSchedulerService schedulerService;
    private final Map<UUID, Long> playerRefreshLoopVersions = new ConcurrentHashMap<>();

    public FoliaMain(FoliaMinimap plugin, FoliaSchedulerService schedulerService) {
        this.plugin = plugin;
        this.schedulerService = schedulerService;
    }

    @Override
    public void registerChannel(String channel) {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, channel);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, channel, this);
    }

    @Override
    public void unregisterChannel(String channel) {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, channel);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, channel, this);
    }

    @Override
    public MinimapServer getServer() {
        return new FoliaServer();
    }

    @Override
    public void saveConfig() {
        MinimapConfig snapshot = snapshotConfig();
        if (!schedulerService.runAsync(() -> saveConfigSnapshot(snapshot))) {
            saveConfigSnapshot(snapshot);
        }
    }

    @Override
    public ConfigurationLoader<CommentedConfigurationNode> getConfigLoader() {
        File defaultConfig = plugin.getDataFolder();

        if (!defaultConfig.exists()) defaultConfig.mkdirs();
        return YamlConfigurationLoader.builder()
                .defaultOptions(opts -> opts.shouldCopyDefaults(true))
                .path(defaultConfig.toPath().resolve("config.yml"))
                .build();
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        schedulerService.runForPlayer(player, () -> this.onPluginMessage(channel, new FoliaPlayer(player), message));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        startPlayerSettingsRefreshLoop(player);
        schedulePlayerInitialSettings(player);
    }

    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        if (isMinimapChannel(event.getChannel())) {
            startPlayerSettingsRefreshLoop(event.getPlayer());
            schedulePlayerInitialSettings(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        schedulePlayerSettingsRefresh(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        schedulePlayerSettingsRefresh(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        restartPlayerSettingsRefreshLoop(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPostRespawn(PlayerPostRespawnEvent event) {
        restartPlayerSettingsRefreshLoop(event.getPlayer());
        schedulePlayerSettingsRefresh(event.getPlayer(), true);
    }

    @EventHandler
    public void onLeft(PlayerQuitEvent event) {
        playerRefreshLoopVersions.remove(event.getPlayer().getUniqueId());
        this.handlePlayerLeft(new FoliaPlayer(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        schedulePlayerSettingsRefresh(event.getPlayer(), true);
    }

    private void schedulePlayerInitialSettings(Player player) {
        schedulerService.runForPlayer(player, () -> this.handlePlayerJoined(new FoliaPlayer(player)));
    }

    private void schedulePlayerSettingsRefresh(Player player, boolean includePostTransitionRefresh) {
        schedulerService.runForPlayer(player, () -> this.refreshPlayerSettings(new FoliaPlayer(player)));
        if (!includePostTransitionRefresh) {
            return;
        }

        Collection<Long> refreshTicks = getConfig().transitionSettingsRefreshTicks;
        if (refreshTicks == null) {
            return;
        }

        refreshTicks.stream()
                .filter(delayTicks -> delayTicks != null && delayTicks > 0L)
                .distinct()
                .sorted()
                .forEach(delayTicks -> schedulerService.runForPlayerLater(player, delayTicks, () -> this.refreshPlayerSettings(new FoliaPlayer(player))));
    }

    private void startPlayerSettingsRefreshLoop(Player player) {
        UUID playerId = player.getUniqueId();
        long loopVersion = 1L;
        if (playerRefreshLoopVersions.putIfAbsent(playerId, loopVersion) == null) {
            scheduleNextPlayerSettingsRefresh(player, loopVersion);
        }
    }

    private void restartPlayerSettingsRefreshLoop(Player player) {
        UUID playerId = player.getUniqueId();
        long loopVersion = playerRefreshLoopVersions.merge(playerId, 1L, Long::sum);
        scheduleNextPlayerSettingsRefresh(player, loopVersion);
    }

    private void scheduleNextPlayerSettingsRefresh(Player player, long loopVersion) {
        long intervalTicks = getConfig().settingsRefreshIntervalTicks;
        if (intervalTicks <= 0L || !isCurrentPlayerRefreshLoop(player, loopVersion)) {
            return;
        }

        schedulerService.runForPlayerLater(player, intervalTicks, () -> {
            if (!isCurrentPlayerRefreshLoop(player, loopVersion)) {
                return;
            }

            refreshPlayerSettings(new FoliaPlayer(player));
            scheduleNextPlayerSettingsRefresh(player, loopVersion);
        });
    }

    private boolean isCurrentPlayerRefreshLoop(Player player, long loopVersion) {
        return playerRefreshLoopVersions.getOrDefault(player.getUniqueId(), 0L) == loopVersion;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        schedulerService.runGlobal(FoliaServer::refreshWorldSnapshot);
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        schedulerService.runGlobal(FoliaServer::refreshWorldSnapshot);
    }
}
