package com.darkcontinent.nenfoundation.enemy.perception;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Junta visao, audicao e memoria num unico snapshot por tick -- com orcamento.
 *
 * <p><b>O que ele impede, e que nao daria erro nenhum:</b> um mob que varre o
 * mundo todo tick. Com um mob na tela isso e imperceptivel; com uma manada e um
 * ninho, o TPS cai devagar ao longo da sessao e o relato de bug vira "o servidor
 * fica lento com o tempo". Por isso a varredura cara passa obrigatoriamente por
 * {@link PerceptionBudget}, e {@link #varredurasFeitas()} existe para um portao
 * poder REPROVAR quem varrer demais -- regua que ninguem le e folclore.</p>
 *
 * <p><b>Entre duas varreduras o mob nao fica cego.</b> Ele opera de MEMORIA, que
 * decai todo tick. E essa a diferenca entre economizar e ficar burro: o alvo
 * continua existindo, so nao e re-medido a cada tick.</p>
 *
 * <p>Uma instancia por ENTIDADE. Um controller compartilhado seria o erro
 * classico deste projeto -- estado de jogador num campo de singleton -- e aqui
 * ele apareceria como dois mobs perseguindo o alvo um do outro.</p>
 */
public final class PerceptionController {

    /**
     * Afastar-se mais do que isto entre duas VARREDURAS conta como recuo.
     *
     * <p>Nao e botao de balanceamento: e o ruido de uma medida contra a outra. Com
     * visao a cada 6 ticks, um jogador andando de lado cobre quase meio bloco sem
     * estar fugindo, e um limiar menor faria todo passo lateral parecer recuo.</p>
     */
    private static final double TOLERANCIA_DE_RECUO = 0.6D;

    private final PerceptionBudget orcamento;
    private final VisionCone cone;
    private final ThreatMemory memoria;
    private final TargetEvaluator avaliador;
    private final PercepcaoDeAura aura;
    private final double alcanceDeAudicao;
    private final int desfasagem;

    /** Sons deste tick; esvaziados em toda leitura para nao virar fila infinita. */
    private final List<HearingEvent> sonsDoTick = new ArrayList<>();

    private long varreduras;
    private PerceptionSnapshot ultimo = PerceptionSnapshot.vazio(false);
    private double distanciaAnterior = Double.NaN;

    public PerceptionController(PerceptionBudget orcamento, VisionCone cone, ThreatMemory memoria,
            TargetEvaluator avaliador, PercepcaoDeAura aura, double alcanceDeAudicao, int desfasagem) {
        this.orcamento = Objects.requireNonNull(orcamento, "orcamento ausente");
        this.cone = Objects.requireNonNull(cone, "cone ausente");
        this.memoria = Objects.requireNonNull(memoria, "memoria ausente");
        this.avaliador = Objects.requireNonNull(avaliador, "avaliador ausente");
        this.aura = Objects.requireNonNull(aura, "porta de aura ausente");
        if (!Double.isFinite(alcanceDeAudicao) || alcanceDeAudicao <= 0.0D) {
            throw new IllegalArgumentException("alcance de audicao invalido");
        }
        this.alcanceDeAudicao = alcanceDeAudicao;
        // Math.floorMod aceita negativo; mesmo assim o desfase e normalizado aqui
        // para que dois mobs com ids opostos nao caiam no mesmo tick por acidente.
        this.desfasagem = Math.floorMod(desfasagem, Math.max(1, orcamento.ticksDeScanCaro()));
    }

    /** Quantas vezes o sensor CARO foi chamado. E a regua do orcamento. */
    public long varredurasFeitas() { return varreduras; }

    public PerceptionSnapshot ultimo() { return ultimo; }

    public Optional<UUID> alvoLembrado() { return memoria.alvo(); }

    /**
     * Entrega um som ao mob. Quem chama e o lado que JA estava tratando o evento.
     *
     * <p>Nao ha varredura de sons aqui de proposito: ouvir por scan custaria o
     * mesmo que ver, sem nenhum dos limites da visao, e o custo seria invisivel
     * porque ninguem procura gasto em "audicao".</p>
     */
    public void ouvir(HearingEvent evento) {
        Objects.requireNonNull(evento, "evento de audicao ausente");
        if (evento.audivel(alcanceDeAudicao)) sonsDoTick.add(evento);
    }

    /**
     * Um tick de percepcao.
     *
     * @param tickDoMundo relogio do servidor; quem decide o intervalo e o orcamento
     * @param sensor porta para o mundo, chamada SO quando o orcamento autoriza
     * @param vidaCritica fato do mob, medido por quem chama
     * @param oportunidadeDeEmboscada fato do mob (enterrado, escondido), nao da visao
     */
    public PerceptionSnapshot tick(int tickDoMundo, SensorDeVisao sensor,
            boolean vidaCritica, boolean oportunidadeDeEmboscada) {
        Objects.requireNonNull(sensor, "sensor ausente");

        Optional<TargetCandidate> visto = Optional.empty();
        if (orcamento.visaoNesteTick(tickDoMundo, desfasagem)) {
            varreduras++;
            List<TargetCandidate> candidatos = aura.aplicar(List.copyOf(sensor.varrer()));
            visto = avaliador.melhor(candidatos)
                    .filter(c -> cone.enxerga(c.distancia(), c.cossenoDoOlhar(), c.linhaDeVisao()));
            visto.ifPresent(c -> memoria.reforcar(c.id(), c.distancia()));
        }

        boolean audivel = processarSons();
        memoria.tick();

        PerceptionSnapshot snapshot = montar(visto, audivel, vidaCritica, oportunidadeDeEmboscada);
        this.ultimo = snapshot;
        return snapshot;
    }

    /**
     * Esquecimento explicito: morte, unload, troca de dimensao, alvo invalido.
     *
     * <p>Quem liga, desliga. A limpeza mora AQUI, e nao espalhada pelos pontos de
     * saida da entidade, porque um ponto de saida esquecido nao da erro -- deixa
     * o mob perseguindo um fantasma ate o servidor reiniciar.</p>
     */
    public void limpar() {
        memoria.esquecer();
        sonsDoTick.clear();
        distanciaAnterior = Double.NaN;
        ultimo = PerceptionSnapshot.vazio(false);
    }

    /** Sons do tick reforcam a memoria do alvo lembrado; o resto e ruido. */
    private boolean processarSons() {
        if (sonsDoTick.isEmpty()) return false;
        boolean ouviuOAlvo = false;
        UUID lembrado = memoria.alvo().orElse(null);
        HearingEvent maisProximo = null;
        for (HearingEvent som : sonsDoTick) {
            if (lembrado != null && lembrado.equals(som.fonte())) {
                memoria.reforcar(som.fonte(), som.distancia());
                ouviuOAlvo = true;
            } else if (maisProximo == null || som.distancia() < maisProximo.distancia()) {
                maisProximo = som;
            }
        }
        // Sem alvo na memoria, o som mais proximo VIRA a suspeita -- e assim que
        // um mob ocioso investiga um barulho sem nunca ter visto quem o fez.
        if (!ouviuOAlvo && lembrado == null && maisProximo != null) {
            memoria.reforcar(maisProximo.fonte(), maisProximo.distancia());
            ouviuOAlvo = true;
        }
        sonsDoTick.clear();
        return ouviuOAlvo;
    }

    private PerceptionSnapshot montar(Optional<TargetCandidate> visto, boolean audivel,
            boolean vidaCritica, boolean oportunidadeDeEmboscada) {
        if (!memoria.lembra()) {
            distanciaAnterior = Double.NaN;
            return PerceptionSnapshot.vazio(vidaCritica);
        }
        UUID alvo = memoria.alvo().orElseThrow();
        double distancia = memoria.ultimaDistancia();

        // "Recuando" so tem sentido entre duas medidas da MESMA fonte; comparar
        // uma distancia vista com uma ouvida produziria recuo imaginario, e o mob
        // voltaria para casa no meio de uma perseguicao sem que nada acusasse.
        boolean recuando = visto.isPresent() && !Double.isNaN(distanciaAnterior)
                && distancia > distanciaAnterior + TOLERANCIA_DE_RECUO;
        if (visto.isPresent()) distanciaAnterior = distancia;

        return new PerceptionSnapshot(alvo, visto.isPresent(), audivel,
                visto.map(TargetCandidate::dentroDoTerritorio).orElse(false),
                recuando, memoria.restante(), distancia, oportunidadeDeEmboscada, vidaCritica);
    }
}
