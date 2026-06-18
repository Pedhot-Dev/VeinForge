package me.grish.veinforge.feature.impl.AutoGetStats.tasks.impl;

import me.grish.veinforge.feature.impl.AutoGetStats.tasks.AbstractInventoryTask;
import me.grish.veinforge.feature.impl.AutoGetStats.tasks.TaskStatus;
import me.grish.veinforge.feature.impl.BlockMiner.BlockMiner;
import me.grish.veinforge.util.InventoryUtil;
import me.grish.veinforge.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * A task that retrieves the players pickaxe ability from the HOTM GUI.
 */
public class PickaxeAbilityRetrievalTask extends AbstractInventoryTask<BlockMiner.PickaxeAbility> {

    private final Minecraft mc = Minecraft.getInstance();
    private final Clock timer = new Clock();
    private BlockMiner.PickaxeAbility pickaxeAbility;

    @Override
    public void init() {
        pickaxeAbility = BlockMiner.PickaxeAbility.NONE;
        taskStatus = TaskStatus.RUNNING;

        // The case that the HOTM menu is already open
        if (!InventoryUtil.getInventoryName().equals("Heart of the Mountain")) {
            if (mc.gui.screen() != null) {
                InventoryUtil.closeScreen();
            }

            if (mc.player != null) {
                mc.player.connection.sendCommand("hotm");
            }
        }

        timer.schedule(1000);
    }

    @Override
    public void onTick() {
        if (!timer.passed() && timer.isScheduled()) {
            return;
        }

        if (isSelected("Pickobulus")) {
            pickaxeAbility = BlockMiner.PickaxeAbility.PICKOBULUS;
        } else {
            pickaxeAbility = BlockMiner.PickaxeAbility.MINING_SPEED_BOOST;
        }
        taskStatus = TaskStatus.SUCCESS;
    }

    private boolean isSelected(String name) {
        final Slot slot = InventoryUtil.getSlotOfItemInContainer(name);
        final Block block = slot != null
                ? Block.byItem(slot.getItem().getItem())
                : null;

        return block != null && block == Blocks.EMERALD_BLOCK;
    }

    @Override
    public void end() {
        InventoryUtil.closeScreen();
    }

    @Override
    public BlockMiner.PickaxeAbility getResult() {
        return pickaxeAbility;
    }
}