package me.grish.veinforge.macro.impl.MiningMacro;

import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.feature.FeatureManager;
import me.grish.veinforge.feature.impl.AutoDrillRefuel.AutoDrillRefuel;
import me.grish.veinforge.feature.impl.AutoGetStats.AutoGetStats;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.impl.MiningSpeedRetrievalTask;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.impl.PickaxeAbilityRetrievalTask;
import me.grish.veinforge.feature.impl.BlockMiner.BlockMiner;
import me.grish.veinforge.macro.AbstractMacro;
import me.grish.veinforge.util.InventoryUtil;
import me.grish.veinforge.util.helper.MineableBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>This macro retrieves the player's mining speed before starting the mining loop.
 * It determines which blocks to mine based on VeinForge configs, and coordinates
 * with the {@link BlockMiner} to perform mining actions.</p>
 */
public class MiningMacro extends AbstractMacro {

    @Getter
    private static final MiningMacro instance = new MiningMacro();
    private static final int LOW_FUEL_THRESHOLD = 100;

    private final BlockMiner miner = BlockMiner.getInstance();
    private final List<String> necessaryItems = new ArrayList<>();

    private MiningSpeedRetrievalTask miningSpeedRetrievalTask;
    private PickaxeAbilityRetrievalTask pickaxeAbilityRetrievalTask;
    private int miningSpeed = 0;
    private BlockMiner.PickaxeAbility pickaxeAbility =
            BlockMiner.PickaxeAbility.NONE;

    private MineableBlock[] blocksToMine = {};
    private boolean isMining = false;

    @Override
    public String getName() {
        return "Mining Macro";
    }

    private boolean handleRefuelIfNeeded() {
        if (!VeinForge.config().general.drillRefuel) return false;

        String tool = VeinForge.config().general.miningTool;
        if (tool == null) return false;

        if (!tool.toLowerCase().contains("drill")) return false;

        int fuel = InventoryUtil.getDrillRemainingFuel(tool);

        if (fuel <= LOW_FUEL_THRESHOLD && !AutoDrillRefuel.getInstance().isRunning()) {
            log("Low drill fuel detected (" + fuel + "). Starting auto refuel.");

            miner.stop();          // stop mining safely
            isMining = false;      // allow restart after refuel

            AutoDrillRefuel.FuelType[] fuelTypeMap = {
                    AutoDrillRefuel.FuelType.VOLTA,
                    AutoDrillRefuel.FuelType.OIL_BARREL,
                    AutoDrillRefuel.FuelType.SUNFLOWER_OIL
            };
            AutoDrillRefuel.getInstance().start(tool, fuelTypeMap[VeinForge.config().general.refuelMachineFuel]);
            return true;
        }

        return AutoDrillRefuel.getInstance().isRunning();
    }

    @Override
    public List<String> getNecessaryItems() {
        if (necessaryItems.isEmpty()) {
            necessaryItems.add(VeinForge.config().general.miningTool);
            log("Necessary items initialized: " + necessaryItems);
        }
        return necessaryItems;
    }

    @Override
    public void onEnable() {
        log("Enabling Mining Macro");
        resetVariables();
        setBlocksToMineBasedOnOreType();

        if (miningSpeed == 0) {
            miningSpeedRetrievalTask = new MiningSpeedRetrievalTask();
            pickaxeAbilityRetrievalTask = new PickaxeAbilityRetrievalTask();
            AutoGetStats.getInstance().startTask(miningSpeedRetrievalTask);
            AutoGetStats.getInstance().startTask(pickaxeAbilityRetrievalTask);
        }
    }

    @Override
    public void onDisable() {
        log("Disabling Mining Macro");
        miner.stop();
        isMining = false;
        resetVariables();
    }

    private void resetVariables() {
        miningSpeed = 0;
        necessaryItems.clear();
        isMining = false;
    }

    @Override
    public void onPause() {
        FeatureManager.getInstance().pauseAll();
        log("Mining Macro paused");
    }

    @Override
    public void onResume() {
        FeatureManager.getInstance().resumeAll();
        log("Mining Macro resumed");
    }

