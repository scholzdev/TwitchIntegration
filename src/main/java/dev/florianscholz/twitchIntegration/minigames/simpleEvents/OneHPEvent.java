package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OneHPEvent extends AbstractSimpleGameProvider {

    private Map<UUID, Double> originalHealth = new HashMap<>();

    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        return new SimpleGameEvent.Builder(plugin)
                .name("One HP")
                .description("One HP")
                .votingName("one_hp")
                .duration(600)
                .onStart(() -> {
                    forEachPlayer(p -> originalHealth.put(p.getUniqueId(), p.getHealth()));
                    forEachPlayer(p -> p.setHealth(1));
                })
                .onTick(() -> {
                    forEachPlayer(p -> {
                        if(p.getHealth() > 1.0) {
                            p.setHealth(1.0);
                        }
                    });
                })
                .onFinish(() -> forEachPlayer(p -> {
                    p.setHealth(originalHealth.get(p.getUniqueId()));
                }))
                .on(EntityRegainHealthEvent.class, event -> {
                    event.setCancelled(true);
                })
                .build();
    }
}
