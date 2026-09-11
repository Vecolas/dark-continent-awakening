---
name: neoforge-1-21-1
description: NeoForge 1.21.1 engineering rules for this Minecraft mod. Use when adding or changing Java code, registration, networking, persistence, data, events, client/server logic, configs, entities, items, or build setup.
---

# NeoForge 1.21.1 Engineering

This project targets Minecraft/NeoForge 1.21.1. Treat version correctness as a hard requirement.

Do not copy APIs from old Forge tutorials or newer NeoForge versions without checking that the API exists in 1.21.1.

## Before editing

1. Inspect `gradle.properties`, `build.gradle`, `settings.gradle`, and the mod metadata.
2. Confirm the exact Minecraft and NeoForge versions already used by the repository.
3. Search the existing codebase for established patterns before creating parallel infrastructure.
4. Prefer the official NeoForge 1.21.1 documentation and the project's installed mappings over memory.
5. If an API differs between versions, follow the repository's actual dependency version rather than a generic example.

## Java/toolchain

NeoForge 1.21.1 requires Java 21.

Do not downgrade language/toolchain settings to Java 17 based on older Minecraft modding guides.

Recommended verification commands on Windows:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

Use the wrapper committed by the project.

## Registration

Prefer `DeferredRegister` and NeoForge's specialized variants when available.

Examples of appropriate specialized registrars include blocks, items, data components, and entities.

Registration rules:

- register on the mod event bus;
- do not instantiate registry-dependent objects through arbitrary static initialization;
- use namespaced resource locations consistently;
- centralize registration by domain instead of creating one giant registry class;
- do not access deferred objects before registration is complete.

## Event buses

Know which bus the event belongs to.

- `NeoForge.EVENT_BUS`: gameplay/game events.
- mod event bus: lifecycle, registration, setup, payload registration, GameTest registration, and other `IModBusEvent` events.

Do not register an event on both buses "just in case".

Many mod lifecycle events can run in parallel. If an operation must run on the main thread, use the supported enqueue mechanism for that event.

## Physical and logical sides

Never assume client and server are the same process.

Rules:

- authoritative gameplay state lives on the logical server;
- the client requests actions and renders synchronized state;
- client-only classes must not be referenced from code that can load on a dedicated server;
- use explicit client registration for renderers, key mappings, HUD, screens, and client payload handlers;
- always test on a dedicated server, not only integrated single-player.

Any Nen action that changes aura, damage, cooldowns, conditions, progression, or ability state must be validated by the server.

## Player Nen data: Data Attachments

For persistent custom data attached to players/entities in 1.21.1, use NeoForge Data Attachments unless the existing codebase has a justified alternative.

Examples of suitable player attachment data:

- awakened state;
- Nen profile;
- current/max aura;
- max output;
- technique mastery;
- learned Hatsu IDs;
- active Nen stance;
- cooldown/condition state that must persist.

Important 1.21.1 facts:

- attachment types are registered in `NeoForgeRegistries.ATTACHMENT_TYPES`;
- persistence is opt-in through a serializer/codec;
- attachment data is not automatically synchronized to clients;
- `copyOnDeath` or `PlayerEvent.Clone` must be chosen intentionally;
- item-stack custom data should use vanilla Data Components rather than item attachments.

Do not send the entire attachment every tick.

## Item custom data: Data Components

Use `DataComponentType` for persistent/synchronized data attached to `ItemStack`s.

Examples:

- custom Nen weapon metadata;
- condition IDs;
- learned/imprinted ability data;
- tool configuration.

Define codecs deliberately and avoid storing runtime-only references in persistent components.

## World/global data

Use `SavedData` for state associated with a level/world rather than a specific entity/block/chunk.

Examples:

- world Nen progression flags;
- generated dojo metadata;
- world-level unlocked systems;
- global ritual state.

Call `setDirty()` when mutating `SavedData`.

## Networking

Use NeoForge 1.21.1 custom payloads through `RegisterPayloadHandlersEvent`, `CustomPacketPayload`, `StreamCodec`, and `PacketDistributor`.

