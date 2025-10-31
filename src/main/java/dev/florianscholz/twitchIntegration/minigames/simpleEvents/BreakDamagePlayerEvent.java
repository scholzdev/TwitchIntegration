package dev.florianscholz.twitchIntegration.minigames.simpleEvents;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.simple.AbstractSimpleGameProvider;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class BreakDamagePlayerEvent extends AbstractSimpleGameProvider {

    @Override
    public SimpleGameEvent create(TwitchIntegration plugin) {

        return new SimpleGameEvent.Builder(plugin)
                .name("Break Damage")
                .description("Breaking Blocks deals damage")
                .votingName("break_damage")
                .duration(200)
                .onStart(() -> plugin.getServer()
                        .broadcast(Component.text("Break Damage started!", NamedTextColor.RED)))
                .onFinish(() -> plugin.getServer()
                        .broadcast(Component.text("Break Damage stopped!", NamedTextColor.RED)))
                .<BlockBreakEvent>on(BlockBreakEvent.class, event -> {
                    Player player = event.getPlayer();
                    player.damage(1f);
                })
                .build();
    }
}