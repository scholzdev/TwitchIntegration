package dev.florianscholz.twitchIntegration.minigames.base;

public interface IGameEvent {

    String getName();
    String getDescription();
    long getDuration();
    String getVotingName();

    void start();
    void stop();
    boolean isRunning();
}
