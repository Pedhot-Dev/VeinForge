package me.grish.veinforge.failsafe.impl;

import lombok.Getter;
import me.grish.veinforge.failsafe.AbstractFailsafe;
import me.grish.veinforge.macro.MacroManager;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.helper.Clock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;


/*Hours wasted on this: None, this was easy as fuck*/
public class BedrockBlockChangeFailsafe extends AbstractFailsafe {

    @Getter
    private static final BedrockBlockChangeFailsafe instance = new BedrockBlockChangeFailsafe();
    private static final int THRESHOLD = 20;
    private static final long TIME_WINDOW = 100;
    private static final int RADIUS = 10;
    private final Clock timer = new Clock();
    private final List<Long> bedrockChangeTimestamps = new ArrayList<>();

    @Override
    public String getName() {
        return "BedrockBlockChangeFailsafe";
    }

    @Override
    public Failsafe getFailsafeType() {
        return Failsafe.BLOCK_CHANGE;
    }

    @Override
    public int getPriority() {
        return 7;
    }

    @Override
    public boolean onTick() {
        long currentTime = System.currentTimeMillis();

        List<Long> validTimestamps = new ArrayList<>();

        for (Long timestamp : bedrockChangeTimestamps) {
            if (currentTime - timestamp <= TIME_WINDOW) {
                validTimestamps.add(timestamp);
            }
        }

        bedrockChangeTimestamps.clear();
        bedrockChangeTimestamps.addAll(validTimestamps);

        if (bedrockChangeTimestamps.size() >= THRESHOLD) {
            Logger.sendWarning("Too many Bedrock block changes in the last " + TIME_WINDOW / 1000.0 + " seconds. Triggering failsafe.");
            return true;
        }

        return false;
    }

    @Override
    public boolean onPacketReceive(Packet<?> packet) {
        if (packet instanceof ClientboundBlockUpdatePacket blockUpdatePacket) {
            BlockPos blockPos = blockUpdatePacket.getPos();
            net.minecraft.world.level.block.Block block = blockUpdatePacket.getBlockState().getBlock();

            if (block == Blocks.BEDROCK) {
                BlockPos playerPos = mc.player.blockPosition();

                double distance = playerPos.distSqr(blockPos);

                double radiusSquared = RADIUS * RADIUS;

                if (distance <= radiusSquared) {
                    long currentTime = System.currentTimeMillis();
                    bedrockChangeTimestamps.add(currentTime);
//                    log("Bedrock block change detected at: " + currentTime + " within radius at position " + blockPos);
                }
            }
        }

        return false;
    }


    @Override
    public boolean react() {
        // Disable macro (iam a lazy mf and haven`t done more here)
        MacroManager.getInstance().disable();
        Logger.sendWarning("Too many Bedrock block changes nearby! Disabling macro.");
        return true;
    }

    @Override
    public void resetStates() {
        this.bedrockChangeTimestamps.clear(); // Clear the recorded timestamps
        this.timer.reset();
    }
}
