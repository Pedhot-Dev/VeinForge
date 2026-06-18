package me.grish.veinforge.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Event fired when a scoreboard line updates.
 */
public record UpdateScoreboardLineEvent(String line) {

    private static final List<Consumer<UpdateScoreboardLineEvent>> listeners = new ArrayList<>();

    public static void register(Consumer<UpdateScoreboardLineEvent> listener) {
        listeners.add(listener);
    }

    public static void fire(String line) {
        UpdateScoreboardLineEvent event = new UpdateScoreboardLineEvent(line);
        for (Consumer<UpdateScoreboardLineEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
