package me.grish.veinforge.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Event fired when a block changes state.
 */
public record BlockChangeEvent(BlockPos pos, BlockState oldState, BlockState newState) {

    private static final List<Consumer<BlockChangeEvent>> listeners = new ArrayList<>();

    public static void register(Consumer<BlockChangeEvent> listener) {
        listeners.add(listener);
    }

    public static void fire(BlockPos pos, BlockState oldState, BlockState newState) {
        BlockChangeEvent event = new BlockChangeEvent(pos, oldState, newState);
        for (Consumer<BlockChangeEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
