package com.funniray.minimap.folia.service;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * Centralized Folia-aware scheduling boundary for all platform-side tasks.
 */
public class FoliaSchedulerService {
    private final Plugin plugin;

    public FoliaSchedulerService(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void runForPlayer(Player player, Runnable task) {
        Objects.requireNonNull(player, "player")
                .getScheduler()
                .run(plugin, scheduledTask -> runTask(task), null);
    }

    public void runForPlayerLater(Player player, long delayTicks, Runnable task) {
        Objects.requireNonNull(player, "player")
                .getScheduler()
                .runDelayed(plugin, scheduledTask -> runTask(task), null, delayTicks);
    }

    public void runAtLocation(Location location, Runnable task) {
        plugin.getServer()
                .getRegionScheduler()
                .run(plugin, Objects.requireNonNull(location, "location"), scheduledTask -> runTask(task));
    }

    public void runGlobal(Runnable task) {
        plugin.getServer()
                .getGlobalRegionScheduler()
                .run(plugin, scheduledTask -> runTask(task));
    }

    public void runAsync(Runnable task) {
        plugin.getServer()
                .getAsyncScheduler()
                .runNow(plugin, scheduledTask -> runTask(task));
    }

    private void runTask(Runnable task) {
        Objects.requireNonNull(task, "task").run();
    }
}
