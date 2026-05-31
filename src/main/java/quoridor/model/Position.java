package quoridor.model;

/**
 * Immutable representation of a board position (row, col).
 * Rows and columns are 0-indexed, ranging from 0 to 8 on a 9x9 board.
 */
public class Position {

    public final int row;
    public final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /** Returns true if this position falls within the 9x9 board. */
    public boolean isValid() {
        return row >= 0 && row < Board.SIZE && col >= 0 && col < Board.SIZE;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Position)) return false;
        Position other = (Position) obj;
        return row == other.row && col == other.col;
    }

    @Override
    public int hashCode() {
        return 31 * row + col;
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}
