package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DiscoEvent extends AbstractSimpleGameProvider {

    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        Map<UUID, Boolean> originalGlowing = new HashMap<>();
        Random random = new Random();

        return new SimpleGameEvent.Builder(plugin)
                .name("Disco")
                .description("Disco Disco")
                .votingName("disco")
                .duration(200)
                .tickInterval(5)
                .onStart(() -> forEachPlayer(p -> {
                    originalGlowing.put(p.getUniqueId(), p.isGlowing());
                }))
                .onTick(() -> forEachPlayer(player -> {
                    boolean glow = random.nextBoolean();
                    player.setGlowing(glow);
                    player.getWorld().spawnParticle(Particle.NOTE, player.getLocation().add(0,1,0), 250,1,1,1);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 1, true, false, false));
                }))
                .onFinish(() -> forEachPlayer(player -> {
                    Boolean glow = originalGlowing.get(player.getUniqueId());
                    if (glow != null) {
                        player.setGlowing(glow);
                        player.removePotionEffect(PotionEffectType.GLOWING);
                    }
                }))
                .build();
    }
}
