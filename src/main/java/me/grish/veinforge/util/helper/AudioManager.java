package me.grish.veinforge.util.helper;


import lombok.Getter;
import lombok.Setter;
import me.grish.veinforge.VeinForge;
import me.grish.veinforge.util.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import javax.sound.sampled.*;
import java.io.File;

public class AudioManager {

   private static volatile AudioManager instance;
   private static Clip clip;
   private final Minecraft mc = Minecraft.getInstance();
   private final Clock delayBetweenPings = new Clock();
   @Getter
   @Setter
   private boolean minecraftSoundEnabled = false;
   private int numSounds = 15;
   @Setter
   private float soundBeforeChange = 0;

   private AudioManager() {
      // Private constructor for Singleton
   }

   public static AudioManager getInstance() {
      if (instance == null) {
         synchronized (AudioManager.class) {
            if (instance == null) {
               instance = new AudioManager();
            }
         }
      }
      return instance;
   }


   public void resetSound() {
      if (clip != null && clip.isRunning()) {
         clip.stop();
         clip.close();
      }
      minecraftSoundEnabled = false;
      if (VeinForge.config().failsafe.maxOutMinecraftSounds) {
         mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set((double) soundBeforeChange);
      }
   }

   public void playSound() {
      if (VeinForge.config().failsafe.failsafeSoundType == 0) {
         if (minecraftSoundEnabled) return;
         startMinecraftSound();
      } else {
         playCustomSound();
      }
   }


   private void startMinecraftSound() {
      numSounds = 15;
      minecraftSoundEnabled = true;
      if (VeinForge.config().failsafe.maxOutMinecraftSounds) {
         mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1.0);
      }
   }


   private void playCustomSound() {
      VeinForge.executor().execute(() -> {
         try {
            AudioInputStream inputStream = getAudioStreamForSelectedSound();
            if (inputStream == null) {
               Logger.sendError("[Audio Manager] Failed to load sound file!");
               return;
            }

            clip = AudioSystem.getClip();
            clip.open(inputStream);
            setSoundVolume(VeinForge.config().failsafe.failsafeSoundVolume);
            clip.start();

            clip.addLineListener(event -> {
               if (event.getType() == LineEvent.Type.STOP) {
                  try {
                     clip.close();
                  } catch (Exception ignored) {
                  }
               }
            });
         } catch (Exception e) {
            Logger.sendError("[Audio Manager] Error playing sound: " + e.getMessage());
         }
      });
   }

   private AudioInputStream getAudioStreamForSelectedSound() throws Exception {
      switch (VeinForge.config().failsafe.failsafeSoundSelected) {
         case 0: // Custom sound file
            File audioFile = new File(mc.gameDirectory.getAbsolutePath() + "/veinforge_sound.wav");
            if (audioFile.exists() && audioFile.isFile()) {
               return AudioSystem.getAudioInputStream(audioFile);
            }
            break;
         case 1:
            return AudioSystem.getAudioInputStream(getClass().getResource("/veinforge/sounds/staff_check_voice_notification.wav"));
         case 2:
            return AudioSystem.getAudioInputStream(getClass().getResource("/veinforge/sounds/metal_pipe.wav"));
         case 3:
            return AudioSystem.getAudioInputStream(getClass().getResource("/veinforge/sounds/AAAAAAAAAA.wav"));
         case 4:
            return AudioSystem.getAudioInputStream(getClass().getResource("/veinforge/sounds/loud_buzz.wav"));
         default:
            Logger.sendError("[Audio Manager] Invalid sound selection!");
      }
      return null;
   }

   private void setSoundVolume(float volumePercentage) {
      if (clip == null) return;
      FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
      float dB = (float) (Math.log(volumePercentage / 100f) / Math.log(10.0) * 20.0);
      volume.setValue(dB);
   }

   public boolean isSoundPlaying() {
      return (clip != null && clip.isRunning()) || minecraftSoundEnabled;
   }

   public void onTick() {
      if (mc.player == null || mc.level == null) return;
      if (VeinForge.config().failsafe.failsafeSoundType != 0 || !minecraftSoundEnabled) return;
      handleMinecraftSoundTick();
   }

   private void handleMinecraftSoundTick() {
      if (delayBetweenPings.isScheduled() && !delayBetweenPings.passed()) return;

      if (numSounds <= 0) {
         resetSound();
         return;
      }

      net.minecraft.sounds.SoundEvent event = VeinForge.config().failsafe.failsafeMcSoundSelected == 0 ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.ANVIL_LAND;
      mc.level.playSound(mc.player, mc.player.getX(), mc.player.getY(), mc.player.getZ(), event, SoundSource.PLAYERS, 10.0F, 1.0F);

      delayBetweenPings.schedule(100);
      numSounds--;
   }
}
