package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import org.bukkit.event.block.BlockBreakEvent;

public class FarmBlocksEvent extends AbstractSimpleGameProvider {

    private int destroyedBlocks = 0;

    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {
        return new SimpleGameEvent.Builder(plugin)
                .name("Farm Blocks")
                .description("Farming Blocks")
                .votingName("farm_blocks")
                .untilCondition()
                .on(BlockBreakEvent.class, event -> {
                    if (event.getBlock().getType().isSolid()) {
                        destroyedBlocks++;
                    }
                })
                .finishWhen(() -> destroyedBlocks >= 10)
                .build();
    }
}
