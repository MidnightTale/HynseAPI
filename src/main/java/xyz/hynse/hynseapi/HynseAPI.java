    package xyz.hynse.hynseapi;

    import com.google.gson.Gson;
    import com.google.gson.reflect.TypeToken;
    import me.clip.placeholderapi.PlaceholderAPI;
    import org.bukkit.Bukkit;
    import org.bukkit.OfflinePlayer;
    import org.bukkit.Statistic;
    import org.bukkit.entity.Player;
    import org.bukkit.permissions.Permission;
    import org.bukkit.plugin.java.JavaPlugin;

    import java.io.File;
    import java.io.FileReader;
    import java.io.FileWriter;
    import java.io.IOException;
    import java.lang.reflect.Type;
    import java.util.*;
    import java.util.concurrent.TimeUnit;

    public final class HynseAPI extends JavaPlugin {

        private static final Gson GSON = new Gson();
        private File DATA_FILE;
        private File DISCORD_IDS_CACHE_FILE;
        private Map<UUID, String> discordUserIdsCache;

        @Override
        public void onEnable() {
            // Create the data folder and data file if they do not exist
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            DATA_FILE = new File(getDataFolder(), "data.json");
            DISCORD_IDS_CACHE_FILE = new File(getDataFolder(), "discordUserIdsCache.json");
            loadDiscordUserIdsCache();

            // Schedule a task to run every minute
            Bukkit.getAsyncScheduler().runAtFixedRate(this, task -> exportServerData(), 0, 60, TimeUnit.SECONDS);
        }
        private void loadDiscordUserIdsCache() {
            if (!DISCORD_IDS_CACHE_FILE.exists()) {
                discordUserIdsCache = new HashMap<>();
                return;
            }

            try (FileReader reader = new FileReader(DISCORD_IDS_CACHE_FILE)) {
                Type type = new TypeToken<HashMap<UUID, String>>(){}.getType();
                discordUserIdsCache = GSON.fromJson(reader, type);
            } catch (IOException e) {
                getLogger().warning("Failed to load Discord User IDs cache.");
                e.printStackTrace();
                discordUserIdsCache = new HashMap<>();
            }
        }
        private void saveDiscordUserIdsCache() {
            try (FileWriter writer = new FileWriter(DISCORD_IDS_CACHE_FILE)) {
                GSON.toJson(discordUserIdsCache, writer);
            } catch (IOException e) {
                getLogger().warning("Failed to save Discord User IDs cache.");
                e.printStackTrace();
            }
        }

        private void exportServerData() {
            // Add basic server data
            Map<String, Object> data = new HashMap<>();
            data.put("online", Bukkit.getOnlinePlayers().size());
            data.put("max_online", Bukkit.getMaxPlayers());
            data.put("total_join", Bukkit.getOfflinePlayers().length);
            data.put("group_whitelist_count", getPlayersWithPermission());
            data.put("server_age", getServerAgeInDays());
            data.put("world_size_global_gb", getWorldSizeInGB());
            data.put("version_git", Bukkit.getVersion());

            // Add top player data
            List<OfflinePlayer> allPlayers = new ArrayList<>(Arrays.asList(Bukkit.getOfflinePlayers()));
            allPlayers.sort((p1, p2) -> Long.compare(p2.getStatistic(Statistic.PLAY_ONE_MINUTE), p1.getStatistic(Statistic.PLAY_ONE_MINUTE)));

            for (int i = 0; i < 10 && i < allPlayers.size(); i++) {
                OfflinePlayer player = allPlayers.get(i);
                String playerName = player.getName();
                String playerUUID = player.getUniqueId().toString();
                double onlineTimeSeconds = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20.0;

                // Add the player data to the main server data map
                data.put("top_username_" + (i + 1), playerName);
                data.put("top_uuid_" + (i + 1), playerUUID);
                data.put("top_time_" + (i + 1), onlineTimeSeconds);

                // Add DiscordSRV user ID if available
                UUID uuid = player.getUniqueId();
                if (player.isOnline() && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    String discordUserId = PlaceholderAPI.setPlaceholders((Player) player, "%discordsrv_user_id%");
                    if (!discordUserId.equals(discordUserIdsCache.get(uuid))) {
                        discordUserIdsCache.put(uuid, discordUserId);
                        saveDiscordUserIdsCache();
                    }
                }
                if (discordUserIdsCache.containsKey(uuid)) {
                    data.put("top_placeholder_data_from_discordsrv_user_id_" + (i + 1), discordUserIdsCache.get(uuid));
                }
            }

            // Write the server data to a file
            String json = GSON.toJson(data);
            try (FileWriter writer = new FileWriter(DATA_FILE)) {
                writer.write(json);
            } catch (IOException e) {
                getLogger().warning("Failed to export server data.");
                e.printStackTrace();
            }
        }







        private int getPlayersWithPermission() {
            int count = 0;
            Permission perm = new Permission("group.whitelist");

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(perm)) {
                    count++;
                }
            }

            return count;
        }

        private long getServerAgeInDays() {
            long worldTimeInTicks = Bukkit.getServer().getWorlds().get(0).getFullTime();
            return worldTimeInTicks / (20 * 60 * 24);
        }

        private double getWorldSizeInGB() {
            // Calculate world size in gigabytes
            // You may need to adjust this based on the actual path to your world folders
            long totalSize = 0;
            for (org.bukkit.World world : Bukkit.getServer().getWorlds()) {
                totalSize += getFolderSize(world.getWorldFolder());
            }
            double worldSizeGB = totalSize / (double) (1024 * 1024 * 1024);
            return Math.round(worldSizeGB * 100.0) / 100.0;
        }


        private long getFolderSize(java.io.File folder) {
            long size = 0;
            for (java.io.File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += getFolderSize(file);
                }
            }
            return size;
        }
    }
