package me.grish.veinforge.config;

import me.grish.veinforge.VeinForge;
import me.grish.veinforge.VeinForgeClient;
import me.grish.veinforge.ui.screen.HUDEditorScreen;
import me.grish.veinforge.util.Logger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public class ConfigActions {

    public static void openHudEditor() {
        Minecraft client = Minecraft.getInstance();
        Screen parent = client.gui.screen();
        client.gui.setScreen(new HUDEditorScreen(parent));
    }

    public static void setMiningTool() {
        handleItemSet(
                name -> VeinForge.config().general.miningTool = name,
                "Mining Tool",
                false,
                "Config GUI"
        );
    }

    public static void setMiningToolCommand() {
        handleItemSet(
                name -> VeinForge.config().general.miningTool = name,
                "Mining Tool",
                false,
                "Command /vf set mining-tool"
        );
    }

    public static void setAltMiningTool() {
        handleItemSet(
                name -> VeinForge.config().commission.dwarvenCommission.altMiningTool = name,
                "Alternative Mining Tool",
                false,
                "Config GUI"
        );
    }

    public static void setAltMiningToolCommand() {
        handleItemSet(
                name -> VeinForge.config().commission.dwarvenCommission.altMiningTool = name,
                "Alternative Mining Tool",
                false,
                "Command /vf set alt-mining-tool"
        );
    }

    public static void setSlayerWeapon() {
        handleItemSet(
                name -> VeinForge.config().commission.dwarvenCommission.slayerWeapon = name,
                "Slayer Weapon",
                true,
                "Config GUI"
        );
    }

    public static void setSlayerWeaponCommand() {
        handleItemSet(
                name -> VeinForge.config().commission.dwarvenCommission.slayerWeapon = name,
                "Slayer Weapon",
                true,
                "Command /vf set slayer-weapon"
        );
    }

    public static void setFishingRod() {
        handleItemSet(
                name -> VeinForge.config().fishing.generalFishing.fishingRod = name,
                "Fishing Rod",
                false,
                "Config GUI"
        );
    }

    public static void setFishingRodCommand() {
        handleItemSet(
                name -> VeinForge.config().fishing.generalFishing.fishingRod = name,
                "Fishing Rod",
                false,
                "Command /vf set fishing-rod"
        );
    }

    public static void setGalateaAxe() {
        handleItemSet(
                name -> VeinForge.config().fishing.galateaFishing.galateaAxe = name,
                "Galatea Axe",
                false,
                "Config GUI"
        );
    }

    public static void setGalateaAxeCommand() {
        handleItemSet(
                name -> VeinForge.config().fishing.galateaFishing.galateaAxe = name,
                "Galatea Axe",
                false,
                "Command /vf set fishing-axe"
        );
    }

    public static void setGalateaFishingWeapon() {
        handleItemSet(
                name -> VeinForge.config().fishing.galateaFishing.galateaFishingWeapon = name,
                "Galatea Secondary Weapon",
                false,
                "Config GUI"
        );
    }

    public static void setGalateaFishingWeaponCommand() {
        handleItemSet(
                name -> VeinForge.config().fishing.galateaFishing.galateaFishingWeapon = name,
                "Galatea Secondary Weapon",
                false,
                "Command /vf set fishing-weapon"
        );
    }

    private static void handleItemSet(
            java.util.function.Consumer<String> setter,
            String toolName,
            boolean strictSanitize,
            String source
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack currentItem = mc.player
                .getInventory()
                .getItem(mc.player.getInventory().getSelectedSlot());

        if (currentItem.isEmpty()) {
            Logger.sendMessage("Don't hold an empty hand.");
            return;
        }

        String strippedName = ChatFormatting.stripFormatting(
                currentItem.getHoverName().getString()
        );
        if (strictSanitize) {
            strippedName = strippedName.replaceAll("[^\\x20-\\x7E]", "");
        }

        setter.accept(strippedName);
        VeinForgeClient.configManager.saveConfig();
        Logger.sendMessage(
                toolName + " set to: " +
                        currentItem.getHoverName().getString() +
                        " (via " + source + ", saved config/veinforge/veinforge.json)"
        );
    }
}
