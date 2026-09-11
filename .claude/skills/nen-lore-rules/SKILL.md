---
name: nen-lore-rules
description: Canon and design reference for Hunter x Hunter Nen. Use when designing, implementing, reviewing, balancing, documenting, or debugging Nen mechanics, aura, Ten, Zetsu, Ren, Hatsu, advanced techniques, affinities, conditions, vows, abilities, or progression.
---

# Nen Lore Rules

Use this skill as the project's lore source of truth for Nen-related mechanics. Its purpose is not to force the game to simulate every manga detail literally; its purpose is to stop implementations from contradicting the internal logic of Nen.

When canon is explicit, preserve it. When canon is ambiguous, model the uncertainty instead of inventing a hard universal rule. When gameplay needs an abstraction, document the abstraction and keep the underlying relationship faithful.

## Core mental model

Nen is the conscious control of aura, the life energy produced by living beings. A Nen user is not merely a character with a mana bar. Their effectiveness is shaped by:

- total aura capacity;
- remaining aura;
- aura output at a given moment;
- control and efficiency;
- natural Nen affinity;
- proficiency in each category;
- technique being used;
- mental/emotional state;
- restrictions, conditions, risk, and vows;
- physical ability and tactical execution.

Never reduce Nen power to a single integer if the mechanic being implemented depends on more than raw reserve.

## Aura and awakening

Living beings naturally produce aura. Untrained people normally leak it through aura nodes and cannot consciously perceive or control it.

A person can awaken to Nen gradually through training or forcibly by having their aura nodes opened by another Nen user. Forced awakening is dangerous and must not be treated as a harmless shortcut in lore-facing systems.

For implementation, distinguish at minimum:

- `nenAwakened`: can consciously use Nen;
- `auraMax`: maximum accessible reserve;
- `auraCurrent`: reserve currently available;
- `auraOutputMax`: maximum aura that can be effectively manifested at once;
- `auraControl`: efficiency/precision of use;
- `nenAffinity`: natural category;
- `proficiencyByCategory`: trained proficiency, distinct from affinity.

Do not assume physical stamina and aura are the same resource. Aura exhaustion can produce fatigue, but physical conditioning remains independently relevant in the source material.

## The Four Major Principles

Everything else is built on aura-flow control. The four foundations are Ten, Zetsu, Ren, and Hatsu.

### Ten

Ten keeps aura from leaking away and wraps it around the body.

Lore consequences:

- basic defense against hostile aura;
- conserves the user's aura better than uncontrolled leakage;
- eventually becomes nearly automatic for experienced users;
- forms the basis of several advanced techniques.

Implementation direction:

- low continuous cost or improved retention rather than a large drain;
- improves passive Nen defense;
- should be the default combat-ready state for trained users;
- mastering Ten can reduce maintenance overhead rather than only increasing a numeric defense multiplier.

### Zetsu

Zetsu closes aura nodes and suppresses outward aura flow.

Lore consequences:

- hides the user's aura presence extremely well;
- can improve recovery from fatigue;
- removes or severely reduces aura defense;
- makes the user highly vulnerable to hostile Nen.

Implementation direction:

- aura output becomes zero or nearly zero;
- active Nen abilities that require ongoing output should usually stop unless their rules explicitly allow persistence;
- detection signature drops sharply;
- aura recovery may increase;
- Nen defense drops drastically;
- do not let Zetsu function as a free stealth toggle with no combat risk.

### Ren

Ren increases the volume/output of aura released around the body.

Lore consequences:

- boosts attack and defense;
- supports advanced techniques and abilities;
- consumes aura much faster than Ten;
- hostile Ren can exert psychological and physical pressure on unprotected targets.

Implementation direction:

- raises available output and combat performance;
- increases aura drain over time;
- can expose the user's presence more strongly;
- high-level hostile Ren may apply fear/pressure effects to inadequately protected targets, but avoid turning this into an unconditional stun.

### Hatsu

Hatsu is the personal expression/application of Nen. A Nen ability is an application of Hatsu, often combined with basic and advanced techniques.

Important design rule: Hatsu is not just a spell slot. Good abilities reflect the user's nature, training, habits, preferences, risks, and chosen conditions.

Do not make every user learn the same catalog of fixed powers. Shared fundamentals are universal; personal abilities should remain personalized.

## Advanced techniques

### Gyo

Gyo focuses a larger share of aura into a specific body part.

Typical uses:

- eyes: perceive faint aura and detect aura concealed with In;
- limbs: temporarily increase offense or defense in that region.

Trade-off: concentrating aura somewhere leaves less aura elsewhere.

Implementation direction:

- model as aura allocation, not a flat global buff;
- eye Gyo should interact with stealth/visibility systems;
- limb Gyo should improve local performance while increasing vulnerability elsewhere.

### In

