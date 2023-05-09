package xyz.hynse.hynseapi.Cache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DiscordUserIdsCache {
    private final JavaPlugin plugin;
    private final Gson GSON = new Gson();
    private final java.io.File DISCORD_USER_IDS_CACHE_FILE;
    private Map<UUID, String> discordUserIdsCache;

    public DiscordUserIdsCache(JavaPlugin plugin) {
        this.plugin = plugin;
        this.DISCORD_USER_IDS_CACHE_FILE = new java.io.File(plugin.getDataFolder(), "discordUserIdsCache.json");
        loadDiscordUserIdsCache();
    }

    private void loadDiscordUserIdsCache() {
        if (!DISCORD_USER_IDS_CACHE_FILE.exists()) {
            discordUserIdsCache = new HashMap<>();
            return;
        }

        try (FileReader reader = new FileReader(DISCORD_USER_IDS_CACHE_FILE)) {
            Type type = new TypeToken<HashMap<UUID, String>>(){}.getType();
            discordUserIdsCache = GSON.fromJson(reader, type);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load Discord User IDs cache.");
            e.printStackTrace();
            discordUserIdsCache = new HashMap<>();
        }
    }

    public void saveDiscordUserIdsCache() {
        try (FileWriter writer = new FileWriter(DISCORD_USER_IDS_CACHE_FILE)) {
            GSON.toJson(discordUserIdsCache, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save Discord User IDs cache.");
            e.printStackTrace();
        }
    }

    public Map<UUID, String> getDiscordUserIdsCache() {
        return discordUserIdsCache;
    }
}
