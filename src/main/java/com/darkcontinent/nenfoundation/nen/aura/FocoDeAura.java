package com.darkcontinent.nenfoundation.nen.aura;

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
 * <p>SEM TIPO DE MINECRAFT, de proposito: assim a regra de cada tecnica da para
 * ser provada sem subir o jogo. Quem monta este objeto e o servidor.
 *
 * <p>O que AINDA nao cabe aqui: Ryu redistribui entre varias regioes ao mesmo
 * tempo, e um foco unico nao descreve isso. Quando Ryu chegar, este record
 * cresce de novo -- e sera a terceira vez que o custo de mudar se paga por
 * haver casos reais para validar.
 *
 * @param regiaoEscolhida onde o jogador mandou concentrar (Gyo, Ko)
 * @param bracoPrincipal  o braco da mao dominante (Shu)
 */
public record FocoDeAura(RegiaoDoCorpo regiaoEscolhida, RegiaoDoCorpo bracoPrincipal) {

    public FocoDeAura {
        if (regiaoEscolhida == null || bracoPrincipal == null) {
            throw new IllegalArgumentException("foco incompleto");
        }
    }

    /**
     * O foco de quem nao escolheu nada.
     *
     * <p>CABECA e destro, que sao os padroes de cada campo pelo motivo proprio
     * de cada um: Gyo nos olhos e o uso mais reconhecivel da tecnica, e destro
     * e o padrao do Minecraft.
     */
    public static FocoDeAura padrao() {
        return new FocoDeAura(RegiaoDoCorpo.CABECA, RegiaoDoCorpo.BRACO_DIREITO);
    }
}
