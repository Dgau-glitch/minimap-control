package com.funniray.minimap.folia;

import com.funniray.minimap.common.version.Version;
import com.funniray.minimap.folia.impl.FoliaServer;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.Integer.parseInt;

public class ViaHook {
    private final Object api;
    private final Method getPlayerVersion;
    private final Method getProtocol;
    private final Method isRegistered;
    private final Method getName;

    public ViaHook() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> viaClass = Class.forName("com.viaversion.viaversion.api.Via");
        Class<?> viaApiClass = Class.forName("com.viaversion.viaversion.api.ViaAPI");
        Class<?> protocolVersionClass = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");

        this.api = viaClass.getMethod("getAPI").invoke(null);
        this.getPlayerVersion = viaApiClass.getMethod("getPlayerVersion", Object.class);
        this.getProtocol = protocolVersionClass.getMethod("getProtocol", int.class);
        this.isRegistered = protocolVersionClass.getMethod("isRegistered", int.class);
        this.getName = protocolVersionClass.getMethod("getName");
    }

    public Version getPlayerVersion(Player player) {
        try {
            int protoVersion = (int) getPlayerVersion.invoke(api, player);
            Object version = getProtocol.invoke(null, protoVersion);

            if (!(boolean) isRegistered.invoke(null, protoVersion)) {
                FoliaMinimap.getInstance().getLogger().info("ViaVersion returned unknown for player " + player.getName() + " (protocol version " + protoVersion + "). This may cause issues if they're using Xaero's minimap. Consider updating ViaVersion");
                return new FoliaServer().getMinecraftVersion();
            }

            return parseVersionName((String) getName.invoke(version));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to read ViaVersion player protocol version", e);
        }
    }

    private Version parseVersionName(String versionName) {
        String[] ver = versionName.replaceAll("x","0").split("-")[0].split("\\.");
        if (ver.length < 3) {
            return new Version(parseInt(ver[0]), parseInt(ver[1]), 0);
        } else if (ver.length == 3) {
            return new Version(parseInt(ver[0]), parseInt(ver[1]), parseInt(ver[2]));
        } else {
            throw new RuntimeException("Cannot parse version " + versionName);
        }
    }
}
