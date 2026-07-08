package top.steve3184.webmc.chunk;

import net.minecraft.world.level.ChunkPos;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Intelligent chunk prefetcher that uses player movement prediction
 * to prioritize loading chunks in the direction of travel.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Tracks player velocity and direction</li>
 *   <li>Predicts next chunks to load based on movement</li>
 *   <li>Prioritizes chunks in movement direction (70% forward, 30% backward)</li>
 *   <li>Implements 2-chunk look-ahead in movement direction</li>
 *   <li>Configurable directional bias ratios</li>
 * </ul>
 */
public final class ChunkPrefetcher {

    // ============================================================
    // Configuration constants
    // ============================================================

    /** Velocity smoothing factor (0-1, higher = more responsive) */
    private static final double VELOCITY_SMOOTHING = 0.3;

    /** Direction change threshold in radians to trigger new prefetch */
    private static final double DIRECTION_CHANGE_THRESHOLD = 0.5;

    /** Look-ahead distance in chunks in movement direction */
    private static final int LOOK_AHEAD_DISTANCE = 2;

    /** Forward direction bias ratio (70% of prefetch budget) */
    private static final double FORWARD_BIAS = 0.7;

    /** Backward direction bias ratio (30% of prefetch budget) */
    private static final double BACKWARD_BIAS = 0.3;

    /** Maximum recent loads to track for deduplication */
    private static final int MAX_RECENT_LOADS = 64;

    /** Minimum velocity to consider player "moving" (blocks per frame at 20fps) */
    private static final double MOVEMENT_THRESHOLD = 0.05;

    // ============================================================
    // Player state tracking
    // ============================================================

    /** Current player position (in block coordinates) */
    private double playerX;
    private double playerZ;

    /** Previous player position for velocity calculation */
    private double prevPlayerX;
    private double prevPlayerZ;

    /** Smoothed velocity (blocks per second) */
    private double velocityX;
    private double velocityZ;

    /** Raw velocity before smoothing */
    private double rawVelocityX;
    private double rawVelocityZ;

    /** Movement direction in radians (-PI to PI) */
    private double movementDirection;

    /** Current player chunk position */
    private int currentChunkX;
    private int currentChunkZ;

    /** Normalized velocity direction vector */
    private double dirX;
    private double dirZ;

    // ============================================================
    // Rendering and caching state
    // ============================================================

    /** Render distance for priority calculation */
    private int renderDistance = 8;

    /** Forward direction bias multiplier */
    private double forwardBiasMultiplier = 1.0;

    /** Backward direction bias multiplier */
    private double backwardBiasMultiplier = 1.0;

    /** Cache of recently loaded chunks for deduplication */
    private final Set<ChunkPos> recentLoads = new HashSet<>();
    private final List<ChunkPos> recentLoadsOrder = new ArrayList<>();

    public ChunkPrefetcher() {
        reset();
    }

    // ============================================================
    // Position and velocity update methods
    // ============================================================

    /**
     * Update player position and calculate velocity/direction.
     * This is the main entry point for movement tracking.
     *
     * @param x Current player X position (block coordinates)
     * @param z Current player Z position (block coordinates)
     */
    public void updatePlayerPosition(double x, double z) {
        prevPlayerX = playerX;
        prevPlayerZ = playerZ;
        playerX = x;
        playerZ = z;

        // Calculate raw velocity (delta per update)
        double dx = playerX - prevPlayerX;
        double dz = playerZ - prevPlayerZ;

        rawVelocityX = dx;
        rawVelocityZ = dz;

        // Apply exponential smoothing
        velocityX = velocityX * (1 - VELOCITY_SMOOTHING) + rawVelocityX * VELOCITY_SMOOTHING;
        velocityZ = velocityZ * (1 - VELOCITY_SMOOTHING) + rawVelocityZ * VELOCITY_SMOOTHING;

        // Update movement direction if moving significantly
        if (isMoving()) {
            updateMovementDirection();
            updateDirectionalBias();
        }

        // Update current chunk position
        int newChunkX = ChunkPos.getX(playerX);
        int newChunkZ = ChunkPos.getZ(playerZ);

        if (newChunkX != currentChunkX || newChunkZ != currentChunkZ) {
            currentChunkX = newChunkX;
            currentChunkZ = newChunkZ;
        }
    }

