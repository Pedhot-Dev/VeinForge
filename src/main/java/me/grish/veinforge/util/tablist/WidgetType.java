package me.grish.veinforge.util.tablist;

import me.grish.veinforge.util.helper.location.Location;

import java.util.Arrays;
import java.util.HashMap;

public enum WidgetType {
   GENERAL_INFO("General Info", "Area"),
   PROFILE("Profile", "Profile"),
   STATS("Stats", "Stats"),
   COLLECTIONS("Collections", "Collection"),
   DAILY_QUESTS("Daily Quests", "Daily Quests"),
   EFFECTS("Effects", "Active Effects"),
   ELECTION("Election", "Election"),
   EVENTS("Events", "Event"),
   FIRE_SALES("Fire Sales", "Fire Sales"),
   FORGE("Forge", "Forges"),
   TIMERS("Timers", "Timers"),
   BESTIARY("Bestiary", "Bestiary"),
   PET("Pet", "Pet"),
   SKILLS("Skill", "Skill"),
   ESSENCE("Essence", "Essence"),

   MINIONS("Minions", "Minions", Location.PRIVATE_ISLAND),
   CATACOMBS("Catacombs", "Dungeons", Location.DUNGEON_HUB),
   PARTY("Party", "Party", Location.DUNGEON_HUB),
   TRAPPER("Trapper", "Trapper", Location.THE_FARMING_ISLANDS),
   COMPOSTER("Composter", "Composter", Location.GARDEN),
   CROP_MILESTONES("Crop Milestones", "Crop Milestones", Location.GARDEN),
   VISITORS("Visitors", "Visitors", Location.GARDEN),
   PESTS("Pests", "Pests", Location.GARDEN),
   JACOBS_CONTEST(
           "Jacob's Contest",
           "Jacob's Contest",
           Location.HUB,
           Location.THE_FARMING_ISLANDS,
           Location.GARDEN
   ),
   CRYSTALS(
           "Crystals",
           "Crystals",
           Location.DWARVEN_MINES,
           Location.CRYSTAL_HOLLOWS
   ),
   COMMISSIONS(
           "Commissions",
           "Commissions",
           Location.DWARVEN_MINES,
           Location.CRYSTAL_HOLLOWS,
           Location.GLACITE_MINESHAFT
   ),
   POWDER(
           "Powder",
           "Powders",
           Location.DWARVEN_MINES,
           Location.CRYSTAL_HOLLOWS
   ),
   DRAGON("Dragon", "Dragon", Location.THE_END),

   MAGE_REPUTATION("Reputation", "Mage Reputation", Location.CRIMSON_ISLE),
   BARBARIAN_REPUTATION(
           "Reputation",
           "Barbarian Reputation",
           Location.CRIMSON_ISLE
   ),

   FACTION_QUESTS("Faction Quests", "Faction Quests", Location.CRIMSON_ISLE),
   TROPHY_FISH("Trophy Fish", "Trophy Fish", Location.CRIMSON_ISLE),
   RIFT("Rift", "Good to know", Location.RIFT),
   BARRY("Barry", "Advertisement", Location.RIFT),
   SLAYER(
           "Slayer",
           "Slayer",
           Location.HUB,
           Location.THE_PARK,
           Location.SPIDER_DEN,
           Location.THE_END,
           Location.CRIMSON_ISLE,
           Location.RIFT
   );

   private static final HashMap<String, WidgetType> BY_PREFIX;

   static {
      BY_PREFIX = new HashMap<>();
      for (WidgetType type : values()) {
         BY_PREFIX.put(type.prefix, type);
      }

      // Hypixel occasionally uses singular headers.
      BY_PREFIX.put("Commission", COMMISSIONS);
   }

   private final String name;
   private final String prefix;
   private final Location[] locations;
   private final boolean everywhere;

   WidgetType(String name, String prefix, Location... locations) {
      this.name = name;
      this.locations = locations;
      this.prefix = prefix;
      this.everywhere = false;
   }

   WidgetType(String name, String prefix) {
      this.name = name;
      this.prefix = prefix;
      this.locations = null;
      this.everywhere = true;
   }

   public static WidgetType byPrefix(String prefix) {
      return BY_PREFIX.get(prefix);
   }

   public String getPrefix() {
      return prefix;
   }

   public boolean isActive(Location location) {
      if (everywhere) return true;
      //noinspection ConstantConditions
      return Arrays.stream(locations).anyMatch(loc -> loc == location);
   }
}
