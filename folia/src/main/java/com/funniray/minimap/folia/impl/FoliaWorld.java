package com.funniray.minimap.folia.impl;

import com.funniray.minimap.common.api.MinimapLocation;
import com.funniray.minimap.common.api.MinimapWorld;
import org.bukkit.Location;
import org.bukkit.World;

public class FoliaWorld implements MinimapWorld {
    private final World nativeWorld;
    private final String name;
    private final String keyedName;

    public FoliaWorld(World nativeWorld) {
        this.nativeWorld = nativeWorld;
        this.name = nativeWorld.getName();
        this.keyedName = nativeWorld.getKey().toString();
    }

    @Override
    public String getName() {
        return name;
    }

    public String getKeyedName() {
        return keyedName;
    }

    @Override
    public MinimapLocation getLocation(double x, double y, double z) {
        Location location = new Location(nativeWorld, x, y, z);

        return new FoliaLocation(location);
    }
}