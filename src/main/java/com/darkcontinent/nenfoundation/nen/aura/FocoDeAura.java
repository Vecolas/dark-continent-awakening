package com.darkcontinent.nenfoundation.nen.aura;

import java.util.Optional;

/**
 * Os fatos de UM jogador que as tecnicas de distribuicao precisam saber.
 *
 * <p>ELE CRESCEU QUANDO A SEGUNDA TECNICA CHEGOU, e era esse o combinado. A
 * primeira versao passava so um {@code RegiaoDoCorpo} -- bastava para Gyo, que
 * concentra onde o jogador escolheu. Shu concentra no braco da mao que segura o
 * item, que e outro fato, e a escolha de Gyo nao serve para ela.
 *
 * <p>A alternativa seria Shu ignorar o argumento e assumir o braco direito.
 * Funcionaria para a maioria e estaria errado para quem joga canhoto -- um
 * defeito que nao da erro, so poe a aura no braco errado de algumas pessoas.
 *
 * <p><b>ELE CRESCEU DE NOVO EM 2026-09-26, e o motivo foi visto em jogo:</b> Ko
 * concentrava na CABECA. O campo era um {@code RegiaoDoCorpo} ja resolvido, e
 * quem o resolvia era {@code NenGyoService}, com um padrao unico escolhido
 * pensando em Gyo -- <i>"Gyo nos olhos e o uso mais reconhecivel"</i>. Ko
 * herdava esse padrao, e concentrar 95% da aura na cabeca e o oposto do que a
 * tecnica faz: Ko e o golpe, e o reforco e medido no BRACO.
 *
 * <p>Agora a escolha e um {@link Optional}, e <b>cada tecnica declara o proprio
 * padrao</b> em {@link #regiaoOu}. {@code regiaoEscolhida()} foi REMOVIDO de
 * proposito: sem ele, o compilador obriga toda tecnica nova a responder "e se o
 * jogador nao escolher nada?" -- que e a pergunta cuja resposta silenciosa
 * causou o defeito.
 *
 * <p>SEM TIPO DE MINECRAFT, de proposito: assim a regra de cada tecnica da para
 * ser provada sem subir o jogo. Quem monta este objeto e o servidor.
 *
 * <p>O que AINDA nao cabe aqui: Ryu redistribui entre varias regioes ao mesmo
 * tempo, e um foco unico nao descreve isso. Quando Ryu chegar, este record
 * cresce de novo.
 *
 * @param escolha        a regiao que o jogador apontou, se apontou alguma
 * @param bracoPrincipal o braco da mao dominante (Shu, e o padrao de Ko)
 */
public record FocoDeAura(Optional<RegiaoDoCorpo> escolha, RegiaoDoCorpo bracoPrincipal) {

    public FocoDeAura {
        if (escolha == null || bracoPrincipal == null) {
            throw new IllegalArgumentException("foco incompleto");
        }
    }

    /** Atalho para quem tem a regiao em maos. */
    public static FocoDeAura de(RegiaoDoCorpo escolhida, RegiaoDoCorpo bracoPrincipal) {
        return new FocoDeAura(Optional.ofNullable(escolhida), bracoPrincipal);
    }

    /**
     * A regiao escolhida, ou o padrao DESTA tecnica.
     *
     * <p>O PADRAO VEM DE QUEM PERGUNTA, e nao de um campo resolvido antes. Gyo
     * nasce na cabeca porque percepcao mora nos olhos; Ko nasce no punho porque
     * Ko e o golpe. Sao respostas diferentes para a mesma ausencia de escolha, e
     * um padrao unico so pode estar certo para uma delas.
     */
    public RegiaoDoCorpo regiaoOu(RegiaoDoCorpo padrao) {
        if (padrao == null) {
            throw new IllegalArgumentException("padrao obrigatorio");
        }
        return this.escolha.orElse(padrao);
    }

    /** Se o jogador apontou alguma regiao nesta sessao. */
    public boolean escolheu() {
        return this.escolha.isPresent();
    }

    /**
     * O foco de quem nao escolheu nada.
     *
     * <p>SEM REGIAO, e destro. A ausencia e o dado: cada tecnica decide o que
     * fazer com ela. Destro e o padrao do Minecraft.
     */
    public static FocoDeAura padrao() {
        return new FocoDeAura(Optional.empty(), RegiaoDoCorpo.BRACO_DIREITO);
    }
}
