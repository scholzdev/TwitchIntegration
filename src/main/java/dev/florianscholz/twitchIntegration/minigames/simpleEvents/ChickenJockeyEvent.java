package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
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
                .cleanupSpawned()
                .saveAndRestoreInventory()
                .onStartWithEvent(event -> {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
                        for (int i = 0; i < amount; i++) {
                            spawnChickenJockey(event, player);
                        }
                    });
                })
                .build();
    }

    private static void spawnChickenJockey(SimpleGameEvent event, Player player) {
        Location playerLoc = player.getLocation();
        World world = player.getWorld();

        double radius = 5.0;
        double randomX = (Math.random() * (radius * 2)) - radius;
        double randomZ = (Math.random() * (radius * 2)) - radius;

        Location spawnLoc = playerLoc.clone().add(randomX, 0, randomZ);
        spawnLoc.setY(world.getHighestBlockYAt(spawnLoc) + 1);

        Chicken chicken = event.spawnAndTrack(spawnLoc, Chicken.class);
        Zombie zombie = event.spawnAndTrack(spawnLoc, Zombie.class);

        zombie.setBaby();
        zombie.setTarget(player);

        zombie.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
        zombie.getEquipment().setHelmetDropChance(0.0f);

        chicken.addPotionEffect(PotionEffectType.SPEED.createEffect(600, 1));

        chicken.addPassenger(zombie);
    }

}