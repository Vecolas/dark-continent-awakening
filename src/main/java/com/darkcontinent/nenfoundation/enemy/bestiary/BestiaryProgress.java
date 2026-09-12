package com.darkcontinent.nenfoundation.enemy.bestiary;

import java.util.Map;

/** Progresso monotono de descoberta; texto completo fica em recurso/localizacao. */
public final class BestiaryProgress {
    private final Map<String, BestiaryStatus> entries;

    public BestiaryProgress() { this(Map.of()); }

    public BestiaryProgress(Map<String, BestiaryStatus> entries) {
        if (entries == null || entries.entrySet().stream().anyMatch(e -> e.getKey() == null
                || e.getKey().isBlank() || e.getValue() == null)) {
            throw new IllegalArgumentException("progresso invalido");
        }
        this.entries = Map.copyOf(entries);
    }

    public BestiaryStatus status(String entityId) {
        if (entityId == null || entityId.isBlank()) throw new IllegalArgumentException("id invalido");
        return entries.getOrDefault(entityId, BestiaryStatus.UNKNOWN);
    }

    public BestiaryProgress advance(String entityId, BestiaryStatus next) {
        if (entityId == null || entityId.isBlank() || next == null) throw new IllegalArgumentException("progresso invalido");
        BestiaryStatus current = status(entityId);
        if (next.ordinal() < current.ordinal()) return this;
        java.util.HashMap<String, BestiaryStatus> copy = new java.util.HashMap<>(entries);
        copy.put(entityId, next);
        return new BestiaryProgress(copy);
    }

    public Map<String, BestiaryStatus> entries() { return entries; }
}
