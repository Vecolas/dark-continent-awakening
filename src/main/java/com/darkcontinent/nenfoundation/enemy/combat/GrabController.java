package com.darkcontinent.nenfoundation.enemy.combat;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * O agarrao de UM predador: quem esta preso, ha quanto tempo, e quando sai.
 *
 * <p><b>Por que ele existe depois de o sapo ja agarrar (issue #140).</b> O
 * Frog-In-Waiting resolveu o agarrao inteiro dentro da propria classe, e resolveu
 * bem. O problema aparece no segundo bicho: Melanin Lizard e Crab Heavy precisam
 * do MESMO contrato, e "copiar do sapo" produz tres implementacoes parecidas que
 * divergem na primeira correcao. O CLAUDE.md e explicito -- um unico ciclo de
 * vida por familia, nao quatro sistemas parecidos -- e a divergencia aqui custa
 * caro: a que esquecer um ponto de saida deixa um jogador preso ate o restart.</p>
 *
 * <p><b>Ele guarda o UUID, nunca a entidade.</b> Segurar a vitima num campo nao
 * da erro; so impede o objeto de morrer. Quem quiser a entidade pede ao servidor,
 * e vazio e a resposta certa quando ela deixou de existir.</p>
 *
 * <p><b>Ele nao toca em mundo.</b> Nada aqui move, monta, machuca ou teleporta:
 * o controller diz <em>o que aconteceu</em> e quem aplica e a entidade, no lado
 * autoritativo. E o que permite provar as dez saidas sem servidor de pe.</p>
 */
public final class GrabController {

    private final GrabRules regras;
    private final double alturaMaximaDaVitima;
    private final double larguraMaximaDaVitima;

    private UUID vitima;
    private int ticksAgarrado;
    private float danoNoPredador;

    /**
     * @param regras duracao, pulso e dano de escape -- vem do perfil, nunca daqui
     * @param alturaMaximaDaVitima altura maxima que cabe na boca, em blocos
     * @param larguraMaximaDaVitima largura maxima que cabe na boca, em blocos
     */
    public GrabController(GrabRules regras, double alturaMaximaDaVitima, double larguraMaximaDaVitima) {
        this.regras = Objects.requireNonNull(regras, "regras de agarrao ausentes");
        if (!Double.isFinite(alturaMaximaDaVitima) || alturaMaximaDaVitima <= 0.0D
                || !Double.isFinite(larguraMaximaDaVitima) || larguraMaximaDaVitima <= 0.0D) {
            throw new IllegalArgumentException("limite de tamanho de vitima invalido");
        }
        this.alturaMaximaDaVitima = alturaMaximaDaVitima;
        this.larguraMaximaDaVitima = larguraMaximaDaVitima;
    }

    public GrabRules regras() { return regras; }
    public boolean agarrando() { return vitima != null; }
    public Optional<UUID> vitima() { return Optional.ofNullable(vitima); }
    public int ticksAgarrado() { return ticksAgarrado; }
    public float danoNoPredador() { return danoNoPredador; }

    /**
     * Pode agarrar? E se nao, POR QUE.
     *
     * <p>A recusa e consultada antes de qualquer efeito -- nada de morder
     * primeiro e descobrir depois que o alvo nao cabe. O sapo aprendeu isso do
     * jeito caro no sentido contrario: condicionar o agarrao ao dano da mordida
     * parece natural e quebra o mob no dia em que a vitima tem armadura.</p>
     */
    public GrabRefusal podeAgarrar(double alturaDaVitima, double larguraDaVitima,
            boolean alvoJaMontado, boolean alvoValido) {
        if (agarrando()) return GrabRefusal.JA_AGARRANDO;
        if (!alvoValido) return GrabRefusal.ALVO_INVALIDO;
        if (alvoJaMontado) return GrabRefusal.ALVO_OCUPADO;
        if (!Double.isFinite(alturaDaVitima) || !Double.isFinite(larguraDaVitima)
                || alturaDaVitima <= 0.0D || larguraDaVitima <= 0.0D) {
            return GrabRefusal.ALVO_INVALIDO;
        }
        if (alturaDaVitima > alturaMaximaDaVitima || larguraDaVitima > larguraMaximaDaVitima) {
            return GrabRefusal.ALVO_GRANDE_DEMAIS;
        }
        return GrabRefusal.NENHUMA;
    }

    /**
     * Prende a vitima. Chame SO depois de {@link #podeAgarrar} devolver NENHUMA.
     *
     * @throws IllegalStateException se ja houver alguem preso -- uma boca, uma
     *         vitima; sobrescrever deixaria a primeira presa para sempre, com o
     *         relogio dela perdido e nenhum erro no log
     */
    public void agarrar(UUID novaVitima) {
        Objects.requireNonNull(novaVitima, "vitima ausente");
        if (agarrando()) {
            throw new IllegalStateException("agarrao recusado: ja segurando " + vitima
                    + ". Sobrescrever deixaria a primeira vitima presa para sempre.");
        }
        this.vitima = novaVitima;
        this.ticksAgarrado = 0;
        this.danoNoPredador = 0.0F;
    }

    /**
     * Acumula dano sofrido pelo PREDADOR enquanto ele segura alguem.
     *
     * <p>E isto que faz bater funcionar. Fora do agarrao o dano nao conta: somar
     * sempre faria o predador soltar a proxima vitima instantaneamente por causa
     * de uma briga anterior, e o sintoma seria um agarrao que "as vezes nao
     * pega".</p>
     */
    public void registrarDanoNoPredador(float dano) {
        if (!agarrando()) return;
        if (!Float.isFinite(dano) || dano < 0.0F) {
            throw new IllegalArgumentException("dano de escape invalido: " + dano);
        }
        danoNoPredador += dano;
    }

    /**
     * Um tick do agarrao.
     *
     * @param vitimaViva fato medido pelo servidor
     * @param vitimaAindaPresa false quando outro sistema soltou a vitima por fora
     *        (logout, {@code /kill}, outro mod a desmontou)
     * @param predadorVivo fato medido pelo servidor
     * @return o que aconteceu neste tick
     */
    public GrabTick tick(boolean vitimaViva, boolean vitimaAindaPresa, boolean predadorVivo) {
        if (!agarrando()) return GrabTick.PARADO;

        if (!vitimaAindaPresa) {
            // A vitima sumiu por fora do nosso ciclo. Sem esta checagem o predador
            // ficaria "agarrando" um fantasma, e o relogio correria sozinho ate o
            // fim sem nada acontecer -- inclusive impedindo o proximo agarrao.
            return new GrabTick(false, soltar(GrabRelease.VITIMA_SUMIU));
        }

        ticksAgarrado++;
        boolean pulso = regras.aplicaDano(ticksAgarrado);

        if (!predadorVivo) return new GrabTick(pulso, soltar(GrabRelease.PREDADOR_MORTO));
        if (!vitimaViva) return new GrabTick(pulso, soltar(GrabRelease.VITIMA_MORTA));
        if (regras.soltaPorDano(danoNoPredador)) return new GrabTick(pulso, soltar(GrabRelease.DANO));
        if (regras.soltaPorTempo(ticksAgarrado)) return new GrabTick(pulso, soltar(GrabRelease.TEMPO));
        return new GrabTick(pulso, null);
    }

    /**
     * SAIDA UNICA. Todo caminho -- tempo, dano, morte, unload, dimensao,
     * teleporte, interrupcao -- passa por aqui.
     *
     * @return o motivo, para quem chamou aplicar a consequencia
     */
    public GrabRelease soltar(GrabRelease motivo) {
        Objects.requireNonNull(motivo, "soltura sem motivo");
        vitima = null;
        ticksAgarrado = 0;
        danoNoPredador = 0.0F;
        return motivo;
    }

    /** Limpeza sem consequencia: morte do predador, remocao, troca de dimensao. */
    public void limpar() {
        soltar(GrabRelease.UNLOAD);
    }

    /**
     * O que um tick de agarrao produziu.
     *
     * @param pulsoDeDano se a vitima deve levar um pulso NESTE tick
     * @param soltura o motivo da soltura, ou {@code null} se o agarrao continua
     */
    public record GrabTick(boolean pulsoDeDano, GrabRelease soltura) {
        /** Nao ha agarrao em curso. */
        public static final GrabTick PARADO = new GrabTick(false, null);

        public boolean soltou() { return soltura != null; }
    }
}
