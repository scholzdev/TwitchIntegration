package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Phantom;

import java.util.Random;

public class PhantomEvent extends AbstractSimpleGameProvider {
    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        return new SimpleGameEvent.Builder(plugin)
                .name("Phantoms")
                .description("Phantoms")
                .votingName("phantoms")
                .cleanupSpawned()
                .duration(200)
                .onStartWithEvent(event -> {
                    forEachPlayer(player -> {
                        Random random = new Random();
                            int count = 2 + random.nextInt(2);
                            Location loc = player.getLocation().add(random.nextDouble(4), random.nextDouble(2) + 1, random.nextDouble(4));

                            for(int i = 0; i < count; i++) {
                                Phantom phantom = event.spawnAndTrack(loc, Phantom.class);
                                phantom.setTarget(player);
                                phantom.setPersistent(true);
                                phantom.setAware(true);
                                phantom.customName(Component.text("Nightmare"));
                            }
                    });
                })
                .build();
    }
}