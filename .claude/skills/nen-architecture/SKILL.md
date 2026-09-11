---
name: nen-architecture
description: Architecture rules for implementing the mod's extensible Nen foundation. Use when creating or changing aura systems, Nen states, techniques, Hatsu, conditions, progression, persistence, synchronization, APIs, or future Nen modules.
---

# Nen Architecture

This skill translates Nen lore into a maintainable NeoForge 1.21.1 architecture.

When changing lore semantics, read `../nen-lore-rules/SKILL.md`. When changing NeoForge integration, also follow `../neoforge-1-21-1/SKILL.md`.

## Architectural goal

Build a Nen foundation that can support future abilities without adding special-case code to a giant player tick handler.

The system must support:

- basic techniques;
- advanced techniques;
- personal Hatsu;
- conditions and vows;
- multiple Nen categories;
- off-category efficiency;
- aura reserve/output/control;
- multiplayer;
- save/load;
- NPC users;
- future compatibility with other mods and datapacks.

## Core separation

Do not merge all concepts into `NenData` with hundreds of fields and methods.

Prefer these conceptual layers.

### `NenProfile`

Stable identity/progression information.

Suggested fields:

- awakened/unawakened;
- primary affinity;
- optional leaning/secondary learning bias;
- proficiency per category;
- mastery per universal technique;
- unlocked ability IDs;
- long-term progression values.

### `AuraState`

Runtime resource state.

Suggested fields:

- current aura;
- maximum aura;
- current output;
- maximum output;
- control/efficiency;
- regeneration state;
- exhaustion state.

Invariants:

```text
0 <= auraCurrent <= auraMax
0 <= auraOutputCurrent <= auraOutputMax
aura spent must never make auraCurrent negative
server is authoritative
```

### `NenStanceState`

Current universal state/technique mode.

Examples:

- Ten;
- Zetsu;
- Ren;
- Ken;
- En;
- In;
- Gyo target/body region;
- Ryu allocation;
- Ko target/body region.

Do not model mutually incompatible states as unrelated booleans such as `isZetsu`, `isRen`, `isKen`, `isKo` if that permits impossible combinations.

Use a state machine or composable state model with explicit compatibility rules.

### `AuraAllocation`

Represents distribution of active aura across body/action regions.

Future-friendly regions may include:

- head/eyes;
- torso;
- left arm;
- right arm;
- legs;
- held item;
- emitted/remote pool.

Keep the representation abstract enough that Ryu and Ko do not require rewriting the combat engine.

Validation:

```text
sum(local allocations) <= available output
Ko ~= near-total concentration
Ken ~= broadly distributed defense
Ryu = dynamic distribution while maintaining Ken-like protection
```

### `NenAbilityDefinition`

Immutable/configurable description of an ability.

Suggested metadata:

- ID;
- display data;
- required categories and weights;
- base aura costs;
- activation requirements;
- maintenance costs;
- cooldown rules;
- range;
- target rules;
- persistence rules;
- ability tags/subtypes;
- whether it can survive unconsciousness/Zetsu/death;
- server executor type.

Do not serialize executable Java lambdas.

### `NenAbilityInstance`

Runtime instance owned by a user.

Stores only state needed by an active/learned ability:

- owner;
- definition ID;
- mastery;
- cooldown;
- prepared marks/targets;
- active state;
- ability-specific serializable state.

### `ConditionEngine`

Conditions must be first-class mechanics, not text descriptions.

Each condition should expose something equivalent to:

```text
canActivate(context)
canContinue(context)
powerModifier(context)
onViolation(context)
```

Examples:

- target belongs to a defined group;
- user touched target;
- user explained ability;
- required item is present;
- user is below a health threshold;
- only usable in rain;
- prepared mark exists;
- vow would be violated.

Do not award large multipliers from conditions the engine cannot actually enforce.

### `NenTechniqueService`

Universal techniques should use shared services, not personal Hatsu code.

Responsibilities:

- validate technique unlock/mastery;
- calculate upkeep;
- transition states;
- modify aura allocation;
- apply perception/detection rules;
- expose hooks used by combat/abilities.

### `NenCombatService`

Centralize Nen-aware combat calculations.

Inputs may include:

- vanilla damage source;
- attacker's effective aura allocation;
- defender's local aura allocation;
- affinity/proficiency modifier;
- ability-specific multiplier;
- conditions/vows;
- physical stats;
- target state.

Avoid a single opaque formula. Break calculations into named stages so they can be tested independently.

## No duplicate Nen stamina bar

Do not create a second generic "Nen stamina" resource that mirrors aura.

For Nen combat, use:

- reserve (`auraCurrent`);
- output (`auraOutputMax/current`);
- efficiency/control;
- continuous upkeep;
- exhaustion consequences.

Minecraft hunger/exhaustion or a future physical stamina system may represent bodily exertion, but it must remain conceptually separate from aura.

## Aura regeneration

Do not hardcode a canon regeneration-per-second value; the source material does not provide a universal one.

Make regeneration a gameplay abstraction based on:

- active stance;
- combat state;
- mastery;
- sleep/rest;
- Zetsu;
- buffs/debuffs;
- configuration.

Keep base values data/config driven.

## Universal technique dependencies

Recommended unlock dependency graph:

```text
Awakening
├── Ten
├── Zetsu
└── Ren
    └── Hatsu training

Ten + Ren -> Ken
Ren -> Gyo
Zetsu mastery + concealment -> In
Ten + Ren + sensing -> En
Ten -> Shu
Ten + Zetsu + Ren + Hatsu + Gyo -> Ko
Ken + Gyo + control mastery -> Ryu
```

This graph is a game abstraction inspired by the technique relationships in canon. Keep it configurable rather than encoding every edge deep in UI code.

