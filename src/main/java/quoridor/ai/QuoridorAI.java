package quoridor.ai;

import quoridor.logic.GameState;
import quoridor.logic.PathFinder;
import quoridor.model.*;

import java.util.*;

/**
 * AI opponent for Quoridor – Easy / Medium / Hard.
 *
 * EASY   – pure greedy BFS walk, never places walls.
 * MEDIUM – minimax depth-2, places walls that block the human's shortest path.
 * HARD   – minimax depth-3, smarter wall selection + richer evaluation.
 */
public class QuoridorAI {

    public enum Difficulty { EASY, MEDIUM, HARD }

    private static final int INF             = 1_000_000;
    private static final int WIN_SCORE       = 100_000;
    private static final int NO_PATH_PENALTY =  50_000;

    private static final int MAX_WALLS_MEDIUM = 10;
    private static final int MAX_WALLS_HARD   = 18;

    private final Difficulty difficulty;
    private final int        maxDepth;

    // Track AI's last two positions to detect and penalise oscillation
    private Position lastPosition     = null;
    private Position prevLastPosition = null;

    public QuoridorAI(Difficulty difficulty) {
        this.difficulty = difficulty;
        switch (difficulty) {
            case MEDIUM: this.maxDepth = 2; break;
            case HARD:   this.maxDepth = 3; break;
            default:     this.maxDepth = 1; break;
        }
    }

    // =========================================================================
    // Public entry point
    // =========================================================================

    public Object getBestMove(GameState gameState, Player aiPlayer) {
        Object move;
        switch (difficulty) {
            case EASY:   move = easyMove(gameState, aiPlayer);                      break;
            case MEDIUM: move = minimaxRoot(gameState, aiPlayer, MAX_WALLS_MEDIUM); break;
            case HARD:   move = minimaxRoot(gameState, aiPlayer, MAX_WALLS_HARD);   break;
            default:     move = easyMove(gameState, aiPlayer);                      break;
        }
        // Record history so next call can penalise returning here
        prevLastPosition = lastPosition;
        lastPosition     = aiPlayer.getPosition();
        return move;
    }

    // =========================================================================
    // EASY – greedy BFS walk
    // =========================================================================

    private Object easyMove(GameState gameState, Player aiPlayer) {
        List<Position> validMoves = gameState.getValidMoves(aiPlayer);
        if (validMoves.isEmpty()) return null;

        Board board   = gameState.getBoard();
        int   goalRow = aiPlayer.getGoalRow();

        Position bestPos  = validMoves.get(0);
        int      bestDist = INF;

        for (Position pos : validMoves) {
            int dist = PathFinder.shortestPathLength(board, pos, goalRow);
            if (dist >= 0 && dist < bestDist) {
                bestDist = dist;
                bestPos  = pos;
            }
        }
        return bestPos;
    }

    // =========================================================================
    // MEDIUM / HARD – minimax root
    // =========================================================================

    private Object minimaxRoot(GameState gameState, Player aiPlayer, int maxWalls) {
        List<Object> moves = generateMoves(gameState, aiPlayer, maxWalls);

        Object bestMove  = null;
        int    bestScore = -INF;

        Board board   = gameState.getBoard();
        int   goalRow = aiPlayer.getGoalRow();
        int   curDist = PathFinder.shortestPathLength(board, aiPlayer.getPosition(), goalRow);

        for (Object move : moves) {
            GameState next = applyMove(gameState, move);
            if (next == null) continue;

            int score = minimax(next, maxDepth - 1, -INF, INF, false, aiPlayer, maxWalls);

            // Tiebreak: prefer moves that actually reduce our distance to goal
            if (move instanceof Position) {
                int moveDist = PathFinder.shortestPathLength(board, (Position) move, goalRow);
                if (moveDist < curDist)  score += 5;   // small forward-progress bonus
                if (moveDist > curDist)  score -= 15;  // penalise moving backward
                // Also penalise returning to the position we were just at
                if (lastPosition != null && ((Position) move).equals(lastPosition))  score -= 50;
                if (prevLastPosition != null && ((Position) move).equals(prevLastPosition)) score -= 70;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMove  = move;
            }
        }

        return bestMove != null ? bestMove : easyMove(gameState, aiPlayer);
    }

