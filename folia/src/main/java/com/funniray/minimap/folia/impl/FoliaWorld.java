package com.funniray.minimap.folia.impl;

import com.funniray.minimap.common.api.MinimapLocation;
import com.funniray.minimap.common.api.MinimapWorld;
import org.bukkit.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FoliaWorld implements MinimapWorld {
    private final World nativeWorld;
    private final String name;
    private final String keyedName;

    public FoliaWorld(World nativeWorld) {
        this.nativeWorld = nativeWorld;
        this.name = nativeWorld.getName();
        this.keyedName = resolveKeyedName(nativeWorld, name);
    }

    @Override
    public String getName() {
        return name;
    }

    public String getKeyedName() {
        return keyedName;
    }

    private String resolveKeyedName(World world, String fallbackName) {
        try {
            Method getKey = world.getClass().getMethod("getKey");
            NamespacedKey key = (NamespacedKey) getKey.invoke(world);
            return key.toString();
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            return fallbackName;
        }
    }

    @Override
    public MinimapLocation getLocation(double x, double y, double z) {
        Location location = new Location(nativeWorld, x, y, z);

        return new FoliaLocation(location);
    }
}