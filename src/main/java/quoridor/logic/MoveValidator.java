package quoridor.logic;

import quoridor.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates all game moves according to Quoridor rules:
 *  - Pawn movement (including straight and diagonal jumps over the opponent).
 *  - Wall placement (overlap, crossing, and path-blocking checks).
 */
public class MoveValidator {

    private final Board  board;
    private final Player player1;
    private final Player player2;

    // Four cardinal directions: up, down, left, right
    private static final int[][] DIRECTIONS = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} };

    public MoveValidator(Board board, Player player1, Player player2) {
        this.board   = board;
        this.player1 = player1;
        this.player2 = player2;
    }

    // =========================================================================
    // Pawn movement
    // =========================================================================

    /**
     * Returns all positions the {@code currentPlayer} can legally move to
     * on this turn, accounting for walls and the opponent pawn's position.
     */
    public List<Position> getValidPawnMoves(Player currentPlayer) {
        Player   opponent = getOpponent(currentPlayer);
        Position pos      = currentPlayer.getPosition();
        Position oppPos   = opponent.getPosition();

        List<Position> moves = new ArrayList<>();

        for (int[] dir : DIRECTIONS) {
            int nr = pos.row + dir[0];
            int nc = pos.col + dir[1];

            // Out of bounds or blocked by wall → skip
            if (!inBounds(nr, nc)) continue;
            if (PathFinder.isBlocked(board, pos.row, pos.col, nr, nc)) continue;

            if (nr == oppPos.row && nc == oppPos.col) {
                // Adjacent to opponent → attempt jump
                addJumpMoves(moves, nr, nc, dir, pos);
            } else {
                moves.add(new Position(nr, nc));
            }
        }
        return moves;
    }

    /**
     * Returns true if moving {@code currentPlayer}'s pawn to {@code target}
     * is a legal move this turn.
     */
    public boolean isValidPawnMove(Player currentPlayer, Position target) {
        return getValidPawnMoves(currentPlayer).stream()
                .anyMatch(p -> p.equals(target));
    }

    // =========================================================================
    // Wall placement
    // =========================================================================

    /**
     * Returns true if {@code currentPlayer} may legally place {@code wall}:
     *  1. Wall anchor is within bounds.
     *  2. Player has walls remaining.
     *  3. Wall does not overlap or cross an existing wall.
     *  4. Wall does not completely block either player's path to their goal.
     */
    public boolean isValidWallPlacement(Wall wall, Player currentPlayer) {
        if (!wall.isValid())                         return false;
        if (currentPlayer.getWallsRemaining() <= 0)  return false;
        if (wallOverlapsExisting(wall))               return false;

        // Temporarily place wall on a copy of the board and check paths
        Board tempBoard = new Board(board);
        tempBoard.placeWall(wall);

        boolean p1Safe = PathFinder.shortestPathLength(tempBoard, player1.getPosition(),
                                                        player1.getGoalRow()) >= 0;
        boolean p2Safe = PathFinder.shortestPathLength(tempBoard, player2.getPosition(),
                                                        player2.getGoalRow()) >= 0;

        return p1Safe && p2Safe;
    }

    // =========================================================================
    // Jump helpers
    // =========================================================================

    /**
     * When the player is adjacent to the opponent at (oppRow, oppCol) and
     * approached from direction (dir), determines valid landing squares.
     *
     * Rule summary:
     *  - Straight jump (continue in same direction) is valid when not blocked
     *    by a wall and the destination is in bounds.
     *  - Diagonal jump (perpendicular) is valid when the straight option is
     *    unavailable (wall or board edge) AND the diagonal isn't wall-blocked.
     */
    private void addJumpMoves(List<Position> moves, int oppRow, int oppCol,
                               int[] dir, Position fromPos) {
        int straightRow = oppRow + dir[0];
        int straightCol = oppCol + dir[1];

        boolean straightClear =
                inBounds(straightRow, straightCol) &&
                !PathFinder.isBlocked(board, oppRow, oppCol, straightRow, straightCol);

        if (straightClear) {
            moves.add(new Position(straightRow, straightCol));
        } else {
            // Diagonal jumps in the two perpendicular directions
            for (int[] perp : perpendicularTo(dir)) {
                int diagRow = oppRow + perp[0];
                int diagCol = oppCol + perp[1];
                if (inBounds(diagRow, diagCol) &&
                    !PathFinder.isBlocked(board, oppRow, oppCol, diagRow, diagCol)) {
                    moves.add(new Position(diagRow, diagCol));
                }
            }
        }
    }

    // =========================================================================
    // Wall overlap / crossing check
    // =========================================================================

    /**
     * Returns true if placing {@code wall} would overlap or cross any wall
     * already on the board.
     *
     * A horizontal wall at (r, c) covers cell-edges at columns c and c+1.
     * It conflicts with:
     *   - Another horizontal wall at (r, c-1) or (r, c+1) [shared cell-edge]
     *   - Another horizontal wall at (r, c)               [exact duplicate]
     *   - A vertical wall at (r, c)                        [crossing at center]
     *
     * The vertical case is symmetric (swap rows ↔ columns).
     */
    private boolean wallOverlapsExisting(Wall wall) {
        int r = wall.row;
        int c = wall.col;

        if (wall.orientation == WallOrientation.HORIZONTAL) {
            if (board.isHorizontalWall(r, c))      return true;  // exact same
            if (board.isHorizontalWall(r, c - 1))  return true;  // left neighbour shares edge
            if (board.isHorizontalWall(r, c + 1))  return true;  // right neighbour shares edge
            if (board.isVerticalWall(r, c))         return true;  // crossing
        } else {
            if (board.isVerticalWall(r, c))          return true;  // exact same
            if (board.isVerticalWall(r - 1, c))      return true;  // top neighbour shares edge
            if (board.isVerticalWall(r + 1, c))      return true;  // bottom neighbour shares edge
            if (board.isHorizontalWall(r, c))        return true;  // crossing
        }
        return false;
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    private Player getOpponent(Player player) {
        return (player == player1) ? player2 : player1;
    }

    private static boolean inBounds(int row, int col) {
        return row >= 0 && row < Board.SIZE && col >= 0 && col < Board.SIZE;
    }

    /** Returns the two directions perpendicular to {@code dir}. */
    private static int[][] perpendicularTo(int[] dir) {
        if (dir[0] != 0) {
            // Moving vertically → perpendicular is horizontal
            return new int[][]{ {0, -1}, {0, 1} };
        } else {
            // Moving horizontally → perpendicular is vertical
            return new int[][]{ {-1, 0}, {1, 0} };
        }
    }
}
