package me.grish.veinforge.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Event fired when scoreboard updates.
 */
public record UpdateScoreboardEvent(List<String> scoreboard, long timestamp) {

    private static final List<Consumer<UpdateScoreboardEvent>> listeners = new ArrayList<>();

    public static void register(Consumer<UpdateScoreboardEvent> listener) {
        listeners.add(listener);
    }

    public static void fire(List<String> scoreboard) {
        UpdateScoreboardEvent event = new UpdateScoreboardEvent(scoreboard, System.currentTimeMillis());
        for (Consumer<UpdateScoreboardEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
