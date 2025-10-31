package dev.florianscholz.twitchIntegration.minigames;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import org.bukkit.Bukkit;

public class WrathOfKoko extends GameEvent {

    public WrathOfKoko(TwitchIntegration plugin) {
        super(plugin);
    }

    @Override
    protected void onStart() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.getWorld().strikeLightning(player.getLocation());
            player.damage(10.f);
        });
    }

    @Override
    protected void onFinish() {
        Bukkit.getOnlinePlayers().forEach(player -> {

        });
    }

    @Override
    public String getName() {
        return "Wrath of Koko";
    }

    @Override
    public String getDescription() {
        return "I smite thee";
    }

    @Override
    public long getDuration() {
        return 200;
    }

    @Override
    public String getVotingName() {
        return "wrath_of_koko";
    }
}
