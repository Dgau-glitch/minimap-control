package com.funniray.minimap.folia.impl;

import com.funniray.minimap.common.api.MinimapLocation;
import com.funniray.minimap.common.api.MinimapWorld;
import org.bukkit.Location;

public class FoliaLocation implements MinimapLocation {
    private Location nativeLocation;

    public FoliaLocation(Location nativeLocation) {
        this.nativeLocation = nativeLocation;
    }

    @Override
    public double getX() {
        return nativeLocation.getX();
    }

    @Override
    public double getY() {
        return nativeLocation.getY();
    }

    @Override
    public double getZ() {
        return nativeLocation.getZ();
    }

    @Override
    public MinimapWorld getWorld() {
        return new FoliaWorld(nativeLocation.getWorld());
    }

    public Location getNativeLocation() {
        return nativeLocation;
    }
}