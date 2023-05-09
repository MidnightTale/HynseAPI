package xyz.hynse.hynseapi.Cache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class WhitelistCache {
    private final JavaPlugin plugin;
    private final File playerDataFolder;
    private final Gson gson = new Gson();
    private final File WHITELIST_CACHE_FILE;
    private Map<String, Map<String, Integer>> whitelistCache;

    public WhitelistCache(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getServer().getWorldContainer(), "playerdata");
        this.WHITELIST_CACHE_FILE = new File(plugin.getDataFolder(), "whitelistCache.json");
        loadWhitelistCache();
    }

    private void loadWhitelistCache() {
        if (!WHITELIST_CACHE_FILE.exists()) {
            whitelistCache = new HashMap<>();
            return;
        }

        try (FileReader reader = new FileReader(WHITELIST_CACHE_FILE)) {
            Type type = new TypeToken<HashMap<String, Map<String, Integer>>>(){}.getType();
            whitelistCache = gson.fromJson(reader, type);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load whitelist cache.");
            e.printStackTrace();
            whitelistCache = new HashMap<>();
        }
    }

    public void saveWhitelistCache() {
        try (FileWriter writer = new FileWriter(WHITELIST_CACHE_FILE)) {
            gson.toJson(whitelistCache, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save whitelist cache.");
            e.printStackTrace();
        }
    }

    public Map<String, Integer> getWhitelistCount() {
        return getPermissionCounts("group.whitelist");
    }

    public Map<String, Integer> getPermissionCounts(String permissionName) {
        if (!whitelistCache.containsKey(permissionName)) {
            whitelistCache.put(permissionName, new HashMap<>());
            updateCache(permissionName);
        }
        return whitelistCache.get(permissionName);
    }

    private void updateCache(String permissionName) {
        Permission perm = new Permission(permissionName);
        for (File playerFile : Objects.requireNonNull(playerDataFolder.listFiles())) {
            if (playerFile.getName().endsWith(".dat")) {
                try {
                    YamlConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
                    UUID uuid = UUID.fromString(playerFile.getName().replace(".dat", ""));
                    boolean hasPerm = playerData.getStringList("permissions").contains(perm.getName());
                    Map<String, Integer> playerPermissions = whitelistCache.get(permissionName);
                    playerPermissions.put(uuid.toString(), hasPerm ? 1 : 0);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to read player data file: " + playerFile.getName());
                    e.printStackTrace();
                }
            }
        }
    }
}
