package com.darkcontinent.nenfoundation.enemy.combat;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A teia de UMA Spider Webber: quem esta preso, ha quanto tempo, e por onde sai.
 *
 * <p><b>Estado de instancia, nunca de classe.</b> Este projeto ja sabe que vai
 * cometer o erro de guardar estado de jogador num campo da classe; a versao dele
 * aqui seria um contador estatico -- e o sintoma seria a primeira aranha do
 * esquadrao soltando a presa da segunda. Uma instancia por mob.</p>
 *
 * <p><b>Ela guarda o UUID, nunca a entidade.</b> Segurar a vitima num campo nao
 * da erro; so impede o objeto de morrer. Quem quiser a entidade pede ao servidor,
 * e vazio e a resposta certa quando ela deixou de existir -- resposta que este
 * estado traduz em {@link SolturaDaTeia#PRESA_SUMIU}.</p>
 *
 * <p><b>Ela nao toca em mundo.</b> Nada aqui move, imobiliza, machuca ou manda
 * mensagem: o estado diz <em>o que aconteceu</em> e quem aplica e a entidade, no
 * lado autoritativo. E isso que permite provar as cinco saidas sem servidor de
 * pe -- e as cinco precisam de prova, porque a que faltar deixa um jogador preso
 * ate o restart.</p>
 */
public final class EstadoDeTeia {

    private final RegrasDeTeia regras;

    private UUID presa;
    private int ticksRestantes;
    private float danoNaFiandeira;

    public EstadoDeTeia(RegrasDeTeia regras) {
        this.regras = Objects.requireNonNull(regras, "regras de teia ausentes: sem elas nao ha"
                + " prazo nem limiar de rompimento, e a presa ficaria sem nenhuma das duas saidas");
    }

    public RegrasDeTeia regras() { return regras; }
    public boolean prendendo() { return presa != null; }
    public Optional<UUID> presa() { return Optional.ofNullable(presa); }
    public int ticksRestantes() { return ticksRestantes; }
    public float danoNaFiandeira() { return danoNaFiandeira; }

    /**
     * Prende alguem. Chame SO depois de {@link RegrasDeTeia#podeLancar} aprovar.
     *
     * @throws IllegalStateException se ja houver alguem preso -- uma fiandeira,
     *         uma presa. Sobrescrever nao daria erro: deixaria a PRIMEIRA vitima
     *         imobilizada com o relogio de outra pessoa, e ninguem ligaria as
     *         duas coisas
     */
    public void prender(UUID nova) {
        Objects.requireNonNull(nova, "presa ausente");
        if (prendendo()) {
            throw new IllegalStateException("teia recusada: a fiandeira ja segura " + presa
                    + ". Uma segunda teia orfaniza o relogio da primeira, e a primeira vitima"
                    + " fica presa sem prazo.");
        }
        this.presa = nova;
        this.ticksRestantes = regras.ticksDeImobilizacao();
        this.danoNaFiandeira = 0.0F;
    }

    /**
     * Soma dano levado pela ARANHA enquanto ela segura alguem.
     *
     * <p>E isto que faz bater funcionar: o fio sai da fiandeira dela, e quem
     * acerta a fiandeira rasga o fio. Fora do agarramento o dano nao conta --
     * somar sempre faria a proxima presa ser solta instantaneamente por causa de
     * uma briga anterior, e o sintoma seria uma teia que "as vezes nao pega".</p>
     *
     * <p>O valor e o dano medido pelo servidor. Nenhum pacote de cliente entra
     * aqui: se entrasse, qualquer um se soltaria afirmando ter batido.</p>
     */
    public void registrarDanoNaFiandeira(float dano) {
        if (!prendendo()) return;
        if (!Float.isFinite(dano) || dano < 0.0F) {
            throw new IllegalArgumentException("dano de rompimento invalido: " + dano
                    + ". Um NaN somado aqui contamina o acumulado para sempre, e a comparacao com"
                    + " o limiar passa a ser falsa em todo tick -- a teia nunca mais rasgaria");
        }
        danoNaFiandeira += dano;
    }

    /**
     * Um tick da teia.
     *
     * <p><b>A ordem das saidas e a ordem das prioridades.</b> Presa e predador
     * vem antes do dano e do relogio porque um dos dois ter sumido invalida as
     * outras duas perguntas. O dano vem antes do relogio porque quem reagiu tem
     * de sair no MESMO tick em que reagiu: descontar o tick primeiro deixaria o
     * ultimo golpe parecendo ter chegado tarde, e "bati e nao soltou" e o relato
     * que faz o jogador parar de tentar.</p>
     *
     * @param presaViva fato medido pelo servidor
     * @param predadorVivo fato medido pelo servidor
     * @return a saida deste tick; {@link SolturaDaTeia#NENHUMA} enquanto segura
     */
    public SolturaDaTeia tick(boolean presaViva, boolean predadorVivo) {
        if (!prendendo()) return SolturaDaTeia.NENHUMA;
        if (!presaViva) return soltar(SolturaDaTeia.PRESA_SUMIU);
        if (!predadorVivo) return soltar(SolturaDaTeia.PREDADOR_CAIU);
        if (danoNaFiandeira >= regras.danoQueLiberta()) return soltar(SolturaDaTeia.FIO_ROMPIDO);
        ticksRestantes--;
        if (ticksRestantes <= 0) return soltar(SolturaDaTeia.TEMPO_ESGOTADO);
        return SolturaDaTeia.NENHUMA;
    }

    /**
     * Limpeza simetrica: morte, remocao, unload, troca de dimensao.
     *
     * <p>Os tres campos saem JUNTOS. Zerar so o relogio deixaria o dano acumulado
     * pronto para rasgar a teia seguinte antes de ela existir; zerar so a presa
     * deixaria um relogio correndo sem ninguem dentro. Estado pela metade nao da
     * erro -- so aparece na tela de quem joga, um encontro depois.</p>
     */
    public void limpar() {
        presa = null;
        ticksRestantes = 0;
        danoNaFiandeira = 0.0F;
    }

    private SolturaDaTeia soltar(SolturaDaTeia motivo) {
        limpar();
        return motivo;
    }
}
