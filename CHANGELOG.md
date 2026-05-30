# VeinForge Changelog

## v1.0.0-private (2026-05-30)

### Features
- Began Fabric 1.21.10 migration by updating build configuration and metadata.
- Ported BlockUtil to Fabric/Yarn types (MinecraftClient, BlockPos, Direction, Vec3d, BlockState) and restored visibility/side‑point logic.
- CommissionDebugMode mining simulation is back using the ported BlockUtil (debug candidates + cost logic).
- Added feature render dispatch in EventManager so feature HUD/world rendering actually runs under Fabric.
- Added RenderUtil.parseColor(...) for config string colors.
- Added tablist widget parsing (SkyBlock "Info"/"Commissions") and updated location/commission parsing to prefer widgets when available.
- Updated location coverage for newer SkyBlock areas by adding `Backwater Bayou`/`Galatea` sub-location entries, plus location alias handling for `Glacite Mineshaft(s)`.
- Merged Dwarven + Glacial config tabs into a single `Commission` category with collapsible sub-sections, including legacy config value migration from `glacialCommission`.
- Added a new `Galatea Fishing` config category with shared General Fishing settings, Galatea-specific kill mode (Melee/Slayer Weapon), Strider scan thresholds (radius/min/max), axe/secondary-weapon fields, and item binding actions (`/vf set fishing-rod`, `/vf set fishing-axe`, `/vf set fishing-weapon`).
- Added `Galatea Fishing` macro selection with default graph bootstrap (`Galatea Fishing.json`) and active-graph switching on macro enable.
- Added Galatea Fishing stage-1 flow: auto-warp to Galatea (`/warp galatea`) before pathfinding, plus `/debug fishing-stage` snapshot output (graph, coordinates, yaw/pitch, nearest node).
- Added Galatea Fishing divergence pathing stage with two hardcoded South Reaches targets, random branch selection, graph-based navigation, and target look-angle alignment on arrival.
- Added chat packet extraction + dispatch so failsafes/macros/features can react to chat messages.
- Added debug helpers: `/debug scoreboard dump` (raw sidebar dump to console) and `/debug commission` (commission/widget inspection).
- Added `/debug nbt` to dump full held-item components/custom data to console logs for copy/paste debugging.
- Added `tools/graph_visualizer.py` to inspect graph stats and render graph layouts before route/handler changes.
- Added `tools/graph_gui.py`, a Tkinter graph explorer with multi-graph selection and Dijkstra shortest-path visualization.
- Added `/graph debug` overlay mode (`/graph debug <name>`, `/graph debug list`, `/graph debug off`) with graph-name autocomplete for render-only inspection.
- Added `/graph tutorial` with plain-language graph terms and a step-by-step first-time editing walkthrough.
- Added `General -> Ignore Fall Damage In Pathfinding` to allow risky drop routes, and tied fall-death warning logic to this setting.
- Added pathfinding telemetry instrumentation (search/smoothing timing, expanded nodes, open-set peak, path lengths, and failure reason) with `/debug path stats` and a new optional Pathfinder Stats HUD.
- Added full Graph Editor Etherwarp node support with dedicated keybinds (place/select WALK or ETHERWARP nodes), updated controls/help text, and robust same-block multi-transport node selection.
- Added `/rb new <name>` and `/graph new <name>` creation commands, plus route command autocomplete for names and waypoint types.
- Introduced graph schema V2 with validation, normalization, and a Python conversion tool.
- Added graph editing command with edit/save actions for managing route graphs.
- Migrated from manual builds to automated GitHub Releases via tags.
- Added `release.yml` workflow for automated artifact generation.
- Added Sunflower Oil fuel and improved anticheat prediction.
- Smart waypoint selection for commissions.
- Support for overlapping commissions.
- Added configurable 'Max Safe Fall Blocks' for pathfinding.
- Enhanced CommissionDebugMode with detailed mining simulation.
- Improved GitHub integration with workflows, templates, and comprehensive documentation.

