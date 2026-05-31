package quoridor.model;

/**
 * Represents the 9×9 Quoridor board and all placed walls.
 *
 * Wall storage uses two 8×8 boolean arrays:
 *   hWalls[r][c] = true  →  HORIZONTAL wall at anchor (r,c)
 *                            blocks movement between row r and r+1
 *                            at both column c and column c+1.
 *
 *   vWalls[r][c] = true  →  VERTICAL wall at anchor (r,c)
 *                            blocks movement between col c and c+1
 *                            at both row r and row r+1.
 */
public class Board {

    public static final int SIZE       = 9;   // board cells per side
    public static final int WALL_SLOTS = 8;   // wall anchor positions per side

    private final boolean[][] hWalls;   // horizontal walls [0..7][0..7]
    private final boolean[][] vWalls;   // vertical   walls [0..7][0..7]

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public Board() {
        hWalls = new boolean[WALL_SLOTS][WALL_SLOTS];
        vWalls = new boolean[WALL_SLOTS][WALL_SLOTS];
    }

    /** Deep-copy constructor used by move validation without mutating the real board. */
    public Board(Board other) {
        this.hWalls = new boolean[WALL_SLOTS][WALL_SLOTS];
        this.vWalls = new boolean[WALL_SLOTS][WALL_SLOTS];
        for (int i = 0; i < WALL_SLOTS; i++) {
            System.arraycopy(other.hWalls[i], 0, this.hWalls[i], 0, WALL_SLOTS);
            System.arraycopy(other.vWalls[i], 0, this.vWalls[i], 0, WALL_SLOTS);
        }
    }

    // -------------------------------------------------------------------------
    // Mutators
    // -------------------------------------------------------------------------

    public void placeWall(Wall wall) {
        if (wall.orientation == WallOrientation.HORIZONTAL) {
            hWalls[wall.row][wall.col] = true;
        } else {
            vWalls[wall.row][wall.col] = true;
        }
    }

    // -------------------------------------------------------------------------
    // Wall queries
    // -------------------------------------------------------------------------

    public boolean isHorizontalWall(int row, int col) {
        if (row < 0 || row >= WALL_SLOTS || col < 0 || col >= WALL_SLOTS) return false;
        return hWalls[row][col];
    }

    public boolean isVerticalWall(int row, int col) {
        if (row < 0 || row >= WALL_SLOTS || col < 0 || col >= WALL_SLOTS) return false;
        return vWalls[row][col];
    }

    public boolean[][] getHWalls() { return hWalls; }
    public boolean[][] getVWalls() { return vWalls; }

    // -------------------------------------------------------------------------
    // Movement blocking queries
    //
    // A pawn at (row, col) is blocked in a direction if any wall covers that edge.
    // Each wall covers TWO consecutive edges, so we check two anchor positions.
    // -------------------------------------------------------------------------

    /**
     * Is movement from (row, col)  →  (row+1, col) blocked?
     * Blocked by hWalls[row][col]  or  hWalls[row][col-1].
     */
    public boolean isBlockedDown(int row, int col) {
        if (row < 0 || row >= WALL_SLOTS) return false;   // row+1 out of bounds handled by caller
        boolean left  = (col > 0)            && hWalls[row][col - 1];
        boolean right = (col < WALL_SLOTS)   && hWalls[row][col];
        return left || right;
    }

    /**
     * Is movement from (row, col)  →  (row-1, col) blocked?
     */
    public boolean isBlockedUp(int row, int col) {
        return isBlockedDown(row - 1, col);
    }

    /**
     * Is movement from (row, col)  →  (row, col+1) blocked?
     * Blocked by vWalls[row][col]  or  vWalls[row-1][col].
     */
    public boolean isBlockedRight(int row, int col) {
        if (col < 0 || col >= WALL_SLOTS) return false;
        boolean top    = (row < WALL_SLOTS) && vWalls[row][col];
        boolean bottom = (row > 0)          && vWalls[row - 1][col];
        return top || bottom;
    }

    /**
     * Is movement from (row, col)  →  (row, col-1) blocked?
     */
    public boolean isBlockedLeft(int row, int col) {
        return isBlockedRight(row, col - 1);
    }
}
