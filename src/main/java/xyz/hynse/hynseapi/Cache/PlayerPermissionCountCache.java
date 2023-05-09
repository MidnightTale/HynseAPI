package xyz.hynse.hynseapi.Cache;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerPermissionCountCache {
    private final Map<UUID, Integer> cache = new HashMap<>();
    private final String permission;

    public PlayerPermissionCountCache(String permission) {
        this.permission = permission;
    }

    public void updateCache() {
        int count = 0;
        Permission perm = new Permission(permission);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(perm)) {
                count++;
            }
        }

        cache.put(UUID.randomUUID(), count);
    }

    public int getCount() {
        int count = 0;

        for (Integer value : cache.values()) {
            count += value;
        }

        return count;
    }
}
