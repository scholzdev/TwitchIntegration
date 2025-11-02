package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InventoryShuffleEvent extends AbstractSimpleGameProvider {

    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        return new SimpleGameEvent.Builder(plugin)
                .name("Inventory Shuffle")
                .description("Shuffles the inventory of all players")
                .votingName("inventory_shuffle")
                .duration(300)
                .onTick(() -> {
                    forEachPlayer(player -> {
                        List<ItemStack> items = new ArrayList<>();

                        for(int i = 0; i < 36; i++) {
                            items.add(player.getInventory().getItem(i));
                        }

                        Collections.shuffle(items);

                        for(int i = 0; i < 36; i++) {
                            player.getInventory().setItem(i, items.get(i));
                        }
                    });
                })
                .tickInterval(60)
                .build();
    }
}
