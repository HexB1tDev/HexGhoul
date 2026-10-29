package com.hex.kagune.control;

/** Server-side hit volume of a combo stage. Damage is only ever resolved on the server. */
public enum HitShape {
    /** Only the aimed target (the original swipe behaviour). */
    SINGLE,
    /** Horizontal arc in front of the user; {@code ComboStage#shapeParam} is the arc width in degrees. */
    SWEEP,
    /** Narrow line along the strike direction; {@code shapeParam} is the line radius in blocks. */
    PIERCE,
    /** Sphere around the impact point; {@code shapeParam} is the radius in blocks. */
    SLAM,
    /** Full circle around the user; {@code shapeParam} is the fraction of attack range used as radius. */
    SPIN
}
