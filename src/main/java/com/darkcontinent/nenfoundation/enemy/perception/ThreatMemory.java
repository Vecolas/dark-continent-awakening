package com.darkcontinent.nenfoundation.enemy.perception;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Memoria de UMA ameaca, com prazo de validade.
 *
 * <p><b>Ela guarda o ID, e nunca a entidade.</b> Segurar uma referencia de
 * entidade num campo de mob e como um chunk deixa de descarregar: nada da erro,
 * o objeto so nao morre. Com o uuid, quem quiser o alvo de volta pede ao
 * servidor -- e se ele nao existir mais, a resposta e vazia, que e exatamente a
 * resposta certa.</p>
 *
 * <p><b>Ela expira.</b> Memoria sem prazo transforma "perdi de vista" em
 * "persigo para sempre", e o mob nunca volta para casa. O decaimento acontece em
 * {@link #tick()}, chamado uma vez por tick pelo controller -- nao ha relogio
 * proprio aqui, porque dois relogios para a mesma verdade divergem.</p>
 */
public final class ThreatMemory {
    private final int ticksDeMemoria;
    private UUID alvo;
    private int restante;
    private double ultimaDistancia = Double.NaN;

    public ThreatMemory(int ticksDeMemoria) {
        if (ticksDeMemoria < 1) throw new IllegalArgumentException("memoria tem de durar ao menos 1 tick");
        this.ticksDeMemoria = ticksDeMemoria;
    }

    public int ticksDeMemoria() { return ticksDeMemoria; }
    public int restante() { return restante; }
    public boolean lembra() { return restante > 0 && alvo != null; }
    public Optional<UUID> alvo() { return lembra() ? Optional.of(alvo) : Optional.empty(); }
    public double ultimaDistancia() { return ultimaDistancia; }

    /** Ver ou ouvir o alvo RECARREGA a memoria inteira; nao soma, recarrega. */
    public void reforcar(UUID id, double distancia) {
        Objects.requireNonNull(id, "ameaca sem id");
        if (!Double.isFinite(distancia) || distancia < 0.0D) {
            throw new IllegalArgumentException("distancia de ameaca invalida");
        }
        if (!id.equals(alvo)) ultimaDistancia = Double.NaN;
        this.alvo = id;
        this.restante = ticksDeMemoria;
        this.ultimaDistancia = distancia;
    }

    /** Um tick de esquecimento. Chegando a zero, a memoria se limpa INTEIRA. */
    public void tick() {
        if (restante == 0) return;
        restante--;
        if (restante == 0) esquecer();
    }

    /**
     * Esquecimento explicito -- morte do alvo, troca de dimensao, unload.
     *
     * <p>Limpa os tres campos juntos. Zerar so o contador deixaria o uuid vivo e
     * a proxima leitura reconstruiria um alvo que ja nao existe; limpar so o uuid
     * deixaria {@link #lembra()} mentindo. Estado pela metade e pior do que
     * estado errado, porque so um dos dois aparece nos testes.</p>
     */
    public void esquecer() {
        alvo = null;
        restante = 0;
        ultimaDistancia = Double.NaN;
    }
}
