package me.grish.veinforge.util.helper.graph;

public record GraphValidationResult(int nodeCount, int edgeCount, int duplicateEdges, int selfLoops,
                                    int danglingEdges) {

    public boolean hasViolations() {
        return duplicateEdges > 0 || selfLoops > 0 || danglingEdges > 0;
    }
}
