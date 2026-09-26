package com.darkcontinent.nenfoundation.nen.technique;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.FocoDeAura;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ko: quase toda a aura num ponto, por um golpe.
 *
 * <p>NO CANONE, Ko combina Ten, Zetsu, Ren, Hatsu e Gyo para concentrar
 * praticamente toda a aura numa regiao. O punho fica devastador -- e o resto do
 * corpo <b>perde a defesa</b>. Errar o golpe e levar um no tronco e
 * catastrofico. E a tecnica de risco mais alto do material.
 *
 * <p>O QUE SEPARA KO DE "GYO COM UM NUMERO MAIOR": <b>Ko tem prazo.</b>
 *
 * <p>Essa pergunta ficou em aberto quando a camada de dano foi planejada, e a
 * resposta esta no proprio cânone: Gyo se sustenta -- e percepcao e
 * concentracao que se mantem. Ko e <i>um golpe</i>. Sem prazo, os dois seriam a
 * mesma tecnica com constantes diferentes, e a mais forte tornaria a outra
 * inutil.
 *
 * <p>O prazo tambem e o que torna o risco real: durante a janela, o resto do
 * corpo esta quase nu, e o jogador nao pode desistir no meio para se defender.
 * Ele se comprometeu.
 *
 * <p>{@link StopReason#EXPIRED} EXISTIA DESDE SEMPRE E NINGUEM PODIA USA-LO --
 * o enum previa tecnica com prazo e nao havia nenhuma. Ko e a primeira, como
 * Shu foi a primeira de {@code TARGET_LOST}.
 *
 * <p>ELE EXCLUI GYO. As duas concentram, e deixar as duas ligadas faria a regra
 * "vence a mais concentrada" decidir em silencio qual das escolhas do jogador
 * vale. Com Ken, Ko CONVIVE: concentrar dentro de um Ken e o caminho que o
 * cânone descreve para Ryu.
 */
public final class Ko implements NenTechnique, RedistribuiAura, ConsomeAura,
        ReforcaGolpe {

    /** Id congelado: vai para NBT, datapack e quest. */
    public static final ResourceLocation ID = NenFoundation.id("ko");

    private static final double TICKS_POR_SEGUNDO = 20.0D;

    private final DoubleSupplier custoPorSegundo;
    private final DoubleSupplier fracaoConcentrada;
    private final DoubleSupplier reforco;
    private final IntSupplier duracaoEmTicks;
    private final RelogioDeKo relogio;

    /**
     * O relogio de Ko, injetado inteiro.
     *
     * <p>ELE NAO PODE MORAR NUM CAMPO DA TECNICA: a implementacao e
     * compartilhada por todos os jogadores, e um contador aqui faria dois
     * jogadores em Ko dividirem o mesmo relogio -- o erro numero 2 da lista do
     * CLAUDE.md.
     *
     * <p>AS TRES OPERACOES VEM JUNTAS de proposito. A primeira versao passava
     * so o tick e resolvia a partida e a limpeza por uma classe com estado
     * estatico mutavel -- um service locator disfarcado, com o mesmo problema
     * que ele tenta resolver. Uma interface com tres metodos nao precisa de
     * truque nenhum.
     */
    public interface RelogioDeKo {
        /** Comeca o prazo deste jogador. */
        void iniciar(ServerPlayer jogador, int ticks);

        /** Passa um tick e devolve se o prazo acabou. */
        boolean passarTickEVerSeAcabou(ServerPlayer jogador);

        /** Apaga a contagem deste jogador. */
        void limpar(ServerPlayer jogador);
    }

    public Ko(DoubleSupplier custoPorSegundo, DoubleSupplier fracaoConcentrada,
            IntSupplier duracaoEmTicks, RelogioDeKo relogio, DoubleSupplier reforco) {
        this.custoPorSegundo = custoPorSegundo;
        this.fracaoConcentrada = fracaoConcentrada;
        this.reforco = reforco;
        this.duracaoEmTicks = duracaoEmTicks;
        this.relogio = relogio;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Set<ResourceLocation> incompativeisCom() {
        return Set.of(Zetsu.ID, Gyo.ID);
    }

    /**
     * Onde Ko concentra quando o jogador nao apontou nada.
     *
     * <p>O PUNHO DOMINANTE, e ate 2026-09-26 era a CABECA -- visto em jogo, e
     * era defeito. Ko herdava o padrao de Gyo, que fora escolhido porque "Gyo
     * nos olhos e o uso mais reconhecivel". A justificativa e boa para Gyo e
     * absurda para Ko: no canone Ko e o golpe, com praticamente toda a aura no
     * punho.
     *
     * <p>E NAO ERA SO ESTETICO. {@code NenDanoService} mede o reforco do golpe
     * pela alocacao no BRACO PRINCIPAL. Com a aura na cabeca, o jogador pagava
     * a tecnica mais cara do jogo, ficava com o corpo nu por um segundo, e o
     * soco saia sem o reforco pelo qual ele pagou. Nada disso levantava
     * excecao.
     *
     * <p>O braco vem do jogador, e nao e assumido destro: quem joga canhoto tem
     * a mao dominante a esquerda.
     */
    @Override
    public AlocacaoDeAura alocacaoDesejada(FocoDeAura foco) {
        return AlocacaoDeAura.concentrando(foco.regiaoOu(foco.bracoPrincipal()),
                (float) this.fracaoConcentrada.getAsDouble());
    }

    @Override
    public double custoPorTick() {
        return this.custoPorSegundo.getAsDouble() / TICKS_POR_SEGUNDO;
    }

    @Override
    public TechniqueActivationResult canActivate(ServerPlayer jogador, NenContext ctx) {
        if (!ctx.perfil().awakened()) {
            return TechniqueActivationResult.negado("nenfoundation.error.nao_desperto");
        }
        return TechniqueActivationResult.aceito();
    }

    @Override
    public void onActivate(ServerPlayer jogador, NenContext ctx) {
        // O PRAZO COMECA AQUI, e este e o unico `onActivate` que faz alguma
        // coisa em todo o mod. As outras tecnicas sao derivadas do conjunto de
        // ativas; Ko tem um relogio proprio, e relogio precisa de partida.
        this.relogio.iniciar(jogador, this.duracaoEmTicks.getAsInt());
    }

    @Override
    public void serverTick(ServerPlayer jogador, NenContext ctx) {
        if (this.relogio.passarTickEVerSeAcabou(jogador)) {
            ctx.desligar(ID, StopReason.EXPIRED);
        }
    }

    @Override
    public void onDeactivate(ServerPlayer jogador, NenContext ctx, StopReason motivo) {
        // QUEM LIGA, DESLIGA -- e aqui isso vale para o relogio. Sem esta
        // linha, um Ko interrompido por morte ou conflito deixaria a contagem
        // viva, e o proximo Ko duraria o que sobrou do anterior.
        this.relogio.limpar(jogador);
    }


    @Override
    public double reforcoBase() {
        return this.reforco.getAsDouble();
    }
}
