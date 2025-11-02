package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import org.bukkit.potion.PotionEffectType;

public class GravityFlipEvent extends AbstractSimpleGameProvider {

    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        return new SimpleGameEvent.Builder(plugin)
            .name("Gravity Flip")
            .description("Flips gravity")
            .votingName("gravity_flip")
            .duration(200)
            .onStartWithEvent(event -> {
                event.forEach(player -> {
                    player.addPotionEffect(PotionEffectType.LEVITATION.createEffect(200, 2));
                });
            })
            .onFinishWithEvent(event -> {
                event.forEach(player -> {
                    player.removePotionEffect(PotionEffectType.LEVITATION);
                });
            })
            .build();
    }
}
