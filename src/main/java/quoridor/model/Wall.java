package quoridor.model;

/**
 * Represents a wall placed on the board.
 *
 * Walls always span exactly 2 cells:
 *   HORIZONTAL wall at (r, c): blocks downward movement from row r to r+1
 *                               at columns c  AND  c+1.
 *   VERTICAL   wall at (r, c): blocks rightward movement from col c to c+1
 *                               at rows    r  AND  r+1.
 *
 * Valid anchor positions: row and col each in [0, 7]  (wall grid is 8×8).
 */
public class Wall {

    public final int row;               // anchor row    (0–7)
    public final int col;               // anchor column (0–7)
    public final WallOrientation orientation;

    public Wall(int row, int col, WallOrientation orientation) {
        this.row = row;
        this.col = col;
        this.orientation = orientation;
    }

    /** Returns true if anchor coordinates are within the valid wall grid. */
    public boolean isValid() {
        return row >= 0 && row <= 7 && col >= 0 && col <= 7;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Wall)) return false;
        Wall other = (Wall) obj;
        return row == other.row && col == other.col && orientation == other.orientation;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * row + col) + orientation.hashCode();
    }

    @Override
    public String toString() {
        return orientation + " wall at (" + row + ", " + col + ")";
    }
}
