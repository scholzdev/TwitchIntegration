package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import org.bukkit.potion.PotionEffectType;

public class SlownessEvent extends AbstractSimpleGameProvider {

    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        return new SimpleGameEvent.Builder(plugin)
                .name("Slowness")
                .description("Slowness")
                .votingName("slowness")
                .duration(400)
                .onStart(() -> {
                    forEachPlayer(p -> p.addPotionEffect(PotionEffectType.SLOWNESS.createEffect(400, 3)));
                })
                .onFinish(() -> forEachPlayer(p -> p.removePotionEffect(PotionEffectType.SLOWNESS)))
                .build();
    }
}
