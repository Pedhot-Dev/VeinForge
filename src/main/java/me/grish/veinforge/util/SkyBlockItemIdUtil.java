package me.grish.veinforge.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/**
 * Utilities for extracting stable Hypixel SkyBlock item IDs.
 */
public final class SkyBlockItemIdUtil {

    private SkyBlockItemIdUtil() {
    }

    /**
     * Returns the SkyBlock internal item id from ExtraAttributes.id if present, otherwise an empty string.
     */
    public static String getSkyBlockId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return "";
        }

        CompoundTag root = customData.copyTag();
        if (root == null || root.isEmpty()) {
            return "";
        }

        Tag extra = root.get("ExtraAttributes");
        if (!(extra instanceof CompoundTag extraAttributes)) {
            return "";
        }

        Optional<String> idOpt = extraAttributes.getString("id");
        String id = idOpt.orElse("").trim();
        return id.isEmpty() ? "" : id;
    }

    /**
     * Returns SkyBlock id when available, otherwise a stripped display name fallback.
     */
    public static String getSkyBlockIdOrDisplayName(ItemStack stack) {
        String id = getSkyBlockId(stack);
        if (!id.isEmpty()) {
            return id;
        }
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        String displayName = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        return displayName == null ? "" : displayName;
    }
}
