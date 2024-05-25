    package xyz.hynse.hynseapi;

    import com.google.gson.Gson;
    import com.sun.net.httpserver.HttpServer;
    import org.bukkit.Bukkit;
    import org.bukkit.plugin.java.JavaPlugin;
    import xyz.hynse.hynseapi.Cache.DiscordUserIdsCache;
    import xyz.hynse.hynseapi.Cache.DiscordUsernamesCache;
    import xyz.hynse.hynseapi.Util.BlockPlaceListener;
    import xyz.hynse.hynseapi.Util.SchedulerUtil;

    import java.io.File;
    import java.io.FileWriter;
    import java.io.IOException;
    import java.io.OutputStream;
    import java.net.InetSocketAddress;
    import java.util.Map;
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
            SchedulerUtil.runGlobalFixRateScheduler(this, this::exportServerData, 1, 60);
            Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(this), this);
            startHttpServer();
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
        private void startHttpServer() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(7697), 0);
                server.createContext("/player", exchange -> {
                    String clientIP = exchange.getRemoteAddress().getAddress().getHostAddress();
                    if (!isRateLimited(clientIP)) {
                        String query = exchange.getRequestURI().getQuery();
                        if (query != null && query.startsWith("name=")) {
                            String playerName = query.split("=")[1];
                            Map<String, Object> playerData = serverDataExporter.getPlayerData(playerName);

                            if (playerData != null) {
                                String response = GSON.toJson(playerData);
                                exchange.sendResponseHeaders(200, response.getBytes().length);
                                OutputStream os = exchange.getResponseBody();
                                os.write(response.getBytes());
                                os.close();
                            } else {
                                String response = "Player not found";
                                exchange.sendResponseHeaders(404, response.getBytes().length);
                                OutputStream os = exchange.getResponseBody();
                                os.write(response.getBytes());
                                os.close();
                            }
                        } else {
                            String response = "Invalid request";
                            exchange.sendResponseHeaders(400, response.getBytes().length);
                            OutputStream os = exchange.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                        }
                    } else {
                        String response = "Rate limit exceeded";
                        exchange.sendResponseHeaders(429, response.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                });
                server.setExecutor(Executors.newCachedThreadPool());
                server.start();
                getLogger().info("HTTP server started on port 7697");
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
