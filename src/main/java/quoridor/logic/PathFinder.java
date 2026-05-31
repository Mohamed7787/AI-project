package quoridor.logic;

import quoridor.model.Board;
import quoridor.model.Player;
import quoridor.model.Position;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Provides path-finding utilities using Breadth-First Search (BFS).
 *
 * Used for two purposes:
 *  1. Validating wall placements – every player must retain at least one path
 *     to their goal row after a wall is placed.
 *  2. AI heuristic – estimating how many moves each player needs to win.
 */
public class PathFinder {

    // Four orthogonal directions: up, down, left, right
    private static final int[][] DIRECTIONS = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} };

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns true if the player has at least one valid path from their current
     * position to their goal row on the given board.
     */
    public static boolean hasPath(Board board, Player player) {
        return shortestPathLength(board, player.getPosition(), player.getGoalRow()) >= 0;
    }

    /**
     * Returns the BFS shortest-path distance (in moves) from {@code start} to
     * any cell in {@code goalRow}, ignoring opponent pawns.
     *
     * Returns -1 if no path exists.
     */
    public static int shortestPathLength(Board board, Position start, int goalRow) {
        if (start.row == goalRow) return 0;

        boolean[][] visited = new boolean[Board.SIZE][Board.SIZE];
        Queue<int[]> queue  = new LinkedList<>();

        queue.add(new int[]{ start.row, start.col, 0 });
        visited[start.row][start.col] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int r    = current[0];
            int c    = current[1];
            int dist = current[2];

            for (int[] dir : DIRECTIONS) {
                int nr = r + dir[0];
                int nc = c + dir[1];

                if (!inBounds(nr, nc))      continue;
                if (visited[nr][nc])        continue;
                if (isBlocked(board, r, c, nr, nc)) continue;

                if (nr == goalRow) return dist + 1;

                visited[nr][nc] = true;
                queue.add(new int[]{ nr, nc, dist + 1 });
            }
        }
        return -1;  // no path found
    }

    // -------------------------------------------------------------------------
    // Movement blocking helper (used by MoveValidator too)
    // -------------------------------------------------------------------------

    /**
     * Returns true if movement from (fromRow, fromCol) to (toRow, toCol) is
     * blocked by a wall.  Only checks the four cardinal directions.
     */
    public static boolean isBlocked(Board board, int fromRow, int fromCol,
                                     int toRow,   int toCol) {
        int dr = toRow  - fromRow;
        int dc = toCol  - fromCol;

        if (dr ==  1 && dc == 0) return board.isBlockedDown (fromRow, fromCol);
        if (dr == -1 && dc == 0) return board.isBlockedUp   (fromRow, fromCol);
        if (dr ==  0 && dc == 1) return board.isBlockedRight(fromRow, fromCol);
        if (dr ==  0 && dc ==-1) return board.isBlockedLeft (fromRow, fromCol);

        return true;  // diagonal or multi-step movement is always "blocked"
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static boolean inBounds(int row, int col) {
        return row >= 0 && row < Board.SIZE && col >= 0 && col < Board.SIZE;
    }
}
