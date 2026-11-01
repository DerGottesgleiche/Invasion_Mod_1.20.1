# Invasion

*So you think your base is tough, do you?*

A port of the Invasion mod (originally for Minecraft 1.7.10 by Lieu, Elsee and UnstoppableN; 1.12.2 port by
Tobias Romke and Fabian During) to **Minecraft 26.3** with **NeoForge 26.3.0.99**.

Build a Nexus, feed it a catalyst, and defend it against waves of invaders that dig through your walls, climb
your ladders and throw boulders at your base.

## Installing

1. Install NeoForge for Minecraft 26.3 (26.3.0.99 or newer) with the NeoForge installer.
2. Put `invasion-26.3-2.0.0.jar` into the `mods` folder of that installation.
3. Start the game with the NeoForge profile. Minecraft 26.3 needs Java 25 (the launcher brings its own).

## Playing

| Step | How |
|---|---|
| Phase Crystal | Diamond in the centre, lapis above and below, redstone left and right (or rotated) |
| Nexus | Phase Crystal on top, obsidian in the centre, redstone left, right and below |
| Catalyst Mixture | Bone, redstone, rotten flesh in a row, a bowl below |
| Nexus Catalyst | Smelt a Catalyst Mixture |

Put a **Nexus Catalyst** into the left slot of the Nexus. After 10 seconds the Nexus activates and the first
wave comes. Every player within range is bound to the Nexus: if it is destroyed, they die with it (this rule can
be switched off in the config). Each wave ends after its timer runs out; the next one starts after a short break.
Waves 1-11 are the original hand-made waves, every wave after that scales up.

- **Strong Nexus Catalyst** (smelted from a Strong Catalyst Mixture, made with a redstone block) starts at wave 10.
- **Stable Nexus Catalyst** (bone, coal, bone, bowl below; smelted) starts *continuous mode*: the Nexus grows in
  power over time and is attacked every few nights. Damping Agents slow or reverse the power growth.
- While an invasion runs, the Nexus slowly produces **Rift Flux** in the right slot. Rift Flux crafts into arrows,
  gunpowder, diamonds, iron, redstone, lapis and string, and into the special items below.
- An **Empty Trap** placed in the Nexus is turned into a **Rift Trap**.

### Invaders

| Mob | What it does |
|---|---|
| Zombie (tiers 1-3) | Digs through blocks towards the Nexus. Variants carry swords, armour or axes; the tar zombie bursts into flames; the tier 3 brute is big, tough and knocks you away |
| Zombie Pigman (tiers 1-3) | Fire immune digger |
| Skeleton | Shoots players on sight |
| Spider | Climbs walls. Jumping spiders pounce, mother spiders hatch babies |
| Creeper | Walks to the Nexus and blows up next to it, or blows open the wall it is stuck at |
| Pigman Engineer | Builds ladders up walls, ladder towers under overhangs, bridges over gaps and scaffolds up to a high Nexus; also digs |
| Thrower | Big and slow, throws boulders (tier 1) or lit TNT (tier 2) at players and at the walls in its way |
| Imp | Small, fast, sets you on fire |

How hard a block is for invaders is shown by the **Material Probe**: explosion resistance, plus 10% for every
neighbour of the same block. Obsidian and stronger, ladders and chests cannot be dug. With the game rule
`mob_griefing` off, invaders cannot dig at all.

### Items

| Item | Use |
|---|---|
| Nexus Adjuster | Right-click the Nexus to change its spawn range (sneak to lower it) |
| Material Probe | Right-click a block to see its strength |
| Infused Sword | Diamond-strength sword. Hits charge it; when full, right-click heals 3 hearts (sneak: restores hunger) |
| Searing Bow | Shoots burning arrows that hit harder |
| Strange Bone | Give it to a tamed wolf near a Nexus to turn it into a Nexus guardian |
| Traps | Placed on the ground. Rift traps hurt and stun, flame traps set fire, poison traps poison. Right-click to pick up |

### Commands (operators)

`/invasion begin [wave]`, `/invasion continuous`, `/invasion end`, `/invasion status`, `/invasion range [radius]`
act on the nearest Nexus within 16 blocks.

## Differences from the original

This is a rewrite on today's Minecraft, not a line-by-line port. The Nexus, the wave definitions, the spawner,
the items and the recipes follow the original closely; these parts were changed:

- The invader pathfinding follows the original design: an A* search over nodes with actions (walk,
  climb, swim, dig, ladder, ladder tower, bridge, scaffold). Digging costs the time it takes, so invaders tunnel
  through the weakest part of a wall; crowded spots cost more, so the horde spreads out. Pigman engineers
  build ladders, ladder towers under overhangs and bridges, and the Nexus' attacker AI plans scaffolds when
  there is no other way up. Invaders without a way to the Nexus wait with an engineer and speed up its
  building. Fighting players still uses Minecraft's own pathfinding.
- Traps are flat blocks instead of entities.
- Boulders look like cobblestone; mother spiders hatch babies directly instead of laying egg entities; the
  Nexus zaps leftover mobs with (harmless) lightning instead of its own bolt entity.
- Spawn eggs use the vanilla egg of the closest mob.
- Not ported: the vulture/bird and the burrower (they were debug-only or not used by any wave), the night
  spawns (already disabled in the 1.12.2 port), the debug wand, Waila support and the update checker.
- Config: `config/mod_invasion-local.toml` (drop dug blocks, bound players dying, days between attacks in
  continuous mode, mob sight and sense range).

## Status

- Builds against Minecraft 26.3 / NeoForge 26.3.0.99.
- Twelve automated game tests pass on a headless server. They show that:
  - a wave spawns invaders;
  - invaders spawned 24 blocks away walk in and hit the Nexus;
  - a zombie on open ground walks to the Nexus and hits it;
  - a zombie digs through a cobblestone wall to the Nexus;
  - a zombie facing a stone wall with one dirt column tunnels through the dirt and leaves the stone alone;
  - a pigman engineer builds a ladder, climbs a 3-high wall and hits the Nexus on top;
  - a zombie that cannot dig climbs the engineer's ladder;
  - an engineer builds a ladder tower under a platform 4 blocks up;
  - for a platform 8 blocks up, the attacker AI plans a scaffold and the engineer builds it and reaches the Nexus;
  - on a tower whose top overhangs (the situation from a real game), the engineer bridges and builds a ladder
    tower around the overhang and both it and a zombie reach the Nexus;
  - the data files (recipes, loot tables) load without errors.
- **Not yet checked in a running game client**: models, textures, the Nexus screen and sounds have not been
  looked at on screen. Please report anything that renders wrong.

## Building from source

```bash
./gradlew build
```

The jar ends up in `build/libs/`. `./gradlew runClient` starts a development client,
`./gradlew runGameTestServer` runs the automated tests.
