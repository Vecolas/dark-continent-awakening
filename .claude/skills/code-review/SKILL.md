---
name: code-review
description: Review checklist for this NeoForge 1.21.1 Nen mod. Use when reviewing a diff, pull request, implementation, refactor, networking change, persistence change, or gameplay system before merge.
---

# Code Review

Review for correctness first, then architecture, security, compatibility, performance, lore consistency, and style.

Do not spend most of the review on formatting while missing server-authority or persistence bugs.

## Review procedure

1. Read the task/PR intent.
2. Inspect the full diff.
3. Identify affected subsystems.
4. Trace data ownership and lifecycle.
5. Check client/server boundaries.
6. Check persistence and death/logout behavior.
7. Check security/packet validation.
8. Check performance/tick behavior.
9. Check Nen lore invariants where relevant.
10. Check tests.
11. Build/run the relevant validation if possible.

Rank findings by severity and explain concrete failure modes.

## Severity model

### Critical

Examples:

- remote client can set authoritative aura/damage/progression;
- dedicated server crashes from client-only class loading;
- world/player save corruption;
- duplication/exploit with severe impact;
- infinite loop or catastrophic per-tick work.

### High

Examples:

- Nen data resets unexpectedly on death/relog;
- packets allow bypassing cooldown/conditions;
- large multiplayer desync;
- ability can target through invalid distance/dimension;
- En performs huge world scans every tick.

### Medium

Examples:

- incorrect affinity calculation;
- technique state combination contradicts design;
- inconsistent UI sync;
- unnecessary coupling that will block future Hatsu;
- missing edge-case tests.

### Low

Examples:

- naming clarity;
- duplicated helper code;
- documentation gaps;
- minor UX inconsistency.

## NeoForge 1.21.1 checks

Look for:

- use of APIs from wrong Minecraft/NeoForge versions;
- wrong event bus;
- unsafe static initialization;
- incorrect `DeferredRegister` lifecycle;
- client-only imports reachable on dedicated server;
- outdated Forge Capability patterns where 1.21.1 Data Attachments/Data Components are more appropriate;
- attachment data assumed to sync automatically;
- missing `PlayerEvent.Clone`/`copyOnDeath` decision;
- persistent mutable data changed without proper dirty/save semantics;
- incorrect payload codecs/registration;
- network handlers mutating game state off the appropriate thread.

## Network/security checks

For every serverbound packet ask:

- can a modified client spam it?
- can it provide arbitrary target/entity IDs?
- is distance checked?
- is dimension checked?
- is ownership checked?
- is the technique learned?
- is the user awakened?
- is aura sufficient?
- is cooldown complete?
- are conditions satisfied?
- is current Nen state compatible?
- can NaN, negative, overflow, or absurd values enter calculations?

The packet should represent intent rather than trusted outcome.

## Persistence checks

For every persistent field ask:

- who owns it?
- is it serialized?
- is the codec version-tolerant enough for expected changes?
- what happens on death?
- what happens when returning from the End?
- what happens on logout mid-ability?
- what happens after server restart?
- is runtime cache accidentally serialized?

## Nen architecture checks

When reviewing Nen code, also apply `../nen-architecture/SKILL.md` and `../nen-lore-rules/SKILL.md`.

Flag designs where:

- aura capacity, remaining aura, and output are collapsed into one stat when the mechanic requires them separately;
- a second stamina bar duplicates aura;
- Zetsu gives stealth without vulnerability;
- Ko keeps full-body defense;
- Ryu is implemented as a passive damage buff instead of redistribution;
- In has no Gyo/detection counterplay;
- En has no upkeep or performance safeguards;
- arbitrary powers are labeled Specialization solely to avoid category design;
- severe condition bonuses can be gained through trivial restrictions;
- off-category abilities ignore affinity/proficiency entirely.

## Combat calculation checks

Look for:

- double application of modifiers;
- mismatch between displayed and server damage;
- damage recursion through events;
- bypassing invulnerability/armor unintentionally;
- integer overflow/precision loss;
- division by zero;
- negative aura costs or healing through malformed values;
- order-of-operations bugs in percentage modifiers;
- local aura allocation not reflected in defense.

## Performance checks

Search specifically for code that runs every tick.

Estimate cost as:

```text
cost per operation × players × entities × ticks/second
```

Flag:

- dimension-wide scans;
- block iteration over large volumes;
- repeated registry lookups that can be cached;
- repeated JSON/codec serialization;
- packet broadcast every tick without dirty checks;
- creation of large lists/maps each tick;
- synchronous file IO;
- expensive pathfinding/spatial logic tied to aura sensing.

## Modpack compatibility checks

Prefer cooperative hooks/events/tags.

Flag:

- overwriting vanilla behavior when an event exists;
- replacing loot tables rather than stacking modifiers when interoperability matters;
- assuming a specific third-party mod is always present without a load condition;
- hardcoded item IDs when tags/data maps are intended;
- mixins/core modifications for problems solvable with supported NeoForge extension points.

## Test review

A feature should include tests proportional to its risk.

At minimum ask whether there are tests for:

- normal path;
- insufficient aura;
- invalid state;
- death/respawn;
- save/reload;
- multiplayer/client-server sync;
- malicious/invalid packet inputs;
- boundary values;
- interaction with another technique.

## Review output format

When asked to review code, prefer:

```text
[High] Short title — file:line
Failure mode and why it matters.
Concrete fix direction.

[Medium] ...
```

Then provide:

- overall merge recommendation;
- tests executed/not executed;
- remaining uncertainty.

Do not claim code is safe because it compiles.
