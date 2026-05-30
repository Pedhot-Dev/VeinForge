package me.grish.veinforge.pathfinder.calculate;

public class PathfindingTelemetry {

   private final long startedAtMs;
   private final long completedAtMs;
   private final boolean success;
   private final String failureReason;
   private final double searchMs;
   private final double smoothingMs;
   private final int expandedNodes;
   private final int openSetPeak;
   private final int iterations;
   private final int pathLength;
   private final int smoothedPathLength;
   private final boolean directWalk;

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

   public long getStartedAtMs() {
      return startedAtMs;
   }

   public long getCompletedAtMs() {
      return completedAtMs;
   }

   public boolean isSuccess() {
      return success;
   }

   public String getFailureReason() {
      return failureReason;
   }

   public double getSearchMs() {
      return searchMs;
   }

   public double getSmoothingMs() {
      return smoothingMs;
   }

   public double getTotalMs() {
      return searchMs + smoothingMs;
   }

   public int getExpandedNodes() {
      return expandedNodes;
   }

   public int getOpenSetPeak() {
      return openSetPeak;
   }

   public int getIterations() {
      return iterations;
   }

   public int getPathLength() {
      return pathLength;
   }

   public int getSmoothedPathLength() {
      return smoothedPathLength;
   }

   public boolean isDirectWalk() {
      return directWalk;
   }
}