    // =========================================================================
    // Minimax with alpha-beta pruning
    // =========================================================================

    private int minimax(GameState state, int depth, int alpha, int beta,
                        boolean maximising, Player aiPlayer, int maxWalls) {

        if (state.isGameOver()) {
            return state.getWinnerId() == aiPlayer.getId() ? WIN_SCORE : -WIN_SCORE;
        }
        if (depth == 0) {
            return evaluate(state, aiPlayer);
        }

        Player       current = state.getCurrentPlayer();
        List<Object> moves   = generateMoves(state, current, maxWalls);

        if (maximising) {
            int best = -INF;
            for (Object move : moves) {
                GameState next = applyMove(state, move);
                if (next == null) continue;
                int eval = minimax(next, depth - 1, alpha, beta, false, aiPlayer, maxWalls);
                if (eval > best)  best  = eval;
                if (eval > alpha) alpha = eval;
                if (beta <= alpha) break;
            }
            return best == -INF ? evaluate(state, aiPlayer) : best;
        } else {
            int best = INF;
            for (Object move : moves) {
                GameState next = applyMove(state, move);
                if (next == null) continue;
                int eval = minimax(next, depth - 1, alpha, beta, true, aiPlayer, maxWalls);
                if (eval < best)  best = eval;
                if (eval < beta)  beta = eval;
                if (beta <= alpha) break;
            }
            return best == INF ? evaluate(state, aiPlayer) : best;
        }
    }

    // =========================================================================
    // Evaluation
    // =========================================================================

    /**
     * Higher = better for AI.
     *
     * The key insight that fixes the oscillation bug:
     *   We scale the AI's own distance with a LARGER weight than the opponent's.
     *   This means "move myself forward" always beats "don't move" even if the
     *   opponent also moves forward by the same amount.
     *
     *   score = oppDist * 8  -  aiDist * 12
     *
     * Both terms push the AI to move forward AND slow the opponent, but the AI's
     * own progress is weighted more heavily so it never chooses to stand still or
     * step backward when a forward move is available.
     */
    private int evaluate(GameState state, Player aiPlayer) {
        Player opponent = state.getOpponent(aiPlayer);
        Board  board    = state.getBoard();

        int aiDist  = PathFinder.shortestPathLength(board,
                          aiPlayer.getPosition(),  aiPlayer.getGoalRow());
        int oppDist = PathFinder.shortestPathLength(board,
                          opponent.getPosition(), opponent.getGoalRow());

        if (aiDist  < 0) aiDist  = NO_PATH_PENALTY;
        if (oppDist < 0) oppDist = NO_PATH_PENALTY;

        // Asymmetric: forward progress weighted more than blocking
        int score = oppDist * 8 - aiDist * 12;

        // ── Repetition penalty ───────────────────────────────────────────────
        // If the AI is considering a move that lands on where it just came from,
        // that is almost certainly an oscillation. Penalise it hard.
        Position aiPos = aiPlayer.getPosition();
        if (lastPosition != null && aiPos.equals(lastPosition)) {
            score -= 40;   // strong penalty for returning to last position
        }
        if (prevLastPosition != null && aiPos.equals(prevLastPosition)) {
            score -= 60;   // even stronger for the position before that
        }

        if (difficulty == Difficulty.HARD) {
            score += (aiPlayer.getWallsRemaining() - opponent.getWallsRemaining()) * 3;
        }

        return score;
    }

    // =========================================================================
    // Move generation
    // =========================================================================

