package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import io.papermc.paper.event.player.AsyncChatEvent;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.util.*;

public class RiddleGameEvent extends AbstractSimpleGameProvider {
    private final List<Riddle> riddles = new ArrayList<>();
    private Riddle currentRiddle;
    private boolean riddleSolved = false;
    private Player winner = null;
    
    // For tracking hint timers
    private boolean hintGiven = false;
    private boolean hint2Given = false;
    
    // For tracking chat messages
    private final Map<UUID, List<String>> playerChatHistory = new HashMap<>();
    
    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        loadRiddlesFromConfig(plugin);

        return new SimpleGameEvent.Builder(plugin)
                .name("Riddle Game")
                .description("Solve the riddle by typing the answer in chat!")
                .votingName("riddle_game")
                .duration(1200L) // 60 seconds
                .onStartWithEvent(event -> {
                    // Reset state
                    riddleSolved = false;
                    winner = null;
                    hintGiven = false;
                    hint2Given = false;
                    playerChatHistory.clear();
                    
                    // Select a random riddle for everyone
                    currentRiddle = getRandomRiddle();
                    
                    // Announce the riddle to all players
                    Bukkit.broadcast(Component.text("=== RIDDLE GAME ===").color(NamedTextColor.GOLD));
                    Bukkit.broadcast(Component.text("Type the answer in chat to win!").color(NamedTextColor.YELLOW));
                    Bukkit.broadcast(Component.text(currentRiddle.getQuestion()).color(NamedTextColor.LIGHT_PURPLE));
                    
                    // Play sound for all players
                    forEachPlayer(player -> {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
                    });
                    
                    // Schedule first hint after 15 seconds
                    event.runTaskLater(300L, () -> {
                        if (!riddleSolved) {
                            Bukkit.broadcast(Component.text("Hint: " + currentRiddle.getHint()).color(NamedTextColor.YELLOW));
                            hintGiven = true;
                            
                            // Schedule second hint after another 15 seconds
                            event.runTaskLater(300L, () -> {
                                if (!riddleSolved) {
                                    Bukkit.broadcast(Component.text("Hint 2: " + currentRiddle.getHint2()).color(NamedTextColor.YELLOW));
                                    hint2Given = true;
                                }
                            });
                        }
                    });
                })
                .on(AsyncChatEvent.class, (event, gameEvent) -> {
                    // Skip if riddle is already solved
                    if (riddleSolved) return;
                    
                    Player player = event.getPlayer();
                    
                    // Extract the actual message content from the Component
                    String message = event.message().toString();
                    
                    // Try to extract just the text content from the Component
                    if (message.contains("content=")) {
                        int startIndex = message.indexOf("content=") + 9; // length of "content=" + quotes
                        int endIndex = message.indexOf("\"", startIndex);
                        if (endIndex > startIndex) {
                            message = message.substring(startIndex, endIndex);
                        }
                    }
                    
                    // Clean up and normalize the message
                    message = message.toLowerCase().trim();
                    
                    // Store the message in player's chat history
                    playerChatHistory.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(message);
                    
                    // Debug log
                    Bukkit.getLogger().info("Chat message received: '" + message + "', solution: '" + currentRiddle.getSolution() + "'");
                    Bukkit.getLogger().info("Original message: '" + event.message().toString() + "'");
                    
                    // Check if the message matches the solution
                    if (message.equalsIgnoreCase(currentRiddle.getSolution())) {
                        // Mark as solved to prevent multiple winners
                        riddleSolved = true;
                        winner = player;
                        
                        // Calculate score based on hints given
                        int score;
                        if (!hintGiven && !hint2Given) {
                            score = 30; // No hints used - max points
                        } else if (!hint2Given) {
                            score = 20; // Only first hint used
                        } else {
                            score = 10; // Both hints used - min points
                        }
                        
                        // Announce winner
                        Bukkit.broadcast(Component.text("Riddle Game ended!").color(NamedTextColor.GOLD));
                        Bukkit.broadcast(Component.text("Winner: " + player.getName() + " solved the riddle!").color(NamedTextColor.GREEN));
                        Bukkit.broadcast(Component.text("Answer: " + currentRiddle.getSolution()).color(NamedTextColor.YELLOW));
                        Bukkit.broadcast(Component.text("Score: " + score + " points").color(NamedTextColor.YELLOW));
                        
                        // Play sound for all players
                        forEachPlayer(p -> {
                            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            if (p.equals(winner)) {
                                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                            }
                        });
                        
                        // End the event after a short delay
                        Bukkit.getScheduler().runTaskLater(TwitchIntegration.getPlugin(TwitchIntegration.class), () -> {
                            TwitchIntegration.getPlugin(TwitchIntegration.class).getGameEventManager().stopAllEvents();
                        }, 40L); // 2 second delay
                    }
                })
                .withReward(
                    // Win condition: player is the winner
                    player -> player.equals(winner),
                    // Reward action: give diamonds
                    player -> {
                        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));
                        player.sendMessage(Component.text("You received 3 diamonds as a reward!").color(NamedTextColor.GOLD));
                    }
                )
                .onFinish(() -> {
                    // If the event ends without a winner (e.g., timeout), show a message
                    if (!riddleSolved) {
                        Bukkit.broadcast(Component.text("Riddle Game ended! No one solved the riddle.").color(NamedTextColor.GOLD));
                        Bukkit.broadcast(Component.text("The answer was: " + currentRiddle.getSolution()).color(NamedTextColor.YELLOW));
                    }
                })
                .build();
    }
    
    /**
     * Gets a random riddle from the config
     */
    private Riddle getRandomRiddle() {
        if (riddles.isEmpty()) {
            // If no riddles are loaded, create a default one
            return new Riddle(
                "Ice Riddle",
                "I am hard as rock, but melt in the sun. What am I?",
                "You can find me in cold biomes.",
                "I'm frozen water.",
                "ice"
            );
        }
        
        // Just pick a random riddle from the config
        Riddle selected = riddles.get(new Random().nextInt(riddles.size()));
        Bukkit.getLogger().info("Selected riddle: " + selected.getName() + ", solution: '" + selected.getSolution() + "'");
        return selected;
    }
    
    /**
     * Loads riddles from the YAML configuration file
     */
    private void loadRiddlesFromConfig(TwitchIntegration plugin) {
        // Clear existing riddles
        riddles.clear();

        try {
            // Try to load the config file
            File configFile = new File(plugin.getDataFolder(), "riddles.yml");
            YamlConfiguration config;
            
            // If the file doesn't exist, create it from the default resource
            if (!configFile.exists()) {
                plugin.saveResource("riddles.yml", false);
            } else {
                // Force reload the resource to ensure we have the latest version
                plugin.saveResource("riddles.yml", true);
            }
            
            // Load the config
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // Load tasks/riddles
            if (config.contains("tasks") && config.isList("tasks")) {
                List<?> tasksList = config.getList("tasks");
                
                if (tasksList != null) {
                    for (Object obj : tasksList) {
                        if (obj instanceof Map) {
                            Map<?, ?> taskMap = (Map<?, ?>) obj;
                            
                            // Debug the entire map content
                            plugin.getLogger().info("Riddle map keys: " + taskMap.keySet());
                            
                            String name = String.valueOf(taskMap.get("name"));
                            String question = String.valueOf(taskMap.get("question"));
                            String hint = String.valueOf(taskMap.get("hint"));
                            String hint2 = String.valueOf(taskMap.get("hint2"));
                            
                            // Check if solution or item key is present
                            String solution;
                            if (taskMap.containsKey("solution")) {
                                solution = String.valueOf(taskMap.get("solution"));
                            } else if (taskMap.containsKey("item")) {
                                // Fall back to item key if solution is not present
                                solution = String.valueOf(taskMap.get("item")).toLowerCase().replace("_", " ");
                            } else {
                                solution = "unknown";
                            }
                            
                            // Debug log
                            plugin.getLogger().info("Loading riddle: " + name + ", solution: '" + solution + "'");
                            
                            // Create and add the riddle
                            riddles.add(new Riddle(name, question, hint, hint2, solution));
                        }
                    }
                }
            }
            
            plugin.getLogger().info("Loaded " + riddles.size() + " riddles from config.");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading riddles from config: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Class to represent a riddle
     */
    private static class Riddle {
        private final String name;
        private final String question;
        private final String hint;
        private final String hint2;
        private final String solution;
        
        public Riddle(String name, String question, String hint, String hint2, String solution) {
            this.name = name;
            this.question = question;
            this.hint = hint;
            this.hint2 = hint2;
            this.solution = solution;
        }
        
        public String getName() {
            return name;
        }
        
        public String getQuestion() {
            return question;
        }
        
        public String getHint() {
            return hint;
        }
        
        public String getHint2() {
            return hint2;
        }
        
        public String getSolution() {
            return solution;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Riddle riddle = (Riddle) o;
            return name.equals(riddle.name) && 
                   question.equals(riddle.question) && 
                   solution.equals(riddle.solution);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name, question, solution);
        }
    }
}