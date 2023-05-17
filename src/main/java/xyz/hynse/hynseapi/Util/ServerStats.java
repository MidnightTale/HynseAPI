package xyz.hynse.hynseapi.Util;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServerStats {

    public long getServerAgeInDays() {
        long worldTimeInTicks = Bukkit.getServer().getWorlds().get(0).getFullTime();
        return worldTimeInTicks / (20 * 60 * 60 * 24);
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
    public List<String> getOnlinePlayerNames() {
        List<String> onlinePlayerNames = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlinePlayerNames.add(player.getName());
        }
        return onlinePlayerNames;
    }
    public List<String> getBannedPlayerNames() {
        List<String> bannedPlayerNames = new ArrayList<>();
        for (OfflinePlayer bannedPlayer : Bukkit.getBannedPlayers()) {
            bannedPlayerNames.add(bannedPlayer.getName());
        }
        return bannedPlayerNames;
    }
    public int getBannedPlayerCount() {
        return Bukkit.getBannedPlayers().size();
    }
}