    /**
     * Update movement direction based on velocity vector.
     */
    private void updateMovementDirection() {
        double newDirection = Math.atan2(velocityZ, velocityX);

        if (movementDirection == 0 && rawVelocityX == 0 && rawVelocityZ == 0) {
            // Initial state
            movementDirection = newDirection;
        } else {
            // Check for significant direction change
            double delta = normalizeAngle(newDirection - movementDirection);
            if (Math.abs(delta) > DIRECTION_CHANGE_THRESHOLD) {
                movementDirection = newDirection;
                log("Direction changed to " + Math.toDegrees(movementDirection) + " degrees");
            }
        }

        // Update normalized direction vector
        double mag = getVelocityMagnitude();
        if (mag > MOVEMENT_THRESHOLD) {
            dirX = velocityX / mag;
            dirZ = velocityZ / mag;
        }
    }

    /**
     * Update directional bias multipliers based on movement direction.
     * Implements 70% forward, 30% backward bias.
     */
    private void updateDirectionalBias() {
        // Apply directional bias based on velocity components
        // Moving in positive X: bias positive X chunks higher
        // Moving in positive Z: bias positive Z chunks higher
        double absDirX = Math.abs(dirX);
        double absDirZ = Math.abs(dirZ);

        // Calculate bias based on direction alignment
        // Forward bias applies to chunks in the same direction as movement
        // Backward bias applies to chunks behind the player
        if (absDirX > absDirZ) {
            // Primarily horizontal movement
            if (dirX > 0) {
                // Moving in +X direction
                forwardBiasMultiplier = 1.0 + FORWARD_BIAS;
                backwardBiasMultiplier = 1.0 + BACKWARD_BIAS;
            } else {
                // Moving in -X direction
                forwardBiasMultiplier = 1.0 + FORWARD_BIAS;
                backwardBiasMultiplier = 1.0 + BACKWARD_BIAS;
            }
        } else {
            // Primarily vertical movement
            if (dirZ > 0) {
                // Moving in +Z direction
                forwardBiasMultiplier = 1.0 + FORWARD_BIAS;
                backwardBiasMultiplier = 1.0 + BACKWARD_BIAS;
            } else {
                // Moving in -Z direction
                forwardBiasMultiplier = 1.0 + FORWARD_BIAS;
                backwardBiasMultiplier = 1.0 + BACKWARD_BIAS;
            }
        }
    }

    /**
     * Normalize angle to [-PI, PI].
     */
    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    // ============================================================
    // Query methods
    // ============================================================

    /**
     * Get prioritized list of chunks to prefetch based on movement direction.
     * Implements 70% forward, 30% backward bias with 2-chunk look-ahead.
     *
     * @param centerChunkX Current chunk X
     * @param centerChunkZ Current chunk Z
     * @return Prioritized list of chunk positions
     */
    public List<ChunkPos> getMovementBasedPrefetch(int centerChunkX, int centerChunkZ) {
        return getDirectionalChunks(centerChunkX, centerChunkZ, renderDistance,
                                    FORWARD_BIAS, BACKWARD_BIAS);
    }

