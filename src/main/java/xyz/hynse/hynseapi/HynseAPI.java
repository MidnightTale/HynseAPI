package xyz.hynse.hynseapi;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.hynse.hynseapi.Cache.DiscordUserIdsCache;
import xyz.hynse.hynseapi.Cache.DiscordUsernamesCache;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class HynseAPI extends JavaPlugin {

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

        // Schedule a task to run every minute
        SchedulerUtil.runAsyncFixRateScheduler(this, this::exportServerData, 0, 60);

        startHttpServer();
    }

    private void startHttpServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(7699), 0);
            server.createContext("/player", exchange -> {
                String clientIP = exchange.getRemoteAddress().getAddress().getHostAddress();
                if (!isRateLimited(clientIP)) {
                    String requestURI = exchange.getRequestURI().toString();
                    String[] parts = requestURI.split("/");
                    if (parts.length == 3) {
                        String identifier = parts[2];
                        Map<String, Object> playerData;

                        // Check if the identifier is a trimmed UUID
                        if (identifier.length() == 32) {
                            String playerUUIDWithDashes = identifier.substring(0, 8) + "-" + identifier.substring(8, 12) + "-" + identifier.substring(12, 16) + "-" + identifier.substring(16, 20) + "-" + identifier.substring(20);
                            playerData = serverDataExporter.getPlayerDataByUUID(playerUUIDWithDashes);
                        } else {
                            // Check if the identifier is a valid UUID
                            try {
                                UUID playerUUID = UUID.fromString(identifier);
                                playerData = serverDataExporter.getPlayerDataByUUID(playerUUID.toString());
                            } catch (IllegalArgumentException e) {
                                // Identifier is not a valid UUID, assume it's a player name
                                playerData = serverDataExporter.getPlayerDataByName(identifier);
                            }
                        }

                        if (playerData != null) {
                            String response = GSON.toJson(playerData);
                            exchange.getResponseHeaders().set("Content-Type", "application/json"); // Set content type to JSON
                            exchange.sendResponseHeaders(200, response.getBytes().length);
                            OutputStream os = exchange.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                        } else {
                            String response = "Player not found";
                            exchange.getResponseHeaders().set("Content-Type", "text/plain"); // Set content type to plain text
                            exchange.sendResponseHeaders(404, response.getBytes().length);
                            OutputStream os = exchange.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                        }
                    } else {
                        String response = "Invalid request";
                        exchange.getResponseHeaders().set("Content-Type", "text/plain"); // Set content type to plain text
                        exchange.sendResponseHeaders(400, response.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                } else {
                    String response = "Rate limit exceeded";
                    exchange.getResponseHeaders().set("Content-Type", "text/plain"); // Set content type to plain text
                    exchange.sendResponseHeaders(429, response.getBytes().length);
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
