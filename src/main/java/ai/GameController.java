package ai;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import quoridor.ai.QuoridorAI;
import quoridor.logic.GameState;
import quoridor.model.*;

import java.util.List;

/**
 * Main game controller.
 *
 * Responsibilities:
 * – Renders the board on a Canvas (cells, walls, pawns, highlights, preview).
 * – Handles mouse clicks for pawn moves and wall placement.
 * – Delegates AI turns to QuoridorAI with a short visual delay.
 * – Updates all status labels after every action.
 */
public class GameController {

    // ── FXML injected fields ────────────────────────────────────────────────
    @FXML private Canvas       gameCanvas;
    @FXML private Label        lblTurn;
    @FXML private Label        lblMode;
    @FXML private Label        lblP1Walls;
    @FXML private Label        lblP2Walls;
    @FXML private Label        lblMessage;
    @FXML private ToggleButton btnWallMode;
    @FXML private ToggleButton btnOrientation;

    // ── Drawing constants ───────────────────────────────────────────────────
    private static final int CELL_SIZE  = 56;
    private static final int WALL_GAP   = 10;
    private static final int STEP       = CELL_SIZE + WALL_GAP;   // 66
    private static final int PADDING    = 30;

    // ── Colors ──────────────────────────────────────────────────────────────
    private static final Color C_BOARD_BG    = Color.web("#8B5E3C");
    private static final Color C_CELL        = Color.web("#F5DEB3");
    private static final Color C_CELL_STROKE = Color.web("#A0845C");
    private static final Color C_GOAL_P1     = Color.web("#FADBD8");
    private static final Color C_GOAL_P2     = Color.web("#D6EAF8");
    private static final Color C_HIGHLIGHT   = Color.web("#82E0AA");
    private static final Color C_PLAYER1     = Color.web("#E74C3C");
    private static final Color C_PLAYER2     = Color.web("#2980B9");
    private static final Color C_WALL        = Color.web("#1A252F");
    private static final Color C_PREVIEW     = Color.web("#F39C1280");  // semi-transparent

    // ── Game state ──────────────────────────────────────────────────────────
    private GameState        gameState;
    private GameMode         gameMode;
    private boolean          placingWall    = false;
    private WallOrientation  wallOrientation = WallOrientation.HORIZONTAL;
    private Wall             previewWall    = null;   // shown on mouse hover
    private List<Position>   validMoves     = List.of();
    private boolean          aiThinking     = false;

    // ── Initialization ──────────────────────────────────────────────────────

    /**
     * Called by MainMenuController after loading the FXML.
     * Sets up the game for the chosen mode.
     */
    public void initGame(GameMode mode) {
        this.gameMode  = mode;
        this.gameState = new GameState();
        resetUIState();
        attachCanvasListeners();
        refreshValidMoves();
        drawBoard();
        updateStatus();
    }

    @FXML
    public void initialize() {
        // Canvas mouse listeners are attached in initGame() once the game state is ready.
    }

    private void attachCanvasListeners() {
        gameCanvas.setOnMouseClicked(this::handleCanvasClick);
        gameCanvas.setOnMouseMoved(this::handleCanvasHover);
    }

    // ── Canvas drawing ──────────────────────────────────────────────────────