### Improvements
- RouteNavigator and RouteMiner MovingState now use BlockUtil.getClosestVisibleSidePos(...) / bestPointsOnBestSide(...) again.
- Route navigation/render now uses Vec3d/BlockPos and Fabric render callbacks, with rotation targets based on block centers instead of BlockUtil side probing (to avoid old Forge dependencies).
- Commission debug HUD/render now uses Fabric DrawContext/WorldRenderContext, modern player/world access, and a safe color parser instead of missing toJavaColor.
- RouteMiner MovingState now uses Vec3d directly and no longer depends on BlockUtil point sampling.
- Improved `tools/graph_gui.py` UI/UX with themed layout, graph/node search filters, path table navigation, and canvas grid/legend/hover feedback.
- Improved in-game route/graph editing UX with clearer keybind descriptions, `/rb keys` and `/graph keys` help, `/graph list`, and HUD control hints showing actual bound keys.
- Improved graph editor rendering with clearer node colors, highlighted in/out connections, and arrowheads for selected-node direction.
- Updated graph render palette and visibility: light-green nodes, red selected node, lime one-way links, and dark-green two-way links.
- Simplified graph edge rendering to a single thin overlay line (plus slight height lift) to remove side-view triple-line artifacts.
- Improved `/graph` command chat readability with cleaner sectioned output formatting.
- Updated thin-line world rendering to use a reliable see-through fallback, and switched pathfinder route lines to thin see-through rendering.
- Improved graph route selection to choose the best entry waypoint by total travel cost (player-to-start + start-to-goal) and use weighted shortest paths instead of unweighted hop count.
- Improved PathExecutor jump decisions for slabs/stairs by using directional surface-height checks (top/bottom slab and stair orientation) instead of raw block-Y delta.
- Improved PathExecutor jump triggering to be route-target-aware, with stricter jump-arc clearance checks and faster jump key timing for one-block ascents.
- Improved HUD fitting globally: anchored HUD elements now clamp to screen bounds, and Text HUD panels auto-scale/fit to viewport width/height to avoid off-screen clipping.
- Rewrote `canWalkBetween` walkability checks to use sampled segment validation (standability, stair/slab height deltas, and diagonal corner clearance), and switched path smoothing to use the improved check.
- Reduced pathing snap in PathExecutor by blending lookahead yaw targets, using wrap-safe yaw-difference checks, and adding obstacle-aware strafe bias when forward movement is blocked.
- Improved AutoMobKiller slayer behavior with Goblin/Glacite zone-aware targeting and fallback movement toward search anchors when no mobs are found.
- Tuned AutoMobKiller combat pacing with faster line-of-sight retargeting, quicker chase repaths, and relaxed slayer crowding filters in busy lobbies.
- Further tuned AutoMobKiller target switching to repath more aggressively and abandon stalled targets sooner during commission mob hunts.
- Improved Goblin Slayer target chaining to prefer nearest goblins consistently and avoid drifting to far targets after each kill.
- Switched commission selection to static score-based prioritization (Titanium 5, Mithril 10, Mithril Miner 15, Glacite 20, Goblin/Mines Slayer 30, Treasure Hoarder 50).
- Added progress-weighted commission selection so higher completion percent can beat lower-progress alternatives with similar base score.
- Reworked commission completion handling: chat completion now triggers immediate claim flow, while tablist remains secondary verification/fallback.
- Commission Macro now enters claim flow immediately on commission-complete chat and uses faster commission-claim GUI timings (open/click/close) to resume mining sooner.
- Hardened commission tablist claim detection to accept `Done`/`Completed`/`Complete` variants (and checkmark lines), preventing post-completion pathing loops.
- Added GUI-first post-claim target selection with pending tablist validation and automatic path restart when commission target changes mid-navigation.
- Commission completion chat now immediately enters claim routing, with a short tablist guard to ignore stale non-claim updates during initial claim handoff.
- Hardened waypoint route loading/selection: supports legacy `routes.json` layout fallback, always restores a valid `Default` route, and rebinds selected route after load to avoid stale/empty references.
- Improved Route Miner and `/rb` diagnostics to show waypoint counts and clearer guidance when the selected route is empty.
- Added a movable `Fishing HUD` panel with live Galatea fishing state, Strider counters, trigger range, cap-kill status, and attack/cast timing details.
- Improved Galatea fishing safety/consistency with small combat micro-aim adjustments, a fixed 5-block fishing anchor radius gate (auto-repath when outside), and warp-state guards that avoid auto-warp commands when not connected to Hypixel.
- Improved Galatea Fishing strider clear flow: faster return to fishing when no reachable targets remain, forced fishing yaw/pitch reset before recast, Melee-mode 4-block axe-only cap kill, and Slayer-mode repeated weapon-assist + axe cycles with an extended cap-kill reach.
- Updated Galatea Fishing control flow to remove macro-local GUI/chat/unexpected-movement safety stops, tighten micro-adjust aiming around live targets, force 20-40s no-catch recasts at the hardcoded fishing yaw/pitch, and use direct Pathfinder routing for better center-snap consistency.
- Refined commission route graphs with updated waypoint coordinates and connectivity.
- Refined commission HUD layout with status/progress display.
- Added forward-movement collision checks to path executor.
- Adjusted Gradle JVM settings (heap size/encoding) and disabled daemon.
- Added send-packet hook and gated disable stack traces behind debug mode.
- Updated pathing to use keybind-based movement, yaw-based jump decisions, and disabled strafe util during execution.
- Extended pathfinder timers while active and guarded against long-running jobs.

