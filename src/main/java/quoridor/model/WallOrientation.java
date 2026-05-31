package quoridor.model;

/**
 * Orientation of a wall on the board.
 *
 * HORIZONTAL  – spans two cells left-to-right, blocking vertical movement.
 * VERTICAL    – spans two cells top-to-bottom, blocking horizontal movement.
 */
public enum WallOrientation {
    HORIZONTAL,
    VERTICAL
}