    public void onTick() {
        if (miningSpeed == 0) {
            handleGettingStats();
            return;
        }

        if (handleRefuelIfNeeded()) {
            return;
        }

        if (!isMining) {
            miner.setWaitThreshold(
                    VeinForge.config().general.oreRespawnWaitThreshold * 1000
            );
            miner.start(
                    blocksToMine,
                    miningSpeed,
                    pickaxeAbility,
                    determinePriority(),
                    VeinForge.config().general.miningTool
            );

            isMining = true;
            log("Started mining with speed: " + miningSpeed);
            log(
                    "Started mining with pickaxe ability: " + pickaxeAbility.name()
            );
        }

        handleMining();
    }

    private void handleGettingStats() {
        if (!AutoGetStats.getInstance().hasFinishedAllTasks()) return;

        if (miningSpeedRetrievalTask.getError() != null) {
            super.disable(
                    "Failed to get stats with the following error: " +
                            miningSpeedRetrievalTask.getError()
            );
            return;
        }

        if (pickaxeAbilityRetrievalTask.getError() != null) {
            super.disable(
                    "Failed to get pickaxe ability with the following error: " +
                            pickaxeAbilityRetrievalTask.getError()
            );
            return;
        }

        miningSpeed = miningSpeedRetrievalTask.getResult();
        pickaxeAbility = VeinForge.config().general.usePickaxeAbility
                ? pickaxeAbilityRetrievalTask.getResult()
                : BlockMiner.PickaxeAbility.NONE;
    }

    private void handleMining() {
        switch (miner.getError()) {
            case NO_POINTS_FOUND:
                log("Restarting because the block chosen cannot be mined");
                isMining = false;
                break;
            case NO_TARGET_BLOCKS:
                disable(
                        "Please set at least one type of target block in configs!"
                );
                break;
            case NOT_ENOUGH_BLOCKS:
                disable("Not enough blocks nearby! Please move to a new vein");
                break;
            case NO_TOOLS_AVAILABLE:
                disable(
                        "Cannot find tools in hotbar! Please set it in configs"
                );
                break;
            case NO_PICKAXE_ABILITY:
                disable(
                        "Cannot find messages for pickaxe ability! " +
                                "Either enable any pickaxe ability in HOTM or enable chat messages. You can also disable pickaxe ability in configs."
                );
                break;
        }
    }

    private void setBlocksToMineBasedOnOreType() {
        log(
                "Setting blocks to mine based on ore type: " +
                        VeinForge.config().miningMacro.oreType
        );
        switch (VeinForge.config().miningMacro.oreType) {
            case 0:
                blocksToMine = new MineableBlock[]{
                        MineableBlock.GRAY_MITHRIL,
                        MineableBlock.GREEN_MITHRIL,
                        MineableBlock.BLUE_MITHRIL,
                        MineableBlock.TITANIUM,
                };
                break;
            case 1:
                blocksToMine = new MineableBlock[]{MineableBlock.DIAMOND};
                break;
            case 2:
                blocksToMine = new MineableBlock[]{MineableBlock.EMERALD};
                break;
            case 3:
                blocksToMine = new MineableBlock[]{MineableBlock.REDSTONE};
                break;
            case 4:
                blocksToMine = new MineableBlock[]{MineableBlock.LAPIS};
                break;
            case 5:
                blocksToMine = new MineableBlock[]{MineableBlock.GOLD};
                break;
            case 6:
                blocksToMine = new MineableBlock[]{MineableBlock.IRON};
                break;
            case 7:
                blocksToMine = new MineableBlock[]{MineableBlock.COAL};
                break;
            default:
                blocksToMine = new MineableBlock[]{};
                log("Invalid ore type selected");
                break;
        }
        log("Blocks to mine: " + Arrays.toString(blocksToMine));
    }

    private int[] determinePriority() {
        if (VeinForge.config().miningMacro.oreType == 0) {
            return new int[]{
                    VeinForge.config().miningMacro.mineGrayMithril
                            ? VeinForge.config().miningMacro.mithrilPriorityGrayDefault
                            : 0,
                    VeinForge.config().miningMacro.mineGreenMithril
                            ? VeinForge.config().miningMacro.mithrilPriorityGreenDefault
                            : 0,
                    VeinForge.config().miningMacro.mineBlueMithril
                            ? VeinForge.config().miningMacro.mithrilPriorityBlueDefault
                            : 0,
                    VeinForge.config().miningMacro.mineTitanium
                            ? VeinForge.config().miningMacro.mithrilPriorityTitaniumDefault
                            : 0,
            };
        }
        return new int[]{1, 1, 1, 1};
    }
}