Server-authoritative pattern:

1. client sends intent, not trusted state;
2. server validates prerequisites;
3. server mutates authoritative data;
4. server synchronizes the resulting state to relevant clients.

Bad packet:

```text
SetAuraPacket(999999)
```

Better packet:

```text
ActivateTechniquePacket(TechniqueId.GYO_EYES)
```

The server then checks awakening, learned technique, aura cost, current state, cooldowns, and conditions.

Never trust client-supplied damage, aura amount, category, mastery, target validity, distance, or cooldown completion.

Keep payloads small. Sync deltas/events where practical instead of full state snapshots every tick.

## Nen ticking

Do not create one expensive `PlayerTickEvent` routine that recalculates the entire Nen system each tick.

Classify updates:

- every tick: only truly time-sensitive combat state;
- every few ticks: HUD sync/detection updates when sufficient;
- event-driven: technique activation, damage, item use, equipment changes;
- scheduled: expensive En scans or regeneration recalculation;
- cached: derived affinity multipliers and static ability definitions.

Use dirty flags/change detection before sending synchronization packets.

## Data-driven design

Prefer data-driven definitions for content that designers may tune frequently.

Good candidates:

- Hatsu definitions;
- aura costs;
- condition definitions;
- training requirements;
- NPC Nen profiles;
- ability tags/subtypes;
- balance multipliers.

Use codecs/datapack registries/data maps where they genuinely improve reloadability and interoperability. Do not data-drive runtime logic that is clearer and safer in Java.

## Damage and combat

When adding Nen-specific damage behavior:

- prefer registered damage types/tags over hardcoded checks scattered across events;
- separate base Minecraft damage from Nen amplification and defense calculations;
- avoid recursive damage event loops;
- preserve compatibility with armor, enchantments, other mods, and invulnerability rules unless the ability explicitly overrides them;
- document any ability that bypasses normal defenses.

## Performance rules

Never:

- scan all entities in a dimension every tick;
- iterate every block in a large En sphere every tick;
- serialize large state objects continuously;
- perform filesystem/network operations on the game thread;
- allocate large temporary collections every tick for every player.

For spatial Nen systems:

- use bounding boxes/radius queries;
- cache stable results;
- throttle checks;
- cap practical radius for gameplay/performance where needed;
- separate visual radius from high-resolution sensing logic if necessary.

## Compatibility rules

This is a modpack project. Avoid invasive replacements of vanilla or third-party behavior when events, tags, data maps, loot modifiers, or extension points can solve the same problem.

Prefer additive behavior.

Do not assume no other combat, attribute, animation, or HUD mod is present.

## Error handling and logging

- use the project's logger;
- log identifiers and state relevant to failures;
- do not spam logs every tick;
- invalid datapack ability definitions should fail with actionable messages;
- reject malformed network requests safely rather than crashing the server.

## Build completion checklist

Before declaring an implementation complete:

1. `gradlew build` passes;
2. client starts;
3. dedicated server starts;
4. no client-only classloading crash occurs;
5. save/reload preserves intended data;
6. death/respawn behavior matches design;
7. multiplayer synchronization is tested;
8. obvious packet abuse is rejected server-side;
9. no major tick-time regression is introduced;
10. relevant GameTests/unit tests are added or updated.

## Official references

- NeoForge 1.21.1 Getting Started: https://docs.neoforged.net/docs/1.21.1/gettingstarted/
- Events: https://docs.neoforged.net/docs/1.21.1/concepts/events/
- Data Attachments: https://docs.neoforged.net/docs/1.21.1/datastorage/attachments/
- Saved Data: https://docs.neoforged.net/docs/1.21.1/datastorage/saveddata/
- Data Components: https://docs.neoforged.net/docs/1.21.1/items/datacomponents/
- Networking/Payloads: https://docs.neoforged.net/docs/1.21.1/networking/payload/
- Game Tests: https://docs.neoforged.net/docs/1.21.1/misc/gametest/

If official docs and remembered API syntax disagree, trust the 1.21.1 docs and the repository's actual dependencies.
