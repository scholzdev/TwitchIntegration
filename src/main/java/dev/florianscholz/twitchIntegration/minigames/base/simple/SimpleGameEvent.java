package dev.florianscholz.twitchIntegration.minigames.base.simple;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.manager.ScoreboardManager;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.Random;

public class SimpleGameEvent extends GameEvent {

    @Getter private final String name;
    @Getter private final String description;
    @Getter private final String votingName;
    @Getter private final long duration;

    private final Consumer<SimpleGameEvent> startAction;
    private final Consumer<SimpleGameEvent> finishAction;
    private final Consumer<SimpleGameEvent> tickAction;
    private final long tickInterval;
    private final Supplier<Boolean> finishCondition;

    private final List<EventRegistration<?>> eventHandlers;
    private final List<Listener> registeredListeners = new ArrayList<>();
    private BukkitTask tickTask;

    @Getter private final List<Entity> spawnedEntities = new ArrayList<>();
    private final boolean cleanupSpawned;
    private final boolean saveInventory;
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();

    private final boolean teleportPlayers;
    private final String teleportWorld;
    private final double teleportX;
    private final double teleportY;
    private final double teleportZ;
    private final Map<UUID, Location> originalLocations = new HashMap<>();

    private final int countdownSeconds;
    private boolean hasShownStart = false;

    private final BiPredicate<Player, SimpleGameEvent> winCondition;
    private final BiConsumer<Player, SimpleGameEvent> rewardAction;
    
    // Kill tracking system
    private final Map<UUID, Integer> playerKills = new HashMap<>();
    
    // Scoreboard system
    @Getter private ScoreboardManager scoreboardManager;
    
    // Time tracking
    private long startTime = -1;
    private long endTime = -1;

    private SimpleGameEvent(Builder builder) {
        super(builder.plugin);
        this.name = builder.name;
        this.description = builder.description;
        this.votingName = builder.votingName;
        this.duration = builder.duration;
        this.startAction = builder.startAction;
        this.finishAction = builder.finishAction;
        this.tickAction = builder.tickAction;
        this.tickInterval = builder.tickInterval;
        this.finishCondition = builder.finishCondition;
        this.eventHandlers = builder.eventHandlers;
        this.cleanupSpawned = builder.cleanupSpawned;
        this.saveInventory = builder.saveInventory;
        this.countdownSeconds = builder.countdownSeconds;
        this.teleportPlayers = builder.teleportPlayers;
        this.teleportWorld = builder.teleportWorld;
        this.teleportX = builder.teleportX;
        this.teleportY = builder.teleportY;
        this.teleportZ = builder.teleportZ;
        this.winCondition = builder.winCondition;
        this.rewardAction = builder.rewardAction;
        this.scoreboardManager = builder.scoreboardTitle != null 
            ? new ScoreboardManager(this, builder.scoreboardTitle) 
            : null;
    }

    /**
     * Spawns an entity and automatically tracks it for cleanup if cleanupSpawned() is enabled.
     */
    public <T extends Entity> T spawnAndTrack(Location loc, Class<T> entityClass) {
        T entity = loc.getWorld().spawn(loc, entityClass);
        spawnedEntities.add(entity);
        return entity;
    }
    
