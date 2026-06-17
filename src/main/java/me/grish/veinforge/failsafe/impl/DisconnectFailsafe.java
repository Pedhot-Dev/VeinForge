package me.grish.veinforge.failsafe.impl;

import lombok.Getter;
import me.grish.veinforge.failsafe.AbstractFailsafe;
import me.grish.veinforge.macro.MacroManager;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;

public class DisconnectFailsafe extends AbstractFailsafe {

   @Getter
   private static final DisconnectFailsafe instance = new DisconnectFailsafe();

   @Override
   public String getName() {
      return "DisconnectFailsafe";
   }

   @Override
   public Failsafe getFailsafeType() {
      return Failsafe.DISCONNECT;
   }

   @Override
   public int getPriority() {
      return 10;
   }

   @Override
   public boolean onPacketReceive(Packet<?> packet) {
      return packet instanceof ClientboundDisconnectPacket || mc.gui.screen() instanceof DisconnectedScreen;
   }

   @Override
   public boolean react() {
      warn("Disconnected. Disabling Macro");
      MacroManager.getInstance().disable();
      return true;
   }
}
