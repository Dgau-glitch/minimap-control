package com.funniray.minimap.folia;

import com.funniray.minimap.folia.service.FoliaSchedulerService;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;

public final class FoliaMinimap extends JavaPlugin {
    private static FoliaMinimap instance;
    private final FoliaSchedulerService schedulerService = new FoliaSchedulerService(this);
    private final FoliaMain main = new FoliaMain(this, schedulerService);

    public ViaHook viaHook;
    public boolean viaHooked;

    private BukkitAudiences adventure;

    public BukkitAudiences adventure() {
        if(this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        this.adventure = BukkitAudiences.create(this);
        getServer().getPluginManager().registerEvents(main, this);
        main.enableSelf();

        try {
            this.viaHook = new ViaHook();
            this.viaHooked = true;
        } catch (ClassNotFoundException | NoClassDefFoundError e ) {
            // failed to hook viaversion. Expected if viaversion isn't installed.
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        main.disableSelf();
        if(this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

    public FoliaSchedulerService getSchedulerService() {
        return schedulerService;
    }

    public static FoliaMinimap getInstance() {
        return instance;
    }
}