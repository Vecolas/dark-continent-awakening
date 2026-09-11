package com.darkcontinent.nenfoundation.nen.aura;

import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;

/**
 * Motor de recursos primários de um jogador: Aura Pool (POP) e Aura Output (AOP).
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. OS PARAMETROS NUMERICOS SAO INJETADOS, NAO LIDOS DIRETAMENTE DA CONFIG.
 * O metodo de producao usa Parametros.daConfig(), que le a config na hora.
 * Nos testes, Parametros.de criam conjuntos de valores sem precisar carregar o jogo.
 * Isso impede o erro "Cannot get config value before config is loaded".
 *
 * <p>2. DIRTY FLAG GRANULAR. DirtyFlag distingue se so a Aura mudou, se o Output
 * mudou, ou ambos. O sync envia apenas o que precisa.
 *
 * <p>3. INVARIANTES SAO IMPOSTOS AQUI. Aura nunca fica negativa, nunca NaN, nunca
 * acima do maximo. Output nunca passa de 0 a 1.0 (100%).
 *
 * <p>4. O MAXIMO E DERIVADO, NAO GUARDADO. Calculado na hora a partir do
 * perfil e dos parametros. Guardar o maximo aqui seria uma segunda fonte.
 */
public final class AuraPool {

    /** Granularidade do dirty flag. */
    public enum DirtyFlag {
        LIMPO, AURA, OUTPUT, AMBOS;

        /** Une dois flags sem perder informacao de nenhum deles. */
        public DirtyFlag unirCom(DirtyFlag outro) {
            if (this == LIMPO) return outro;
            if (outro == LIMPO) return this;
            if (this == outro) return this;
            return AMBOS;
        }

        public boolean inclui(DirtyFlag alvo) {
            return this == alvo || this == AMBOS;
        }
    }

    /** Parametros numericos que governam o pool. */
    public record Parametros(
            double auraBaseMaxima,
            double auraRegenPorTick,
            double auraLimiarExaustao) {

        /** Fabrica de producao: le a config na hora. */
        public static Parametros daConfig() {
            return new Parametros(
                    NenConfig.auraBaseMaxima(),
                    NenConfig.auraRegenPorTick(),
                    NenConfig.auraLimiarExaustao());
        }

        /** Fabrica de teste: valores explicitos, sem config carregada. */
        public static Parametros de(
                double auraBaseMaxima, double auraRegenPorTick, double auraLimiarExaustao) {
            return new Parametros(auraBaseMaxima, auraRegenPorTick, auraLimiarExaustao);
        }
    }

    private double auraAtual;
    private float outputPercent = 1.0F; // 100% por padrao
    private DirtyFlag dirty = DirtyFlag.LIMPO;

    public double auraAtual() { return this.auraAtual; }
    public float outputPercent() { return this.outputPercent; }

    public static double auraMaxima(PersistentNenData perfil, Parametros p) {
        return p.auraBaseMaxima() * (1.0D + perfil.auraPotential());
    }

    public DirtyFlag dirty() { return this.dirty; }

    public boolean gastarAura(double custo, PersistentNenData perfil, Parametros p) {
        if (custo < 0) throw new IllegalArgumentException("custo nao pode ser negativo: " + custo);
        if (custo == 0.0D) return true;
        if (this.auraAtual < custo) return false;
        this.auraAtual = Math.max(0.0D, Math.min(this.auraAtual - custo, auraMaxima(perfil, p)));
        this.dirty = this.dirty.unirCom(DirtyFlag.AURA);
        return true;
    }

    /** Ajusta o output limitando entre 0.0 (0%) e 1.0 (100%). */
    public void ajustarOutput(float novoPercent) {
        float ajustado = Math.max(0.0F, Math.min(novoPercent, 1.0F));
        if (ajustado != this.outputPercent) {
            this.outputPercent = ajustado;
            this.dirty = this.dirty.unirCom(DirtyFlag.OUTPUT);
        }
    }

    public void tick(PersistentNenData perfil, Parametros p) {
        double maxAura = auraMaxima(perfil, p);
        if (this.auraAtual < maxAura) {
            double nova = Math.min(this.auraAtual + p.auraRegenPorTick(), maxAura);
            if (nova != this.auraAtual) {
                this.auraAtual = nova;
                this.dirty = this.dirty.unirCom(DirtyFlag.AURA);
            }
        }
    }

    public void resetarParaMaximo(PersistentNenData perfil, Parametros p) {
        this.auraAtual = auraMaxima(perfil, p);
        this.outputPercent = 1.0F;
        this.dirty = DirtyFlag.AMBOS;
    }

    public void marcarSincronizado() {
        this.dirty = DirtyFlag.LIMPO;
    }

    public boolean emExaustaoDeAura(PersistentNenData perfil, Parametros p) {
        return this.auraAtual <= auraMaxima(perfil, p) * p.auraLimiarExaustao();
    }
}