package xyz.hynse.hynseapi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import xyz.hynse.hynseapi.Cache.DiscordUserIdsCache;
import xyz.hynse.hynseapi.Cache.DiscordUsernamesCache;
import xyz.hynse.hynseapi.Util.ServerStats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ServerDataExporter {
    private DiscordUserIdsCache discordUserIdsCache;
    private DiscordUsernamesCache discordUsernamesCache;
    public ServerDataExporter(DiscordUserIdsCache discordUserIdsCache, DiscordUsernamesCache discordUsernamesCache) {
        this.discordUserIdsCache = discordUserIdsCache;
        this.discordUsernamesCache = discordUsernamesCache;
    }
    public Map<String, Object> getServerData() {
        // Add basic server data
        Map<String, Object> data = new HashMap<>();
        ServerStats serverStats = new ServerStats();
        data.put("online", Bukkit.getOnlinePlayers().size());
        data.put("max_online", Bukkit.getMaxPlayers());
        data.put("total_join", Bukkit.getOfflinePlayers().length);
        data.put("group_whitelist_count", serverStats.getPlayersWithPermission());
        data.put("server_age", serverStats.getServerAgeInDays());
        data.put("world_size_global_gb", serverStats.getWorldSizeInGB());
        data.put("version_git", Bukkit.getVersion());

        // Add top player data
        List<OfflinePlayer> allPlayers = new ArrayList<>(Arrays.asList(Bukkit.getOfflinePlayers()));
        allPlayers.sort((p1, p2) -> Long.compare(p2.getStatistic(Statistic.PLAY_ONE_MINUTE), p1.getStatistic(Statistic.PLAY_ONE_MINUTE)));

        for (int i = 0; i < 10 && i < allPlayers.size(); i++) {
            OfflinePlayer player = allPlayers.get(i);
            String playerName = player.getName();
            String playerUUID = player.getUniqueId().toString();
            int onlineTimeSeconds = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;

            int days = onlineTimeSeconds / (24 * 60 * 60);
            int hours = (onlineTimeSeconds % (24 * 60 * 60)) / (60 * 60);
            int minutes = (onlineTimeSeconds % (60 * 60)) / 60;

            // Add the player data to the main server data map
            data.put("top_username_" + (i + 1), playerName);
            data.put("top_uuid_" + (i + 1), playerUUID);
            data.put("top_time_day_" + (i + 1), days);
            data.put("top_time_hour_" + (i + 1), hours);
            data.put("top_time_minute_" + (i + 1), minutes);


            // Add DiscordSRV user ID and user name if available
            UUID uuid = player.getUniqueId();
            if (player.isOnline() && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                Player onlinePlayer = (Player) player;
                String discordUserId = PlaceholderAPI.setPlaceholders(onlinePlayer, "%discordsrv_user_id%");
                String discordUserName = PlaceholderAPI.setPlaceholders(onlinePlayer, "%discordsrv_user_name%");

                if (!discordUserId.equals(discordUserIdsCache.getDiscordUserIdsCache().get(uuid))) {
                    discordUserIdsCache.getDiscordUserIdsCache().put(uuid, discordUserId);
                    discordUserIdsCache.saveDiscordUserIdsCache();
                }

// Store Discord username in cache
                if (!discordUserName.equals(discordUsernamesCache.getDiscordUserNamesCache().get(uuid))) {
                    discordUsernamesCache.getDiscordUserNamesCache().put(uuid, discordUserName);
                    discordUsernamesCache.saveDiscordUserNamesCache();
                }

                data.put("top_discord_user_id_" + (i + 1), discordUserIdsCache.getDiscordUserIdsCache().get(uuid));
                data.put("top_discord_user_name_" + (i + 1), discordUsernamesCache.getDiscordUserNamesCache().get(uuid));
            } else {
                if (discordUserIdsCache.getDiscordUserIdsCache().containsKey(uuid)) {
                    data.put("top_discord_user_id_" + (i + 1), discordUserIdsCache.getDiscordUserIdsCache().get(uuid));
                }
                if (discordUsernamesCache.getDiscordUserNamesCache().containsKey(uuid)) {
                    data.put("top_discord_user_name_" + (i + 1), discordUsernamesCache.getDiscordUserNamesCache().get(uuid));
                }
            }
        }
        return data;
    }
}