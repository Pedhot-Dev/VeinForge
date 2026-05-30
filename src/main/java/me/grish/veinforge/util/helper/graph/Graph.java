package me.grish.veinforge.util.helper.graph;

import com.google.gson.annotations.Expose;

import java.util.*;

public class Graph<T> {

   @Expose
   public final Map<T, Set<T>> map = new HashMap<>();

   public void add(T source) {
      map.computeIfAbsent(source, k -> new LinkedHashSet<>());
   }

   public void add(T source, T target, boolean bidi) {
      if (source == null || target == null) {
         throw new IllegalArgumentException("Nodes cannot be null");
      }

      map.computeIfAbsent(source, k -> new LinkedHashSet<>());
      map.computeIfAbsent(target, k -> new LinkedHashSet<>());

      if (!source.equals(target)) {
         map.get(source).add(target);
      }

      if (bidi && !source.equals(target)) {
         map.get(target).add(source);
      }
   }

   public void update(T old, T now) {
      if (old == null || now == null) {
         throw new IllegalArgumentException("Nodes cannot be null");
      }

      Set<T> edges = map.remove(old);
      if (edges != null) {
         map.put(now, edges);
      }

      for (Map.Entry<T, Set<T>> entry : map.entrySet()) {
         Set<T> updatedEdges = new LinkedHashSet<>();
         for (T edge : entry.getValue()) {
            if (edge.equals(old)) {
               updatedEdges.add(now);
            } else {
               updatedEdges.add(edge);
            }
         }
         map.put(entry.getKey(), updatedEdges);
      }
   }

   public void remove(T node) {
      if (map.remove(node) == null) {
         return;
      }

      for (T key : map.keySet()) {
         map.get(key).removeIf(edge -> edge.equals(node));
      }
   }

   // Breadth First Search - Our graph is small enough so it finds path instantly. not worth using a star and wasting so much memory for that
   public List<T> findPath(T start, T end) {

      if (start == null || end == null || !map.containsKey(start) || !map.containsKey(end)) {
         return new ArrayList<>();
      }

      Queue<T> queue = new LinkedList<>();
      Set<T> visited = new HashSet<>();
      HashMap<T, T> parent = new HashMap();

      queue.add(start);
      visited.add(start);
      parent.put(start, null);

      while (!queue.isEmpty()) {
         T curr = queue.poll();
         if (curr.equals(end)) {
            LinkedList<T> path = new LinkedList<>();
            for (T at = end; at != null; at = parent.get(at)) {
               path.addFirst(at);
            }
            return path;
         }

         for (T neighbour : map.get(curr)) {
            if (!visited.contains(neighbour)) {
               queue.offer(neighbour);
               visited.add(neighbour);
               parent.put(neighbour, curr);
            }
         }
      }
      return new ArrayList<>();
   }
}
