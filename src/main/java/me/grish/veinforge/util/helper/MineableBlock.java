package me.grish.veinforge.util.helper;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum MineableBlock {

   QUARTZ(Blocks.QUARTZ_BLOCK, Blocks.NETHER_QUARTZ_ORE),
   DIAMOND(Blocks.DIAMOND_BLOCK),
   EMERALD(Blocks.EMERALD_BLOCK),
   REDSTONE(Blocks.REDSTONE_BLOCK),
   LAPIS(Blocks.LAPIS_BLOCK),
   GOLD(Blocks.GOLD_BLOCK),
   IRON(Blocks.IRON_BLOCK),
   COAL(Blocks.COAL_BLOCK),
   SULPHUR(Blocks.SPONGE),
   HARDSTONE(Blocks.STONE),
   TITANIUM(Blocks.POLISHED_DIORITE),
   // SkyBlock mining blocks (vanilla representations)
   GRAY_MITHRIL(Blocks.GRAY_WOOL, Blocks.CYAN_TERRACOTTA),
   GREEN_MITHRIL(Blocks.PRISMARINE, Blocks.DARK_PRISMARINE, Blocks.PRISMARINE_BRICKS),
   BLUE_MITHRIL(Blocks.LIGHT_BLUE_WOOL),
   // Gemstones
   OPAL(Blocks.WHITE_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS_PANE),
   JASPER(Blocks.MAGENTA_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS_PANE),
   TOPAZ(Blocks.ORANGE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS_PANE),
   AMBER(Blocks.YELLOW_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS_PANE),
   SAPPHIRE(Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE),
   JADE(Blocks.LIME_STAINED_GLASS, Blocks.LIME_STAINED_GLASS_PANE),
   AMETHYST(Blocks.PURPLE_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS_PANE),
   RUBY(Blocks.RED_STAINED_GLASS, Blocks.RED_STAINED_GLASS_PANE),
   AQUAMARINE(Blocks.CYAN_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS_PANE),
   PERIDOT(Blocks.GREEN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS_PANE),
   ONYX(Blocks.BLACK_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS_PANE),
   CITRINE(Blocks.BROWN_STAINED_GLASS, Blocks.BROWN_STAINED_GLASS_PANE),
   // Glacite / Umber / Tungsten (approx. vanilla representations)
   GLACITE(Blocks.PACKED_ICE),
   UMBER(Blocks.TERRACOTTA, Blocks.CLAY),
   TUNGSTEN(Blocks.TERRACOTTA, Blocks.CLAY),
   ;

   public final List<Block> blocks;

   MineableBlock(Block... blocks) {
      List<Block> list = new ArrayList<>();
      if (blocks != null) {
         list.addAll(Arrays.asList(blocks));
      }
      this.blocks = Collections.unmodifiableList(list);
   }

}
