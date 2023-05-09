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

public class DiscordUsernamesCache {
    private final JavaPlugin plugin;
    private final Gson GSON = new Gson();
    private final java.io.File DISCORD_USERNAMES_CACHE_FILE;
    private Map<UUID, String> discordUserNamesCache;

    public DiscordUsernamesCache(JavaPlugin plugin) {
        this.plugin = plugin;
        this.DISCORD_USERNAMES_CACHE_FILE = new java.io.File(plugin.getDataFolder(), "discordUserNamesCache.json");
        loadDiscordUserNamesCache();
    }

    private void loadDiscordUserNamesCache() {
        if (!DISCORD_USERNAMES_CACHE_FILE.exists()) {
            discordUserNamesCache = new HashMap<>();
            return;
        }

        try (FileReader reader = new FileReader(DISCORD_USERNAMES_CACHE_FILE)) {
            Type type = new TypeToken<HashMap<UUID, String>>(){}.getType();
            discordUserNamesCache = GSON.fromJson(reader, type);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load Discord User Names cache.");
            e.printStackTrace();
            discordUserNamesCache = new HashMap<>();
        }
    }

    public void saveDiscordUserNamesCache() {
        try (FileWriter writer = new FileWriter(DISCORD_USERNAMES_CACHE_FILE)) {
            GSON.toJson(discordUserNamesCache, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save Discord User Names cache.");
            e.printStackTrace();
        }
    }

    public Map<UUID, String> getDiscordUserNamesCache() {
        return discordUserNamesCache;
    }
}
