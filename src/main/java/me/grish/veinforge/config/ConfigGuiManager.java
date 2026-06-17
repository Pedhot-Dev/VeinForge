package me.grish.veinforge.config;

import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import me.grish.veinforge.VeinForgeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigGuiManager {
   private static final Component CONFIG_TITLE = Component.literal("VeinForge Settings");
   public static MoulConfigEditor<VeinForgeConfig> editor = null;

   public static void openConfigGui(String search) {
      Minecraft client = Minecraft.getInstance();
      client.gui.setScreen(createConfigScreen(client.gui.screen(), search));
   }

   public static MoulConfigScreenComponent createConfigScreen(Screen parent) {
      return createConfigScreen(parent, null);
   }

   public static MoulConfigScreenComponent createConfigScreen(Screen parent, String search) {
      ensureEditor();
      if (search != null) editor.search(search);
      MoulConfigScreenComponent screen = new MoulConfigScreenComponent(
              CONFIG_TITLE,
              new GuiContext(new GuiElementComponent(editor)),
              parent
      ) {
         @Override
         public void onClose() {
            super.onClose();
            VeinForgeClient.configManager.saveConfig();
         }

         @Override
         public void removed() {
            super.removed();
            VeinForgeClient.configManager.saveConfig();
         }
      };
      return screen;
   }

   private static void ensureEditor() {
      if (editor == null) {
         editor = new MoulConfigEditor<>(
                 VeinForgeClient.configManager.processor
         );
      }
   }
}
