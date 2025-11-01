// ============= EventDisplayManager.java =============
package dev.florianscholz.twitchIntegration.manager;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;

public class EventDisplayManager {

    private final TwitchIntegration plugin;
    private BossBar currentBossBar;
    private BukkitTask actionBarTask;
    private long eventEndTime;

    public EventDisplayManager(TwitchIntegration plugin) {
        this.plugin = plugin;
    }

    public void showEventStart(GameEvent event) {
        Component title = Component.text(event.getName())
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD);

        Component subtitle = Component.text(event.getDescription())
                .color(NamedTextColor.YELLOW);

        Title titleDisplay = Title.title(
                title,
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(500),  // fade in
                        Duration.ofMillis(3000), // stay
                        Duration.ofMillis(1000)  // fade out
                )
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(titleDisplay);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }

        this.eventEndTime = System.currentTimeMillis() + (event.getDuration() * 50);
        startBossBar(event.getName(), event.getDuration());
    }

    public void showEventEnd(GameEvent event) {
        Component title = Component.text("Event finished")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD);

        Component subtitle = Component.text(event.getName())
                .color(NamedTextColor.GRAY);

        Title titleDisplay = Title.title(
                title,
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(2000),
                        Duration.ofMillis(500)
                )
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(titleDisplay);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        stopBossBar();
    }

    private void startBossBar(String eventName, long durationTicks) {
        stopBossBar();

        currentBossBar = BossBar.bossBar(
                Component.text(eventName).color(NamedTextColor.GOLD),
                1.0f,
                BossBar.Color.RED,
                BossBar.Overlay.PROGRESS
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(currentBossBar);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (currentBossBar == null) {
                    cancel();
                    return;
                }

                long remaining = eventEndTime - System.currentTimeMillis();
                if (remaining <= 0) {
                    cancel();
                    return;
                }

                float progress = (float) remaining / (durationTicks * 50);
                currentBossBar.progress(Math.max(0, Math.min(1, progress)));

                if (progress < 0.25f) {
                    currentBossBar.color(BossBar.Color.RED);
                } else if (progress < 0.5f) {
                    currentBossBar.color(BossBar.Color.YELLOW);
                } else {
                    currentBossBar.color(BossBar.Color.GREEN);
                }
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    public void showCountdown(String eventName, int seconds, Runnable onComplete) {
        final int[] secondsLeft = {seconds};

        BukkitTask countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (secondsLeft[0] <= 0) {
                    cancel();
                    onComplete.run();
                    return;
                }

                Component title = Component.text(eventName)
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD);

                Component subtitle = Component.text("Starts in " + secondsLeft[0] + " second" + (secondsLeft[0] == 1 ? "" : "s"))
                        .color(NamedTextColor.YELLOW);

                Title titleDisplay = Title.title(
                        title,
                        subtitle,
                        Title.Times.times(
                                Duration.ofMillis(0),    // fade in
                                Duration.ofMillis(1000), // stay
                                Duration.ofMillis(200)   // fade out
                        )
                );

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.showTitle(titleDisplay);
                }

                secondsLeft[0]--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void stopBossBar() {
        if (currentBossBar != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.hideBossBar(currentBossBar);
            }
            currentBossBar = null;
        }
    }

    public void cleanup() {
        stopBossBar();
    }
}
