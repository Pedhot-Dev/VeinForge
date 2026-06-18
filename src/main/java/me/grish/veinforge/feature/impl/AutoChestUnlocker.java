package me.grish.veinforge.feature.impl;

import me.grish.veinforge.event.BlockChangeEvent;
import me.grish.veinforge.event.SpawnParticleEvent;
import me.grish.veinforge.feature.AbstractFeature;
import me.grish.veinforge.handler.RotationHandler;
import me.grish.veinforge.util.*;
import me.grish.veinforge.util.helper.RotationConfiguration;
import me.grish.veinforge.util.helper.Target;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoChestUnlocker extends AbstractFeature {

    public static AutoChestUnlocker instance = new AutoChestUnlocker();

    public static List<BlockPos> chestQueue = new ArrayList<>();
    public ChestFailure chestFailure = ChestFailure.NONE;
    private State state = State.STARTING;
    private Vec3 particlePos = null;
    private boolean particleSpawned = true;
    private boolean chestSolved = false;
    private BlockPos chestSolving = null;
    private List<BlockPos> walkableBlocks = new ArrayList<>();
    private boolean clickChest = false;

    @Override
    public String getName() {
        return "TreasureChestUnlocker";
    }

    public void start(boolean clickChest) {
        this.start("", clickChest);
    }

    public void start(String itemToHold, boolean clickChest) {
        if (!itemToHold.isEmpty()) {
            // doesn't matter if I can hold it or not
            InventoryUtil.holdItem(itemToHold);
        }
        this.chestFailure = ChestFailure.NONE;
        this.clickChest = clickChest;
        this.enabled = true;
        note("Started");
    }

    @Override
    public void stop() {
        this.enabled = false;
        this.particlePos = null;
        this.chestSolving = null;
        this.walkableBlocks.clear();
        this.clickChest = false;
        this.resetStatesAfterStop();
        note("Stopped");
    }

    @Override
    public void resetStatesAfterStop() {
        this.state = State.STARTING;
    }

    private void stop(ChestFailure failure) {
        this.chestFailure = failure;
        this.stop();
    }

    private void changeState(State to, int time) {
        this.state = to;
        if (time == 0) {
            this.timer.reset();
        } else {
            this.timer.schedule(time);
        }
    }

    @Override
    protected void onTick() {
        if (!this.enabled) {
            return;
        }

        switch (this.state) {
            case STARTING:
                if (chestQueue.isEmpty()) {
                    log("Chestqueue is empty");
                    this.changeState(State.ENDING, 0);
                    break;
                }
                chestQueue.sort(Comparator.comparingDouble(pos -> mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))));
                this.chestSolving = chestQueue.remove(0);
                if (PlayerUtil.getPlayerEyePos().distanceTo(Vec3.atCenterOf(this.chestSolving)) > 4) {
                    this.changeState(State.FINDING_WALKABLE_BLOCKS, 0);
                } else {
                    this.changeState(State.LOOKING, 0);
                }
                break;
            case FINDING_WALKABLE_BLOCKS:
                List<BlockPos> pos = new ArrayList<>();
                for (int y = -4; y < 0; y++) {
                    for (int x = -1; x < 2; x++) {
                        for (int z = -1; z < 2; z++) {
                            BlockPos newPos = this.chestSolving.offset(x, y, z);
                            if (BlockUtil.canStandOn(newPos)) {
                                pos.add(newPos);
                            }
                        }
                    }
                }
                pos.sort(Comparator.comparingDouble(p -> mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p))));
                this.walkableBlocks = pos;
                this.changeState(State.WALKING, 0);
                log("Found block list size: " + pos.size());
                break;
            case WALKING:
                if (this.walkableBlocks.isEmpty()) {
                    sendError("no walkable blocks around this chest. ignoring");
                    this.changeState(State.STARTING, 0);
                    break;
                }
                BlockPos target = this.walkableBlocks.remove(0);
                log("Walking to " + target);
                Pathfinder.getInstance().setInterpolationState(true);
                Pathfinder.getInstance().queue(PlayerUtil.getBlockStandingOn(), target);
                Pathfinder.getInstance().start();
                this.changeState(State.WAITING, 0);
                break;
            case WAITING:
                if (Pathfinder.getInstance().isRunning()) {
                    if (PlayerUtil.getPlayerEyePos().distanceTo(Vec3.atCenterOf(this.chestSolving)) < 3) {
                        log("distance < 4");
                        Pathfinder.getInstance().stop();
                        this.changeState(State.LOOKING, 5000);
                    }
                } else {
                    if (Pathfinder.getInstance().succeeded()) {
                        log("Pathfinder succeeded");
                        this.changeState(State.LOOKING, 5000);
                    } else {
                        sendError("failed walking to block. retrying");
                        this.changeState(State.WALKING, 0);
                    }
                }
                break;
            case LOOKING:
                if (!this.chestSolving.equals(BlockUtil.getBlockLookingAt()) && !RotationHandler.getInstance().isEnabled()) {
                    RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(BlockUtil.getClosestVisibleSidePos(this.chestSolving)), 400, null));
                }
                if (this.clickChest) {
                    this.changeState(State.VERIFYING_ROTATION, 3000);
                    break;
                }

                if (this.hasTimerEnded()) {
                    sendError("no particle spawned in over 2 seconds.");
                    this.changeState(State.STARTING, 0);
                    break;
                }

                if (this.chestSolved) {
                    log("chest solved. stopping rotation and going to next");
                    RotationHandler.getInstance().stop();
                    this.changeState(State.STARTING, 0);
                    this.chestSolved = false;
                    break;
                }

                if (this.particleSpawned && this.particlePos != null) {
                    RotationHandler.getInstance().stopFollowingTarget();
                    RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(this.particlePos), 400, null).followTarget(true));
                    this.timer.schedule(5000);
                    this.particleSpawned = false;
                }
                break;
            case VERIFYING_ROTATION:
                if (this.hasTimerEnded()) {
                    sendError("Couldn't look at chest.");
                    this.changeState(State.STARTING, 0);
                }
                if (this.chestSolving.equals(BlockUtil.getBlockLookingAt())) {
                    RotationHandler.getInstance().stop();
                    KeyBindUtil.releaseAllExcept();
                    this.changeState(State.CLICKING, 250);
                } else if (!RotationHandler.getInstance().isEnabled()) {
                    KeyBindUtil.holdThese(mc.options.keyAttack);
                }
                break;
            case CLICKING:
                if (this.isTimerRunning()) {
                    return;
                }
                KeyBindUtil.rightClick();
                this.changeState(State.STARTING, 0);
                break;
            case ENDING:
                log("Ended. Stopping");
                this.stop();
                break;
        }
    }

    @Override
    protected void onParticleSpawn(SpawnParticleEvent event) {
        if (this.state != State.LOOKING || !this.clickChest) {
            return;
        }
        if (event.getParticleType() == ParticleTypes.CRIT && mc.player.position().distanceToSqr(event.getPos()) < 64) {
            BlockState state = mc.level.getBlockState(this.chestSolving);
            if (state.getBlock() instanceof ChestBlock && this.chestSolving.relative(state.getValue(ChestBlock.FACING))
                    .equals(new BlockPos((int) event.getPos().x, (int) event.getPos().y, (int) event.getPos().z))) {
                this.particlePos = event.getPos();
                this.particleSpawned = true;
            }
        }
    }

    @Override
    protected void onWorldRender(LevelRenderContext context) {
        chestQueue.forEach(it -> {
            RenderUtil.drawBlock(it, new Color(0, 255, 255, 100));
        });

        if (!this.enabled) {
            return;
        }

        if (this.chestSolving != null) {
            RenderUtil.drawBlock(this.chestSolving, new Color(0, 255, 0, 100));
        }

        if (this.particlePos != null) {
            RenderUtil.drawPoint(this.particlePos, new Color(255, 0, 0, 100));
        }
    }

    // maybe move this into powdermacro class
    @Override
    protected void onBlockChange(BlockChangeEvent event) {
        if (mc.player == null) {
            return;
        }
        BlockPos eventPos = event.pos();
        if (mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(eventPos)) > 64) {
            return;
        }
        Block newBlock = event.newState().getBlock();
        Block oldBlock = event.oldState().getBlock();

        if (oldBlock instanceof AirBlock && newBlock instanceof ChestBlock) {
            chestQueue.add(eventPos);
        } else if (newBlock instanceof AirBlock && oldBlock instanceof ChestBlock) {
            if (eventPos.equals(this.chestSolving)) {
                log("Chest removed; solved");
                this.chestSolved = true;
            } else {
                log("Chest Despawned");
                chestQueue.remove(eventPos);
            }
        }
    }

    @Override
    protected void onWorldUnload(ClientLevel world) {
        chestQueue.clear();
    }

    @Override
    protected void onChat(String message) {
        if (!this.enabled) {
            return;
        }
        if (message.equals("CHEST LOCKPICKED")) {
            this.chestSolved = true;
        }
    }

    enum State {
        STARTING, FINDING_WALKABLE_BLOCKS, WALKING, WAITING, LOOKING, VERIFYING_ROTATION, CLICKING, ENDING
    }

    enum ChestFailure {
        NONE, NO_TOOL_IN
    }
}
