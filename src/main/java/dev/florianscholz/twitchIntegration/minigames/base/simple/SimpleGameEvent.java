package dev.florianscholz.twitchIntegration.minigames.base.simple;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A simple, builder-based game event system.
 * Listeners and tick tasks are active only while the event is running.
 */
public class SimpleGameEvent extends GameEvent {

    @Getter private final String name;
    @Getter private final String description;
    @Getter private final String votingName;
    @Getter private final long duration;

    private final Runnable startAction;
    private final Runnable finishAction;
    private final Runnable tickAction;
    private final long tickInterval;

    private final List<EventRegistration<?>> eventHandlers;
    private final List<Listener> registeredListeners = new ArrayList<>();
    private BukkitTask tickTask;

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
        this.eventHandlers = builder.eventHandlers;
    }

    @Override
    protected void onStart() {
        // Register event listeners only while event is active
        for (EventRegistration<?> registration : eventHandlers) {
            Listener listener = registration.createListener();
            registration.register(plugin, listener);
            registeredListeners.add(listener);
        }

        // Start tick task if defined
        if (tickAction != null) {
            tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 1L, tickInterval);
        }

        if (startAction != null) startAction.run();
    }

    @Override
    protected void onTick() {
        if (tickAction != null) tickAction.run();
    }

    @Override
    protected void onFinish() {
        // Cancel tick task
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        // Unregister listeners
        registeredListeners.forEach(HandlerList::unregisterAll);
        registeredListeners.clear();

        if (finishAction != null) finishAction.run();
    }

    /** Builder for creating a SimpleGameEvent instance */
    public static class Builder {
        private final TwitchIntegration plugin;
        @Getter private String name;
        @Getter private String description;
        @Getter private String votingName;
        @Getter private long duration;

        private Runnable startAction;
        private Runnable finishAction;
        private Runnable tickAction;
        private long tickInterval = 1L; // default: every tick

        private final List<EventRegistration<?>> eventHandlers = new ArrayList<>();

        public Builder(TwitchIntegration plugin) {
            this.plugin = plugin;
        }

        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder votingName(String votingName) { this.votingName = votingName; return this; }
        public Builder duration(long duration) { this.duration = duration; return this; }

        public Builder onStart(Runnable action) { this.startAction = action; return this; }
        public Builder onFinish(Runnable action) { this.finishAction = action; return this; }
        public Builder onTick(Runnable action) { this.tickAction = action; return this; }
        public Builder tickInterval(long intervalTicks) { this.tickInterval = intervalTicks; return this; }

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
