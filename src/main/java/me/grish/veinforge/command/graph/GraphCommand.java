package me.grish.veinforge.command.graph;

import me.grish.veinforge.handler.GraphHandler;
import me.grish.veinforge.util.Logger;

import java.util.StringJoiner;

public class GraphCommand {

   private static final String FALLBACK_GRAPH = "Commission Macro";

   public void main() {
      header("Graph Editor Commands");
      item("/graph list", "Show available graph names.");
      item("/graph new <name>", "Create a new graph.");
      item("/graph edit [name]", "Toggle edit mode for a graph.");
      item("/graph save", "Save current graph immediately.");
      item("/graph keys", "Show keybinds with plain-language actions.");
      item("/graph tutorial", "Beginner step-by-step quickstart.");
      item("/graph debug [show <name>|list|off]", "Render-only graph overlay.");
   }

   public void list() {
      if (!GraphHandler.instance.hasGraphs()) {
         header("Graph List");
         line("No graphs loaded.");
         return;
      }

      StringJoiner joiner = new StringJoiner(", ");
      for (String name : GraphHandler.instance.getKnownGraphNames()) {
         if (name.equals(GraphHandler.instance.getActiveGraphKey())) {
            joiner.add(name + "*");
         } else {
            joiner.add(name);
         }
      }

      String suffix = GraphHandler.instance.isEditing() ? " (editing)" : " (idle)";
      header("Graph List");
      line(joiner + suffix);
   }

   public void keys() {
      header("Graph Keys");
      for (String hint : GraphHandler.instance.getEditorControlHelpLines()) {
         line(hint);
      }
      line("Colors: node=light green, selected=red, one-way=lime green, two-way=dark green.");
   }

   public void tutorial() {
      header("Graph Tutorial");
      step(1, "Words:");
      line("Node = one location.");
      line("Edge/Link = connection between two nodes.");
      line("One-way = A -> B only.");
      line("Two-way = A <-> B.");
      step(2, "Choose graph:");
      line("Use /graph list");
      line("Use /graph edit <name>");
      step(3, "Build links (while standing on blocks):");
      for (String hint : GraphHandler.instance.getEditorControlHelpLines()) {
         line(hint);
      }
      step(4, "Save + inspect:");
      line("Save now: /graph save");
      line("Render-only view: /graph debug show <name>");
      line("Stop debug render: /graph debug off");
   }

   public void debugMain() {
      header("Graph Debug Overlay");
      if (GraphHandler.instance.isDebugRenderEnabled()) {
         line("Status: ON (" + GraphHandler.instance.getDebugGraphKey() + ")");
      } else {
         line("Status: OFF");
      }
      line("Use /graph debug list | /graph debug show <name> | /graph debug off");
   }

   public void debugList() {
      list();
   }

   public void debugOff() {
      GraphHandler.instance.disableDebugRender();
      header("Graph Debug Overlay");
      line("Disabled.");
   }

   public void debug(final String graphName) {
      if (graphName == null || graphName.trim().isEmpty()) {
         debugMain();
         return;
      }

      String key = resolveGraphKey(graphName);
      if (!GraphHandler.instance.enableDebugRender(key)) {
         Logger.sendError("Unknown graph: " + key);
         header("Graph List");
         line(joinGraphNames());
         return;
      }

      header("Graph Debug Overlay");
      line("Enabled for: " + key);
      line("Render only. No movement/path actions are executed.");
   }

   public void edit() {
      String active = GraphHandler.instance.getActiveGraphKey();
      if (active == null || active.trim().isEmpty() || !GraphHandler.instance.hasGraph(active)) {
         active = FALLBACK_GRAPH;
      }
      toggle(active);
   }

   public void edit(final String graphName) {
      if (graphName == null || graphName.trim().isEmpty()) {
         edit();
         return;
      }
      toggle(resolveGraphKey(graphName));
   }

   public void create(final String graphName) {
      String key = normalizeName(graphName);
      if (key.isEmpty()) {
         Logger.sendError("Graph name cannot be empty.");
         return;
      }
      if (!GraphHandler.instance.createGraph(key)) {
         Logger.sendError("Graph already exists or name is invalid: " + key);
         return;
      }
      header("Graph");
      line("Created graph: " + key);
   }

   public void save() {
      GraphHandler.instance.saveNow();
   }

   private void toggle(String graphName) {
      String resolved = resolveGraphKey(graphName);
      if (!GraphHandler.instance.hasGraph(resolved)) {
         Logger.sendError("Unknown graph: " + graphName);
         header("Graph List");
         line(joinGraphNames());
         return;
      }
      GraphHandler.instance.toggleEdit(resolved);
   }

   private void header(String text) {
      Logger.addMessage("§6§l[Graph] §f" + text);
   }

   private void item(String command, String meaning) {
      Logger.addMessage("§8[Graph] §e" + command + " §8- §7" + meaning);
   }

   private void line(String text) {
      Logger.addMessage("§8[Graph] §7" + text);
   }

   private void step(int number, String text) {
      Logger.addMessage("§8[Graph] §b" + number + ") §f" + text);
   }

   private String joinGraphNames() {
      StringJoiner joiner = new StringJoiner(", ");
      for (String name : GraphHandler.instance.getKnownGraphNames()) {
         joiner.add(name);
      }
      return joiner.toString();
   }

   private String resolveGraphKey(String rawName) {
      String resolved = GraphHandler.instance.resolveGraphKey(rawName);
      return resolved == null ? normalizeName(rawName) : resolved;
   }

   private static String normalizeName(String rawName) {
      if (rawName == null) {
         return "";
      }
      return rawName.trim().replaceAll("\\s+", " ");
   }
}