In conceals aura without stopping its flow.

It can hide the user's aura or aura-based constructs and is useful for traps and surprise attacks.

Counterplay:

- Gyo in the eyes is the standard counter;
- skilled users may infer In indirectly from suspicious absence of aura.

Implementation direction:

- In should modify aura visibility, not simply grant vanilla invisibility;
- hidden Nen effects should still exist and function;
- detection should depend on observer skill and active perception technique.

### En

En extends aura around the user and maintains it as a sensing field.

Inside En, the user can sense shapes and movement with a precision dependent on skill.

Canon establishes that En is demanding to maintain. Skilled users vary enormously in range; do not make one universal range progression curve mandatory.

Implementation direction:

- continuous high aura drain;
- radius and sensory resolution are separate progression dimensions;
- server-side spatial queries should be throttled/cached for performance;
- avoid scanning every entity/block every tick over huge radii.

### Shu

Shu extends Ten around an object, treating it as an extension of the user's body and reinforcing it with aura.

Implementation direction:

- usable on weapons/tools/items;
- increases durability/effectiveness based on aura allocation and proficiency;
- can combine with other techniques;
- do not hardcode Shu to swords only.

### Ko

Ko concentrates essentially all active aura into one body part by combining the principles behind Ten, Zetsu, Ren, Hatsu, and Gyo.

Result:

- extremely high attack or local defense;
- the rest of the body becomes almost completely unprotected.

Implementation direction:

- treat Ko as near-total aura allocation to one region/action;
- damage should be high because risk is high;
- punish missed attacks and predictable wind-up;
- never implement Ko as a normal high-damage cooldown with full-body defense retained.

### Ken

Ken sustains a powerful Ren-like shroud around the whole body for prolonged defense.

Result:

- strong balanced protection;
- significant continuous aura drain;
- safer than Ko because it does not leave most of the body exposed.

Implementation direction:

- defensive stance with meaningful upkeep;
- aura efficiency should strongly affect duration;
- advanced users can sustain it much longer than novices.

### Ryu

Ryu is dynamic redistribution of aura while maintaining Ken. The user shifts percentages of aura between offense and defense in real time.

Example model:

- 70% to attacking arm / 30% to the rest;
- 20% to attacking arm / 80% to defense;
- rapidly changing allocation in response to enemy movement.

Implementation direction:

- this is the ideal basis for an advanced combat mastery system;
- model allocation as percentages or weighted regions;
- both speed of redistribution and accuracy of reading the opponent should improve with mastery;
- slow redistribution can telegraph attacks.

## Aura quantification: reserve is not output

Knuckle's explanation establishes a useful three-part model.

### Maximum Aura Power (MAP/MOP)

The maximum amount of aura the user can store/access.

Game concept: `auraMax`.

### Potential Aura Power (PAP/POP)

The amount of aura currently remaining.

Game concept: `auraCurrent`.

### Actual Aura Power (AAP/AOP)

The amount of aura effectively used at a given moment: practical output.

Game concept: `auraOutputCurrent`, capped by `auraOutputMax`.

Design consequence:

A character may have a huge reserve but mediocre combat output, or a smaller reserve with excellent output and control. Do not derive all three values from one stat.

Efficiency also matters. Aura spent on a technique does not guarantee equivalent effective force when category affinity or proficiency is poor.

## Nen categories

There are six primary aura categories.

### Enhancement

Core function: strengthen the natural properties/capabilities of the body or an object.

Typical implementation space:

- physical attack;
- durability;
- healing/recovery;
- growth or reinforcement effects.

Do not assume Enhancement must be simplistic. It is mechanically straightforward, but advanced applications can still be sophisticated.

### Transmutation

Core function: change the properties of aura so it behaves like another substance/property or assumes a specific form.

Examples of design space:

- elasticity;
- electricity-like behavior;
- threads;
- heat/cold-like properties;
- shaped aura constructs.

Important distinction: transmuted aura remains aura. It is not automatically a real physical substance.

### Emission

Core function: separate aura from the user's body while maintaining useful strength/function at range.

Typical design space:

- projectiles and beams;
- remote effects;
- propulsion;
- teleportation-like effects at advanced levels;
- sustaining detached aura.

Design principle: distance, amount of detached aura, persistence, and output should create trade-offs.

### Conjuration

Core function: materialize physical constructs from aura and, in advanced cases, create special rules/areas/spaces tied to those constructs.

Typical design space:

- weapons/tools;
- containers;
- specialized devices;
- rule-bound spaces;
- pocket spaces.

Important rule: a Conjurer cannot simply create an omnipotent object with impossible absolute properties for free. Conditions and restrictions are central to pushing an ability beyond ordinary limits.

### Manipulation

Core function: control living beings, objects, or aura constructs.

Control strength is strongly shaped by activation conditions and medium.