    /**
     * Helper method to run code for each online player.
     * @param action The action to run for each player
     */
    public void forEach(Consumer<Player> action) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            action.accept(player);
        }
    }
    
    /**
     * Helper method to run a task later.
     * @param delayTicks The delay in ticks
     * @param action The action to run
     * @return The BukkitTask that was scheduled
     */
    public BukkitTask runTaskLater(long delayTicks, Runnable action) {
        return Bukkit.getScheduler().runTaskLater(plugin, action, delayTicks);
    }
    
    /**
     * Tracks a kill by a player and increments their kill count.
     * @param player The player who made the kill
     */
    public void trackKill(Player player) {
        UUID playerId = player.getUniqueId();
        playerKills.put(playerId, playerKills.getOrDefault(playerId, 0) + 1);
    }
    
    /**
     * Gets the number of kills for a player.
     * @param player The player to check
     * @return The number of kills
     */
    public int getKillCount(Player player) {
        return playerKills.getOrDefault(player.getUniqueId(), 0);
    }
    
    /**
     * Gets the total number of kills across all players.
     * @return The total kill count
     */
    public int getTotalKillCount() {
        int total = 0;
        for (int kills : playerKills.values()) {
            total += kills;
        }
        return total;
    }
    
    /**
     * Checks if all spawned entities are dead or removed.
     * @return true if all spawned entities are dead or removed, false otherwise
     */
    public boolean areAllSpawnedEntitiesDead() {
        if (spawnedEntities.isEmpty()) {
            return false; // No entities were spawned yet
        }
        
        // Remove dead or invalid entities from the list
        spawnedEntities.removeIf(entity -> !entity.isValid() || entity.isDead());
        
        // If the list is empty, all entities are dead
        return spawnedEntities.isEmpty();
    }
    
    /**
     * Gets the scoreboard manager for this event.
     * @return The scoreboard manager, or null if no scoreboard was configured
     */
    public ScoreboardManager getScoreboard() {
        return scoreboardManager;
    }
    
    /**
     * Sets a line in the scoreboard.
     * Shorthand for getScoreboard().setLine(line, text)
     * @param line The line number
     * @param text The text to display
     */
    public void setScoreboardLine(int line, String text) {
        if (scoreboardManager != null) {
            scoreboardManager.setLine(line, text);
        }
    }
    
    /**
     * Updates the scoreboard display.
     * Shorthand for getScoreboard().update()
     */
    public void updateScoreboard() {
        if (scoreboardManager != null) {
            scoreboardManager.update();
        }
    }
    
    /**
     * Gets the remaining time in seconds for this event.
     * Returns -1 if the event has no fixed duration (untilCondition).
     * Returns 0 if the event has finished.
     * 
     * @return The remaining time in seconds, -1 if no duration, 0 if finished
     */
    public long getRemainingTime() {
        if (duration == -1) {
            return -1; // No fixed duration
        }
        
        if (startTime == -1) {
            return duration / 20; // Event hasn't started yet, return full duration in seconds
        }
        
        if (endTime != -1 && System.currentTimeMillis() >= endTime) {
            return 0; // Event has finished
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - startTime;
        long remaining = (duration * 50) - elapsed; // duration is in ticks, convert to milliseconds
        
        return Math.max(0, remaining / 1000); // Convert to seconds
    }
    
    /**
     * Gets the remaining time formatted as MM:SS.
     * Returns "∞" if the event has no fixed duration.
     * 
     * @return The remaining time formatted as MM:SS
     */
    public String getRemainingTimeFormatted() {
        long seconds = getRemainingTime();
        
        if (seconds == -1) {
            return "∞";
        }
        
        long minutes = seconds / 60;
        long secs = seconds % 60;
        
        return String.format("%02d:%02d", minutes, secs);
    }
    
    /**
     * Gets the elapsed time in seconds since the event started.
     * Returns 0 if the event hasn't started yet.
     * 
     * @return The elapsed time in seconds
     */
    public long getElapsedTime() {
        if (startTime == -1) {
            return 0; // Event hasn't started yet
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - startTime;
        
        return elapsed / 1000; // Convert to seconds
    }
    
    /**
     * Gets the elapsed time formatted as MM:SS.
     * 
     * @return The elapsed time formatted as MM:SS
     */
    public String getElapsedTimeFormatted() {
        long seconds = getElapsedTime();
        long minutes = seconds / 60;
        long secs = seconds % 60;
        
        return String.format("%02d:%02d", minutes, secs);
    }
    
    /**
     * Finds a safe location in the given world for teleporting players.
     * A safe location is one that is not in water, lava, or inside blocks.
     * 
     * @param world The world to find a safe location in
     * @return A safe location for teleporting
     */
    private Location findSafeLocation(World world) {
        Random random = new Random();
        Location safeLoc = null;
        int attempts = 0;
        int maxAttempts = 50;
        
        while (safeLoc == null && attempts < maxAttempts) {
            attempts++;
            
            // Generate random coordinates within reasonable bounds
            int x = random.nextInt(2000) - 1000; // -1000 to 1000
            int z = random.nextInt(2000) - 1000; // -1000 to 1000
            
            // Get the highest block at this x,z coordinate
            int y = world.getHighestBlockYAt(x, z);
            
            // Create a potential location
            Location loc = new Location(world, x, y, z);
            
            // Check if the location is safe
            if (isSafeLocation(loc)) {
                safeLoc = loc;
            }
        }
        
        // If we couldn't find a safe location, use spawn as fallback
        if (safeLoc == null) {
            safeLoc = world.getSpawnLocation();
        }
        
        return safeLoc;
    }
    
    /**
     * Checks if a location is safe for teleporting players.
     * 
     * @param loc The location to check
     * @return True if the location is safe, false otherwise
     */
    private boolean isSafeLocation(Location loc) {
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        
        // Check if there's enough space for the player (2 blocks high)
        if (world.getBlockAt(x, y, z).getType().isSolid() || 
            world.getBlockAt(x, y + 1, z).getType().isSolid()) {
            return false;
        }
        
        // Check if there's a solid block below
        if (!world.getBlockAt(x, y - 1, z).getType().isSolid()) {
            return false;
        }
        
        // Check if we're not in water or lava
        Material blockType = world.getBlockAt(x, y, z).getType();
        if (blockType == Material.WATER || blockType == Material.LAVA) {
            return false;
        }
        
        return true;
    }

    @Override
    protected boolean shouldShowStartImmediately() {
        return countdownSeconds == 0;
    }

    @Override
    protected void onStart() {
        hasShownStart = (countdownSeconds == 0);

        if (countdownSeconds > 0) {
            plugin.getEventDisplayManager().showCountdown(name, countdownSeconds, this::actualStart);
            return;
        }

        actualStart();
    }

    private void actualStart() {
        if (!hasShownStart) {
            plugin.getEventDisplayManager().showEventStart(this);
            hasShownStart = true;
        }
        
        // Set start time
        startTime = System.currentTimeMillis();
        if (duration > 0) {
            endTime = startTime + (duration * 50); // duration is in ticks, convert to milliseconds
        }

        if (saveInventory) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                savedInventories.put(player.getUniqueId(), player.getInventory().getContents());
                player.getInventory().clear();
            }
        }
        
        if (teleportPlayers) {
            World world = Bukkit.getWorld(teleportWorld);
            if (world != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    originalLocations.put(player.getUniqueId(), player.getLocation().clone());
                    
                    // Check if we should do random teleport
                    if (teleportX == Double.MIN_VALUE && teleportY == Double.MIN_VALUE && teleportZ == Double.MIN_VALUE) {
                        // Random teleport
                        Location safeLoc = findSafeLocation(world);
                        player.teleport(safeLoc);
                    } else {
                        // Fixed location teleport
                        Location teleportLoc = new Location(world, teleportX, teleportY, teleportZ);
                        player.teleport(teleportLoc);
                    }
                }
            } else {
                plugin.getLogger().warning("Could not teleport players: World '" + teleportWorld + "' not found");
            }
        }

        for (EventRegistration<?> registration : eventHandlers) {
            registration.setGameEvent(this);
            Listener listener = registration.createListener();
            registration.register(plugin, listener);
            registeredListeners.add(listener);
        }

        if (tickAction != null || finishCondition != null) {
            tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 1L, tickInterval);
        }
        
        if (scoreboardManager != null) {
            scoreboardManager.show();
        }

        if (startAction != null) startAction.accept(this);

        scheduleDurationTask();
    }

    @Override
    protected void onTick() {
        if (tickAction != null) tickAction.accept(this);

        if (finishCondition != null && finishCondition.get()) {
            stop();
        }
    }

    @Override
    protected void onFinish() {
        // Cleanup spawned entities
        if (cleanupSpawned) {
            spawnedEntities.forEach(e -> {
                if (e.isValid()) e.remove();
            });
            spawnedEntities.clear();
        }

        if (saveInventory) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack[] saved = savedInventories.get(player.getUniqueId());
                if (saved != null) {
                    player.getInventory().setContents(saved);
                }
            }
            savedInventories.clear();
        }
        
        if (teleportPlayers) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Location originalLoc = originalLocations.get(player.getUniqueId());
                if (originalLoc != null) {
                    player.teleport(originalLoc);
                }
            }
            originalLocations.clear();
        }

        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        registeredListeners.forEach(HandlerList::unregisterAll);
        registeredListeners.clear();
        
        if (scoreboardManager != null) {
            scoreboardManager.hide();
        }

        // Apply rewards to winning players
        if (winCondition != null && rewardAction != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                boolean won = winCondition.test(player, this);
                if (won) {
                    rewardAction.accept(player, this);
                }
            }
        }
        
        // Clear kill tracking data
        playerKills.clear();
        
        if (finishAction != null) finishAction.accept(this);
    }

    public static class Builder {
        private final TwitchIntegration plugin;
        @Getter private String name;
        @Getter private String description;
        @Getter private String votingName;
        @Getter private long duration;

        private Consumer<SimpleGameEvent> startAction;
        private Consumer<SimpleGameEvent> finishAction;
        private Consumer<SimpleGameEvent> tickAction;
        private long tickInterval = 1L;
        private Supplier<Boolean> finishCondition;

        private final List<EventRegistration<?>> eventHandlers = new ArrayList<>();
        private boolean cleanupSpawned = false;
        private boolean saveInventory = false;
        private boolean teleportPlayers = false;
        private String teleportWorld = "world";
        private double teleportX = 0;
        private double teleportY = 64;
        private double teleportZ = 0;
        private int countdownSeconds = 5;
        
        private BiPredicate<Player, SimpleGameEvent> winCondition;
        private BiConsumer<Player, SimpleGameEvent> rewardAction;
        
        private String scoreboardTitle;

        public Builder(TwitchIntegration plugin) {
            this.plugin = plugin;
        }

        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder votingName(String votingName) { this.votingName = votingName; return this; }
        public Builder duration(long duration) { this.duration = duration; return this; }

        public Builder untilCondition() { this.duration = -1; return this; }

        public Builder onStart(Runnable action) {
            this.startAction = (event) -> action.run();
            return this;
        }

        public Builder onStartWithEvent(Consumer<SimpleGameEvent> action) {
            this.startAction = action;
            return this;
        }

        public Builder onFinish(Runnable action) {
            this.finishAction = (event) -> action.run();
            return this;
        }

        public Builder onFinishWithEvent(Consumer<SimpleGameEvent> action) {
            this.finishAction = action;
            return this;
        }

        public Builder onTick(Runnable action) { 
            this.tickAction = (event) -> action.run(); 
            return this; 
        }
        
        public Builder onTick(Consumer<SimpleGameEvent> action) { 
            this.tickAction = action; 
            return this; 
        }
        public Builder tickInterval(long intervalTicks) { this.tickInterval = intervalTicks; return this; }
        public Builder finishWhen(Supplier<Boolean> condition) { this.finishCondition = condition; return this; }
        
        /**
         * Sets a condition for when the event should finish early.
         * The condition is checked on each tick.
         * 
         * @param condition The condition to check, which has access to the SimpleGameEvent instance
         * @return Builder instance for chaining
         */
        public Builder finishWhen(Predicate<SimpleGameEvent> condition) {
            // Store the original tick action if any
            Consumer<SimpleGameEvent> originalTickAction = this.tickAction;
            
            // Create a new tick action that includes the condition check
            this.tickAction = (event) -> {
                // Run the original tick action if it exists
                if (originalTickAction != null) {
                    originalTickAction.accept(event);
                }
                
                // Check the condition and stop the event if it's met
                if (condition.test(event)) {
                    event.stop();
                }
            };
            
            // Make sure we have a tick interval
            if (this.tickInterval <= 0) {
                this.tickInterval = 20; // Default to 1 second
            }
            
            return this;
        }

        public Builder cleanupSpawned() { this.cleanupSpawned = true; return this; }
        public Builder saveAndRestoreInventory() { this.saveInventory = true; return this; }

        public Builder countdown(int seconds) { this.countdownSeconds = seconds; return this; }
        
        /**
         * Teleports all players to the specified location at the start of the event
         * and returns them to their original locations when the event ends.
         * 
         * @param worldName The name of the world to teleport to
         * @param x The x coordinate
         * @param y The y coordinate
         * @param z The z coordinate
         * @return Builder instance for chaining
         */
        public Builder onTeleport(String worldName, double x, double y, double z) {
            this.teleportPlayers = true;
            this.teleportWorld = worldName;
            this.teleportX = x;
            this.teleportY = y;
            this.teleportZ = z;
            return this;
        }
        
        /**
         * Teleports all players to random safe locations in the specified world at the start of the event
         * and returns them to their original locations when the event ends.
         * The locations are chosen to be safe (not in water, lava, or inside blocks).
         * 
         * @param worldName The name of the world to teleport to
         * @return Builder instance for chaining
         */
        public Builder onTeleportRandom(String worldName) {
            this.teleportPlayers = true;
            this.teleportWorld = worldName;
            // Set special values to indicate random teleport
            this.teleportX = Double.MIN_VALUE;
            this.teleportY = Double.MIN_VALUE;
            this.teleportZ = Double.MIN_VALUE;
            return this;
        }
        
        /**
         * Sets a reward for players who meet the win condition.
         * 
         * @param condition The condition that determines if a player has won
         * @param action The action to perform for winning players
         * @return Builder instance for chaining
         */
        public Builder withReward(Predicate<Player> condition, Consumer<Player> action) {
            this.winCondition = (player, event) -> condition.test(player);
            this.rewardAction = (player, event) -> action.accept(player);
            return this;
        }
        
        public Builder withReward(BiPredicate<Player, SimpleGameEvent> condition, BiConsumer<Player, SimpleGameEvent> action) {
            this.winCondition = condition;
            this.rewardAction = action;
            return this;
        }
        
        /**
         * Enables a scoreboard for this event.
         * @param title The title of the scoreboard (supports color codes with &)
         * @return Builder instance for chaining
         */
        public Builder withScoreboard(String title) {
            this.scoreboardTitle = title;
            return this;
        }

        public <T extends Event> Builder on(Class<T> eventClass, Consumer<T> handler) {
            return on(eventClass, EventPriority.NORMAL, handler);
        }

        public <T extends Event> Builder on(Class<T> eventClass, EventPriority priority, Consumer<T> handler) {
            EventRegistration<T> registration = new EventRegistration<>(eventClass, handler, priority);
            eventHandlers.add(registration);
            return this;
        }
        
        // Wrapper class to handle BiConsumer event handlers
        private static class BiConsumerWrapper<T extends Event> implements Consumer<T> {
            private final BiConsumer<T, SimpleGameEvent> handler;
            private SimpleGameEvent gameEvent;
            
            public BiConsumerWrapper(BiConsumer<T, SimpleGameEvent> handler) {
                this.handler = handler;
            }
            
            public void setGameEvent(SimpleGameEvent gameEvent) {
                this.gameEvent = gameEvent;
            }
            
            @Override
            public void accept(T event) {
                if (gameEvent != null) {
                    handler.accept(event, gameEvent);
                }
            }
        }
        
        public <T extends Event> Builder on(Class<T> eventClass, BiConsumer<T, SimpleGameEvent> handler) {
            return on(eventClass, EventPriority.NORMAL, handler);
        }

        public <T extends Event> Builder on(Class<T> eventClass, EventPriority priority, BiConsumer<T, SimpleGameEvent> handler) {
            BiConsumerWrapper<T> wrapper = new BiConsumerWrapper<>(handler);
            EventRegistration<T> registration = new EventRegistration<>(eventClass, wrapper, priority);
            eventHandlers.add(registration);
            return this;
        }
        
        /**
         * Helper method to easily cancel specific events.
         * @param eventClass The event class to cancel
         * @return Builder instance for chaining
         */
        public <T extends Event & Cancellable> Builder cancelEvent(Class<T> eventClass) {
            return on(eventClass, event -> event.setCancelled(true));
        }
        
        /**
         * Helper method to easily cancel specific events with custom priority.
         * @param eventClass The event class to cancel
         * @param priority The event priority
         * @return Builder instance for chaining
         */
        public <T extends Event & Cancellable> Builder cancelEvent(Class<T> eventClass, EventPriority priority) {
            return on(eventClass, priority, event -> event.setCancelled(true));
        }
        
        /**
         * Helper method to run code after a delay.
         * @param delayTicks The delay in ticks before running the action
         * @param action The action to run after the delay
         * @return Builder instance for chaining
         */
        public Builder runLater(long delayTicks, Runnable action) {
            this.startAction = (event) -> {
                Bukkit.getScheduler().runTaskLater(plugin, action, delayTicks);
            };
            return this;
        }
        
        /**
         * Helper method to run code after a delay with access to the event.
         * @param delayTicks The delay in ticks before running the action
         * @param action The action to run after the delay, with access to the event
         * @return Builder instance for chaining
         */
        public Builder runLaterWithEvent(long delayTicks, Consumer<SimpleGameEvent> action) {
            this.startAction = (event) -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> action.accept(event), delayTicks);
            };
            return this;
        }

        public SimpleGameEvent build() {
            Objects.requireNonNull(name, "Event name cannot be null");
            Objects.requireNonNull(description, "Event description cannot be null");
            Objects.requireNonNull(votingName, "Event voting name cannot be null");
            return new SimpleGameEvent(this);
        }
    }

    private static class EventRegistration<T extends Event> {
        private final Class<T> eventClass;
        private final Consumer<T> handler;
        private final EventPriority priority;
        private SimpleGameEvent gameEvent;
        private boolean isBiConsumer;

        public EventRegistration(Class<T> eventClass, Consumer<T> handler, EventPriority priority) {
            this.eventClass = eventClass;
            this.handler = handler;
            this.priority = priority;
            this.isBiConsumer = handler instanceof Builder.BiConsumerWrapper;
        }
        
        public void setGameEvent(SimpleGameEvent gameEvent) {
            this.gameEvent = gameEvent;
            if (isBiConsumer && handler instanceof Builder.BiConsumerWrapper) {
                ((Builder.BiConsumerWrapper<T>) handler).setGameEvent(gameEvent);
            }
        }

        public Listener createListener() { return new Listener() {}; }

        public void register(Plugin plugin, Listener listener) {
            Bukkit.getPluginManager().registerEvent(
                    eventClass,
                    listener,
                    priority,
                    (l, event) -> {
                        if (eventClass.isInstance(event)) {
                            try {
                                handler.accept((T) event);
                            } catch (Throwable ex) {
                                plugin.getLogger().severe("Error in event handler for " + eventClass.getSimpleName());
                                ex.printStackTrace();
                            }
                        }
                    },
                    plugin
            );
        }
    }
}