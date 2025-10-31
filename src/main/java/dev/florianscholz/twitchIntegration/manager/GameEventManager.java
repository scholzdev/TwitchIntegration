package dev.florianscholz.twitchIntegration.manager;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class GameEventManager {

    private final TwitchIntegration plugin;

    @Getter
    private final Map<String, GameEvent> events = new HashMap<>();

    @Getter
    private GameEvent activeEvent = null;

    public GameEventManager(TwitchIntegration plugin) {
        this.plugin = plugin;
    }

    public void registerEvent(GameEvent event) {
        String key = event.getVotingName().toLowerCase();
        if(events.containsKey(key)) {
            plugin.getLogger().warning("Event with name " + key + " already registered.");
            return;
        }

        events.put(key, event);
        plugin.getLogger().info("Registered event " + key);
    }

    public void startEvent(String name) {
        if(activeEvent != null && !activeEvent.isRunning()) {
            plugin.getLogger().warning("Found old event, cleaning");
            activeEvent = null;
        }

        if(activeEvent != null) {
            plugin.getLogger().warning("Event already running " + activeEvent.getName());
            return;
        }

        GameEvent event = events.get(name.toLowerCase());

        if(event == null) {
            plugin.getLogger().warning("Event not found " + name);
            return;
        }

        plugin.getLogger().info("Starting event " + event.getName());
        activeEvent = event;
        event.start();
    }

    public void onEventFinished(GameEvent event) {
        if(activeEvent == event) {
            plugin.getLogger().info("Event finished " + event.getName());
            activeEvent = null;
        }
    }

    public boolean isEventRunning() {
        return activeEvent != null && activeEvent.isRunning();
    }

    public void stopActiveEvent() {
        if(activeEvent != null) {
            activeEvent.stop();
        }
    }

    public void stopAllEvents() {
        events.values().forEach(event -> {
            if(event.isRunning()) {
                event.stop();
            }
        });
        activeEvent = null;
    }

}