    /** Full board redraw.  Called after every state change. */
    private void drawBoard() {
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        double w = gameCanvas.getWidth();
        double h = gameCanvas.getHeight();

        // Background
        gc.setFill(C_BOARD_BG);
        gc.fillRect(0, 0, w, h);

        // Cells
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                double x = cellX(c);
                double y = cellY(r);

                // Goal-row tinting
                if (r == 0)  gc.setFill(C_GOAL_P1);
                else if (r == 8) gc.setFill(C_GOAL_P2);
                else             gc.setFill(C_CELL);

                // Valid-move highlight overrides cell colour
                if (validMoves.contains(new Position(r, c))) {
                    gc.setFill(C_HIGHLIGHT);
                }

                gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                gc.setStroke(C_CELL_STROKE);
                gc.setLineWidth(1);
                gc.strokeRect(x, y, CELL_SIZE, CELL_SIZE);
            }
        }

        // Goal-row labels
        drawGoalLabel(gc, 0, "← P1 Goal");
        drawGoalLabel(gc, 8, "← P2 Goal");

        // Placed walls
        Board board = gameState.getBoard();
        gc.setFill(C_WALL);
        for (int r = 0; r < Board.WALL_SLOTS; r++) {
            for (int c = 0; c < Board.WALL_SLOTS; c++) {
                if (board.isHorizontalWall(r, c)) drawWall(gc, r, c, WallOrientation.HORIZONTAL, C_WALL, 1.0);
                if (board.isVerticalWall(r, c))   drawWall(gc, r, c, WallOrientation.VERTICAL,   C_WALL, 1.0);
            }
        }

        // Wall preview (hover)
        if (previewWall != null && placingWall) {
            drawWall(gc, previewWall.row, previewWall.col, previewWall.orientation, C_PREVIEW, 0.55);
        }

        // Pawns
        drawPawn(gc, gameState.getPlayer1().getPosition(), C_PLAYER1, "1");
        drawPawn(gc, gameState.getPlayer2().getPosition(), C_PLAYER2, "2");
    }

    /** Draws a single pawn as a filled circle with a number label. */
    private void drawPawn(GraphicsContext gc, Position pos, Color color, String label) {
        double cx = cellX(pos.col) + CELL_SIZE / 2.0;
        double cy = cellY(pos.row) + CELL_SIZE / 2.0;
        double r  = CELL_SIZE * 0.36;

        // Shadow
        gc.setFill(Color.color(0, 0, 0, 0.3));
        gc.fillOval(cx - r + 2, cy - r + 3, r * 2, r * 2);

        // Body
        gc.setFill(color);
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);

        // Highlight sheen
        gc.setFill(Color.color(1, 1, 1, 0.25));
        gc.fillOval(cx - r * 0.55, cy - r * 0.65, r * 0.7, r * 0.55);

        // Number
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 16));
        gc.fillText(label, cx - 5, cy + 6);
    }

    /** Draws a wall rectangle for the given anchor, orientation, color, and opacity. */
    private void drawWall(GraphicsContext gc, int row, int col,
                           WallOrientation orientation, Color color, double opacity) {
        gc.save();
        gc.setGlobalAlpha(opacity);
        gc.setFill(color);

        if (orientation == WallOrientation.HORIZONTAL) {
            double x = cellX(col);
            double y = cellY(row) + CELL_SIZE;
            double ww = 2 * CELL_SIZE + WALL_GAP;
            double wh = WALL_GAP;
            gc.fillRoundRect(x, y, ww, wh, 4, 4);
        } else {
            double x = cellX(col) + CELL_SIZE;
            double y = cellY(row);
            double ww = WALL_GAP;
            double wh = 2 * CELL_SIZE + WALL_GAP;
            gc.fillRoundRect(x, y, ww, wh, 4, 4);
        }
        gc.restore();
    }

    /** Draws a small goal-row label on the right side of the board. */
    private void drawGoalLabel(GraphicsContext gc, int row, String text) {
        gc.setFill(Color.web("#555555"));
        gc.setFont(javafx.scene.text.Font.font("Arial", 11));
        double y = cellY(row) + CELL_SIZE / 2.0 + 4;
        gc.fillText(text, cellX(8) + CELL_SIZE + 4, y);
    }

    // ── Mouse handling ──────────────────────────────────────────────────────

    private void handleCanvasClick(MouseEvent e) {
        if (gameState.isGameOver() || aiThinking) return;
        if (isAITurn()) return;   // don't accept human input during AI turn

        double relX = e.getX() - PADDING;
        double relY = e.getY() - PADDING;
        if (relX < 0 || relY < 0) return;

        int colStep = (int) (relX / STEP);
        int rowStep = (int) (relY / STEP);
        double colRem = relX - colStep * STEP;
        double rowRem = relY - rowStep * STEP;

        boolean inColCell = colRem < CELL_SIZE;
        boolean inRowCell = rowRem < CELL_SIZE;

        if (!placingWall) {
            // ── PAWN MOVE ──
            if (inColCell && inRowCell && colStep < Board.SIZE && rowStep < Board.SIZE) {
                Position target = new Position(rowStep, colStep);
                if (gameState.movePawn(target)) {
                    lblMessage.setText("");
                    postMoveUpdate();
                } else {
                    lblMessage.setText("⚠  Invalid move — click a green square.");
                }
            }
        } else {
            // ── WALL PLACEMENT ──
            Wall wall = detectWallSlot(rowStep, colStep, inRowCell, inColCell);
            if (wall != null) {
                if (gameState.placeWall(wall)) {
                    lblMessage.setText("");
                    postMoveUpdate();
                } else {
                    lblMessage.setText("⚠  Cannot place wall there.");
                }
            }
        }
    }

    private void handleCanvasHover(MouseEvent e) {
        if (!placingWall || gameState.isGameOver()) {
            previewWall = null;
            return;
        }

        double relX = e.getX() - PADDING;
        double relY = e.getY() - PADDING;
        if (relX < 0 || relY < 0) { previewWall = null; return; }

        int colStep = (int) (relX / STEP);
        int rowStep = (int) (relY / STEP);
        double colRem = relX - colStep * STEP;
        double rowRem = relY - rowStep * STEP;

        boolean inColCell = colRem < CELL_SIZE;
        boolean inRowCell = rowRem < CELL_SIZE;

        previewWall = detectWallSlot(rowStep, colStep, inRowCell, inColCell);
        drawBoard();
    }

    /**
     * Converts a grid position + cell/gap flags into a Wall anchor, or null
     * if the mouse isn't hovering over a valid wall slot.
     */
    private Wall detectWallSlot(int rowStep, int colStep,
                                  boolean inRowCell, boolean inColCell) {
        boolean inRowGap = !inRowCell && rowStep < Board.WALL_SLOTS;
        boolean inColGap = !inColCell && colStep < Board.WALL_SLOTS;

        if (wallOrientation == WallOrientation.HORIZONTAL) {
            // Click must be in a horizontal gap (between two rows), within a cell column
            if (inRowGap && inColCell && colStep < Board.WALL_SLOTS) {
                return new Wall(rowStep, colStep, WallOrientation.HORIZONTAL);
            }
        } else {
            // Click must be in a vertical gap (between two columns), within a cell row
            if (inColGap && inRowCell && rowStep < Board.WALL_SLOTS) {
                return new Wall(rowStep, colStep, WallOrientation.VERTICAL);
            }
        }
        return null;
    }

    // ── Post-move update ────────────────────────────────────────────────────

    /** Runs after any successful move: refresh display, check win, trigger AI. */
    private void postMoveUpdate() {
        refreshValidMoves();
        drawBoard();
        updateStatus();

        if (gameState.isGameOver()) {
            showWinner();
            return;
        }

        if (isAITurn()) {
            triggerAIMove();
        }
    }

    /** Refreshes the set of valid moves for the current player. */
    private void refreshValidMoves() {
        if (!placingWall && !gameState.isGameOver()) {
            validMoves = gameState.getValidMovesForCurrent();
        } else {
            validMoves = List.of();
        }
    }

    // ── AI ──────────────────────────────────────────────────────────────────

    private boolean isAITurn() {
        return (gameMode == GameMode.HUMAN_VS_AI_EASY
             || gameMode == GameMode.HUMAN_VS_AI_MEDIUM
             || gameMode == GameMode.HUMAN_VS_AI_HARD)
                && gameState.getCurrentPlayerId() == 2;
    }

    /** Schedules an AI move after a 600 ms thinking pause for visual feedback. */
    private void triggerAIMove() {
        aiThinking = true;
        lblTurn.setText("Computer is thinking…");
        validMoves = List.of();
        drawBoard();

        PauseTransition pause = new PauseTransition(Duration.millis(600));
        pause.setOnFinished(ev -> {
            QuoridorAI.Difficulty diff;
            if (gameMode == GameMode.HUMAN_VS_AI_HARD)        diff = QuoridorAI.Difficulty.HARD;
            else if (gameMode == GameMode.HUMAN_VS_AI_MEDIUM) diff = QuoridorAI.Difficulty.MEDIUM;
            else                                               diff = QuoridorAI.Difficulty.EASY;
            QuoridorAI ai = new QuoridorAI(diff);
            Object move   = ai.getBestMove(gameState, gameState.getCurrentPlayer());

            // Replaced pattern matching with standard instanceof and casting
            if (move instanceof Position) {
                Position pos = (Position) move;
                gameState.movePawn(pos);
            } else if (move instanceof Wall) {
                Wall wall = (Wall) move;
                gameState.placeWall(wall);
            }

            aiThinking = false;
            postMoveUpdate();
        });
        pause.play();
    }

    // ── UI controls ─────────────────────────────────────────────────────────

    @FXML
    private void onToggleWallMode(ActionEvent event) {
        placingWall = btnWallMode.isSelected();

        if (placingWall) {
            btnWallMode.setText("✦  Move Pawn");
            btnWallMode.setStyle(btnWallMode.getStyle().replace("#7F8C8D", "#E67E22"));
            lblMode.setText("Mode: Place Wall  (" + orientationLabel() + ")");
            validMoves = List.of();
        } else {
            btnWallMode.setText("🧱  Place Wall");
            btnWallMode.setStyle(btnWallMode.getStyle().replace("#E67E22", "#7F8C8D"));
            lblMode.setText("Mode: Move Pawn");
            previewWall = null;
            refreshValidMoves();
        }
        drawBoard();
    }

    @FXML
    private void onToggleOrientation(ActionEvent event) {
        if (wallOrientation == WallOrientation.HORIZONTAL) {
            wallOrientation = WallOrientation.VERTICAL;
            btnOrientation.setText("↕  Vertical");
        } else {
            wallOrientation = WallOrientation.HORIZONTAL;
            btnOrientation.setText("↔  Horizontal");
        }
        if (placingWall) lblMode.setText("Mode: Place Wall  (" + orientationLabel() + ")");
    }

    @FXML
    private void onReset(ActionEvent event) {
        gameState = new GameState();
        resetUIState();
        refreshValidMoves();
        drawBoard();
        updateStatus();
    }

    @FXML
    private void onBackToMenu(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("MainMenu.fxml"));
            Stage stage = (Stage) gameCanvas.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Quoridor");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Status labels ────────────────────────────────────────────────────────

    private void updateStatus() {
        lblP1Walls.setText(String.valueOf(gameState.getPlayer1().getWallsRemaining()));
        lblP2Walls.setText(String.valueOf(gameState.getPlayer2().getWallsRemaining()));

        if (!gameState.isGameOver()) {
            int id = gameState.getCurrentPlayerId();
            boolean isAiPlayer = (gameMode != GameMode.HUMAN_VS_HUMAN) && id == 2;
            String name = isAiPlayer ? "Computer" : "Player " + id;
            lblTurn.setText(name + "'s Turn");
            lblTurn.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: "
                    + (id == 1 ? "#E74C3C" : "#2980B9") + ";");
        }
    }

    private void showWinner() {
        int id = gameState.getWinnerId();
        String name = (gameMode != GameMode.HUMAN_VS_HUMAN && id == 2)
                ? "Computer" : "Player " + id;
        lblTurn.setText("🎉  " + name + " Wins!");
        lblTurn.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #F0C040;");
        lblMode.setText("Press 'New Game' to play again.");
        lblMessage.setText("");
        validMoves = List.of();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Pixel x of the left edge of column c. */
    private double cellX(int col) { return PADDING + col * STEP; }

    /** Pixel y of the top edge of row r. */
    private double cellY(int row) { return PADDING + row * STEP; }

    private String orientationLabel() {
        return wallOrientation == WallOrientation.HORIZONTAL ? "Horizontal" : "Vertical";
    }

    private void resetUIState() {
        placingWall     = false;
        previewWall     = null;
        aiThinking      = false;
        wallOrientation = WallOrientation.HORIZONTAL;
        validMoves      = List.of();

        btnWallMode.setSelected(false);
        btnWallMode.setText("🧱  Place Wall");
        btnOrientation.setSelected(false);
        btnOrientation.setText("↔  Horizontal");
        lblMode.setText("Mode: Move Pawn");
        lblMessage.setText("");
    }
}