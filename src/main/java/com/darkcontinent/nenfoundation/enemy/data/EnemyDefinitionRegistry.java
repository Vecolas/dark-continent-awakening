package com.darkcontinent.nenfoundation.enemy.data;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.ResourceLocation;

/**
 * Snapshot atomico das definicoes carregadas no servidor.
 *
 * <p>O consumidor nunca observa uma colecao sendo parcialmente recarregada:
 * uma carga valida substitui o snapshot inteiro, e uma carga invalida deixa o
 * snapshot anterior intacto. A leitura e segura para o caminho de tick.</p>
 */
public final class EnemyDefinitionRegistry {
    private final AtomicReference<Map<ResourceLocation, EnemyDefinition>> snapshot =
            new AtomicReference<>(Map.of());

    public Optional<EnemyDefinition> find(ResourceLocation id) {
        return Optional.ofNullable(snapshot.get().get(Objects.requireNonNull(id, "id ausente")));
    }

    public EnemyDefinition require(ResourceLocation id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("inimigo nao carregado: " + id));
    }

    public Map<ResourceLocation, EnemyDefinition> snapshot() {
        return snapshot.get();
    }

    /** Substitui todos os dados somente depois de validar a colecao inteira. */
    public void replaceAll(Collection<EnemyDefinition> definitions) {
        Objects.requireNonNull(definitions, "definicoes ausentes");
        Map<ResourceLocation, EnemyDefinition> next = new LinkedHashMap<>();
        for (EnemyDefinition definition : definitions) {
            if (definition == null) throw new IllegalArgumentException("definicao nula");
            ResourceLocation id = definition.metadata().id();
            if (next.putIfAbsent(id, definition) != null) {
                throw new IllegalArgumentException("id de inimigo duplicado: " + id);
            }
        }
        snapshot.set(Map.copyOf(next));
    }
}
