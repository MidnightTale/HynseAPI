package xyz.hynse.hynseapi;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import xyz.hynse.hynseapi.Cache.DiscordUserIdsCache;
import xyz.hynse.hynseapi.Cache.DiscordUsernamesCache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class HynseAPI extends JavaPlugin implements Listener {

    private static final Gson GSON = new Gson();
    private File DATA_FILE;
    private ServerDataExporter serverDataExporter;
    // Rate limit parameters
    private static final int MAX_REQUESTS_PER_MINUTE = 60; // Adjust the rate limit as needed
    private static final ConcurrentHashMap<String, RateLimiter> RATE_LIMITERS = new ConcurrentHashMap<>();

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

        // Schedule a task to run every 30 min
        FoliaScheduler.getGlobalRegionScheduler().runAtFixedRate(this, this::exportServerData, 1, 36000);
        getServer().getPluginManager().registerEvents(this, this);

//        startHttpServer();
    }
    private void exportServerData(Object ignored) {
        // Your existing exportServerData() implementation
        Map<String, Object> data = serverDataExporter.getServerData();

        // Define the directory path
        File webDirectory = new File(getDataFolder(), "web");

        // Check if the web directory exists, create it if it doesn't
        if (!webDirectory.exists()) {
            if (!webDirectory.mkdirs()) {
                getLogger().warning("Failed to create web directory.");
                return;
            }
        }

        // Define the file path within the web directory
        File serverDataFile = new File(webDirectory, "server.json");

        // Write the server data to the server.json file
        String json = GSON.toJson(data);
        try (FileWriter writer = new FileWriter(serverDataFile)) {
            writer.write(json);
        } catch (IOException e) {
            getLogger().warning("Failed to export server data.");
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Get the joined player
        OfflinePlayer player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Ensure the directory exists
        File playerDirectory = new File(getDataFolder() + "/web/player/");
        if (!playerDirectory.exists()) {
            playerDirectory.mkdirs(); // Create the directory if it doesn't exist
        }

        // Fetch player data and write it to a file
        Map<String, Object> playerData = serverDataExporter.getPlayerDataByUUID(playerUUID);
        if (playerData != null) {
            // Write player data to file
            File playerFile = new File(playerDirectory, playerUUID + ".json");
            try (FileWriter writer = new FileWriter(playerFile)) {
                String json = GSON.toJson(playerData);
                writer.write(json);
            } catch (IOException e) {
                getLogger().warning("Failed to write player data for " + player.getName());
                e.printStackTrace();
            }
        }
    }

    private void startHttpServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(7699), 0);
            server.createContext("/", exchange -> {
                // Log the received request
                getLogger().info("Received request: " + exchange.getRequestURI().toString());

                String requestURI = exchange.getRequestURI().toString();
                File requestedFile = new File(getDataFolder() + "/web" + requestURI);

                if (requestedFile.exists()) {
                    // Serve the requested file
                    byte[] fileContent = Files.readAllBytes(requestedFile.toPath());
                    exchange.sendResponseHeaders(200, fileContent.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(fileContent);
                    os.close();
                } else {
                    // If the requested file doesn't exist, respond with 404 Not Found
                    String response = "File not found";
                    exchange.sendResponseHeaders(404, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            });
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            getLogger().info("HTTP server started on port 7699");
        } catch (IOException e) {
            getLogger().severe("Failed to start HTTP server");
            e.printStackTrace();
        }
    }




    private boolean isRateLimited(String clientIP) {
        RateLimiter rateLimiter = RATE_LIMITERS.computeIfAbsent(clientIP, k -> new RateLimiter(MAX_REQUESTS_PER_MINUTE, 1, TimeUnit.MINUTES));
        return !rateLimiter.allowRequest();
    }

    private static class RateLimiter {
        private final int maxRequests;
        private final long timeframe;
        private int requestCount;
        private long lastRequestTime;

        public RateLimiter(int maxRequests, long timeframe, TimeUnit unit) {
            this.maxRequests = maxRequests;
            this.timeframe = unit.toMillis(timeframe);
            this.requestCount = 0;
            this.lastRequestTime = System.currentTimeMillis();
        }

        public synchronized boolean allowRequest() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime > timeframe) {
                lastRequestTime = currentTime;
                requestCount = 0;
            }
            requestCount++;
            return requestCount <= maxRequests;
        }
    }
}
