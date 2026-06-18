package me.grish.veinforge.pathfinder.calculate.path

import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import me.grish.veinforge.pathfinder.calculate.Path
import me.grish.veinforge.pathfinder.calculate.PathNode
import me.grish.veinforge.pathfinder.calculate.openset.BinaryHeapOpenSet
import me.grish.veinforge.pathfinder.goal.Goal
import me.grish.veinforge.pathfinder.movement.CalculationContext
import me.grish.veinforge.pathfinder.movement.MovementResult
import me.grish.veinforge.pathfinder.movement.Moves

import net.minecraft.core.BlockPos


class AStarPathFinder(val startX: Int, val startY: Int, val startZ: Int, val goal: Goal, val ctx: CalculationContext) {
    companion object {
        private const val MAX_ITERATIONS = 50_000
        private const val ITERATION_STEP = 1
    }

    data class SearchTelemetry(
        val iterations: Int = 0,
        val expandedNodes: Int = 0,
        val openSetPeak: Int = 0,
        val terminationReason: String = "not_started"
    )

    private val closedSet: Long2ObjectMap<PathNode> = Long2ObjectOpenHashMap()
    private var calculating = false
    private var iterations = 0
    @Volatile
    private var lastTelemetry = SearchTelemetry()

    fun getLastTelemetry(): SearchTelemetry {
        return lastTelemetry
    }

    fun calculatePath(): Path? {
        calculating = true
        iterations = 0
        closedSet.clear()
        lastTelemetry = SearchTelemetry(terminationReason = "running")
        val openSet = BinaryHeapOpenSet()
        val startNode = PathNode(startX, startY, startZ, goal)
        val res = MovementResult()
        val moves = Moves.entries.toTypedArray()
        var expandedNodes = 0
        var openSetPeak = 0
        startNode.costSoFar = 0.0
        startNode.totalCost = startNode.costToEnd
        openSet.add(startNode)
        openSetPeak = maxOf(openSetPeak, openSet.size)

        while (!openSet.isEmpty() && calculating && iterations < MAX_ITERATIONS) {
            iterations++

            if (iterations % ITERATION_STEP != 0) continue

            val currentNode = openSet.poll()
            expandedNodes++

            if (goal.isAtGoal(currentNode.x, currentNode.y, currentNode.z)) {
                calculating = false
                lastTelemetry = SearchTelemetry(
                    iterations = iterations,
                    expandedNodes = expandedNodes,
                    openSetPeak = openSetPeak,
                    terminationReason = "goal_reached"
                )
                return Path(startNode, currentNode, goal, ctx)
            }
            for (move in moves) {
                res.reset()
                move.calculate(ctx, currentNode.x, currentNode.y, currentNode.z, res)
                var cost = res.cost
                val isLoaded = isChunkLoaded(res.x, res.y, res.z)
                if (!isLoaded) {
                    cost = ctx.cost.INF_COST / 2 // Достаточно высокая стоимость, но путь возможен
                }

                if (cost >= ctx.cost.INF_COST) continue

                val neighbourNode = getNode(res.x, res.y, res.z, PathNode.longHash(res.x, res.y, res.z))
                val neighbourCostSoFar = currentNode.costSoFar + cost

                if (neighbourNode.costSoFar > neighbourCostSoFar) {
                    neighbourNode.parentNode = currentNode
                    neighbourNode.costSoFar = neighbourCostSoFar
                    neighbourNode.totalCost = neighbourCostSoFar + neighbourNode.costToEnd

                    if (neighbourNode.heapPosition == -1) {
                        openSet.add(neighbourNode)
                    } else {
                        openSet.relocate(neighbourNode)
                    }
                    if (openSet.size > openSetPeak) {
                        openSetPeak = openSet.size
                    }
                }
            }
        }
        val terminationReason = when {
            !calculating -> "stop_requested"
            iterations >= MAX_ITERATIONS -> "max_iterations"
            else -> "open_set_exhausted"
        }
        calculating = false
        lastTelemetry = SearchTelemetry(
            iterations = iterations,
            expandedNodes = expandedNodes,
            openSetPeak = openSetPeak,
            terminationReason = terminationReason
        )


        // Important: never return a partial/closest path here.
        // Callers treat any non-null Path as successful completion to the requested goal.
        return null
    }

    private fun isChunkLoaded(x: Int, y: Int, z: Int): Boolean {
        return ctx.world?.let { world ->
            val pos = BlockPos(x, y, z)
            world.isLoaded(pos) ||
                    world.isLoaded(pos.offset(1, 0, 0)) ||
                    world.isLoaded(pos.offset(-1, 0, 0)) ||
                    world.isLoaded(pos.offset(0, 0, 1)) ||
                    world.isLoaded(pos.offset(0, 0, -1))
        } ?: false
    }

    fun getNode(x: Int, y: Int, z: Int, hash: Long): PathNode {
        var n: PathNode? = closedSet.get(hash)
        if (n == null) {
            n = PathNode(x, y, z, goal)
            closedSet.put(hash, n)
        }
        return n
    }

    fun requestStop() {
        if (!calculating) return
        calculating = false
    }
}
