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
   GRAY_MITHRIL(Blocks.WOOL.gray(), Blocks.DYED_TERRACOTTA.cyan()),
   GREEN_MITHRIL(Blocks.PRISMARINE, Blocks.DARK_PRISMARINE, Blocks.PRISMARINE_BRICKS),
   BLUE_MITHRIL(Blocks.WOOL.lightBlue()),
   // Gemstones
   OPAL(Blocks.STAINED_GLASS.white(), Blocks.STAINED_GLASS_PANE.white()),
   JASPER(Blocks.STAINED_GLASS.magenta(), Blocks.STAINED_GLASS_PANE.magenta()),
   TOPAZ(Blocks.STAINED_GLASS.orange(), Blocks.STAINED_GLASS_PANE.orange()),
   AMBER(Blocks.STAINED_GLASS.yellow(), Blocks.STAINED_GLASS_PANE.yellow()),
   SAPPHIRE(Blocks.STAINED_GLASS.lightBlue(), Blocks.STAINED_GLASS_PANE.lightBlue()),
   JADE(Blocks.STAINED_GLASS.lime(), Blocks.STAINED_GLASS_PANE.lime()),
   AMETHYST(Blocks.STAINED_GLASS.purple(), Blocks.STAINED_GLASS_PANE.purple()),
   RUBY(Blocks.STAINED_GLASS.red(), Blocks.STAINED_GLASS_PANE.red()),
   AQUAMARINE(Blocks.STAINED_GLASS.cyan(), Blocks.STAINED_GLASS_PANE.cyan()),
   PERIDOT(Blocks.STAINED_GLASS.green(), Blocks.STAINED_GLASS_PANE.green()),
   ONYX(Blocks.STAINED_GLASS.black(), Blocks.STAINED_GLASS_PANE.black()),
   CITRINE(Blocks.STAINED_GLASS.brown(), Blocks.STAINED_GLASS_PANE.brown()),
   GLACITE(Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE),
   TUNGSTEN(Blocks.CONCRETE.gray()),
   UMBER(Blocks.CONCRETE.brown()),
   ANHYDRITE(Blocks.CONCRETE.white());

   private final List<Block> block;

   MineableBlock(Block... blocks) {
      this.block = Arrays.asList(blocks);
   }

   public List<Block> getBlocks() {
      return Collections.unmodifiableList(block);
   }

   public static List<Block> getAllMineableBlocks() {
      List<Block> allBlocks = new ArrayList<>();
      for (MineableBlock mineableBlock : MineableBlock.values()) {
         allBlocks.addAll(mineableBlock.getBlocks());
      }
      return allBlocks;
   }
}
