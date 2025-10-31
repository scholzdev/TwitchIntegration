package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffectType;

public class ChickenJockeyEvent extends AbstractSimpleGameProvider {

    private final int amount = 3;

    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        return new SimpleGameEvent.Builder(plugin)
                .name("Chicken Jockey")
                .description("Spawn baby zombies riding chickens for each player")
                .votingName("chicken_jockey")
                .duration(600)
                .onStart(() -> {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        for (int i = 0; i < amount; i++) {
                            spawnChickenJockey(player);
                        }
                    });
                })
                .build();
    }

    private void spawnChickenJockey(Player player) {
        Chicken chicken = (Chicken) player.getWorld().spawnEntity(player.getLocation(), EntityType.CHICKEN);
        Zombie zombie = (Zombie) player.getWorld().spawnEntity(player.getLocation(), EntityType.ZOMBIE);
        zombie.setBaby(true);

        chicken.addPotionEffect(PotionEffectType.SPEED.createEffect(600, 1));
        chicken.addPassenger(zombie);
    }
}
