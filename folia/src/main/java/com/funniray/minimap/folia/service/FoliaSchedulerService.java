package com.funniray.minimap.folia.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    public boolean runForPlayerOrNow(Player player, Runnable task) {
        Player target = Objects.requireNonNull(player, "player");
        if (isOnPlayerThread(target)) {
            runIfPlayerAvailable(target, task);
            return true;
        }

        return runForPlayer(target, task);
    }

    public <T> CompletableFuture<T> supplyForPlayer(Player player, Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Player target = Objects.requireNonNull(player, "player");
        Supplier<T> checkedSupplier = Objects.requireNonNull(supplier, "supplier");

        boolean scheduled = target.getScheduler().run(plugin, scheduledTask -> completeIfPlayerAvailable(target, checkedSupplier, future), () -> completeRetired(future)) != null;
        if (!scheduled) {
            completeRetired(future);
        }

        return future;
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

    public void runForEachOnlinePlayer(Consumer<Player> playerTask) {
        runGlobal(() -> plugin.getServer().getOnlinePlayers().forEach(player -> runForPlayer(player, () -> playerTask.accept(player))));
    }

    public boolean isOnGlobalThread() {
        return Bukkit.isGlobalTickThread();
    }

    public boolean isOnPlayerThread(Player player) {
        return Bukkit.isOwnedByCurrentRegion(Objects.requireNonNull(player, "player"));
    }

    public void ensureOnPlayerThread(Player player) {
        if (!isOnPlayerThread(player)) {
            throw new IllegalStateException("Player-bound MinimapPlayer operations must run on the player's owning Folia entity thread. Schedule with FoliaSchedulerService#runForPlayer first.");
        }
    }

    private <T> void completeIfPlayerAvailable(Player player, Supplier<T> supplier, CompletableFuture<T> future) {
        if (!player.isOnline() || !player.isValid()) {
            completeRetired(future);
            return;
        }

        try {
            future.complete(supplier.get());
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }

    private <T> void completeRetired(CompletableFuture<T> future) {
        future.completeExceptionally(new IllegalStateException("Player scheduler is retired or player is offline."));
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
