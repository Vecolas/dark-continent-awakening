package com.darkcontinent.nenfoundation.nen.profile;

import com.darkcontinent.nenfoundation.api.ability.ActiveAbility;
import com.darkcontinent.nenfoundation.nen.aura.AuraPool;
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

    private final AuraPool aura;
    private boolean auraSuja = true;
    private long revisao;
    private final Set<ResourceLocation> tecnicasAtivas = new HashSet<>();
    private final Map<ResourceLocation, Integer> cooldowns = new HashMap<>();
    private ActiveAbility canalizacao;

    public RuntimeNenState() {
        this.aura = new AuraPool();
    }

    public RuntimeNenState(double auraMaxima) {
        this.aura = new AuraPool(auraMaxima);
    }

    /** Aura disponivel neste instante, validada pela fronteira do pool. */
    public double auraAtual() {
        return this.aura.atual();
    }

    /** Atualiza a medida autoritativa; somente codigo server-side possui este objeto. */
    public void definirAuraAtual(double auraAtual) {
        double antes = this.aura.atual();
        this.aura.definirAtual(auraAtual);
        if (antes != this.aura.atual()) marcarAlterado();
    }

    /** Nao expoe o pool mutavel: toda mutacao precisa marcar a revisao. */
    public double auraMaxima() {
        return this.aura.maxima();
    }

    public float outputPercent() {
        return this.aura.outputPercent();
    }

    public void ajustarOutput(float novoPercent) {
        if (this.aura.ajustarOutput(novoPercent)) {
            marcarAlterado();
        }
    }

    public boolean exausto() {
        return this.aura.maxima() > 0.0D && this.aura.exausto();
    }

    /** Troca a capacidade e marca o runtime para o proximo delta. */
    public void definirAuraMaxima(double auraMaxima) {
        double antes = this.aura.maxima();
        this.aura.definirMaxima(auraMaxima);
        if (antes != this.aura.maxima()) marcarAlterado();
    }

    /** Debita por inteiro ou deixa o pool intacto, marcando somente mudanca real. */
    public boolean gastarAura(double quantidade) {
        boolean gastou = this.aura.gastar(quantidade);
        if (gastou) marcarAlterado();
        return gastou;
    }

    /** Recupera e informa se o valor mudou. */
    public double recuperarAura(double quantidade) {
        double recuperada = this.aura.recuperar(quantidade);
        if (recuperada != 0.0D) marcarAlterado();
        return recuperada;
    }

    public boolean auraSuja() {
        return this.auraSuja;
    }

    /** Consome a marca apenas depois que o transporte aceitou o delta. */
    public void marcarAuraSincronizada() {
        this.auraSuja = false;
    }

    public long revisao() {
        return this.revisao;
    }

    /** Uma mutacao durante o envio permanece pendente para o proximo delta. */
    public void confirmarSincronizacao(long revisaoEnviada) {
        if (this.revisao == revisaoEnviada) this.auraSuja = false;
    }

    private void marcarAlterado() {
        this.auraSuja = true;
        this.revisao++;
    }

    public Set<ResourceLocation> tecnicasAtivas() {
        return Collections.unmodifiableSet(this.tecnicasAtivas);
    }

    public boolean ativarTecnica(ResourceLocation tecnica) {
        boolean mudou = this.tecnicasAtivas.add(Objects.requireNonNull(tecnica, "tecnica"));
        if (mudou) marcarAlterado();
        return mudou;
    }

    public boolean desativarTecnica(ResourceLocation tecnica) {
        boolean mudou = this.tecnicasAtivas.remove(Objects.requireNonNull(tecnica, "tecnica"));
        if (mudou) marcarAlterado();
        return mudou;
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
            removerCooldown(habilidade);
            return;
        }
        Integer antes = this.cooldowns.put(habilidade, ticksRestantes);
        if (!Objects.equals(antes, ticksRestantes)) marcarAlterado();
    }

    public void removerCooldown(ResourceLocation habilidade) {
        if (this.cooldowns.remove(Objects.requireNonNull(habilidade, "habilidade")) != null) {
            marcarAlterado();
        }
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