    /**
     * Generates candidate moves for whoever is the current player in {@code state}.
     *
     * Pawn moves are always included.
     * Wall moves are only generated when:
     *   - the current player has walls remaining
     *   - we generate walls that block the OPPONENT's path (i.e. the player
     *     whose turn it is NOT), which is the tactically correct thing to do.
     */
    private List<Object> generateMoves(GameState state, Player player, int maxWalls) {
        List<Object> moves = new ArrayList<>();

        // ── Pawn moves, sorted best-first (shortest remaining distance first) ──
        List<Position> pawnMoves = new ArrayList<>(state.getValidMoves(player));
        Board board   = state.getBoard();
        int   goalRow = player.getGoalRow();
        pawnMoves.sort(Comparator.comparingInt(
                p -> PathFinder.shortestPathLength(board, p, goalRow)));
        moves.addAll(pawnMoves);

        // ── Wall moves ──────────────────────────────────────────────────────────
        if (player.getWallsRemaining() > 0) {
            // Always generate walls that target the OPPONENT's path
            Player opponent = state.getOpponent(player);
            List<Wall> walls = generateBlockingWalls(state, player, opponent, maxWalls);
            moves.addAll(walls);
        }

        return moves;
    }

    // =========================================================================
    // Smart wall generation – blocks the opponent's BFS path
    // =========================================================================

    /**
     * Generates wall candidates that cut across {@code target}'s BFS shortest
     * path to their goal.  Only walls that are actually valid (no overlap,
     * path still exists for both players) are returned.
     *
     * For each consecutive step (from → to) on the opponent's path we add the
     * two walls that would block that specific step.
     */
    private List<Wall> generateBlockingWalls(GameState state, Player placer,
                                              Player target, int maxWalls) {
        Board board   = state.getBoard();
        int   goalRow = target.getGoalRow();

        List<Position> path = bfsPath(board, target.getPosition(), goalRow);

        Set<String> seen  = new HashSet<>();
        List<Wall>  walls = new ArrayList<>();

        for (int i = 0; i + 1 < path.size() && walls.size() < maxWalls; i++) {
            Position from = path.get(i);
            Position to   = path.get(i + 1);

            int dr = to.row - from.row;
            int dc = to.col - from.col;

            // The two wall anchors that would block this exact step
            if (dr == 1) {
                // Moving down: horizontal wall at the bottom edge of `from`
                tryAdd(new Wall(from.row, from.col - 1, WallOrientation.HORIZONTAL), placer, state, seen, walls, maxWalls);
                tryAdd(new Wall(from.row, from.col,     WallOrientation.HORIZONTAL), placer, state, seen, walls, maxWalls);
            } else if (dr == -1) {
                // Moving up: horizontal wall at the bottom edge of `to`
                tryAdd(new Wall(to.row, to.col - 1, WallOrientation.HORIZONTAL), placer, state, seen, walls, maxWalls);
                tryAdd(new Wall(to.row, to.col,     WallOrientation.HORIZONTAL), placer, state, seen, walls, maxWalls);
            } else if (dc == 1) {
                // Moving right: vertical wall at the right edge of `from`
                tryAdd(new Wall(from.row - 1, from.col, WallOrientation.VERTICAL), placer, state, seen, walls, maxWalls);
                tryAdd(new Wall(from.row,     from.col, WallOrientation.VERTICAL), placer, state, seen, walls, maxWalls);
            } else if (dc == -1) {
                // Moving left: vertical wall at the right edge of `to`
                tryAdd(new Wall(to.row - 1, to.col, WallOrientation.VERTICAL), placer, state, seen, walls, maxWalls);
                tryAdd(new Wall(to.row,     to.col, WallOrientation.VERTICAL), placer, state, seen, walls, maxWalls);
            }
        }

        // HARD: also try walls that extend existing wall segments
        if (difficulty == Difficulty.HARD && walls.size() < maxWalls) {
            for (int r = 0; r < Board.WALL_SLOTS && walls.size() < maxWalls; r++) {
                for (int c = 0; c < Board.WALL_SLOTS && walls.size() < maxWalls; c++) {
                    if (board.isHorizontalWall(r, c)) {
                        tryAdd(new Wall(r, c - 1, WallOrientation.HORIZONTAL), placer, state, seen, walls, maxWalls);
                        tryAdd(new Wall(r, c + 1, WallOrientation.HORIZONTAL), placer, state, seen, walls, maxWalls);
                    }
                    if (board.isVerticalWall(r, c)) {
                        tryAdd(new Wall(r - 1, c, WallOrientation.VERTICAL), placer, state, seen, walls, maxWalls);
                        tryAdd(new Wall(r + 1, c, WallOrientation.VERTICAL), placer, state, seen, walls, maxWalls);
                    }
                }
            }
        }

        return walls;
    }

