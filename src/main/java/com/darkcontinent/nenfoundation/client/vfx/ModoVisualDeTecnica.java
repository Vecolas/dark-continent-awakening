package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * De "quais tecnicas estao ligadas" para "o que a tela mostra".
 *
 * <p>ELE E A UNICA PONTE entre o dominio e o visual, e existe para que o resto
 * de {@code client.vfx} nao precise conhecer id de tecnica nenhum. Sem ela,
 * cada pedaco do VFX passaria a comparar {@code ResourceLocation} por conta
 * propria, e a regra de precedencia estaria escrita em quatro lugares.
 *
 * <p>FUNCAO PURA, de proposito: nao toca no Minecraft, nao guarda estado, e por
 * isso a precedencia da para provar sem subir o jogo.
 *
 * <p>ELE NAO DECIDE NADA DE GAMEPLAY. O conjunto que ele recebe ja vem do
 * servidor, pelo delta de runtime. Se o servidor recusou a ativacao, a tecnica
 * nao esta no conjunto e nada acende -- e essa e a diferenca entre mostrar o
 * estado e adivinhar o pedido.
 */
public final class ModoVisualDeTecnica {

    /**
     * A ordem em que uma tecnica ganha a tela quando ha mais de uma ligada.
     *
     * <p>ZETSU PRIMEIRO, e isso nao e detalhe: Zetsu e supressao, e supressao
     * que perde para um brilho nao suprime nada. Hoje ele exclui as outras duas,
     * entao a disputa nao acontece em jogo -- mas a regra precisa existir antes
     * da primeira tecnica que combine com ele, e nao depois.
     *
     * <p>REN ANTES DE TEN porque os dois CONVIVEM, e essa disputa acontece o
     * tempo todo. Ren e o estado mais alto; mostrar Ten enquanto o jogador esta
     * em Ren seria mostrar o menor dos dois.
     */
    private static final List<ResourceLocation> PRECEDENCIA =
            List.of(Zetsu.ID, Ren.ID, Ten.ID);

    private ModoVisualDeTecnica() {
    }

    /** A tecnica que manda na tela, ou vazio quando nao ha nenhuma conhecida. */
    public static Optional<ResourceLocation> dominante(Set<ResourceLocation> ativas) {
        if (ativas == null || ativas.isEmpty()) {
            return Optional.empty();
        }
        for (ResourceLocation id : PRECEDENCIA) {
            if (ativas.contains(id)) {
                return Optional.of(id);
            }
        }
        // TECNICA DESCONHECIDA NAO ACENDE NADA, e e melhor assim: um datapack
        // pode registrar tecnica que este arquivo nunca viu, e inventar um
        // visual para ela seria mostrar ao jogador um efeito que nao significa
        // coisa nenhuma.
        return Optional.empty();
    }

    /** O modo visual correspondente ao conjunto de tecnicas ligadas. */
    public static AuraVisualMode de(Set<ResourceLocation> ativas) {
        return dominante(ativas).map(ModoVisualDeTecnica::modoDe).orElse(AuraVisualMode.OFF);
    }

    private static AuraVisualMode modoDe(ResourceLocation id) {
        if (Zetsu.ID.equals(id)) {
            return AuraVisualMode.ZETSU;
        }
        if (Ren.ID.equals(id)) {
            return AuraVisualMode.REN;
        }
        if (Ten.ID.equals(id)) {
            return AuraVisualMode.TEN;
        }
        return AuraVisualMode.OFF;
    }
}
