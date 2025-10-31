package dev.florianscholz.twitchIntegration.minigames.base;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;

public abstract class GameEvent implements IGameEvent, Listener {

    protected TwitchIntegration plugin;
    private boolean isRunning = false;
    private BukkitTask durationTask;

    public GameEvent(TwitchIntegration plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    public Collection<? extends Player> getPlayers() {
        return Bukkit.getOnlinePlayers();
    }

    @Override
    public final void start() {
        if (isRunning) {
            plugin.getLogger().warning("Event already running: " + getName());
            return;
        }

        isRunning = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.getEventDisplayManager().showEventStart(this);

        try {
            onStart();
        } catch (Exception e) {
            plugin.getLogger().severe("Error starting event " + getName() + ": " + e.getMessage());
            e.printStackTrace();
            stop(); // Cleanup on error
            return;
        }

        // Schedule auto-stop
        if (getDuration() > 0) {
            durationTask = Bukkit.getScheduler().runTaskLater(plugin, this::stop, getDuration());
        }
    }

    @Override
    public final void stop() {
        if (!isRunning) return;

        isRunning = false;

        if (durationTask != null) {
            durationTask.cancel();
            durationTask = null;
        }

        HandlerList.unregisterAll(this);

        try {
            onFinish();
        } catch (Exception e) {
            plugin.getLogger().severe("Error stopping event " + getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        plugin.getEventDisplayManager().showEventEnd(this);

        plugin.getGameEventManager().onEventFinished(this);
    }

    /**
     * Helper: Schedule a repeating task during the event.
     * Task runs from delay until event ends (getDuration()).
     */
    protected BukkitTask scheduleRepeating(Runnable task, long delay, long period) {
        BukkitTask repeatingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (isRunning) task.run();
        }, delay, period);

        // Auto-cancel when event ends
        if (getDuration() > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, repeatingTask::cancel, getDuration());
        }

        return repeatingTask;
    }

    /**
     * Helper: Run task every X ticks for the entire event duration
     */
    protected BukkitTask runDuringEvent(Runnable task, long period) {
        return scheduleRepeating(task, 0, period);
    }

    /**
     * Helper: Apply effect to all players
     */
    protected void forEachPlayer(java.util.function.Consumer<Player> action) {
        getPlayers().forEach(action);
    }

    protected abstract void onStart();
    protected abstract void onFinish();
    /** Called every tick while the event is active */
    protected void onTick() {}
}
