package com.funniray.minimap.folia.impl;

import com.funniray.minimap.common.api.MinimapLocation;
import com.funniray.minimap.common.api.MinimapWorld;
import org.bukkit.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FoliaWorld implements MinimapWorld {
    private World nativeWorld;

    public FoliaWorld(World nativeWorld) {
        this.nativeWorld = nativeWorld;
    }

    @Override
    public String getName() {
        return nativeWorld.getName();
    }

    public String getKeyedName() {
        try {
            Method getKey = nativeWorld.getClass().getMethod("getKey");
            NamespacedKey key = (NamespacedKey) getKey.invoke(nativeWorld);
            return key.toString();
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            return getName();
        }
    }

    @Override
    public MinimapLocation getLocation(double x, double y, double z) {
        Location location = new Location(nativeWorld, x, y, z);

        return new FoliaLocation(location);
    }
}