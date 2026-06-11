package com.funniray.minimap.folia;

import com.funniray.minimap.common.JavaMinimapPlugin;
import com.funniray.minimap.common.MinimapConfig;
import com.funniray.minimap.common.api.MinimapServer;
import com.funniray.minimap.folia.impl.FoliaPlayer;
import com.funniray.minimap.folia.impl.FoliaServer;
import com.funniray.minimap.folia.service.FoliaSchedulerService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

public class FoliaMain extends JavaMinimapPlugin implements PluginMessageListener, Listener {
    private static final long POST_TRANSITION_REFRESH_DELAY_TICKS = 1L;

    private final FoliaMinimap plugin;
    private final FoliaSchedulerService schedulerService;

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
        schedulePlayerSettingsRefresh(event.getPlayer(), false);
    }

    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        if (isMinimapChannel(event.getChannel())) {
            schedulePlayerSettingsRefresh(event.getPlayer(), false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        schedulePlayerSettingsRefresh(event.getPlayer(), true);
    }

    @EventHandler
    public void onLeft(PlayerQuitEvent event) {
        this.handlePlayerLeft(new FoliaPlayer(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        schedulePlayerSettingsRefresh(event.getPlayer(), true);
    }

    private void schedulePlayerSettingsRefresh(Player player, boolean includePostTransitionRefresh) {
        schedulerService.runForPlayer(player, () -> this.handlePlayerJoined(new FoliaPlayer(player)));
        if (includePostTransitionRefresh) {
            schedulerService.runForPlayerLater(player, POST_TRANSITION_REFRESH_DELAY_TICKS, () -> this.handlePlayerJoined(new FoliaPlayer(player)));
        }
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
