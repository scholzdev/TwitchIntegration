package dev.florianscholz.twitchIntegration.minigames;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.potion.PotionEffectType;

public class GravityFlip extends GameEvent {

    public GravityFlip(TwitchIntegration plugin) {
        super(plugin);
    }

    @Override
    protected void onStart() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.addPotionEffect(PotionEffectType.LEVITATION.createEffect((int)getDuration(), 10));
        });
    }

    @Override
    protected void onFinish() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.removePotionEffect(PotionEffectType.LEVITATION);
        });
    }

    @Override
    public String getName() {
        return "Gravity Flip";
    }

    @Override
    public String getDescription() {
        return "Flips gravity";
    }

    @Override
    public long getDuration() {
        return 200;
    }

    @Override
    public String getVotingName() {
        return "gravity_flip";
    }
}
