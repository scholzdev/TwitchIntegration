package dev.florianscholz.twitchIntegration;

import dev.florianscholz.twitchIntegration.commands.EventCommand;
import dev.florianscholz.twitchIntegration.manager.EventDisplayManager;
import dev.florianscholz.twitchIntegration.manager.GameEventManager;
import dev.florianscholz.twitchIntegration.manager.GameEventRegistrar;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class TwitchIntegration extends JavaPlugin {

    @Getter
    private GameEventManager gameEventManager;

    @Getter
    private GameEventRegistrar gameEventRegistrar;

    @Getter
    private EventDisplayManager eventDisplayManager;

    @Override
    public void onEnable() {

        this.eventDisplayManager = new EventDisplayManager(this);
        this.gameEventRegistrar = new GameEventRegistrar(this);

        getServer().getPluginCommand("events").setExecutor(new EventCommand(this));
        this.gameEventManager = new GameEventManager(this);
        this.gameEventRegistrar.registerAll();


        getLogger().info("TwitchIntegration enabled.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if(gameEventManager != null) {
            gameEventManager.stopAllEvents();
        }

        getLogger().info("TwitchIntegration disabled.");
    }
}
