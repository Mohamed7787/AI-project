package quoridor.logic;

import quoridor.model.*;

import java.util.List;

/**
 * Central game state coordinator.
 *
 * Owns the board, both players, and the turn counter.
 * All game actions (pawn move, wall placement) are routed through here
 * so that the GUI only needs to call methods on GameState.
 *
 * Starting positions (row, col):
 *   Player 1  →  (8, 4)  –  bottom centre, goal: row 0
 *   Player 2  →  (0, 4)  –  top    centre, goal: row 8
 */
public class GameState {

    private final Board         board;
    private final Player        player1;
    private final Player        player2;
    private final MoveValidator validator;

    private int     currentPlayerId;   // 1 or 2
    private boolean gameOver;
    private int     winnerId;          // 0 = no winner yet

    // =========================================================================
    // Construction / Reset
    // =========================================================================

    public GameState() {
        board   = new Board();
        player1 = new Player(1, new Position(8, 4));
        player2 = new Player(2, new Position(0, 4));

        validator       = new MoveValidator(board, player1, player2);
        currentPlayerId = 1;
        gameOver        = false;
        winnerId        = 0;
    }

    // =========================================================================
    // Actions
    // =========================================================================

    /**
     * Attempts to move the current player's pawn to {@code target}.
     *
     * @return true if the move was accepted and applied.
     */
    public boolean movePawn(Position target) {
        if (gameOver) return false;

        Player current = getCurrentPlayer();
        if (!validator.isValidPawnMove(current, target)) return false;

        current.setPosition(target);

        if (current.hasReachedGoal()) {
            gameOver = true;
            winnerId = current.getId();
        } else {
            switchTurn();
        }
        return true;
    }

    /**
     * Attempts to place a wall for the current player.
     *
     * @return true if the placement was accepted and applied.
     */
    public boolean placeWall(Wall wall) {
        if (gameOver) return false;

        Player current = getCurrentPlayer();
        if (!validator.isValidWallPlacement(wall, current)) return false;

        board.placeWall(wall);
        current.useWall();
        switchTurn();
        return true;
    }

    // =========================================================================
    // Queries delegated to MoveValidator
    // =========================================================================

    /** Returns all legal pawn destinations for the current player. */
    public List<Position> getValidMovesForCurrent() {
        return validator.getValidPawnMoves(getCurrentPlayer());
    }

    /** Returns all legal pawn destinations for an arbitrary player (used by AI). */
    public List<Position> getValidMoves(Player player) {
        return validator.getValidPawnMoves(player);
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public Board   getBoard()   { return board;   }
    public Player  getPlayer1() { return player1; }
    public Player  getPlayer2() { return player2; }

    public Player getCurrentPlayer() {
        return currentPlayerId == 1 ? player1 : player2;
    }

    public Player getOpponent(Player player) {
        return (player == player1) ? player2 : player1;
    }

    public int     getCurrentPlayerId() { return currentPlayerId; }
    public boolean isGameOver()         { return gameOver;        }
    public int     getWinnerId()        { return winnerId;        }

    public MoveValidator getValidator() { return validator; }

    // =========================================================================
    // Internal
    // =========================================================================

    private void switchTurn() {
        currentPlayerId = (currentPlayerId == 1) ? 2 : 1;
    }

    /**
     * Package-private: used only by the AI when cloning game states.
     * Do not call from game logic or GUI code.
     */
    public void forceSwitchTurn() {
        switchTurn();
    }
}
