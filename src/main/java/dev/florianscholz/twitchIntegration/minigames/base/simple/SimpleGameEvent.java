package dev.florianscholz.twitchIntegration.minigames.base.simple;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SimpleGameEvent extends GameEvent {

    @Getter private final String name;
    @Getter private final String description;
    @Getter private final String votingName;
    @Getter private final long duration;

    private final Consumer<SimpleGameEvent> startAction;
    private final Consumer<SimpleGameEvent> finishAction;
    private final Runnable tickAction;
    private final long tickInterval;
    private final Supplier<Boolean> finishCondition;

    private final List<EventRegistration<?>> eventHandlers;
    private final List<Listener> registeredListeners = new ArrayList<>();
    private BukkitTask tickTask;

    @Getter private final List<Entity> spawnedEntities = new ArrayList<>();
    private final boolean cleanupSpawned;

    private final boolean saveInventory;
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();

    private final int countdownSeconds;
    private boolean hasShownStart = false;

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
    }

    /**
     * Spawns an entity and automatically tracks it for cleanup if cleanupSpawned() is enabled.
     */
    public <T extends Entity> T spawnAndTrack(Location loc, Class<T> entityClass) {
        T entity = loc.getWorld().spawn(loc, entityClass);
        spawnedEntities.add(entity);
        return entity;
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

        if (saveInventory) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                savedInventories.put(player.getUniqueId(), player.getInventory().getContents());
            }
        }

        for (EventRegistration<?> registration : eventHandlers) {
            Listener listener = registration.createListener();
            registration.register(plugin, listener);
            registeredListeners.add(listener);
        }

        if (tickAction != null || finishCondition != null) {
            tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 1L, tickInterval);
        }

        if (startAction != null) startAction.accept(this);

        scheduleDurationTask();
    }

    @Override
    protected void onTick() {
        if (finishCondition != null && finishCondition.get()) {
            stop();
            return;
        }

        if (tickAction != null) tickAction.run();
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

        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        registeredListeners.forEach(HandlerList::unregisterAll);
        registeredListeners.clear();

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
        private Runnable tickAction;
        private long tickInterval = 1L;
        private Supplier<Boolean> finishCondition;

        private final List<EventRegistration<?>> eventHandlers = new ArrayList<>();
        private boolean cleanupSpawned = false;
        private boolean saveInventory = false;
        private int countdownSeconds = 5;

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

        public Builder onTick(Runnable action) { this.tickAction = action; return this; }
        public Builder tickInterval(long intervalTicks) { this.tickInterval = intervalTicks; return this; }
        public Builder finishWhen(Supplier<Boolean> condition) { this.finishCondition = condition; return this; }

        public Builder cleanupSpawned() { this.cleanupSpawned = true; return this; }
        public Builder saveAndRestoreInventory() { this.saveInventory = true; return this; }

        public Builder countdown(int seconds) { this.countdownSeconds = seconds; return this; }

        public <T extends Event> Builder on(Class<T> eventClass, Consumer<T> handler) {
            return on(eventClass, EventPriority.NORMAL, handler);
        }

        public <T extends Event> Builder on(Class<T> eventClass, EventPriority priority, Consumer<T> handler) {
            EventRegistration<T> registration = new EventRegistration<>(eventClass, handler, priority);
            eventHandlers.add(registration);
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

        public EventRegistration(Class<T> eventClass, Consumer<T> handler, EventPriority priority) {
            this.eventClass = eventClass;
            this.handler = handler;
            this.priority = priority;
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