    /**
     * Get chunks with configurable directional bias.
     * Prioritizes chunks in the forward direction based on movement.
     *
     * @param centerChunkX Current chunk X
     * @param centerChunkZ Current chunk Z
     * @param radius Render distance radius
     * @param forwardRatio Ratio of budget for forward chunks (e.g., 0.7)
     * @param backwardRatio Ratio of budget for backward chunks (e.g., 0.3)
     * @return Prioritized list of chunk positions
     */
    public List<ChunkPos> getDirectionalChunks(int centerChunkX, int centerChunkZ,
                                                int radius,
                                                double forwardRatio,
                                                double backwardRatio) {
        List<ChunkPos> forwardChunks = new ArrayList<>();
        List<ChunkPos> backwardChunks = new ArrayList<>();
        List<ChunkPos> neutralChunks = new ArrayList<>();

        // Determine movement direction signs
        int dirSignX = (int) Math.signum(dirX);
        int dirSignZ = (int) Math.signum(dirZ);

        // Calculate look-ahead range based on velocity
        int lookAhead = isMoving() ? LOOK_AHEAD_DISTANCE : 1;

        // Generate chunks in radius
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Skip current chunk
                if (dx == 0 && dz == 0) continue;

                ChunkPos pos = new ChunkPos(centerChunkX + dx, centerChunkZ + dz);

                // Skip if recently loaded
                if (recentLoads.contains(pos)) continue;

                // Classify chunk by direction
                DirectionClass dirClass = classifyChunkDirection(dx, dz, dirSignX, dirSignZ);

                switch (dirClass) {
                    case FORWARD:
                        // Prioritize forward chunks within look-ahead
                        double distance = Math.sqrt(dx * dx + dz * dz);
                        if (distance <= lookAhead) {
                            forwardChunks.add(0, pos); // Higher priority closer chunks
                        } else {
                            forwardChunks.add(pos);
                        }
                        break;
                    case BACKWARD:
                        backwardChunks.add(pos);
                        break;
                    case LATERAL:
                    case NEUTRAL:
                    default:
                        neutralChunks.add(pos);
                        break;
                }
            }
        }

        // Combine chunks with 70/30 forward/backward bias
        List<ChunkPos> result = new ArrayList<>();

        // Add forward chunks (70% priority)
        int forwardBudget = (int) (forwardChunks.size() * forwardRatio);
        for (int i = 0; i < Math.min(forwardBudget, forwardChunks.size()); i++) {
            result.add(forwardChunks.get(i));
        }

        // Add remaining forward chunks if budget allows
        for (int i = forwardBudget; i < forwardChunks.size() && result.size() < 16; i++) {
            result.add(forwardChunks.get(i));
        }

        // Add backward chunks (30% secondary priority)
        int backwardBudget = (int) (backwardChunks.size() * backwardRatio);
        for (int i = 0; i < Math.min(backwardBudget, backwardChunks.size()); i++) {
            if (result.size() < 16) result.add(backwardChunks.get(i));
        }

        // Fill remaining with lateral/neutral chunks
        for (ChunkPos pos : neutralChunks) {
            if (result.size() >= 16) break;
            result.add(pos);
        }

        return result;
    }

    /**
     * Classify a chunk's direction relative to player movement.
     */
    private DirectionClass classifyChunkDirection(int dx, int dz, int dirSignX, int dirSignZ) {
        if (!isMoving()) {
            return DirectionClass.NEUTRAL;
        }

        // Check if chunk is in the forward direction (same sign as movement)
        boolean inForwardX = (dx * dirSignX) > 0;
        boolean inForwardZ = (dz * dirSignZ) > 0;

        // Check if chunk is in the backward direction (opposite sign)
        boolean inBackwardX = (dx * dirSignX) < 0;
        boolean inBackwardZ = (dz * dirSignZ) < 0;

        // At least one axis in forward direction, none in backward
        if ((inForwardX || inForwardZ) && !(inBackwardX && inBackwardZ)) {
            return DirectionClass.FORWARD;
        }

        // At least one axis in backward direction, none in forward
        if ((inBackwardX || inBackwardZ) && !(inForwardX && inForwardZ)) {
            return DirectionClass.BACKWARD;
        }

        // Diagonal movement or perpendicular
        return DirectionClass.LATERAL;
    }

    /**
     * Get prioritized list of chunks with look-ahead in movement direction.
     * This is the main method for predictive prefetching.
     *
     * @param centerChunkX Current chunk X
     * @param centerChunkZ Current chunk Z
     * @param lookAhead Number of chunks to look ahead
     * @return Prioritized list of chunk positions
     */
    public List<ChunkPos> getLookAheadChunks(int centerChunkX, int centerChunkZ, int lookAhead) {
        List<ChunkPos> result = new ArrayList<>();

        if (!isMoving()) {
            // Not moving, return standard radial chunks
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    ChunkPos pos = new ChunkPos(centerChunkX + dx, centerChunkZ + dz);
                    if (!recentLoads.contains(pos)) {
                        result.add(pos);
                    }
                }
            }
            return result;
        }

        int dirSignX = (int) Math.signum(dirX);
        int dirSignZ = (int) Math.signum(dirZ);

        // Generate chunks in expanding rings
        for (int d = 1; d <= lookAhead + 1; d++) {
            List<ChunkPos> ringForward = new ArrayList<>();
            List<ChunkPos> ringBackward = new ArrayList<>();

            for (int dx = -d; dx <= d; dx++) {
                for (int dz = -d; dz <= d; dz++) {
                    // Only include edge of current ring
                    if (Math.abs(dx) != d && Math.abs(dz) != d) continue;
                    if (dx == 0 && dz == 0) continue;

                    ChunkPos pos = new ChunkPos(centerChunkX + dx, centerChunkZ + dz);
                    if (recentLoads.contains(pos)) continue;

                    boolean inForwardX = (dx * dirSignX) > 0;
                    boolean inForwardZ = (dz * dirSignZ) > 0;

                    if (inForwardX || inForwardZ) {
                        ringForward.add(pos);
                    } else {
                        ringBackward.add(pos);
                    }
                }
            }

            // Add forward ring first (prioritized)
            result.addAll(ringForward);
            // Add backward ring (secondary priority)
            result.addAll(ringBackward);
        }

        return result;
    }

    /**
     * Get priority score for a chunk based on distance and direction.
     *
     * @param pos Chunk position
     * @param centerX Center chunk X
     * @param centerZ Center chunk Z
     * @return Priority score (higher is better)
     */
    public double calculatePriority(ChunkPos pos, int centerX, int centerZ) {
        int dx = pos.x - centerX;
        int dz = pos.z - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);

        // Base priority from distance (closer = higher)
        double priority = 10.0 / (distance + 1.0);

        // Apply directional bias
        if (isMoving()) {
            int dirSignX = (int) Math.signum(dirX);
            int dirSignZ = (int) Math.signum(dirZ);

            boolean inForwardX = (dx * dirSignX) > 0;
            boolean inForwardZ = (dz * dirSignZ) > 0;

            if (inForwardX || inForwardZ) {
                priority *= forwardBiasMultiplier;
            } else {
                priority *= backwardBiasMultiplier;
            }
        }

        // Reduce priority for recently loaded chunks
        if (recentLoads.contains(pos)) {
            priority *= 0.3;
        }

        // Reduce priority beyond look-ahead range
        if (distance > renderDistance + LOOK_AHEAD_DISTANCE) {
            priority *= 0.1;
        }

        return priority;
    }

    /**
     * Check if a chunk was recently loaded.
     */
    public boolean isRecentlyLoaded(ChunkPos pos) {
        return recentLoads.contains(pos);
    }

    /**
     * Mark a chunk as loaded.
     */
    public void onChunkLoaded(ChunkPos pos) {
        if (!recentLoads.contains(pos)) {
            recentLoads.add(pos);
            recentLoadsOrder.add(0, pos);

            // Maintain size limit
            if (recentLoads.size() > MAX_RECENT_LOADS) {
                ChunkPos removed = recentLoadsOrder.remove(recentLoadsOrder.size() - 1);
                recentLoads.remove(removed);
            }
        }
    }

    // ============================================================
    // Configuration and state accessors
    // ============================================================

    /**
     * Set render distance for priority calculation.
     */
    public void setRenderDistance(int distance) {
        this.renderDistance = Math.max(4, Math.min(16, distance));
    }

    /**
     * Get movement direction in radians.
     */
    public double getMovementDirection() {
        return movementDirection;
    }

    /**
     * Get velocity direction as normalized vector.
     * @return Array [dirX, dirZ] or [0, 0] if not moving
     */
    public double[] getVelocityDirection() {
        if (!isMoving()) {
            return new double[] { 0, 0 };
        }
        double mag = getVelocityMagnitude();
        return new double[] { velocityX / mag, velocityZ / mag };
    }

    /**
     * Get current velocity magnitude (blocks per second).
     */
    public double getVelocityMagnitude() {
        return Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
    }

    /**
     * Get raw (unsmoothed) velocity magnitude.
     */
    public double getRawVelocityMagnitude() {
        return Math.sqrt(rawVelocityX * rawVelocityX + rawVelocityZ * rawVelocityZ);
    }

    /**
     * Get current chunk position.
     */
    public ChunkPos getCurrentChunk() {
        return new ChunkPos(currentChunkX, currentChunkZ);
    }

    /**
     * Get current player position.
     * @return Array [x, z]
     */
    public double[] getPlayerPosition() {
        return new double[] { playerX, playerZ };
    }

    /**
     * Check if player is moving.
     */
    public boolean isMoving() {
        return getVelocityMagnitude() > MOVEMENT_THRESHOLD;
    }

    /**
     * Get the forward bias multiplier.
     */
    public double getForwardBiasMultiplier() {
        return forwardBiasMultiplier;
    }

    /**
     * Get the backward bias multiplier.
     */
    public double getBackwardBiasMultiplier() {
        return backwardBiasMultiplier;
    }

    /**
     * Get the configured look-ahead distance.
     */
    public int getLookAheadDistance() {
        return LOOK_AHEAD_DISTANCE;
    }

    // ============================================================
    // Utility methods
    // ============================================================

    /**
     * Reset all state.
     */
    public void reset() {
        playerX = playerZ = 0;
        prevPlayerX = prevPlayerZ = 0;
        velocityX = velocityZ = 0;
        rawVelocityX = rawVelocityZ = 0;
        movementDirection = 0;
        dirX = dirZ = 0;
        currentChunkX = currentChunkZ = 0;
        forwardBiasMultiplier = 1.0;
        backwardBiasMultiplier = 1.0;
        recentLoads.clear();
        recentLoadsOrder.clear();
    }

    /**
     * Get diagnostic info.
     */
    public String getStats() {
        return String.format(
            "ChunkPrefetcher[pos=%.1f,%.1f vel=%.3f dir=%.1f forwardBias=%.2f backwardBias=%.2f lookAhead=%d]",
            playerX, playerZ,
            getVelocityMagnitude(),
            Math.toDegrees(movementDirection),
            forwardBiasMultiplier,
            backwardBiasMultiplier,
            LOOK_AHEAD_DISTANCE
        );
    }

    @org.teavm.jso.JSBody(params = "msg", script = "console.log('[mc-web/chunk-prefetch] ' + msg);")
    private static native void log(String msg);

    // ============================================================
    // Inner types
    // ============================================================

    /**
     * Direction classification for chunk prioritization.
     */
    private enum DirectionClass {
        /** Chunk in the direction of player movement */
        FORWARD,
        /** Chunk opposite to direction of player movement */
        BACKWARD,
        /** Chunk perpendicular to movement direction */
        LATERAL,
        /** Chunk when player is stationary */
        NEUTRAL
    }
}
