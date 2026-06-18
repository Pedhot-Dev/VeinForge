package me.grish.veinforge.feature.impl.AutoDrillRefuel.states;

import me.grish.veinforge.feature.impl.AutoDrillRefuel.AutoDrillRefuel;
import me.grish.veinforge.util.InventoryUtil;
import me.grish.veinforge.util.KeyBindUtil;
import net.minecraft.client.Minecraft;

public class AbiphoneState implements AutoDrillRefuelState {

    private final Minecraft mc = Minecraft.getInstance();

    @Override
    public void onStart(AutoDrillRefuel refueler) {
        int abiphoneSlot = InventoryUtil.getHotbarSlotOfItem("Abiphone");
        if (abiphoneSlot == -1) {
            logError("No abiphone found!");
            refueler.stop();
            refueler.setError(AutoDrillRefuel.AutoDrillRefuelError.NO_ABIPHONE);
            return;
        }

        mc.player.getInventory().setSelectedSlot(abiphoneSlot);
        KeyBindUtil.rightClick();
        log("Entering abiphone state");
    }

    @Override
    public AutoDrillRefuelState onTick(AutoDrillRefuel refueler) {

        if (InventoryUtil.getInventoryName().contains("Abiphone") && InventoryUtil.isInventoryLoaded()) {
            log("Opened Abiphone GUI");
            int greatforgeSlot = InventoryUtil.getSlotIdOfItemInContainer("Greatforge");

            if (greatforgeSlot == -1) {
                logError("No Greatforge contact!");
                refueler.stop();
                refueler.setError(AutoDrillRefuel.AutoDrillRefuelError.NO_GREATFORGE_CONTACT);
                return null;
            }

            InventoryUtil.clickContainerSlot(greatforgeSlot, 0, InventoryUtil.ClickMode.PICKUP);
            return new GreatforgeState();
        }

        return this;
    }

    @Override
    public void onEnd(AutoDrillRefuel refueler) {
        log("Ending abiphone state");
    }
}
