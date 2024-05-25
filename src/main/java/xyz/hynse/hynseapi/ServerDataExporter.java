package xyz.hynse.hynseapi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import xyz.hynse.hynseapi.Cache.DiscordUserIdsCache;
import xyz.hynse.hynseapi.Cache.DiscordUsernamesCache;
import xyz.hynse.hynseapi.Util.ServerStats;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ServerDataExporter {
    private final DiscordUserIdsCache discordUserIdsCache;
    private final DiscordUsernamesCache discordUsernamesCache;
    public ServerDataExporter(DiscordUserIdsCache discordUserIdsCache, DiscordUsernamesCache discordUsernamesCache) {
        this.discordUserIdsCache = discordUserIdsCache;
        this.discordUsernamesCache = discordUsernamesCache;}
    public Map<String, Object> getServerData() {
        // Add basic server data
        Map<String, Object> data = new HashMap<>();
        ServerStats serverStats = new ServerStats();
        List<String> onlinePlayerNames = serverStats.getOnlinePlayerNames();
        List<String> bannedPlayerNames = serverStats.getBannedPlayerNames();
        int bannedPlayerCount = serverStats.getBannedPlayerCount();
        data.put("banned_player_count", bannedPlayerCount);
        data.put("banned_players", bannedPlayerNames);
        data.put("online_players", onlinePlayerNames);
        data.put("online", Bukkit.getOnlinePlayers().size());
        data.put("max_online", Bukkit.getMaxPlayers());
        data.put("total_join", Bukkit.getOfflinePlayers().length);
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

    public Map<String, Object> getPlayerDataByUUID(UUID playerUUID) {
        // Use UUID to get player data
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (player == null || !player.hasPlayedBefore()) {
            return null;
        }

        Map<String, Object> dataplayer = new HashMap<>();
        dataplayer.put("name", player.getName());
        dataplayer.put("uuid", player.getUniqueId().toString());
        // Calculate playtime in seconds and format it
        int onlineTimeSeconds = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
        dataplayer.put("online_time_seconds", onlineTimeSeconds);


        int days = (int) TimeUnit.SECONDS.toDays(onlineTimeSeconds);
        int hours = (int) (TimeUnit.SECONDS.toHours(onlineTimeSeconds) % 24);
        int minutes = (int) (TimeUnit.SECONDS.toMinutes(onlineTimeSeconds) % 60);
        dataplayer.put("playtime", String.format("%d days, %d hours, %d minutes", days, hours, minutes));

        // Add join date
        Date joinDate = new Date(player.getFirstPlayed());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dataplayer.put("join_since", dateFormat.format(joinDate));

        // Add player level (if the player is online)
        if (player.isOnline()) {
            Player onlinePlayer = (Player) player;
            dataplayer.put("level", onlinePlayer.getLevel());
        } else {
            dataplayer.put("level", "N/A"); // Level is not available if the player is offline
        }

        // Add blocks mined and placed statistics
        int blocksMined = 0;
        int blocksPlaced = 0;
        for (Material material : Material.values()) {
            if (material.isBlock()) {
                blocksMined += player.getStatistic(Statistic.MINE_BLOCK, material);
                blocksPlaced += player.getStatistic(Statistic.USE_ITEM, material);
            }
        }
        dataplayer.put("blocks_mined", blocksMined);
        dataplayer.put("blocks_placed", blocksPlaced);

        // Add entities killed and deaths statistics
        int entitiesKilled = 0;
        for (EntityType entityType : EntityType.values()) {
            // Check if EntityType is not UNKNOWN before accessing the statistic
            if (entityType != EntityType.UNKNOWN) {
                entitiesKilled += player.getStatistic(Statistic.KILL_ENTITY, entityType);
            }
        }
        int deaths = player.getStatistic(Statistic.DEATHS);

        dataplayer.put("entities_killed", entitiesKilled);
        dataplayer.put("deaths", deaths);

        UUID uuid = player.getUniqueId();
        if (player.isOnline() && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Player onlinePlayer = (Player) player;
            String discordUserId = PlaceholderAPI.setPlaceholders(onlinePlayer, "%discordsrv_user_id%");
            String discordUserName = PlaceholderAPI.setPlaceholders(onlinePlayer, "%discordsrv_user_name%");

            if (!discordUserId.equals(discordUserIdsCache.getDiscordUserIdsCache().get(uuid))) {
                discordUserIdsCache.getDiscordUserIdsCache().put(uuid, discordUserId);
                discordUserIdsCache.saveDiscordUserIdsCache();
            }

            if (!discordUserName.equals(discordUsernamesCache.getDiscordUserNamesCache().get(uuid))) {
                discordUsernamesCache.getDiscordUserNamesCache().put(uuid, discordUserName);
                discordUsernamesCache.saveDiscordUserNamesCache();
            }

            dataplayer.put("discord_user_id", discordUserIdsCache.getDiscordUserIdsCache().get(uuid));
            dataplayer.put("discord_user_name", discordUsernamesCache.getDiscordUserNamesCache().get(uuid));
        } else {
            if (discordUserIdsCache.getDiscordUserIdsCache().containsKey(uuid)) {
                dataplayer.put("discord_user_id", discordUserIdsCache.getDiscordUserIdsCache().get(uuid));
            }
            if (discordUsernamesCache.getDiscordUserNamesCache().containsKey(uuid)) {
                dataplayer.put("discord_user_name", discordUsernamesCache.getDiscordUserNamesCache().get(uuid));
            }
        }

        return dataplayer;
    }


}