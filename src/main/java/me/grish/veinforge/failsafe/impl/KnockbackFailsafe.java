package me.grish.veinforge.failsafe.impl;

import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.failsafe.AbstractFailsafe;
import me.grish.veinforge.macro.MacroManager;
import me.grish.veinforge.util.Logger;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

public class KnockbackFailsafe extends AbstractFailsafe {

    @Getter
    private static final KnockbackFailsafe instance = new KnockbackFailsafe();

    public int getPriority() {
        return 8;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Failsafe getFailsafeType() {
        return Failsafe.KNOCKBACK;
    }


    @Override
    public boolean onPacketReceive(Packet<?> packet) {
        if (!(packet instanceof ClientboundSetEntityMotionPacket(int id, net.minecraft.world.phys.Vec3 movement))) return false;
        if (id != mc.player.getId()) return false;
        return movement.y >= VeinForge.config().failsafe.verticalKnockbackThreshold;
    }

    @Override
    public boolean react() {
        MacroManager.getInstance().disable();
        Logger.sendWarning("Knockback has been detected! Disabling macro.");
        return true;
    }
}
