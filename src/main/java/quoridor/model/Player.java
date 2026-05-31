package quoridor.model;

/**
 * Represents one of the two Quoridor players.
 *
 * Player 1 starts at (8, 4) and must reach row 0.
 * Player 2 starts at (0, 4) and must reach row 8.
 */
public class Player {

    private final int id;               // 1 or 2
    private Position position;
    private int wallsRemaining;

    public static final int TOTAL_WALLS = 10;

    public Player(int id, Position startPosition) {
        this.id             = id;
        this.position       = startPosition;
        this.wallsRemaining = TOTAL_WALLS;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int      getId()             { return id; }
    public Position getPosition()       { return position; }
    public int      getWallsRemaining() { return wallsRemaining; }

    /** Goal row this player must reach to win. */
    public int getGoalRow() {
        return id == 1 ? 0 : 8;
    }

    public boolean hasReachedGoal() {
        return position.row == getGoalRow();
    }

    // -------------------------------------------------------------------------
    // Mutators
    // -------------------------------------------------------------------------

    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * Decrements wall count and returns true if successful.
     * Returns false if the player has no walls left.
     */
    public boolean useWall() {
        if (wallsRemaining <= 0) return false;
        wallsRemaining--;
        return true;
    }

    /** Refunds one wall (used for undo or temporary checks). */
    public void returnWall() {
        wallsRemaining++;
    }

    @Override
    public String toString() {
        return "Player " + id + " at " + position
                + " | walls remaining: " + wallsRemaining
                + " | goal row: " + getGoalRow();
    }
}
