package me.grish.veinforge.failsafe.impl;

import lombok.Getter;
import me.grish.veinforge.failsafe.AbstractFailsafe;
import me.grish.veinforge.macro.MacroManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class BedrockCheckFailsafe extends AbstractFailsafe {

    @Getter
    private static final BedrockCheckFailsafe instance = new BedrockCheckFailsafe();
    private static final int CHECK_RADIUS = 5;
    private static final int BEDROCK_THRESHOLD = 10;

    @Override
    public String getName() {
        return "BedrockCheckFailsafe";
    }

    @Override
    public Failsafe getFailsafeType() {
        return Failsafe.BEDROCK_CHECK;
    }

    @Override
    public int getPriority() {
        return 6;
    }


    public boolean checkForBedrock(Vec3 playerPos) {
        int bedrockCount = 0;

        for (int x = -CHECK_RADIUS; x <= CHECK_RADIUS; x++) {
            for (int y = -CHECK_RADIUS; y <= CHECK_RADIUS; y++) {
                for (int z = -CHECK_RADIUS; z <= CHECK_RADIUS; z++) {
                    BlockPos blockPos = new BlockPos(
                            (int) (playerPos.x + x),
                            (int) (playerPos.y + y),
                            (int) (playerPos.z + z)
                    );
                    Block block = mc.level.getBlockState(blockPos).getBlock();

                    if (block == Blocks.BEDROCK) {
                        bedrockCount++;
                    }

                    if (bedrockCount >= BEDROCK_THRESHOLD) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean react() {
        MacroManager.getInstance().disable();
        warn("Disabling macro due to bedrock surroundings.");
        return true;
    }

}

