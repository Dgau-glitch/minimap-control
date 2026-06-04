package com.funniray.minimap.folia;

import com.funniray.minimap.common.JavaMinimapPlugin;
import com.funniray.minimap.common.api.MinimapServer;
import com.funniray.minimap.folia.impl.FoliaPlayer;
import com.funniray.minimap.folia.impl.FoliaServer;
import com.funniray.minimap.folia.impl.FoliaWorld;
import com.funniray.minimap.folia.service.FoliaSchedulerService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

public class FoliaMain extends JavaMinimapPlugin implements PluginMessageListener, Listener {
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
    public MinimapServer getServer() {
        return new FoliaServer();
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
        this.onPluginMessage(channel, new FoliaPlayer(player), message);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // The player join event is slightly too early. I unfortunately don't know an event that fires late enough for Xaeros to recognize the packet
        // If anyone knows, please let me know
        Player player = event.getPlayer();
        schedulerService.runForPlayerLater(player, 40L, () -> this.handlePlayerJoined(new FoliaPlayer(player)));
    }

    @EventHandler
    public void onLeft(PlayerQuitEvent event) {
        this.handlePlayerLeft(new FoliaPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        this.handleSwitchWorld(new FoliaWorld(event.getPlayer().getWorld()), new FoliaPlayer(event.getPlayer()));
    }
}