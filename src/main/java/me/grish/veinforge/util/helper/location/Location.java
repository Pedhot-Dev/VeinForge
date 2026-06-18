package me.grish.veinforge.util.helper.location;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum Location {
    PRIVATE_ISLAND("Private Island"),
    HUB("Hub"),
    THE_PARK("The Park"),
    THE_FARMING_ISLANDS("The Farming Islands"),
    FARMING_1("The Barn"),

    SPIDER_DEN("Spider's Den"),
    THE_END("The End"),
    CRIMSON_ISLE("Crimson Isle"),
    GOLD_MINE("Gold Mine"),
    DEEP_CAVERNS("Deep Caverns"),
    DWARVEN_MINES("Dwarven Mines"),
    CRYSTAL_HOLLOWS("Crystal Hollows"),
    JERRY_WORKSHOP("Jerry's Workshop"),
    DUNGEON_HUB("Dungeon Hub"),
    GARDEN("Garden"),
    DUNGEON("Dungeon"),
    LIMBO("UNKNOWN"),
    LOBBY("PROTOTYPE"),
    GLACITE_MINESHAFT("Glacite Mineshaft"),
    JERRYS_WORKSHOP("Jerry's Workshop"),
    RIFT("Rift"),
    BACKWATER_BAYOU("Backwater Bayou"),
    GALATEA("Galatea"),

    // Knowhere - Avengers: Infinity War
    KNOWHERE("Knowhere");

    private static final Map<String, Location> nameToLocationMap = new HashMap<>();
    private static final Map<String, Location> aliasToLocationMap = new HashMap<>();

    static {
        for (Location location : Location.values()) {
            nameToLocationMap.put(location.getName(), location);
        }

        // Some clients/scoreboards use this pluralized variant.
        aliasToLocationMap.put("Glacite Mineshafts", GLACITE_MINESHAFT);
    }

    private final String name;

    Location(String name) {
        this.name = name;
    }

    public static Location fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Location.KNOWHERE;
        }

        String normalized = name.trim();

        final Location loc = nameToLocationMap.get(normalized);
        if (loc != null) {
            return loc;
        }

        final Location aliasLoc = aliasToLocationMap.get(normalized);
        if (aliasLoc != null) {
            return aliasLoc;
        }

        return Location.KNOWHERE;
    }
}