### Fixes
- Fixed SkyBlock location detection guard to avoid enum-ordinal breakage when new `Location` entries are added.
- Removed Brigadier command ambiguity warnings by making debug selectors explicit (`/debug slayer start <mob>`, `/debug path location <name>`, `/graph debug show <name>`).
- Normalized legacy `r:g:b[:a]` config colours to `speed:alpha:r:g:b` to prevent MoulConfig colour editor crashes.
- Updated mining block priority selection to key on `Block`/`BlockState` (avoids unstable raw state IDs).
- Simplified fall-damage pathfinding config to a single `Ignore Fall Damage In Pathfinding` toggle and removed the separate safe-fall slider.
- Added one-shot stuck recovery in PathExecutor: attempt a recovery jump after 1s of no movement, then fail only if still stuck after a short recovery window.
- Fixed A* failure handling to never return partial/closest-node paths when the goal is unreached, preventing false pathfinding success on truncated searches.
- Fixed route and graph command name parsing so selection/edit/delete handle extra spaces reliably (trim + whitespace normalization + tolerant existing-name resolution).
- Fixed pathfinder failure-stop threading by dispatching worker-thread stop requests to the client thread, preventing RenderSystem wrong-thread crashes during chat/log updates.
- Hardened in-game logger and pathfinder stop flow to defer all chat writes and enforce client-thread stop execution, preventing render-pass chat crashes on failure logs.
- Fixed commission mining overlap handling so active titanium commissions are prioritized even when mithril and titanium share the same location.
- Fixed commission auto-claim reliability: prevent duplicate HUD/XP counting for the same claim card and require multiple empty checks before ending claim mode so back-to-back completed commissions are both claimed.
- Fixed The Forge commission waypoint constant to match the Commission Macro graph node coordinates.
- Fixed RouteBuilder input handling so the configured toggle key (default `]`) works again, and added `/rb toggle` as a keybind-independent fallback.
- Fixed RouteBuilder add feedback so "Added Walk/Etherwarp" is only shown when a waypoint is actually inserted (no false success message when editing `Default`).
- Fixed RouteBuilder world rendering regression where newly added route waypoints were not drawn in editor mode.
- Fixed RouteBuilder overlay visibility gating so route lines render only while RouteBuilder is enabled.
- Fixed RouteBuilder HUD visibility so it now appears during Route Editor mode (not only Graph Editor), with route-specific status and keybind hints.
- Fixed Galatea Fishing combat lock jitter by using a stable target body offset (no random Y re-aim), reducing vertical up/down oscillation while keeping fast re-aim and 100-200ms combat click pacing.
- Fixed Galatea Fishing cap-kill re-arming so melee mode does not get stuck idle at/above Strider max count after one clear cycle.
- Increased AutoMobKiller target re-aim interval from 90ms to 240ms to reduce aggressive camera retarget jitter.
- Updated pathfinding door and fence gate passability to respect open state.
- Fixed commission warp behavior and pathing when the commission list is empty.
- Fixed commission pathing and HOTM XP parsing issues (see #23).
- Added pathfinding timeout handling to abort on failure and report TIME_FAIL.
- Prevented commission HUD from showing a recent completion on macro enable.
- Pathfinding Loop Caused by High-Speed Overshooting of Target Nodes.
- Corrected commission HUD timing and improved HUD/pathing behavior.
- Fixed pathing loop at Lava Springs: Bot pathfinds to current location instead of mining.
- Removed deprecated `build.yml` workflow.
- Updated labeler.yml to v5 syntax.
- Fixed BlockMiner crash when pickaxe ability messages are missing.
- Fixed commission waypoint selection logic to use client player position.
- Fixed potential fall damage issues with high safety limits.

### Refactor
- Centralized mining logic in BlockUtil.
- Improved commission parsing and waypoint selection logic using Java Streams.

## v2.7.1 (2025-1-7)

## Changes
- New name-mention failsafe
- New option to auto warp to forge in Commission Macro
- Will no longer start Dwarven Commissions if the slayer weapon has not been set in the config
- Will warp to a new mining lobby if the pathfinding fails
- Changed the error message getMiningStats throws

## Fixes
- Duplicate mining tools causing ItemChangeFailsafe to trigger
- Pickaxe ability issue
- Issues with macro not mining after tabbing out

## v2.7.0 (2025-18-6)

This update marks the first stable update of VeinForge v2.7.0 

## Current Macros Included
- Commission Macro
- Powder Macro

## Changes
- Auto Refuel is now usable
- Fixed lag issues
- Fixed personal compactors not working
- Fixed accidentally clicking on players
- Fixed calling royal pigeons twice
- Detecting item names properly
- Added support for all pickaxe abilities
- Added options to disable using pickaxe ability
- New commission HUD and configs
- Enhanced mining algorithm

## v2.7.0-alpha (2025-5-1)

This update marks the official resumption of development on Mighty Miner.

## Current Macros Included
- Commission Macro
- Powder Macro
- Glacial Macro
- Mining Macro
- Route Mining Macro

## Changes
- Completely rewritten Mining Macro for improved efficiency and maintainability.
- Block Miner has been overhauled with cleaner structure and better logic separation.
- Automatically disable macro when there is an error

## Updated Coding Standards
- Avoid deeply nested switch statements when possible. Implement PROPER state machine patterns (see BlockMiner.java for a reference implementation).
- Add Javadocs and inline comments, especially for abstract classes. (Tip: ChatGPT can assist with generating documentation.)
- Standardize logging and error handling across the project. For example, all errors should be routed through the main macro class for consistency.
- Disable macro automatically when there is an error
