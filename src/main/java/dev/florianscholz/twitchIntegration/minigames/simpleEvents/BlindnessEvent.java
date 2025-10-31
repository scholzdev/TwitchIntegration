package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import org.bukkit.potion.PotionEffectType;

public class BlindnessEvent extends AbstractSimpleGameProvider {
    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        return new SimpleGameEvent.Builder(plugin)
                .name("Blindness")
                .description("Blindness")
                .votingName("blindness")
                .duration(200L)
                .onStart(() -> {
                    forEachPlayer(player -> {
                        player.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(200, 5));
                    });
                })
                .onFinish(() -> {
                    forEachPlayer(player -> {
                        player.removePotionEffect(PotionEffectType.BLINDNESS);
                    });
                })
                .build();
    }
}
