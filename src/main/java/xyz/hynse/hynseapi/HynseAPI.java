    package xyz.hynse.hynseapi;

    import com.google.gson.Gson;
    import org.bukkit.Bukkit;
    import org.bukkit.plugin.java.JavaPlugin;
    import xyz.hynse.hynseapi.Cache.DiscordUserIdsCache;
    import xyz.hynse.hynseapi.Cache.DiscordUsernamesCache;
    import xyz.hynse.hynseapi.Util.BlockPlaceListener;
    import xyz.hynse.hynseapi.Util.SchedulerUtil;

    import java.io.File;
    import java.io.FileWriter;
    import java.io.IOException;
    import java.util.Map;
    import java.util.concurrent.TimeUnit;

    public final class HynseAPI extends JavaPlugin {

        private static final Gson GSON = new Gson();
        private File DATA_FILE;
        private ServerDataExporter serverDataExporter;

        @Override
        public void onEnable() {
            // Create the data folder and data file if they do not exist
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            DATA_FILE = new File(getDataFolder(), "data.json");
            DiscordUsernamesCache discordUsernamesCache = new DiscordUsernamesCache(this);
            DiscordUserIdsCache discordUserIdsCache = new DiscordUserIdsCache(this);
            serverDataExporter = new ServerDataExporter(discordUserIdsCache, discordUsernamesCache);

            // Schedule a task to run every minute
            SchedulerUtil.runAsyncFixRateScheduler(this, this::exportServerData, 0, 60);
            Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        }

        private void exportServerData() {
            // Get the server data using the ServerDataExporter instance
            Map<String, Object> data = serverDataExporter.getServerData();

            // Write the server data to a file
            String json = GSON.toJson(data);
            try (FileWriter writer = new FileWriter(DATA_FILE)) {
                writer.write(json);
            } catch (IOException e) {
                getLogger().warning("Failed to export server data.");
                e.printStackTrace();
            }
        }
    }
