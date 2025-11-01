package dev.florianscholz.twitchIntegration.manager;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.*;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEventProvider;
import dev.florianscholz.twitchIntegration.minigames.simpleEvents.*;
import lombok.Getter;

import java.util.List;

public class GameEventRegistrar {

    @Getter
    private List<GameEvent> gameEvents;

    private final TwitchIntegration plugin;

    public GameEventRegistrar(TwitchIntegration plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {

        var manager = plugin.getGameEventManager();

        manager.registerEvent(new SpawnSilverfish(plugin));
        manager.registerEvent(new GravityFlip(plugin));
        manager.registerEvent(new WrathOfKoko(plugin));
        manager.registerEvent(new RandomTP(plugin));
        manager.registerEvent(new InventoryShuffle(plugin));

        List<SimpleGameEventProvider> providers = List.of(
            new TestEvent(),
            new BreakDamagePlayerEvent(),
            new BlindnessEvent(),
            new DiscoEvent(),
            new OneHPEvent(),
            new ChickenJockeyEvent(),
            new DrunkModeEvent(),
            new SlownessEvent(),
            new FarmBlocksEvent(),
            new PhantomEvent()
        );

        providers.forEach(provider -> manager.registerEvent(provider.create(plugin)));

        plugin.getLogger().info("Registered " + manager.getEvents().size() + " events.");
    }

}
