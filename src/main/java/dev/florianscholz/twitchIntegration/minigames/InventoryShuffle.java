package dev.florianscholz.twitchIntegration.minigames;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class InventoryShuffle extends GameEvent {

    public InventoryShuffle(TwitchIntegration plugin) {
        super(plugin);
    }

    @Override
    protected void onStart() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if(!isRunning()) {
                    cancel();
                    return;
                }

                plugin.getServer().getOnlinePlayers().forEach(player -> {
                    List<ItemStack> items = new ArrayList<>();

                    for(int i = 0; i < 36; i++) {
                        items.add(player.getInventory().getItem(i));
                    }

                    Collections.shuffle(items);

                    for(int i = 0; i < 36; i++) {
                        player.getInventory().setItem(i, items.get(i));
                    }

                });
            }
        }.runTaskTimer(plugin, 0, 60);
    }

    @Override
    protected void onFinish() {

    }

    @Override
    public String getName() {
        return "Inventory Shuffle";
    }

    @Override
    public String getDescription() {
        return "Shuffle your inventory";
    }

    @Override
    public long getDuration() {
        return 300;
    }

    @Override
    public String getVotingName() {
        return "inventory_shuffle";
    }
}
