//package xyz.hynse.hynseapi.Util;
//
//import com.google.gson.Gson;
//import com.google.gson.reflect.TypeToken;
//import org.bukkit.Material;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.Listener;
//import org.bukkit.event.block.BlockPlaceEvent;
//import xyz.hynse.hynseapi.HynseAPI;
//
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.lang.reflect.Type;
//import java.util.HashMap;
//import java.util.Map;
//
//public class BlockPlaceListener implements Listener {
//    private HynseAPI plugin;
//    private static final Gson GSON = new Gson();
//    private static final Type TYPE = new TypeToken<Map<Material, Integer>>(){}.getType();
//    private Map<Material, Integer> blockUsageCache = new HashMap<>();
//    private int blockCount = 0;
//
//    public BlockPlaceListener(HynseAPI plugin) {
//        this.plugin = plugin;
//        this.blockUsageCache = readBlockUsageCache();
//        if (this.blockUsageCache == null) {
//            this.blockUsageCache = new HashMap<>();
//        }
//    }
//
//    @EventHandler
//    public void onBlockPlace(BlockPlaceEvent event) {
//        Material blockType = event.getBlock().getType();
//
//        if (blockUsageCache.containsKey(blockType)) {
//            blockUsageCache.put(blockType, blockUsageCache.get(blockType) + 1);
//        } else {
//            blockUsageCache.put(blockType, 1);
//        }
//
//        blockCount++;
//
//        if (blockCount >= 1000) {
//            writeBlockUsageCache(blockUsageCache);
//            blockCount = 0;
//        }
//    }
//
//    private Map<Material, Integer> readBlockUsageCache() {
//        Map<Material, Integer> blockUsageCache = new HashMap<>();
//        try (FileReader reader = new FileReader(plugin.getDataFolder() + "/mostblocks.json")) {
//            blockUsageCache = GSON.fromJson(reader, TYPE);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return blockUsageCache;
//    }
//
//    private void writeBlockUsageCache(Map<Material, Integer> blockUsageCache) {
//        try (FileWriter writer = new FileWriter(plugin.getDataFolder() + "/mostblocks.json")) {
//            GSON.toJson(blockUsageCache, writer);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
