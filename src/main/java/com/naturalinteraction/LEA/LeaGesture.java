package com.naturalinteraction.LEA;

public enum LeaGesture {
    NONE,
    LEFT,
    RIGHT,
    UP,
    DOWN,
    LEFT_UP,
    RIGHT_UP,
    LEFT_DOWN,
    RIGHT_DOWN;

    public boolean isHorizontal() {
        return this == LEFT || this == RIGHT
                || this == LEFT_UP || this == RIGHT_UP
                || this == LEFT_DOWN || this == RIGHT_DOWN;
    }

    public boolean isVertical() {
        return this == UP || this == DOWN
                || this == LEFT_UP || this == RIGHT_UP
                || this == LEFT_DOWN || this == RIGHT_DOWN;
    }

    public int directionX() {
        switch (this) {
            case LEFT:
            case LEFT_UP:
            case LEFT_DOWN:
                return -1;
            case RIGHT:
            case RIGHT_UP:
            case RIGHT_DOWN:
                return 1;
            default:
                return 0;
        }
    }

    public int directionY() {
        switch (this) {
            case UP:
            case LEFT_UP:
            case RIGHT_UP:
                return -1;
            case DOWN:
            case LEFT_DOWN:
            case RIGHT_DOWN:
                return 1;
            default:
                return 0;
        }
    }

    public static LeaGesture fromDirections(int dx, int dy) {
        int sx = Integer.signum(dx);
        int sy = Integer.signum(dy);
        if (sx < 0 && sy < 0) return LEFT_UP;
        if (sx > 0 && sy < 0) return RIGHT_UP;
        if (sx < 0 && sy > 0) return LEFT_DOWN;
        if (sx > 0 && sy > 0) return RIGHT_DOWN;
        if (sx < 0) return LEFT;
        if (sx > 0) return RIGHT;
        if (sy < 0) return UP;
        if (sy > 0) return DOWN;
        return NONE;
    }
}
