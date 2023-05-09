package xyz.hynse.hynseapi.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import java.io.File;
import java.util.Objects;

public class ServerStats {

    public int getPlayersWithPermission() {
        int count = 0;
        Permission perm = new Permission("group.whitelist");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(perm)) {
                count++;
            }
        }

        return count;
    }

    public long getServerAgeInDays() {
        long worldTimeInTicks = Bukkit.getServer().getWorlds().get(0).getFullTime();
        return worldTimeInTicks / (20 * 60 * 24);
    }

    public double getWorldSizeInGB() {
        long totalSize = 0;
        for (org.bukkit.World world : Bukkit.getServer().getWorlds()) {
            totalSize += getFolderSize(world.getWorldFolder());
        }
        double worldSizeGB = totalSize / (double) (1024 * 1024 * 1024);
        return Math.round(worldSizeGB * 100.0) / 100.0;
    }

    private long getFolderSize(File folder) {
        long size = 0;
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isFile()) {
                size += file.length();
            } else {
                size += getFolderSize(file);
            }
        }
        return size;
    }
}