Canon distinguishes multiple broad control styles, including influence that preserves some free will and coercive control that overrides it.

Implementation direction:

- define target class;
- define activation requirement;
- define degree of control;
- define command complexity;
- define range/maintenance;
- define what happens if another manipulator already controls the target.

Avoid generic "mind control = press key" abilities without conditions.

### Specialization

Specialization covers effects that do not fit the other five categories and can interact with Nen rules in unusual ways.

Implementation rule: Specialization is not permission to ignore balance or system logic.

Specialist abilities should generally be:

- individually authored;
- rare;
- rule-heavy;
- validated case-by-case;
- prevented from becoming a generic "do anything" category.

Do not infer universal specialist affinity percentages where canon remains unclear.

## Affinity, learning, and efficiency

For ordinary non-Specialists, the classic category chart establishes decreasing compatibility as categories become farther from the user's natural type.

A commonly used canon model is:

- natural category: 100%;
- adjacent categories: 80%;
- next categories: 60%;
- opposite category: 40%;
- Specialization is not normally learnable as an ordinary off-category technique.

These percentages affect both how far one can progress in an off-category and how effective equivalent techniques are.

Do not use these percentages as the only balance formula. They describe compatibility, not the user's total combat power.

## Leanings and dual affinities

Supplemental canon material published with the Togashi exhibition introduced the idea that some users lean toward an adjacent type, and some sit effectively between two affinities for learning speed.

Important nuance:

- the user still has a primary category;
- a secondary/leaned category does not automatically become 100% effective;
- leanings can change with training/environment/mindset according to the published explanation.

Implementation direction:

Represent this separately from the primary type:

- `primaryAffinity`;
- optional `affinityLeaning`;
- `learningRateByCategory`;
- `effectivenessByCategory`.

Keep this feature optional for the MVP. It is useful for long-term character individuality but not required to make the basic system work.

## Water Divination

Water Divination identifies Nen affinity by using Ren/Gyo around a glass of water with a leaf.

Expected outcomes:

- Enhancement: water volume changes;
- Transmutation: taste changes;
- Emission: color changes;
- Conjuration: impurities appear;
- Manipulation: leaf moves;
- Specialization: a different/unclassified result.

Implementation direction:

This is a strong diegetic onboarding mechanic. Prefer revealing the player's type through an in-world test rather than a plain character-creation dropdown if the mod's progression supports awakening/training.

Hisoka's personality test is explicitly unreliable and should never be used as a deterministic category assignment rule unless clearly presented as flavor.

## Conditions, activation requirements, limitations, vows, and risk

Restrictions are one of the most important balance laws in Nen.

### Conditions

Conditions constrain when/how an ability can function and can make stronger effects feasible.

Examples of implementation dimensions:

- must touch target;
- must explain the ability;
- only works on a target class;
- requires specific weather/location/time;
- requires an object or prepared mark;
- requires a prior relationship/action;
- only functions while another behavior continues.

### Activation requirements

These are conditions that must be satisfied to start an ability.

Keep them explicit and machine-verifiable. Do not rely on vague flavor text for a mechanic that determines whether a powerful ability is valid.

### Limitations and vows

A user may accept severe consequences or narrow use cases to amplify power.

Strength should correlate with meaningful risk, sacrifice, difficulty, irreversibility, or restriction.

Do not allow players to create fake restrictions that are trivial in practice but grant enormous multipliers.

Evaluate a restriction using:

1. how often it blocks normal use;
2. how difficult it is to satisfy;
3. how dangerous failure is;
4. how much agency the player gives up;
5. how permanent the consequence is;
6. how exploitable the game environment makes it.

### Emotional and subjective factors

Nen conditions can depend on the creator's own worldview and emotional meaning. This is difficult to simulate mechanically.

For the mod, prefer objective rules for multiplayer fairness, but preserve the lore idea by allowing authored abilities to have thematic conditions tied to character progression or chosen vows.

## Post-mortem Nen

Death does not always end a Nen effect. Strong emotions, grudges, loyalty, or death-bound conditions can cause an existing ability to persist or become stronger.

Implementation rule:

- do not make every death trigger stronger Nen;
- treat post-mortem Nen as rare and authored;
- require explicit ability metadata/conditions;
- consider persistence, ownership, exorcism, and save/load consequences.

## Subclasses and patterns of Nen abilities

Later arcs introduce useful classifications. These should be treated as descriptors/patterns rather than a mandatory seventh category system.

Useful future tags include:

- `BARRIER`: area and boundary effects, often supported by prepared objects;
- `COLLABORATIVE`: requires a partner;
- `SYMBIOTIC/JOINED`: multiple users contribute aura/tasks for a greater result;
- `COMPOUND/CUMULATIVE`: multiple effects or effects accumulated through conditions;
- `COUNTERACTIVE`: activates after harm/risk and becomes stronger because the user acts second;
- `CURSE`: persistent harmful Nen imposed on a target;
- `EXORCISM`: removes or transfers curse-like Nen, often with a cost;
- `HAUNTING`: death-triggered harmful persistence;
- `LAND_MINE`: activation anchored to prepared locations;
- `LOAN`: transfers an ability/use to another person;
- `NEN_BEAST`: autonomous/semi-autonomous Nen construct;
- `PARASITIC`: feeds on a host's aura/emotions and may act without their awareness;
- `SUPPORTIVE`: non-combat-focused utility/support behavior.

Do not force every Hatsu to have exactly one tag. Tags can overlap.

## Nen beasts

Nen beasts are constructs produced by Nen and can vary enormously in form, autonomy, and category composition.

Implementation direction:

- represent the beast entity separately from the ability definition;
- ownership and aura upkeep must be explicit;
- define whether the beast persists at distance;
- define whether it is visible to non-users;
- define AI/autonomy separately from combat stats.

## Detection and visibility rules

Keep these concepts distinct:

- seeing ordinary physical objects;
- seeing aura;
- sensing aura presence;
- seeing conjured physical objects;
- detecting aura hidden with In;
- detecting someone in Zetsu;
- detecting a target inside En.

Do not implement one boolean `canSeeNen` and route all mechanics through it.

Suggested model:

- `auraPerception`;
- `auraSensitivity`;
- `gyoPerceptionBonus`;
- `inConcealmentStrength`;
- `zetsuSignature`;
- `enDetectionResolution`.

## Training rules

Nen growth should come from more than generic XP.

Useful separate progression axes:

- Ten stability/efficiency;
- Ren output and duration;
- Zetsu activation speed and concealment;
- Gyo activation speed/precision;
- Ken duration;
- Ryu redistribution speed and accuracy;
- En radius and resolution;
- Shu efficiency;
- category-specific proficiency;
- personal ability mastery;
- aura reserve;
- aura output;
- control.

Source material explicitly supports training multiple categories rather than only the natural category. Off-category training can improve flexibility while still respecting affinity limits.

## Rules for generating new Hatsu

When Claude is asked to create an ability, perform this checklist before proposing final mechanics.

1. Identify the user's natural affinity.
2. Identify every category the ability actually requires.
3. Check distance from natural affinity and expected efficiency.
4. Define what the ability physically does; avoid vague "concept manipulation" unless justified as Specialization.
5. Define activation requirements.
6. Define ongoing costs.
7. Define range, duration, targets, and persistence.
8. Define counterplay.
9. Define meaningful risks/restrictions if the effect is unusually strong.
10. Define whether it remains active during Zetsu, unconsciousness, distance, logout, dimension change, or death.
11. Define multiplayer ownership and synchronization.
12. Check whether a simpler existing Nen category can explain the effect before labeling it Specialization.

## Balance invariants for this mod

Preserve these unless a design document explicitly overrides them:

- More total aura does not automatically mean greater instantaneous power.
- Higher output does not automatically mean better efficiency.
- Affinity does not replace training.
- Conditions must create real gameplay constraints to justify major power gains.
- Ko must expose the user.
- Zetsu must create vulnerability.
- En must have meaningful upkeep/performance cost.
- In must have counterplay through perception/Gyo.
- Ryu should reward skillful redistribution, not just unlock a passive percentage buff.
- Specialization must not become an unrestricted power generator.
- Aura and physical stamina are related through fatigue but are not identical resources.

## Canon uncertainty policy

When the manga/supplemental material is unclear:

- say that it is unclear;
- avoid asserting fan theory as fact;
- prefer a configurable implementation;
- isolate the uncertain rule behind an interface/config/data definition;
- document which part is gameplay abstraction.

Especially avoid hard claims about universal Specialist efficiency rules, universal En ranges, exact aura regeneration rates, or universal numeric relationships between MAP and AAP.

## Source basis

Use these as starting references when verifying lore before major mechanics:

- Hunterpedia Nen overview: https://hunterxhunter.fandom.com/wiki/Nen
- Hunterpedia Chapter 60 (Nen categories, Water Divination, affinity implications): https://hunterxhunter.fandom.com/wiki/Chapter_60
- Hunterpedia Togashi Yoshihiro -Puzzle- exhibition summary (affinity/leaning and proficiency charts): https://hunterxhunter.fandom.com/wiki/Exhibition:_Togashi_Yoshihiro_-Puzzle-
- Manga material summarized in the Nen overview for Knuckle's MAP/PAP/AAP model, Greed Island training, conditions/limitations, advanced techniques, and post-mortem Nen.

When accuracy matters, prefer the manga/officially published supplementary material over community interpretation. Hunterpedia is a navigation/reference layer, not a substitute for primary canon.
