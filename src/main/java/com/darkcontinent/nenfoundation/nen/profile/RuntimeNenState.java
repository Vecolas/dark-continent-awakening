package com.darkcontinent.nenfoundation.nen.profile;

import com.darkcontinent.nenfoundation.api.ability.ActiveAbility;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Estado de combate temporario de um jogador, mantido somente em memoria.
 *
 * <p>Este objeto e mutavel porque muda durante o tick. Ele nunca e attachment,
 * nunca e serializado e nunca carrega progresso. O ciclo de vida que o cria,
 * reinicia e remove mora no servidor; ver ADR-002.
 *
 * <p>As colecoes expostas sao visoes somente-leitura. Toda mudanca passa por
 * esta classe para que futuros motores possam acrescentar invariantes sem
 * procurar mutacoes diretas pelo projeto inteiro.
 */
public final class RuntimeNenState {

    private double auraAtual;
    private final Set<ResourceLocation> tecnicasAtivas = new HashSet<>();
    private final Map<ResourceLocation, Integer> cooldowns = new HashMap<>();
    private ActiveAbility canalizacao;

    /** Aura disponivel neste instante. A formula e os limites nascem no M2. */
    public double auraAtual() {
        return this.auraAtual;
    }

    /** Atualiza a medida autoritativa; somente codigo server-side possui este objeto. */
    public void definirAuraAtual(double auraAtual) {
        this.auraAtual = auraAtual;
    }

    public Set<ResourceLocation> tecnicasAtivas() {
        return Collections.unmodifiableSet(this.tecnicasAtivas);
    }

    public boolean ativarTecnica(ResourceLocation tecnica) {
        return this.tecnicasAtivas.add(Objects.requireNonNull(tecnica, "tecnica"));
    }

    public boolean desativarTecnica(ResourceLocation tecnica) {
        return this.tecnicasAtivas.remove(Objects.requireNonNull(tecnica, "tecnica"));
    }

    /** Ticks restantes por habilidade. A contagem e a politica chegam no M5. */
    public Map<ResourceLocation, Integer> cooldowns() {
        return Collections.unmodifiableMap(this.cooldowns);
    }

    public void definirCooldown(ResourceLocation habilidade, int ticksRestantes) {
        Objects.requireNonNull(habilidade, "habilidade");
        if (ticksRestantes < 0) {
            throw new IllegalArgumentException("ticksRestantes nao pode ser negativo");
        }
        if (ticksRestantes == 0) {
            this.cooldowns.remove(habilidade);
            return;
        }
        this.cooldowns.put(habilidade, ticksRestantes);
    }

    public void removerCooldown(ResourceLocation habilidade) {
        this.cooldowns.remove(Objects.requireNonNull(habilidade, "habilidade"));
    }

    /** Instancia viva em canalizacao, quando houver. */
    public Optional<ActiveAbility> canalizacao() {
        return Optional.ofNullable(this.canalizacao);
    }

    public void iniciarCanalizacao(ActiveAbility instancia) {
        this.canalizacao = Objects.requireNonNull(instancia, "instancia");
    }

    public void encerrarCanalizacao() {
        this.canalizacao = null;
    }
}