## Affinity model

Do not store only one `NenType` and derive everything ad hoc.

Provide a service that can answer:

```text
learningRate(user, category)
maxProficiency(user, category)
effectiveness(user, category)
```

The service should support the standard chart and optional affinity leanings without changing ability code.

Abilities with multiple category requirements should calculate effective performance from each required component rather than using only the user's primary category.

## Hatsu composition

Prefer an ability system built from reusable effect primitives.

Examples:

- damage;
- enhancement modifier;
- emitted projectile;
- persistent emitted field;
- transmuted property;
- conjured entity/item;
- manipulation controller;
- teleport/movement;
- aura drain/loan;
- mark;
- curse;
- sensor;
- condition gate;
- counter trigger.

Complex abilities can combine primitives while still allowing authored Java logic when necessary.

Avoid attempting to create a fully generic no-code language for every possible Nen ability in the first version.

## Visibility and sensing architecture

Do not equate visual rendering with server detection.

Server concepts:

- aura signature;
- detection strength;
- concealment strength;
- En coverage;
- observer perception;
- Gyo state.

Client concepts:

- whether to render aura;
- particles/shaders/HUD;
- visualization of En/Gyo;
- hidden effects only revealed after server-authorized detection.

The server decides whether a player is allowed to know a hidden Nen entity/effect. Do not rely only on client rendering to keep secret information hidden.

## En architecture

Never implement En as a full block/entity sphere scan every player tick.

Use a dedicated `EnService` with:

- radius;
- resolution;
- upkeep cost;
- query interval;
- cached nearby entity set;
- movement-triggered updates where useful;
- maximum practical limits configurable for servers.

If block-level sensing is added later, use coarse spatial data rather than exhaustive block enumeration.

## Ryu/Ko architecture

Ryu should be built on `AuraAllocation`, not special damage multipliers.

Ko should be represented as an extreme allocation state.

This ensures:

- offense and defense use the same underlying data;
- missed attacks remain risky;
- future AI can reason about aura distribution;
- HUD can display the same state the server uses.

## Progression

Separate progression into several axes.

Suggested domains:

- aura capacity;
- output;
- control;
- universal technique mastery;
- category proficiency;
- Hatsu mastery;
- physical attributes/training.

Avoid one global `nenLevel` that improves everything uniformly.

## Training system

Training actions should modify specific capabilities.

Examples:

- maintain Ren -> duration/output/control;
- maintain Ken -> upkeep efficiency;
- rapid Gyo drills -> activation speed;
- perception drills -> detection accuracy;
- Ryu sparring -> redistribution speed;
- category exercises -> proficiency in that category;
- physical drills -> Minecraft/physical stats, not aura reserve by default.

Use diminishing returns and progression caps rather than infinite linear stat farming.

## Persistence strategy

Persistent player Nen data should live in a serializable player Data Attachment.

Split persistent and transient state:

Persistent:

- affinity;
- unlocks;
- progression;
- aura max/output max;
- authored ability metadata;
- long cooldowns if design requires.

Transient/runtime:

- current input state;
- short-lived target cache;
- per-tick calculations;
- client animation progress;
- cached derived multipliers.

Decide explicitly whether current aura persists through logout/death.

## Death, respawn, and post-mortem behavior

Player death must not accidentally duplicate or erase Nen data.

Define separately:

- what long-term profile is copied on death;
- whether current aura resets;
- which active effects are removed;
- which explicit post-mortem abilities persist;
- who becomes owner/controller of persistent effects;
- whether effects survive dimension change/logout/server restart.

Post-mortem persistence is opt-in per ability.

## Networking

The server owns all Nen state.

Client sends intents such as:

- activate/deactivate Ten;
- enter Zetsu;
- start Ren;
- change Ryu allocation;
- attempt Hatsu activation;
- select target.

Server validates and responds with authoritative state.

For frequently changing values such as aura:

- synchronize at a reasonable interval;
- synchronize immediately on major state changes;
- interpolate HUD locally if desired;
- never let the client decide the authoritative value.

## API/extensibility

Expose stable interfaces before exposing implementation classes.

Future external integration may need:

- query Nen profile;
- check whether an entity is awakened;
- read aura percentage;
- register an ability definition;
- add condition types;
- listen for technique transitions;
- modify aura cost through events/hooks;
- detect ability activation.

Do not expose mutable internal collections directly.

## Suggested package structure

Adapt to the repository, but prefer domain separation similar to:

```text
<modid>.nen
├── api
├── aura
├── profile
├── technique
├── ability
│   ├── definition
│   ├── runtime
│   ├── condition
│   └── effect
├── combat
├── perception
├── progression
├── network
├── data
└── client
```

Do not create the structure blindly if equivalent packages already exist.

## Implementation order

Recommended foundation order:

1. persistent Nen profile attachment;
2. aura reserve/output/control model;
3. server synchronization;
4. awakening state;
5. Ten/Zetsu/Ren state machine;
6. HUD feedback;
7. category affinity/proficiency service;
8. Gyo/Ken/Shu;
9. En/In perception layer;
10. Ryu/Ko allocation system;
11. reusable conditions;
12. Hatsu definition/runtime framework;
13. advanced tags: curse, loan, counter, Nen beast, post-mortem;
14. public extension API.

Do not begin with dozens of named character abilities before the foundation has tests.

## Definition of done for a Nen mechanic

A mechanic is not complete until:

- lore assumptions are documented;
- state ownership is clear;
- persistence behavior is clear;
- death/logout behavior is clear;
- client/server authority is correct;
- aura cost/upkeep is enforced;
- invalid combinations are rejected;
- multiplayer has been tested;
- relevant unit/GameTests exist;
- performance impact is acceptable;
- there is defined counterplay for combat mechanics.
