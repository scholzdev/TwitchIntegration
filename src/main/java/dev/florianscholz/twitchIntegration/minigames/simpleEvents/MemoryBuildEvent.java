package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;

import java.util.*;

public class MemoryBuildEvent extends AbstractSimpleGameProvider {

    private static final int GRID_SIZE = 3;
    private static final int DISPLAY_TIME_TICKS = 10 * 20; // 10 seconds
    
    private final Random random = new Random();
    private final Material[] woolColors = new Material[]{
        Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL, 
        Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL, 
        Material.PINK_WOOL, Material.GRAY_WOOL, Material.CYAN_WOOL
    };
    
    private final Map<UUID, List<Material>> playerPatterns = new HashMap<>();
    private final Map<UUID, Location> playerBases = new HashMap<>();
    private final Map<UUID, Map<Location, BlockData>> originalBlocks = new HashMap<>();

    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        return new SimpleGameEvent.Builder(plugin)
                .name("Memory Build")
                .description("Memorize the 3x3 wool pattern and rebuild it.")
                .votingName("memory_build")
                .duration(20 * 30)
                .countdown(5)
                .saveAndRestoreInventory()
                .onStartWithEvent(event -> {
                    forEachPlayer(player -> {
                        Location gridBase = showPatternGrid(player);
                        playerBases.put(player.getUniqueId(), gridBase);
                    });

                    event.runTaskLater(DISPLAY_TIME_TICKS, () -> {
                        forEachPlayer(player -> {
                            removePatternWool(player);
                            givePatternItems(player);
                        });
                    });
                })
                .onFinishWithEvent(event -> {
                    forEachPlayer(player -> {
                        checkBuild(player);
                        removeBuildArea(player);
                        player.getInventory().clear();
                    });
                    playerPatterns.clear();
                    playerBases.clear();
                    originalBlocks.clear();
                })
                .cancelEvent(PlayerDropItemEvent.class)
                .build();
    }

    private Location showPatternGrid(Player player) {
        Location playerLoc = player.getLocation();
        
        Location gridBase = playerLoc.clone().add(
            playerLoc.getDirection().normalize().multiply(3)
        );
        gridBase.setY(playerLoc.getY() + 1);
        
        List<Material> pattern = new ArrayList<>();
        Map<Location, BlockData> originalBlocksMap = new HashMap<>();
        
        for (int row = -1; row <= GRID_SIZE; row++) {
            for (int col = -1; col <= GRID_SIZE; col++) {
                Location loc = gridBase.clone().add(0, row, col);
                Block block = loc.getBlock();
                originalBlocksMap.put(loc, block.getBlockData().clone());
            }
        }
        originalBlocks.put(player.getUniqueId(), originalBlocksMap);
        
        for (int row = -1; row <= GRID_SIZE; row++) {
            for (int col = -1; col <= GRID_SIZE; col++) {
                if (row == -1 || row == GRID_SIZE || col == -1 || col == GRID_SIZE) {
                    Location borderLoc = gridBase.clone().add(0, row, col);
                    borderLoc.getBlock().setType(Material.WHITE_CONCRETE);
                }
            }
        }
        
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Material mat = woolColors[random.nextInt(woolColors.length)];
                pattern.add(mat);
                Location blockLoc = gridBase.clone().add(0, row, col);
                blockLoc.getBlock().setType(mat);
            }
        }
        
        playerPatterns.put(player.getUniqueId(), pattern);
        
        return gridBase;
    }
    
    private void removePatternWool(Player player) {
        Location base = playerBases.get(player.getUniqueId());
        if (base == null) return;
        
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Location blockLoc = base.clone().add(0, row, col);
                blockLoc.getBlock().setType(Material.AIR);
            }
        }
        
        player.sendMessage(ChatColor.YELLOW + "Now rebuild the pattern!");
    }

    private void givePatternItems(Player player) {
        List<Material> pattern = playerPatterns.get(player.getUniqueId());
        if (pattern == null) return;
        
        for (Material wool : pattern) {
            player.getInventory().addItem(new ItemStack(wool, 1));
        }
    }

    private void checkBuild(Player player) {
        Location base = playerBases.get(player.getUniqueId());
        List<Material> pattern = playerPatterns.get(player.getUniqueId());
        if (base == null || pattern == null) return;

        boolean correct = true;
        int index = 0;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Material expected = pattern.get(index);
                Location checkLoc = base.clone().add(0, row, col);
                Material actual = checkLoc.getBlock().getType();
                
                if (actual != expected) {
                    correct = false;
                }
                index++;
            }
        }

        if (correct) {
            player.sendMessage(ChatColor.GOLD + "Correct build.");
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));
        } else {
            player.sendMessage(ChatColor.RED + "Incorrect build.");
            player.damage(4.0);
        }
    }

    private void removeBuildArea(Player player) {
        Location base = playerBases.get(player.getUniqueId());
        Map<Location, BlockData> originalBlocksMap = originalBlocks.get(player.getUniqueId());
        if (base == null || originalBlocksMap == null) return;
        for (Map.Entry<Location, BlockData> entry : originalBlocksMap.entrySet()) {
            Location loc = entry.getKey();
            BlockData data = entry.getValue();
            Block block = loc.getBlock();
            block.setBlockData(data);
        }
        
        originalBlocks.remove(player.getUniqueId());
    }
}
