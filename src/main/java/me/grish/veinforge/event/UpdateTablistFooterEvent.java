package me.grish.veinforge.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Event fired when tablist footer updates.
 */
public record UpdateTablistFooterEvent(List<String> footer) {

    private static final List<Consumer<UpdateTablistFooterEvent>> listeners = new ArrayList<>();

    public static void register(Consumer<UpdateTablistFooterEvent> listener) {
        listeners.add(listener);
    }

    public static void fire(List<String> footer) {
        UpdateTablistFooterEvent event = new UpdateTablistFooterEvent(footer);
        for (Consumer<UpdateTablistFooterEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
