---
name: testing
description: Testing strategy for the NeoForge 1.21.1 Nen mod. Use when adding tests, validating a feature, reproducing a bug, preparing CI, or deciding what must be tested before merge.
---

# Testing Strategy

Use layered testing. Do not rely only on launching a single-player world and trying the happy path.

## Test layers

### 1. Pure Java/unit tests

Use for deterministic logic that does not need Minecraft world state.

Strong candidates:

- aura cost calculations;
- clamping/invariants;
- affinity matrix;
- category effectiveness;
- condition multiplier rules;
- cooldown math;
- aura allocation validation;
- Ryu redistribution;
- progression curves;
- serialization helper logic where practical.

Design core math/services so they can be tested without booting Minecraft.

### 2. NeoForge GameTests

Use GameTests for in-game interactions and server-side world behavior.

NeoForge 1.21.1 supports `@GameTest`, `@GameTestHolder`, `RegisterGameTestsEvent`, and `gradlew runGameTestServer`.

Good candidates:

- player/entity state transitions;
- damage interactions;
- emitted projectiles;
- conjured entities;
- En detecting entities in range;
- ability activation requiring a prepared block/location;
- persistence behavior that can be exercised in-game;
- interaction with blocks/items/entities.

Keep test templates minimal.

### 3. Dedicated-server integration tests

Always run a dedicated server for meaningful milestones.

Validate:

- no client-only class loading;
- players can join;
- payload registration works;
- state is server-authoritative;
- two players see synchronized results;
- hidden Nen information is not leaked to unauthorized clients;
- disconnect/reconnect behavior.

### 4. Manual gameplay tests

Use for feel/UX that automated tests cannot judge well:

- aura HUD readability;
- keybind ergonomics;
- combat pacing;
- En feedback;
- Gyo/In readability;
- perceived fairness of conditions;
- latency tolerance.

Document manual scenarios so both developers repeat the same checks.

## Mandatory AuraState tests

Test these invariants:

```text
current never < 0
current never > max
output never < 0
output never > outputMax
spending exactly current reaches 0
spending more than current is rejected
negative cost is rejected
NaN/infinite numeric inputs cannot enter state
```

Test regeneration:

- idle;
- Ten;
- Ren;
- Zetsu;
- combat lockout if used;
- max cap;
- logout/reload policy.

## Technique state tests

### Ten

- can activate only when awakened;
- applies expected defensive/retention behavior;
- persists/stops according to design.

### Zetsu

- suppresses aura output/signature;
- prevents incompatible active techniques;
- improves recovery if designed;
- increases Nen vulnerability;
- cannot be exploited to preserve forbidden ongoing output.

### Ren

- consumes aura over time;
- raises output appropriately;
- stops when aura is exhausted;
- does not continue after invalid state/death.

### Gyo

- allocation changes only intended region;
- eyes reveal In only when detection threshold succeeds;
- rest-of-body trade-off is applied.

### Ken

- continuous drain;
- balanced defense;
- duration changes with mastery/efficiency.

### Ko

- near-total concentration;
- non-target regions become highly vulnerable;
- attack rejected when allocation/output is invalid.

### Ryu

- allocations sum to allowed budget;
- invalid percentages rejected;
- redistribution affects both offense and defense;
- rate limits/skill limits are enforced if present.

### En

- entities inside radius are detected;
- entities outside radius are not;
- Zetsu/In interactions match design;
- upkeep stops En at zero aura;
- performance stays acceptable with many entities/players.

## Hatsu tests

Every Hatsu should test:

1. activation prerequisites;
2. aura cost;
3. valid target;
4. invalid target;
5. range boundary;
6. cooldown;
7. condition success;
8. condition failure;
9. interruption/cancel;
10. death/logout/dimension-change behavior;
11. persistence if applicable;
12. multiplayer synchronization;
13. counterplay.

For high-power vow-based abilities, explicitly test attempts to bypass the vow.

## Networking tests

Treat a modified client as hostile.

Attempt to send:

- activation while ability is locked;
- activation with zero aura;
- repeated packet spam;
- invalid entity ID;
- target in another dimension;
- target beyond range;
- impossible Ryu percentages;
- arbitrary aura values if any packet carries numbers;
- stale request after state changed.

The server must reject invalid requests without crashing.

## Persistence tests

For Nen profile and aura data test:

- world save/reload;
- logout/login;
- death/respawn;
- returning from End;
- dimension travel;
- server restart;
- upgrading from an older data schema when migration exists.

Never assume attachment persistence means client synchronization.

## Two-player matrix

For multiplayer-sensitive features test at least:

```text
Player A Nen user / Player B Nen user
Player A Nen user / Player B unawakened
A uses In / B normal
A uses In / B Gyo
A uses Zetsu / B En
A attacks with Ko / B Ken
A changes Ryu allocation while B attacks
A disconnects with persistent ability active
```

## Performance tests

For En and other spatial systems create stress scenarios:

- multiple players with En;
- many entities in radius;
- large but valid radius;
- rapidly moving entities;
- simultaneous aura synchronization.

Measure rather than guess when performance becomes questionable.

A test passing functionally while causing severe tick lag is not a success.

## Regression tests

When fixing a bug:

1. reproduce it;
2. write a test that fails for the bug when practical;
3. implement fix;
4. verify test passes;
5. keep the regression test.

## Recommended pre-merge commands

Adapt to the repository's actual Gradle tasks:

```powershell
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat runGameTestServer
```

Also run `runServer` for major networking/client-separation changes.

## CI direction

A future GitHub Actions pipeline should at minimum:

- set up Java 21;
- cache Gradle safely;
- run build;
- run unit tests;
- run GameTest server if stable in CI;
- upload useful failure logs/artifacts;
- gate merges to `main` on required checks.

## Official reference

NeoForge 1.21.1 Game Tests:
https://docs.neoforged.net/docs/1.21.1/misc/gametest/
