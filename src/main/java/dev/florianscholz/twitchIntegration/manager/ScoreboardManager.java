package dev.florianscholz.twitchIntegration.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import dev.florianscholz.twitchIntegration.minigames.base.simple.SimpleGameEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages scoreboards for game events.
 * Handles creation, updating, and cleanup of scoreboards.
 */
public class ScoreboardManager {
    
    private final SimpleGameEvent event;
    private String title;
    private final Map<Integer, String> lines = new HashMap<>();
    private final Map<Player, Scoreboard> playerBoards = new HashMap<>();
    private final Map<Player, Scoreboard> originalBoards = new HashMap<>();
    private Objective objective;
    private int maxLines = 15;
    
    public ScoreboardManager(SimpleGameEvent event, String title) {
        this.event = event;
        this.title = ChatColor.translateAlternateColorCodes('&', title);
    }
    
    /**
     * Sets the title of the scoreboard.
     * @param title The new title (supports color codes with &)
     */
    public void setTitle(String title) {
        this.title = ChatColor.translateAlternateColorCodes('&', title);
        if (objective != null) {
            objective.setDisplayName(this.title);
        }
    }
    
    /**
     * Sets a line in the scoreboard.
     * @param line The line number (1 = top, higher numbers = lower on screen)
     * @param text The text to display (supports color codes with &)
     */
    public void setLine(int line, String text) {
        if (line < 1 || line > maxLines) {
            throw new IllegalArgumentException("Line must be between 1 and " + maxLines);
        }
        lines.put(line, ChatColor.translateAlternateColorCodes('&', text));
    }
    
    /**
     * Removes a line from the scoreboard.
     * @param line The line number to remove
     */
    public void removeLine(int line) {
        lines.remove(line);
    }
    
    /**
     * Clears all lines from the scoreboard.
     */
    public void clearLines() {
        lines.clear();
    }
    
    /**
     * Updates the scoreboard for all online players.
     * Call this in onTick() or whenever you want to refresh the display.
     */
    public void update() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }
    
    /**
     * Updates the scoreboard for a specific player.
     * @param player The player to update
     */
    public void updatePlayer(Player player) {
        Scoreboard board = playerBoards.get(player);
        
        // Create scoreboard if it doesn't exist for this player
        if (board == null) {
            // Save original scoreboard
            originalBoards.put(player, player.getScoreboard());
            
            // Create new scoreboard
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            objective = board.registerNewObjective("game_event", "dummy", title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            playerBoards.put(player, board);
            player.setScoreboard(board);
        }
        
        // Clear existing scores
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }
        
        // Set new scores
        int scoreValue = maxLines;
        for (int i = 1; i <= maxLines; i++) {
            String text = lines.get(i);
            if (text != null) {
                // Handle duplicate lines by adding invisible characters
                String uniqueText = makeUnique(text, i);
                Score score = objective.getScore(uniqueText);
                score.setScore(scoreValue);
            }
            scoreValue--;
        }
    }
    
    /**
     * Makes a line unique by adding invisible color codes if needed.
     * This allows duplicate text on different lines.
     */
    private String makeUnique(String text, int lineNumber) {
        // Add invisible color codes to make each line unique
        StringBuilder unique = new StringBuilder(text);
        for (int i = 0; i < lineNumber; i++) {
            unique.append(ChatColor.RESET);
        }
        
        // Bukkit has a 40 character limit for scoreboard entries
        if (unique.length() > 40) {
            unique.setLength(40);
        }
        
        return unique.toString();
    }
    
    /**
     * Shows the scoreboard to all online players.
     */
    public void show() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }
    
    /**
     * Hides the scoreboard and restores original scoreboards for all players.
     */
    public void hide() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard original = originalBoards.get(player);
            if (original != null) {
                player.setScoreboard(original);
            }
        }
        playerBoards.clear();
        originalBoards.clear();
    }
    
    /**
     * Checks if the scoreboard is currently active.
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return !playerBoards.isEmpty();
    }
    
    /**
     * Gets the current title.
     * @return The scoreboard title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Gets a line's text.
     * @param line The line number
     * @return The text on that line, or null if not set
     */
    public String getLine(int line) {
        return lines.get(line);
    }
    
    /**
     * Sets the maximum number of lines.
     * @param maxLines The maximum number of lines (1-15)
     */
    public void setMaxLines(int maxLines) {
        if (maxLines < 1 || maxLines > 15) {
            throw new IllegalArgumentException("Max lines must be between 1 and 15");
        }
        this.maxLines = maxLines;
    }
}