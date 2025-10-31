package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEventProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Chicken;
import org.bukkit.event.entity.EntityDamageEvent;

public class TestEvent implements SimpleGameEventProvider {

    @Override
    public GameEvent create(TwitchIntegration plugin) {
        return new SimpleGameEvent.Builder(plugin)
        .name("Test Event")
        .description("A test event")
        .votingName("test")
        .duration(200)
        .on(EntityDamageEvent.class, e -> {
            if (e.getEntity() instanceof Chicken
                    && e.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        e.getEntity().getWorld().createExplosion(e.getEntity().getLocation(), 2.0f), 1L);
            }
        })
        .build();
    }
}