    private void tryAdd(Wall wall, Player placer, GameState state,
                        Set<String> seen, List<Wall> result, int maxWalls) {
        if (result.size() >= maxWalls || wall == null) return;
        String key = wall.row + "," + wall.col + "," + wall.orientation;
        if (seen.add(key) && state.getValidator().isValidWallPlacement(wall, placer)) {
            result.add(wall);
        }
    }

    // =========================================================================
    // BFS path reconstruction
    // =========================================================================

    private List<Position> bfsPath(Board board, Position start, int goalRow) {
        if (start.row == goalRow) return Collections.singletonList(start);

        int size = Board.SIZE;
        int[][] prev        = new int[size][size];
        boolean[][] visited = new boolean[size][size];
        for (int[] row : prev) Arrays.fill(row, -1);

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{ start.row, start.col });
        visited[start.row][start.col] = true;

        int[] goal = null;

        outer:
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int r = cur[0], c = cur[1];
            for (int[] dir : new int[][]{ {-1,0},{1,0},{0,-1},{0,1} }) {
                int nr = r + dir[0], nc = c + dir[1];
                if (nr < 0 || nr >= size || nc < 0 || nc >= size) continue;
                if (visited[nr][nc]) continue;
                if (PathFinder.isBlocked(board, r, c, nr, nc))    continue;
                visited[nr][nc] = true;
                prev[nr][nc]    = r * size + c;
                queue.add(new int[]{ nr, nc });
                if (nr == goalRow) { goal = new int[]{ nr, nc }; break outer; }
            }
        }

        if (goal == null) return Collections.emptyList();

        LinkedList<Position> path = new LinkedList<>();
        int r = goal[0], c = goal[1];
        while (!(r == start.row && c == start.col)) {
            path.addFirst(new Position(r, c));
            int p = prev[r][c];
            r = p / size;
            c = p % size;
        }
        path.addFirst(start);
        return path;
    }

    // =========================================================================
    // State simulation
    // =========================================================================

    private GameState applyMove(GameState original, Object move) {
        GameState clone = cloneState(original);
        boolean ok;
        if (move instanceof Position) {
            ok = clone.movePawn((Position) move);
        } else if (move instanceof Wall) {
            ok = clone.placeWall((Wall) move);
        } else {
            return null;
        }
        return ok ? clone : null;
    }

    private GameState cloneState(GameState original) {
        GameState clone = new GameState();

        Board origBoard  = original.getBoard();
        Board cloneBoard = clone.getBoard();
        for (int r = 0; r < Board.WALL_SLOTS; r++) {
            for (int c = 0; c < Board.WALL_SLOTS; c++) {
                if (origBoard.isHorizontalWall(r, c))
                    cloneBoard.placeWall(new Wall(r, c, WallOrientation.HORIZONTAL));
                if (origBoard.isVerticalWall(r, c))
                    cloneBoard.placeWall(new Wall(r, c, WallOrientation.VERTICAL));
            }
        }

        copyPlayer(original.getPlayer1(), clone.getPlayer1());
        copyPlayer(original.getPlayer2(), clone.getPlayer2());

        while (clone.getCurrentPlayerId() != original.getCurrentPlayerId()) {
            clone.forceSwitchTurn();
        }
        return clone;
    }

    private void copyPlayer(Player src, Player dst) {
        dst.setPosition(src.getPosition());
        int wallsUsed = Player.TOTAL_WALLS - src.getWallsRemaining();
        for (int i = 0; i < wallsUsed; i++) dst.useWall();
    }
}
