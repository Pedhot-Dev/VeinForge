package me.grish.veinforge.feature.impl.rift;

import lombok.Getter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.util.KeyBindUtil;
import me.grish.veinforge.util.Logger;
import me.grish.veinforge.util.UseItemAbility;
import me.grish.veinforge.util.helper.Clock;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VampireSlayerTracker {

   private static final String BLOODFIEND_NAME = "Bloodfiend";
   private static final String[] NAME_START = {"§c☠ §4Bloodfiend"};
   private static final Pattern TIMER_PATTERN = Pattern.compile(
           "(?:§[8bef]§l(ASHEN|CRYSTAL|AURIC|SPIRIT)§[8bef] \u2668\\d |§4§lIMMUNE )?§c\\d+:\\d+(?:§r)?"
   );
   private static final Pattern IMPEL_PATTERN =
           Pattern.compile("Impel\\s*:?\\s*(.+?)\\s+([0-9]+(?:\\.[0-9]+)?)s\\b",
                   Pattern.CASE_INSENSITIVE);
   private static final Pattern TWINCLAWS_PATTERN =
           Pattern.compile("TWINCLAWS (\\d(?:\\.\\d)?)s");
   private static final String HOLY_ICE_ID = "HOLY_ICE";
   private static final String STEAK_STAKE_ID = "STEAK_STAKE";
   private static final String[] SUPER_SHARP_STAKE_IDS = {
           "SUPER_SHARP_STEAK_STAKE",
           "SUPER_SHARP_STABBY_STEAK_STAKE",
           "SHARP_STEAK_STAKE"
   };
   private static final HealingItem[] HEALING_ITEMS = {
           new HealingItem("HEALING_MELON", "Healing Melon", 7),
           new HealingItem("JUICY_HEALING_MELON", "Juicy Healing Melon", 9),
           new HealingItem("LUSCIOUS_HEALING_MELON", "Luscious Healing Melon", 12)
   };
   private static final float PANIC_HEARTS = 2.0F;
   private static final int MIN_HEAL_DELAY_MS = 600;
   private static final int MAX_HEAL_DELAY_MS = 1000;
   private static final int MIN_IMPEL_DELAY_MS = 90;
   private static final int MAX_IMPEL_DELAY_MS = 180;
   private static final int MIN_KEY_HOLD_MS = 80;
   private static final int MAX_KEY_HOLD_MS = 140;
   private static final int MIN_HOLY_ICE_DELAY_MS = 1400;
   private static final int MAX_HOLY_ICE_DELAY_MS = 2000;
   private static final int LOCATE_BLOODFIEND_COOLDOWN_MS = 500;
   private static final float MIN_UP_PITCH = -85.0F;
   private static final float MAX_UP_PITCH = -60.0F;
   private static final float MIN_DOWN_PITCH = 60.0F;
   private static final float MAX_DOWN_PITCH = 85.0F;
   private static final int MIN_CLICK_AFTER_LOOK_MS = 40;
   private static final int MAX_CLICK_AFTER_LOOK_MS = 90;
   private static final double HOLY_ICE_TRIGGER_SECONDS = 0.5D;

   @Getter
   private static final VampireSlayerTracker instance = new VampireSlayerTracker();
   private static final Minecraft mc = Minecraft.getInstance();
   private final Clock scanTimer = new Clock();
   private final Clock locateCooldown = new Clock();
   private final Clock healCooldown = new Clock();
   private final Clock impelCooldown = new Clock();
   private final Clock swapCooldown = new Clock();
   private final Clock holyIceCooldown = new Clock();
   @Getter
   private Player bloodfiendEntity;
   @Getter
   private ArmorStand nameEntity;
   @Getter
   private ArmorStand timerEntity;
   @Getter
   private boolean stakeable;
   @Getter
   private boolean twinclawsActive;
   private String lastImpelAction;
   private long lastImpelActionTime;
   private double lastImpelSeconds;
   private boolean impelHandled;
   private boolean lastStakeable;
   private boolean lastTwinclawsActive;
   private boolean missingHolyIceLogged;
   private boolean missingStakeLogged;
   private double twinclawsSeconds = -1.0D;

   public void onTick() {
      if (mc.level == null || mc.player == null) {
         return;
      }
      if (VeinForge.config().rift.riftAutoHeal) {
         attemptHealing();
      }
      if (bloodfiendEntity != null && !bloodfiendEntity.isAlive()) {
         Logger.sendMessage("[Rift] Bloodfiend despawned.");
         resetTracking();
         return;
      }
      if (bloodfiendEntity == null) {
         attemptLocateBloodfiend();
         if (bloodfiendEntity == null && needsRescan()) {
            scanForArmorStands();
         }
         if (bloodfiendEntity == null && nameEntity == null && timerEntity == null) {
            return;
         }
      }
      if (VeinForge.config().rift.riftAutoHolyIce) {
         attemptHolyIce();
      }
      if (VeinForge.config().rift.riftAutoStakeSwap) {
         attemptStakeSwap();
      }
      if (needsRescan()) {
         scanForArmorStands();
      }
   }

   public void onEntityUpdate(Player entity, byte updateType) {
      handlePotentialBloodfiend(entity, updateType);
   }

   public void onEntityUpdate(ArmorStand stand, byte updateType) {
      handleArmorStandUpdate(stand, updateType);
   }

   public void onWorldUnload() {
      resetTracking();
   }

   public void onPacketReceive(Packet<?> packet) {
      if (!VeinForge.config().rift.riftEnabled || bloodfiendEntity == null || !VeinForge.config().rift.riftAutoImpel) {
         return;
      }
      String message = extractImpelMessage(packet);
      if (message == null || message.trim().isEmpty()) {
         return;
      }
      java.util.regex.Matcher matcher = IMPEL_PATTERN.matcher(message.trim());
      if (!matcher.find()) {
         return;
      }
      String action = matcher.group(1).trim();
      ImpelAction impelAction = parseImpelAction(action);
      if (impelAction == null) {
         return;
      }
      long now = System.currentTimeMillis();
      String actionKey = impelAction.name();
      if (actionKey.equals(lastImpelAction) && impelHandled) {
         return;
      }
      lastImpelAction = actionKey;
      lastImpelActionTime = now;
      try {
         lastImpelSeconds = Double.parseDouble(matcher.group(2));
      } catch (NumberFormatException ignored) {
         lastImpelSeconds = 0.0D;
      }
      impelHandled = false;
      Logger.sendMessage("[Rift] Impel: " + impelAction.getDisplayName() + ".");
      performImpelAction(impelAction);
   }

   private String extractImpelMessage(Packet<?> packet) {
      if (packet instanceof ClientboundSetTitleTextPacket) {
         return stripFormatting(((ClientboundSetTitleTextPacket) packet).text().getString());
      }
      if (packet instanceof ClientboundSetSubtitleTextPacket) {
         return stripFormatting(((ClientboundSetSubtitleTextPacket) packet).text().getString());
      }
      if (packet instanceof ClientboundSetActionBarTextPacket) {
         return stripFormatting(((ClientboundSetActionBarTextPacket) packet).text().getString());
      }
      if (packet instanceof ClientboundSystemChatPacket messagePacket) {
         if (!messagePacket.overlay()) {
            return null;
         }
         return stripFormatting(messagePacket.content().getString());
      }
      if (packet instanceof ClientboundPlayerChatPacket chatPacket) {
         return stripFormatting(chatPacket.body().content());
      }
      return null;
   }

   private void handlePotentialBloodfiend(Player entity, byte updateType) {
      String name = stripFormatting(entity.getName().getString());
      if (name == null || !name.contains(BLOODFIEND_NAME)) {
         return;
      }
      if (updateType == 1 || !entity.isAlive()) {
         if (entity == bloodfiendEntity) {
            Logger.sendMessage("[Rift] Bloodfiend removed.");
            resetTracking();
         }
         return;
      }
      if (bloodfiendEntity == null || bloodfiendEntity != entity) {
         bloodfiendEntity = entity;
         scanTimer.reset();
         Logger.sendMessage("[Rift] Bloodfiend detected.");
      }
   }

   private void handleArmorStandUpdate(ArmorStand stand, byte updateType) {
      if (updateType == 1) {
         if (stand == nameEntity) {
            nameEntity = null;
            stakeable = false;
            logStakeableChange(false);
         }
         if (stand == timerEntity) {
            timerEntity = null;
            twinclawsActive = false;
            logTwinclawsChange(false);
         }
         return;
      }
      if (stand == nameEntity) {
         updateNameEntity(getCustomNameTag(stand));
         return;
      }
      if (stand == timerEntity) {
         updateTimerEntity(getCustomNameTag(stand));
      }
   }

   private boolean needsRescan() {
      if (nameEntity == null || timerEntity == null) {
         if (!scanTimer.isScheduled() || scanTimer.passed()) {
            return true;
         }
      }
      return nameEntity != null && !nameEntity.isAlive() || timerEntity != null && !timerEntity.isAlive();
   }

   private void scanForArmorStands() {
      scanTimer.schedule(250L);
      if (mc.level == null || mc.player == null) {
         return;
      }
      if (bloodfiendEntity == null) {
         attemptLocateBloodfiend();
         if (bloodfiendEntity == null) {
            return;
         }
      }
      List<ArmorStand> stands = mc.level.getEntitiesOfClass(
              ArmorStand.class,
              bloodfiendEntity.getBoundingBox().inflate(0.2D, 3.0D, 0.2D),
              stand -> true
      );
      ArmorStand candidateName = null;
      ArmorStand candidateTimer = null;
      boolean foundTwinclaws = false;
      for (ArmorStand stand : stands) {
         String tag = getCustomNameTag(stand);
         if (tag == null || tag.trim().isEmpty()) {
            continue;
         }
         String strippedTag = stripFormatting(tag);
         boolean potentialStand = isPotentialNameStand(stand);
         boolean bloodfiendName = isBloodfiendName(tag);
         Matcher twinclawsMatcher = TWINCLAWS_PATTERN.matcher(strippedTag);
         boolean hasTwinclaws = twinclawsMatcher.find();
         boolean isTimer = TIMER_PATTERN.matcher(tag).matches();
         if (!potentialStand && !bloodfiendName && !hasTwinclaws && !isTimer) {
            continue;
         }
         if (hasTwinclaws) {
            foundTwinclaws = true;
            twinclawsActive = true;
            try {
               twinclawsSeconds = Double.parseDouble(twinclawsMatcher.group(1));
            } catch (NumberFormatException ignored) {
               twinclawsSeconds = -1.0D;
            }
            logTwinclawsChange(true);
         }
         if (candidateName == null && bloodfiendName) {
            candidateName = stand;
            continue;
         }
         if (candidateTimer == null && isTimer) {
            candidateTimer = stand;
         }
      }
      if (candidateName != null) {
         nameEntity = candidateName;
         updateNameEntity(getCustomNameTag(candidateName));
      }
      if (candidateTimer != null) {
         timerEntity = candidateTimer;
         updateTimerEntity(getCustomNameTag(candidateTimer));
      }
      if (!foundTwinclaws && timerEntity == null) {
         twinclawsActive = false;
         twinclawsSeconds = -1.0D;
         logTwinclawsChange(false);
      }
   }

   private boolean isPotentialNameStand(ArmorStand stand) {
      if (!stand.hasCustomName() || !stand.isInvisible()) {
         return false;
      }
      for (EquipmentSlot slot : EquipmentSlot.values()) {
         ItemStack stack = stand.getItemBySlot(slot);
         if (!stack.isEmpty()) {
            return false;
         }
      }
      return true;
   }

   private boolean isBloodfiendName(String name) {
      if (name == null) {
         return false;
      }
      String stripped = stripFormatting(name);
      if (stripped != null && stripped.contains(BLOODFIEND_NAME)) {
         return true;
      }
      for (String start : NAME_START) {
         if (name.startsWith(start)) {
            return true;
         }
      }
      return false;
   }


   private void attemptLocateBloodfiend() {
      if (mc.level == null || mc.player == null) {
         return;
      }
      if (locateCooldown.isScheduled() && !locateCooldown.passed()) {
         return;
      }
      locateCooldown.schedule(LOCATE_BLOODFIEND_COOLDOWN_MS);
      ArmorStand spawnedByStand = findSpawnedByStand();
      if (spawnedByStand != null) {
         Player nearby = findBloodfiendNearStand(spawnedByStand);
         if (nearby != null) {
            bloodfiendEntity = nearby;
            scanTimer.reset();
            Logger.sendMessage("[Rift] Bloodfiend detected.");
            return;
         }
      }
      for (Player otherPlayer : mc.level.players()) {
         if (otherPlayer == mc.player) {
            continue;
         }
         String name = stripFormatting(otherPlayer.getName().getString());
         if (name == null || !name.contains(BLOODFIEND_NAME)) {
            continue;
         }
         bloodfiendEntity = otherPlayer;
         scanTimer.reset();
         Logger.sendMessage("[Rift] Bloodfiend detected.");
         return;
      }
   }

   private ArmorStand findSpawnedByStand() {
      if (mc.level == null || mc.player == null) {
         return null;
      }
      String playerName = getPlayerNameForSpawnedBy();
      if (playerName == null) {
         return null;
      }
      String expected = "Spawned by: " + playerName;
      String playerNameLower = playerName.toLowerCase();
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (!(entity instanceof ArmorStand stand)) {
            continue;
         }
         if (!stand.hasCustomName()) {
            continue;
         }
         String name = getStandName(stand);
         if (name == null) {
            continue;
         }
         String trimmed = name.trim();
         if (expected.equalsIgnoreCase(trimmed)) {
            return stand;
         }
         if (trimmed.toLowerCase().startsWith("spawned by:")) {
            String spawnedBy = trimmed.substring("spawned by:".length()).trim();
            if (!spawnedBy.isEmpty()
                        && (spawnedBy.equalsIgnoreCase(playerName)
                                    || spawnedBy.toLowerCase().contains(playerNameLower))) {
               return stand;
            }
         }
      }
      return null;
   }

   private String getStandName(ArmorStand stand) {
      String custom = getCustomNameTag(stand);
      if (custom != null && !custom.trim().isEmpty()) {
         return stripFormatting(custom);
      }
      return stand.getName() == null ? null : stripFormatting(stand.getName().getString());
   }

   private String getCustomNameTag(ArmorStand stand) {
      if (!stand.hasCustomName()) {
         return null;
      }
      if (stand.getCustomName() == null) {
         return null;
      }
      return stand.getCustomName().getString();
   }


   private Player findBloodfiendNearStand(ArmorStand stand) {
      if (mc.level == null) {
         return null;
      }
      for (Player otherPlayer : mc.level.players()) {
         if (otherPlayer == mc.player) {
            continue;
         }
         String name = stripFormatting(otherPlayer.getName().getString());
         if (name == null || !name.contains(BLOODFIEND_NAME)) {
            continue;
         }
         double distanceSq = stand.distanceToSqr(otherPlayer);
         if (distanceSq <= 9.0D) {
            return otherPlayer;
         }
      }
      return null;
   }

   private String getPlayerNameForSpawnedBy() {
      if (mc.player == null) {
         return null;
      }
      String username = mc.player.getName().getString();
      if (username != null && !username.trim().isEmpty()) {
         return username.trim();
      }
      String formatted = mc.player.getDisplayName().getString();
      String clean = stripFormatting(formatted);
      if (clean == null || clean.trim().isEmpty()) {
         return null;
      }
      String[] parts = clean.trim().split(" ");
      return parts.length == 0 ? null : parts[parts.length - 1];
   }

   private void updateNameEntity(String name) {
      stakeable = name != null && name.contains("\u0489");
      logStakeableChange(stakeable);
   }

   private void updateTimerEntity(String name) {
      if (name == null) {
         twinclawsActive = false;
         twinclawsSeconds = -1.0D;
         logTwinclawsChange(false);
         return;
      }
      Matcher matcher = TWINCLAWS_PATTERN.matcher(stripFormatting(name));
      if (matcher.find()) {
         twinclawsActive = true;
         try {
            twinclawsSeconds = Double.parseDouble(matcher.group(1));
         } catch (NumberFormatException ignored) {
            twinclawsSeconds = -1.0D;
         }
      } else {
         twinclawsActive = name.contains("TWINCLAWS");
         twinclawsSeconds = -1.0D;
      }
      logTwinclawsChange(twinclawsActive);
   }

   private void resetTracking() {
      bloodfiendEntity = null;
      nameEntity = null;
      timerEntity = null;
      stakeable = false;
      twinclawsActive = false;
      scanTimer.reset();
      locateCooldown.reset();
      healCooldown.reset();
      impelCooldown.reset();
      swapCooldown.reset();
      holyIceCooldown.reset();
      lastImpelAction = null;
      lastImpelActionTime = 0L;
      lastImpelSeconds = 0.0D;
      impelHandled = false;
      missingHolyIceLogged = false;
      missingStakeLogged = false;
      twinclawsSeconds = -1.0D;
   }

   private void attemptHealing() {
      if (healCooldown.isScheduled() && !healCooldown.passed()) {
         return;
      }
      float currentHealth = mc.player.getHealth();
      float maxHealth = mc.player.getMaxHealth();
      if (maxHealth <= 0 || currentHealth >= maxHealth) {
         return;
      }
      float missingHearts = (maxHealth - currentHealth) / 2.0F;
      float maxHearts = maxHealth / 2.0F;
      boolean panic = maxHearts >= PANIC_HEARTS * 2
                              && currentHealth <= maxHealth * 0.2F;

      HealingItem selected = null;
      int selectedSlot = -1;
      for (int i = HEALING_ITEMS.length - 1; i >= 0; i--) {
         HealingItem item = HEALING_ITEMS[i];
         int slot = getHotbarSlotOfItemId(item.id);
         if (slot == -1) {
            continue;
         }
         if (panic || missingHearts >= item.healHearts) {
            selected = item;
            selectedSlot = slot;
            break;
         }
      }

      if (selected == null || selectedSlot == -1) {
         return;
      }

      if (UseItemAbility.useItemAbility(selected.displayName, selectedSlot)) {
         Logger.sendMessage("[Rift] Used " + selected.displayName + ".");
         healCooldown.schedule(java.util.concurrent.ThreadLocalRandom.current()
                                       .nextInt(MIN_HEAL_DELAY_MS, MAX_HEAL_DELAY_MS + 1));
      }
   }

   private void attemptHolyIce() {
      if (!twinclawsActive) {
         return;
      }
      if (holyIceCooldown.isScheduled() && !holyIceCooldown.passed()) {
         return;
      }
      if (!twinclawsActive || twinclawsSeconds < 0 || twinclawsSeconds > HOLY_ICE_TRIGGER_SECONDS) {
         return;
      }
      if (!hasItemInInventory(HOLY_ICE_ID)) {
         if (!missingHolyIceLogged) {
            Logger.sendLog("[Rift] Holy Ice not found in inventory.");
            missingHolyIceLogged = true;
         }
         return;
      }
      int slot = getHotbarSlotOfItemId(HOLY_ICE_ID);
      if (slot == -1) {
         if (!missingHolyIceLogged) {
            Logger.sendLog("[Rift] Holy Ice not found in hotbar.");
            missingHolyIceLogged = true;
         }
         return;
      }
      if (UseItemAbility.useItemAbility("Holy Ice", slot)) {
         Logger.sendMessage("[Rift] Used Holy Ice.");
         holyIceCooldown.schedule(java.util.concurrent.ThreadLocalRandom.current()
                                          .nextInt(MIN_HOLY_ICE_DELAY_MS, MAX_HOLY_ICE_DELAY_MS + 1));
      }
   }

   private void attemptStakeSwap() {
      if (!stakeable) {
         return;
      }
      if (swapCooldown.isScheduled() && !swapCooldown.passed()) {
         return;
      }
      String stakeItem = getStakeableItem();
      if (stakeItem == null || stakeItem.trim().isEmpty()) {
         return;
      }
      if (!hasItemInInventory(stakeItem)) {
         if (!missingStakeLogged) {
            Logger.sendLog("[Rift] Steak Stake not found in inventory.");
            missingStakeLogged = true;
         }
         return;
      }
      int slot = getHotbarSlotOfItemId(stakeItem);
      if (slot == -1 || mc.player.getInventory().getSelectedSlot() == slot) {
         if (!missingStakeLogged && slot == -1) {
            Logger.sendLog("[Rift] Steak Stake not found in hotbar.");
            missingStakeLogged = true;
         }
         return;
      }
      mc.player.getInventory().setSelectedSlot(slot);
      Logger.sendMessage("[Rift] Swapped to " + stakeItem + ".");
      swapCooldown.schedule(250L);
   }

   private String getStakeableItem() {
      for (String id : SUPER_SHARP_STAKE_IDS) {
         if (hasItemInInventory(id)) {
            return id;
         }
      }
      if (hasItemInInventory(STEAK_STAKE_ID)) {
         return STEAK_STAKE_ID;
      }
      return null;
   }

   private boolean hasItemInInventory(String itemName) {
      if (itemName == null || itemName.trim().isEmpty()) {
         return false;
      }
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.isEmpty()) {
            continue;
         }
         String id = getItemId(stack);
         if (itemName.equals(id)) {
            return true;
         }
      }
      return false;
   }

   private void logStakeableChange(boolean value) {
      if (lastStakeable == value) {
         return;
      }
      lastStakeable = value;
      Logger.sendMessage(value ? "[Rift] Boss is stakeable." : "[Rift] Boss not stakeable.");
   }

   private void logTwinclawsChange(boolean value) {
      if (lastTwinclawsActive == value) {
         return;
      }
      lastTwinclawsActive = value;
      Logger.sendMessage(value ? "[Rift] Twinclaws active." : "[Rift] Twinclaws ended.");
   }

   private void performImpelAction(ImpelAction action) {
      if (impelCooldown.isScheduled() && !impelCooldown.passed()) {
         return;
      }
      int delay = java.util.concurrent.ThreadLocalRandom.current().nextInt(MIN_IMPEL_DELAY_MS, MAX_IMPEL_DELAY_MS + 1);
      impelCooldown.schedule(delay + 120L);
      VeinForge.executor().execute(() -> {
         try {
            Thread.sleep(delay);
         } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
         }
         mc.execute(() -> executeImpelAction(action));
      });
      impelHandled = true;
   }

   private void executeImpelAction(ImpelAction action) {
      switch (action) {
         case JUMP:
            tapKey(mc.options.keyJump);
            break;
         case SNEAK:
            tapKey(mc.options.keyShift);
            break;
         case CLICK_UP:
            setPitchAndClick(true);
            break;
         case CLICK_DOWN:
            setPitchAndClick(false);
            break;
         case CLICK:
            clickNow();
            break;
         default:
            break;
      }
   }

   private void setPitchAndClick(boolean up) {
      if (mc.player == null) {
         return;
      }
      float targetPitch;
      if (up) {
         targetPitch = java.util.concurrent.ThreadLocalRandom.current().nextFloat() * (MAX_UP_PITCH - MIN_UP_PITCH) + MIN_UP_PITCH;
      } else {
         targetPitch = java.util.concurrent.ThreadLocalRandom.current().nextFloat() * (MAX_DOWN_PITCH - MIN_DOWN_PITCH) + MIN_DOWN_PITCH;
      }
      mc.player.setXRot(Math.max(-90.0F, Math.min(90.0F, targetPitch)));
      int delay = java.util.concurrent.ThreadLocalRandom.current().nextInt(MIN_CLICK_AFTER_LOOK_MS, MAX_CLICK_AFTER_LOOK_MS + 1);
      VeinForge.executor().execute(() -> {
         try {
            Thread.sleep(delay);
         } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
         }
         mc.execute(this::clickNow);
      });
   }

   private void clickNow() {
      KeyBindUtil.resetRightClickDelayTimer();
      KeyBindUtil.rightClick();
   }

   private int getHotbarSlotOfItemId(String itemId) {
      if (itemId == null || itemId.trim().isEmpty()) {
         return -1;
      }
      for (int i = 0; i < 9; i++) {
         ItemStack slot = mc.player.getInventory().getItem(i);
         if (slot.isEmpty()) {
            continue;
         }
         String id = getItemId(slot);
         if (itemId.equals(id)) {
            return i;
         }
      }
      return -1;
   }

   private void tapKey(KeyMapping keyBinding) {
      int holdTime = java.util.concurrent.ThreadLocalRandom.current().nextInt(MIN_KEY_HOLD_MS, MAX_KEY_HOLD_MS + 1);
      KeyBindUtil.setKeyBindState(keyBinding, true);
      VeinForge.executor().execute(() -> {
         try {
            Thread.sleep(holdTime);
         } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
         }
         mc.execute(() -> KeyBindUtil.setKeyBindState(keyBinding, false));
      });
   }

   private String getItemId(ItemStack stack) {
      if (stack.isEmpty()) {
         return "";
      }
      return me.grish.veinforge.util.InventoryUtil.getItemId(stack);
   }

   private String stripFormatting(String text) {
      if (text == null) {
         return null;
      }
      return text.replaceAll("§.", "");
   }

   private ImpelAction parseImpelAction(String action) {
      if (action == null) {
         return null;
      }
      String normalized = action.toUpperCase().replaceAll("[^A-Z ]", " ").trim();
      if (normalized.isEmpty()) {
         return null;
      }
      normalized = normalized.replaceAll("\\s+", " ");
      if (normalized.contains("JUMP")) {
         return ImpelAction.JUMP;
      }
      if (normalized.contains("SNEAK") || normalized.contains("CROUCH")) {
         return ImpelAction.SNEAK;
      }
      if (normalized.contains("CLICK") && normalized.contains("UP")) {
         return ImpelAction.CLICK_UP;
      }
      if (normalized.contains("CLICK") && normalized.contains("DOWN")) {
         return ImpelAction.CLICK_DOWN;
      }
      if (normalized.contains("CLICK")) {
         return ImpelAction.CLICK;
      }
      return null;
   }

   private enum ImpelAction {
      JUMP("Jump"),
      SNEAK("Sneak"),
      CLICK_UP("Click Up"),
      CLICK_DOWN("Click Down"),
      CLICK("Click");

      private final String displayName;

      ImpelAction(String displayName) {
         this.displayName = displayName;
      }

      private String getDisplayName() {
         return displayName;
      }
   }

   private record HealingItem(String id, String displayName, int healHearts) {
   }
}
