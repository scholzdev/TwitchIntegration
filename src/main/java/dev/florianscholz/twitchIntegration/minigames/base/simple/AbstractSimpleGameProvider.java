package dev.florianscholz.twitchIntegration.minigames.base.simple;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public abstract class AbstractSimpleGameProvider implements SimpleGameEventProvider {

    /**
     * Helper to run an action on every online player.
     */
    protected void forEachPlayer(Consumer<Player> action) {
        Bukkit.getOnlinePlayers().forEach(action);
    }

    /**
     * Implementing classes must provide the GameEvent instance.
     */
    @Override
    public abstract SimpleGameEvent create(TwitchIntegration plugin);
}