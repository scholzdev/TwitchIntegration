package dev.florianscholz.twitchIntegration.minigames.base.simple;

import dev.florianscholz.twitchIntegration.TwitchIntegration;
import dev.florianscholz.twitchIntegration.minigames.base.GameEvent;

/**
 * Represents a provider for a GameEvent.
 * Each implementation should return a fully constructed GameEvent.
 */
public interface SimpleGameEventProvider {
    GameEvent create(TwitchIntegration plugin);
}