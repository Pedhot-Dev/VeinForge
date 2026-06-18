package me.grish.veinforge.pathfinder.calculate;

public record PathfindingTelemetry(long startedAtMs, long completedAtMs, boolean success, String failureReason,
                                   double searchMs, double smoothingMs, int expandedNodes, int openSetPeak,
                                   int iterations, int pathLength, int smoothedPathLength, boolean directWalk) {

    public PathfindingTelemetry(
            long startedAtMs,
            long completedAtMs,
            boolean success,
            String failureReason,
            double searchMs,
            double smoothingMs,
            int expandedNodes,
            int openSetPeak,
            int iterations,
            int pathLength,
            int smoothedPathLength,
            boolean directWalk
    ) {
        this.startedAtMs = startedAtMs;
        this.completedAtMs = completedAtMs;
        this.success = success;
        this.failureReason = failureReason == null ? "" : failureReason;
        this.searchMs = searchMs;
        this.smoothingMs = smoothingMs;
        this.expandedNodes = expandedNodes;
        this.openSetPeak = openSetPeak;
        this.iterations = iterations;
        this.pathLength = pathLength;
        this.smoothedPathLength = smoothedPathLength;
        this.directWalk = directWalk;
    }

    public double getTotalMs() {
        return searchMs + smoothingMs;
    }
}
