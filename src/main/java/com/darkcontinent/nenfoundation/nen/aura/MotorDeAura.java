package com.darkcontinent.nenfoundation.nen.aura;

import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import java.util.function.BooleanSupplier;

/**
 * Motor por sessao. Reserva regenera em ticks de simulacao; operacoes
 * instantaneas no mesmo tick compartilham o teto de output. Isso nao cria
 * stamina nem persiste estado de combate. Tecnicas continuas chegam no M4.
 */
public final class MotorDeAura {
    public enum Gasto {
        PERMITIDO, INVALIDO, NAO_DESPERTO, SEM_AURA, OUTPUT_EXCEDIDO
    }

    /** Contadores somente-leitura do intervalo em que diagnostico esteve ligado. */
    public record Medida(long ticks, long gastos, long recusas, long exaustoes,
            double auraGasta, double auraRecuperada, long deltas) { }

    private final RuntimeNenState estado;
    private final ParametrosDeAura parametros;
    private final BooleanSupplier diagnostico;
    private long tickDoOutput = Long.MIN_VALUE;
    private double outputUsado;
    private long ticks;
    private long gastos;
    private long recusas;
    private long exaustoes;
    private double auraGasta;
    private double auraRecuperada;
    private long deltas;

    public MotorDeAura(RuntimeNenState estado, ParametrosDeAura parametros,
            BooleanSupplier diagnostico) {
        this.estado = estado;
        this.parametros = parametros;
        this.diagnostico = diagnostico;
    }

    /** Atualiza a capacidade antes de qualquer leitura ou gasto, incluindo recarga. */
    public void atualizar(PersistentNenData perfil) {
        this.estado.definirAuraMaxima(AuraFormulas.maxima(perfil, this.parametros));
    }

    public void tick(PersistentNenData perfil) {
        atualizar(perfil);
        // O ESTADO DE NEN ATIVO ENTRA AQUI (ADR-010). O multiplicador e
        // derivado das tecnicas ativas e limitado pelo teto de config dentro
        // da formula -- este metodo nao decide nada sobre ele.
        double recuperada = perfil.awakened()
                ? this.estado.recuperarAura(AuraFormulas.regeneracaoPorTick(
                        this.parametros, this.estado.multiplicadorDeRegeneracao()))
                : 0;
        if (this.diagnostico.getAsBoolean()) {
            this.ticks++;
            this.auraRecuperada += recuperada;
        }
    }

    public double output(PersistentNenData perfil) {
        return AuraFormulas.output(perfil, this.parametros);
    }

    public Gasto gastar(PersistentNenData perfil, double quantidade, long tick) {
        atualizar(perfil);
        if (tick != this.tickDoOutput) {
            this.tickDoOutput = tick;
            this.outputUsado = 0;
        }
        Gasto recusa = !Double.isFinite(quantidade) || quantidade <= 0 ? Gasto.INVALIDO
                : !perfil.awakened() ? Gasto.NAO_DESPERTO
                : quantidade > this.estado.auraAtual() ? Gasto.SEM_AURA
                : quantidade > Math.max(0, output(perfil) - this.outputUsado) ? Gasto.OUTPUT_EXCEDIDO
                : Gasto.PERMITIDO;
        if (recusa != Gasto.PERMITIDO) {
            if (this.diagnostico.getAsBoolean()) this.recusas++;
            return recusa;
        }
        boolean estavaExausto = this.estado.exausto();
        if (!this.estado.gastarAura(quantidade)) throw new IllegalStateException("gasto validado recusado");
        this.outputUsado += quantidade;
        if (this.diagnostico.getAsBoolean()) {
            this.gastos++;
            this.auraGasta += quantidade;
            if (!estavaExausto && this.estado.exausto()) this.exaustoes++;
        }
        return Gasto.PERMITIDO;
    }

    public void registrarDelta() {
        if (this.diagnostico.getAsBoolean()) this.deltas++;
    }

    public Medida medida() {
        return new Medida(this.ticks, this.gastos, this.recusas, this.exaustoes,
                this.auraGasta, this.auraRecuperada, this.deltas);
    }
}
