package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import org.bukkit.potion.PotionEffectType;

public class DrunkModeEvent extends AbstractSimpleGameProvider {
    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        return new SimpleGameEvent.Builder(plugin)
                .name("Drunk Mode")
                .description("Drunk Mode")
                .votingName("drunk_mode")
                .duration(200)
                .onStart(() -> {
                    forEachPlayer(p -> {
                        p.addPotionEffect(PotionEffectType.NAUSEA.createEffect(200, 1));
                        p.addPotionEffect(PotionEffectType.SLOWNESS.createEffect(200, 0));

                    });
                })
                .onFinish(() -> forEachPlayer(p -> {
                    p.removePotionEffect(PotionEffectType.NAUSEA);
                    p.removePotionEffect(PotionEffectType.SLOWNESS);
                }))
                .build();
    }
}
