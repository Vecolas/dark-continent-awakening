package com.darkcontinent.nenfoundation.nen.technique;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Qual tecnica MANDA quando ha mais de uma ligada.
 *
 * <p><b>POR QUE ISTO EXISTE, e por que no dominio.</b> Ate 2026-09-25 esta
 * pergunta era respondida em tres lugares, cada um com a propria lista
 * incompleta:
 *
 * <pre>
 *   ModoVisualDeTecnica   List.of(Zetsu, Ren, Ten)          o que VOCE ve
 *   PresencaDeAura        if Zetsu / Ken / Ren / Ten        o que os OUTROS veem
 *   EstadoDeNenNaHud      os sete                            o chip do HUD
 * </pre>
 *
 * <p>As duas primeiras nao conheciam Gyo, Shu nem Ko, e a primeira tambem nao
 * conhecia Ken. Como as quatro podem estar ativas SOZINHAS -- elas so excluem
 * Zetsu (e Ko exclui Gyo) --, o resultado em jogo era: <b>ligar Ken apagava a
 * propria aura</b> enquanto terceiros continuavam vendo, e <b>ligar Ko sozinho
 * nao produzia sinal nenhum para ninguem</b>. Nada disso levantava excecao.
 *
 * <p>A lista mora no DOMINIO porque a pergunta e de dominio: "qual tecnica e a
 * dominante" nao depende de quem esta olhando. Quem olha muda o CODOMINIO -- o
 * cliente traduz para um modo de shell, o servidor traduz para um sinal de
 * presenca --, e essas duas traducoes sao adapters. Se a lista morasse no
 * cliente, o servidor nao poderia usa-la sem quebrar a regra de dependencia.
 *
 * <p><b>A COMPLETUDE E COBRADA.</b> {@code PrecedenciaDeTecnicasTest} varre
 * {@code nen/technique} atras de todo {@code ID} publico e exige que ele esteja
 * aqui. Uma tecnica nova que esqueca esta lista reprova o build nomeando-se.
 */
public final class PrecedenciaDeTecnicas {

    /**
     * Do maior compromisso para o menor.
     *
     * <p>ZETSU PRIMEIRO, e isso nao e estilo: Zetsu e a supressao deliberada da
     * aura, e qualquer coisa que o vencesse anunciaria justamente quem esta
     * tentando sumir. Ele tambem exclui todas as outras, entao a disputa nao
     * acontece em jogo -- mas a regra precisa existir ANTES da primeira tecnica
     * que combine com ele, e nao depois.
     *
     * <p>KO ANTES DE KEN E DE REN. Ko poe quase toda a aura numa regiao e deixa
     * o resto do corpo nu, com relogio proprio correndo. E o estado mais caro de
     * se estar sem perceber. E a escolha nao custa nada na tela: Ko, Ken e Ren
     * desenham o MESMO modo de shell, entao o que muda entre eles e so a cor.
     *
     * <p>KEN ANTES DE REN, pela mesma razao que {@code PresencaDeAura} ja
     * escrevia: Ken e o envelope maior. Os dois se excluem hoje, entao a ordem e
     * inobservavel -- e por isso ela precisa estar escrita.
     *
     * <p>TEN POR ULTIMO entre as que emitem: e o estado de repouso. Qualquer
     * outra coisa ligada e mais digna de nota.
     *
     * <p>SHU E GYO DEPOIS DE TEN. As duas convivem com Ten e nao mudam o
     * envelope -- elas redistribuem. Se ha Ten ligado junto, quem descreve o
     * corpo e o Ten; a concentracao aparece pela ALOCACAO, que viaja em separado.
     */
    private static final List<ResourceLocation> ORDEM = List.of(
            Zetsu.ID, Ko.ID, Ken.ID, Ren.ID, Ten.ID, Shu.ID, Gyo.ID);

    private PrecedenciaDeTecnicas() {
    }

    /** A ordem, imutavel. */
    public static List<ResourceLocation> ordem() {
        return ORDEM;
    }

    /** As tecnicas que esta lista conhece. */
    public static Set<ResourceLocation> conhecidas() {
        return Set.copyOf(ORDEM);
    }

    /**
     * A dominante do conjunto, ou vazio quando nenhuma e conhecida.
     *
     * <p>TECNICA DESCONHECIDA NAO VENCE. Um datapack pode registrar id que esta
     * lista nunca viu; deixa-lo dominar faria o cliente escolher um visual para
     * algo cujo significado ele ignora. Ela continua aparecendo na fila de
     * indicadores do HUD, que tem forma neutra para exatamente isso.
     */
    public static Optional<ResourceLocation> dominante(Set<ResourceLocation> ativas) {
        if (ativas == null || ativas.isEmpty()) {
            return Optional.empty();
        }
        for (ResourceLocation id : ORDEM) {
            if (ativas.contains(id)) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }
}
