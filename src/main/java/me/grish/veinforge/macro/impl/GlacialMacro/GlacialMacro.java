package me.grish.veinforge.macro.impl.GlacialMacro;

import akka.japi.Pair;
import lombok.Getter;
import lombok.Setter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.failsafe.impl.NameMentionFailsafe;
import me.grish.veinforge.feature.FeatureManager;
import me.grish.veinforge.feature.impl.BlockMiner.BlockMiner;
import me.grish.veinforge.handler.GraphHandler;
import me.grish.veinforge.macro.AbstractMacro;
import me.grish.veinforge.macro.impl.GlacialMacro.states.*;
import me.grish.veinforge.util.PlayerUtil;
import me.grish.veinforge.util.TablistUtil;
import me.grish.veinforge.util.helper.MineableBlock;
import me.grish.veinforge.util.helper.route.RouteWaypoint;

import java.util.*;

/**
 * GlacialMacro is responsible for managing the Glacial Macro functionality,
 * including state transitions, mining tasks, and interaction with the game world.
 */
public class GlacialMacro extends AbstractMacro {

    @Getter
    private static final GlacialMacro instance = new GlacialMacro();
    @Getter
    private final ArrayList<GlaciteVeins> typeToMine = new ArrayList<>();
    @Getter
    private final Map<Pair<GlaciteVeins, RouteWaypoint>, Long> previousVeins = new HashMap<>();
    @Setter
    @Getter
    private GlacialMacroState currentState;
    // Shared data for states
    @Getter
    @Setter
    private int miningSpeed = 0;
    @Setter
    @Getter
    private Pair<GlaciteVeins, RouteWaypoint> currentVein = null;
    @Getter
    @Setter
    private BlockMiner.PickaxeAbility pickaxeAbility = BlockMiner.PickaxeAbility.NONE;
    @Getter
    private int commissionCounter = 0;

    @Override
    public String getName() {
        return "Glacial Macro";
    }

    @Override
    public void onEnable() {
        typeToMine.clear();
        previousVeins.clear();
        currentVein = null;
        this.miningSpeed = 0;
        currentState = new StartingState();
        log("Glacial Macro enabled");
    }

    @Override
    public void onDisable() {
        if (currentState != null) {
            currentState.onEnd(this);
        }
        this.miningSpeed = 0;
        currentState = null;
        log("Glacial Macro disabled");
        FeatureManager.getInstance().disableAll();
    }

    @Override
    public void onPause() {
        FeatureManager.getInstance().pauseAll();
        log("Glacial Macro Paused");
    }

    @Override
    public void onResume() {
        FeatureManager.getInstance().resumeAll();
        log("Glacial Macro Resumed");
    }

    public void incrementCommissionCounter() {
        this.commissionCounter++;
    }


    @Override
    public void onTick() {
        if (!this.isEnabled()) {
            return;
        }

        if (NameMentionFailsafe.getInstance().isLobbyChangeRequested()) {
            log("Name mention detected inside GlacialMacro onTick, changing lobbies");
            NameMentionFailsafe.getInstance().resetStates();
            transitionTo(new NewLobbyState());
        }

        if (isTimerRunning() || currentState == null) {
            return;
        }

        GlacialMacroState nextState = currentState.onTick(this);
        transitionTo(nextState);
    }

    public void transitionTo(GlacialMacroState nextState) {
        if (currentState == nextState || nextState == null) {
            return;
        }

        log("Transitioning from " + currentState.getClass().getSimpleName() + " to " + nextState.getClass().getSimpleName());
        currentState.onEnd(this);
        currentState = nextState;
        currentState.onStart(this);
    }

    @Override
    public void onChat(String message) {
        if (isEnabled() && message.contains("Commission Completed!") && !message.contains(":")) {
            log("Commission completion detected by chat message.");
            if (currentState instanceof MiningState || currentState instanceof PathfindingState) {
                transitionTo(new ClaimingCommissionState());
            }
        }
    }

    // Helper methods that states can call

    public void updateMiningTasks() {
        previousVeins.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 1000 * 60 * 5); // 5 minute cooldown
        typeToMine.clear();

        Set<Map.Entry<GlaciteVeins, Double>> commissionEntries = TablistUtil.getGlaciteComs().entrySet();
        for (Map.Entry<GlaciteVeins, Double> entry : commissionEntries) {
            if (entry.getValue() < 100.0) {
                typeToMine.add(entry.getKey());
            }
        }
        log("Updated tasks. Will mine for: " + typeToMine);
    }

    public Pair<GlaciteVeins, RouteWaypoint> findBestVein() {
        List<Pair<GlaciteVeins, RouteWaypoint>> possibleVeins = new ArrayList<>();

        if (typeToMine.isEmpty()) {
            log("No active commissions to mine for.");
            return null;
        }

        for (GlaciteVeins vein : typeToMine) {
            for (RouteWaypoint waypoint : GlaciteVeins.getVeins(vein)) {
                Pair<GlaciteVeins, RouteWaypoint> key = new Pair<>(vein, waypoint);
                if (!previousVeins.containsKey(key)) {
                    possibleVeins.add(key);
                }
            }
        }

        if (possibleVeins.isEmpty()) {
            log("All available veins have been visited recently. Waiting for cooldowns.");
            return null;
        }

        return possibleVeins.stream()
                .min(Comparator.comparingDouble(pair -> GraphHandler.instance.distance(getName(), PlayerUtil.getBlockStandingOn(), pair.second())))
                .orElse(null);
    }

    public MineableBlock[] getBlocksToMine() {
        List<MineableBlock> blocksList = new ArrayList<>();
        if (typeToMine.isEmpty()) updateMiningTasks();

        for (GlaciteVeins veinType : typeToMine) {
            blocksList.addAll(veinType.getMineableBlocks());
        }

        return blocksList.stream().distinct().toArray(MineableBlock[]::new);
    }

    public int[] getBlockPriority() {
        MineableBlock[] blocksToMine = getBlocksToMine();
        int[] priorities = new int[blocksToMine.length];
        Arrays.fill(priorities, 1);
        return priorities;
    }

    @Override
    public List<String> getNecessaryItems() {
        List<String> items = new ArrayList<>();
        items.add(VeinForge.config().general.miningTool);
        items.add("Aspect of the Void");
        items.add("Royal Pigeon");

        if (VeinForge.config().general.drillRefuel) {
            items.add("Abiphone");
        }
        return items;
    }
}