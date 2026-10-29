package com.hex.kagune.control;

/**
 * Pure geometry of the server-side combo hit volumes (no Minecraft types, so every style is unit-tested).
 * Offsets are measured from the attacker's eye to the candidate's body centre, in blocks.
 */
public final class HitGeometry {
    /** Anything this close horizontally is always inside a sweep (point-blank). */
    public static final double POINT_BLANK = 1.2;

    private HitGeometry() {
    }

    /**
     * @param ox,oy,oz   eye -> candidate centre
     * @param dx,dy,dz   normalized strike direction (yaw-deviated aim)
     * @param range      stage reach in blocks
     * @param halfWidth  half of the candidate's bounding-box width
     * @param halfHeight half of the candidate's bounding-box height
     * @param sx,sy,sz   eye -> impact point: the aimed target centre, or a point ahead (SLAM centre, SPIN minimum radius)
     */
    public static boolean inside(HitShape shape, float shapeParam, double ox, double oy, double oz, double dx, double dy, double dz,
                                 double range, double halfWidth, double halfHeight, double sx, double sy, double sz) {
        double verticalReach = 2.5 + halfHeight;
        switch (shape) {
            case SWEEP: {
                double horizontal = Math.sqrt(ox * ox + oz * oz);
                if (horizontal > range + halfWidth || Math.abs(oy) > verticalReach) {
                    return false;
                }
                if (horizontal < POINT_BLANK) {
                    return true;
                }
                double dirLength = Math.sqrt(dx * dx + dz * dz);
                if (dirLength < 1.0E-6) {
                    return false;
                }
                double cosine = (ox * dx + oz * dz) / (horizontal * dirLength);
                double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cosine))));
                return angle <= shapeParam * 0.5 + Math.toDegrees(Math.atan2(halfWidth, horizontal));
            }
            case PIERCE: {
                double along = ox * dx + oy * dy + oz * dz;
                if (along < -0.5 || along > range + halfWidth) {
                    return false;
                }
                double px = ox - dx * along;
                double py = oy - dy * along;
                double pz = oz - dz * along;
                return Math.sqrt(px * px + py * py + pz * pz) <= shapeParam + halfWidth;
            }
            case SLAM: {
                double ex = ox - sx;
                double ey = oy - sy;
                double ez = oz - sz;
                double reach = shapeParam + halfWidth;
                return ex * ex + ey * ey + ez * ez <= reach * reach;
            }
            case SPIN: {
                // The orbit passes through the aimed point, so the radius always reaches it (but never beyond reach).
                double aimed = Math.sqrt(sx * sx + sz * sz);
                double radius = Math.min(range, Math.max(range * shapeParam, aimed));
                double horizontal = Math.sqrt(ox * ox + oz * oz);
                return horizontal <= radius + halfWidth && Math.abs(oy) <= verticalReach;
            }
            default:
                return false;
        }
    }
}
