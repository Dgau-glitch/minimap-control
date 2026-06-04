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

    public boolean runForPlayer(Player player, Runnable task) {
        Player target = Objects.requireNonNull(player, "player");
        Runnable validatedTask = () -> runIfPlayerAvailable(target, task);

        return target.getScheduler().run(plugin, scheduledTask -> validatedTask.run(), () -> { }) != null;
    }

    public boolean runForPlayerLater(Player player, long delayTicks, Runnable task) {
        Player target = Objects.requireNonNull(player, "player");
        Runnable validatedTask = () -> runIfPlayerAvailable(target, task);

        return target.getScheduler().runDelayed(plugin, scheduledTask -> validatedTask.run(), () -> { }, delayTicks) != null;
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

    private void runIfPlayerAvailable(Player player, Runnable task) {
        if (!player.isOnline() || !player.isValid()) {
            return;
        }

        runTask(task);
    }

    private void runTask(Runnable task) {
        Objects.requireNonNull(task, "task").run();
    }
}
