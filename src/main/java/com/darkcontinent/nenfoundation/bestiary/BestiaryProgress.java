package com.darkcontinent.nenfoundation.bestiary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;

/** Progresso individual por entrada. O item nunca é a fonte desse estado. */
public record BestiaryProgress(
        BestiaryKnowledgeLevel knowledgeLevel,
        int timesSeen,
        int timesFought,
        int timesDefeated,
        int researchPoints,
        long firstSeenTime,
        long lastSeenTime,
        Set<String> weakPointsDiscovered,
        Set<String> behaviorFlags,
        Set<String> captureFlags,
        Set<String> specialDiscoveries,
        BestiaryNenStatus nenStatus) {

    public static final Codec<BestiaryProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(BestiaryKnowledgeLevel::valueOf, Enum::name)
                    .optionalFieldOf("knowledge_level", BestiaryKnowledgeLevel.UNKNOWN)
                    .forGetter(BestiaryProgress::knowledgeLevel),
            Codec.INT.optionalFieldOf("times_seen", 0).forGetter(BestiaryProgress::timesSeen),
            Codec.INT.optionalFieldOf("times_fought", 0).forGetter(BestiaryProgress::timesFought),
            Codec.INT.optionalFieldOf("times_defeated", 0).forGetter(BestiaryProgress::timesDefeated),
            Codec.INT.optionalFieldOf("research_points", 0).forGetter(BestiaryProgress::researchPoints),
            Codec.LONG.optionalFieldOf("first_seen_time", 0L).forGetter(BestiaryProgress::firstSeenTime),
            Codec.LONG.optionalFieldOf("last_seen_time", 0L).forGetter(BestiaryProgress::lastSeenTime),
            Codec.STRING.listOf().xmap(list -> Set.copyOf(list), set -> java.util.List.copyOf(set))
                    .optionalFieldOf("weak_points_discovered", Set.of()).forGetter(BestiaryProgress::weakPointsDiscovered),
            Codec.STRING.listOf().xmap(list -> Set.copyOf(list), set -> java.util.List.copyOf(set))
                    .optionalFieldOf("behavior_flags", Set.of()).forGetter(BestiaryProgress::behaviorFlags),
            Codec.STRING.listOf().xmap(list -> Set.copyOf(list), set -> java.util.List.copyOf(set))
                    .optionalFieldOf("capture_flags", Set.of()).forGetter(BestiaryProgress::captureFlags),
            Codec.STRING.listOf().xmap(list -> Set.copyOf(list), set -> java.util.List.copyOf(set))
                    .optionalFieldOf("special_discoveries", Set.of()).forGetter(BestiaryProgress::specialDiscoveries),
            Codec.STRING.xmap(BestiaryNenStatus::valueOf, Enum::name)
                    .optionalFieldOf("nen_status", BestiaryNenStatus.NONE).forGetter(BestiaryProgress::nenStatus)
    ).apply(instance, BestiaryProgress::new));

    public static final BestiaryProgress UNKNOWN = new BestiaryProgress(
            BestiaryKnowledgeLevel.UNKNOWN, 0, 0, 0, 0, 0L, 0L,
            Set.of(), Set.of(), Set.of(), Set.of(), BestiaryNenStatus.NONE);

    public BestiaryProgress {
        if (timesSeen < 0 || timesFought < 0 || timesDefeated < 0 || researchPoints < 0) {
            throw new IllegalArgumentException("contadores do bestiario nao podem ser negativos");
        }
        weakPointsDiscovered = Set.copyOf(weakPointsDiscovered);
        behaviorFlags = Set.copyOf(behaviorFlags);
        captureFlags = Set.copyOf(captureFlags);
        specialDiscoveries = Set.copyOf(specialDiscoveries);
    }

    public BestiaryProgress observe(long gameTime) {
        return new BestiaryProgress(knowledgeLevel.atLeast(BestiaryKnowledgeLevel.OBSERVED)
                ? knowledgeLevel : BestiaryKnowledgeLevel.OBSERVED,
                timesSeen + 1, timesFought, timesDefeated, researchPoints,
                firstSeenTime == 0L ? gameTime : firstSeenTime, gameTime,
                weakPointsDiscovered, behaviorFlags, captureFlags, specialDiscoveries, nenStatus);
    }

    public BestiaryProgress fought() {
        return new BestiaryProgress(knowledgeLevel.atLeast(BestiaryKnowledgeLevel.FOUGHT)
                ? knowledgeLevel : BestiaryKnowledgeLevel.FOUGHT,
                timesSeen, timesFought + 1, timesDefeated, researchPoints,
                firstSeenTime, lastSeenTime, weakPointsDiscovered, behaviorFlags,
                captureFlags, specialDiscoveries, nenStatus);
    }

    public BestiaryProgress defeated() {
        return new BestiaryProgress(knowledgeLevel.atLeast(BestiaryKnowledgeLevel.FOUGHT)
                ? knowledgeLevel : BestiaryKnowledgeLevel.FOUGHT,
                timesSeen, timesFought, timesDefeated + 1, researchPoints,
                firstSeenTime, lastSeenTime, weakPointsDiscovered, behaviorFlags,
                captureFlags, specialDiscoveries, nenStatus);
    }

    public BestiaryProgress withResearchPoints(int points, BestiaryKnowledgeLevel minimum) {
        if (points < 0) throw new IllegalArgumentException("pontos de pesquisa negativos");
        var level = knowledgeLevel.atLeast(minimum) ? knowledgeLevel : minimum;
        return new BestiaryProgress(level, timesSeen, timesFought, timesDefeated,
                researchPoints + points, firstSeenTime, lastSeenTime,
                weakPointsDiscovered, behaviorFlags, captureFlags, specialDiscoveries, nenStatus);
    }

    public BestiaryProgress withSpecialDiscovery(String discovery) {
        if (discovery == null || discovery.isBlank()) {
            throw new IllegalArgumentException("descoberta do bestiario vazia");
        }
        var discoveries = new java.util.HashSet<>(specialDiscoveries);
        discoveries.add(discovery);
        return new BestiaryProgress(knowledgeLevel, timesSeen, timesFought, timesDefeated,
                researchPoints, firstSeenTime, lastSeenTime, weakPointsDiscovered,
                behaviorFlags, captureFlags, Set.copyOf(discoveries), nenStatus);
    }

    public BestiaryProgress withWeakPoint(String weakPoint) {
        if (weakPoint == null || weakPoint.isBlank()) {
            throw new IllegalArgumentException("ponto fraco do bestiario vazio");
        }
        var points = new java.util.HashSet<>(weakPointsDiscovered);
        points.add(weakPoint);
        return new BestiaryProgress(knowledgeLevel, timesSeen, timesFought, timesDefeated,
                researchPoints, firstSeenTime, lastSeenTime, Set.copyOf(points), behaviorFlags,
                captureFlags, specialDiscoveries, nenStatus);
    }

    public BestiaryProgress withNenStatus(BestiaryNenStatus status) {
        return new BestiaryProgress(knowledgeLevel, timesSeen, timesFought, timesDefeated,
                researchPoints, firstSeenTime, lastSeenTime, weakPointsDiscovered,
                behaviorFlags, captureFlags, specialDiscoveries, status);
    }
}
