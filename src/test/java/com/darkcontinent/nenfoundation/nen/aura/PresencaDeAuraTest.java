package com.darkcontinent.nenfoundation.nen.aura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O que os outros percebem -- e, principalmente, o que eles NAO percebem.
 *
 * <p>Esta e a tabela que substituiu uma decisao que morava no cliente. Ela cabe
 * inteira num teste puro justamente porque nao depende de nada do jogo: e uma
 * funcao de "estado do alvo" para "um de tres valores".
 */
class PresencaDeAuraTest {

    @Test
    @DisplayName("Zetsu e indistinguivel de quem nunca despertou")
    void zetsuNaoVaza() {
        // ESTE E O TESTE QUE JUSTIFICA O DESENHO INTEIRO.
        //
        // Nao basta que o cliente "nao desenhe" quem esta em Zetsu: se o valor
        // que atravessa a rede for diferente, um cliente modificado le a
        // diferenca e desenha assim mesmo. O unico jeito de Zetsu funcionar
        // contra alguem disposto a trapacear e os dois casos serem o MESMO
        // valor.
        // COM REN LIGADO JUNTO, de proposito. Com Zetsu sozinho este teste era
        // fraco: apagar a supressao de Zetsu ainda devolvia NENHUM, porque a
        // tecnica caia no padrao seguro do final. Descoberto alimentando o
        // portao com o defeito -- ele passava com a regra removida.
        //
        // Com uma tecnica ruidosa no conjunto, so a supressao explicita salva.
        SinalDeAura emZetsu = PresencaDeAura.percebida(true, Set.of(Ren.ID, Zetsu.ID));
        SinalDeAura nuncaDespertou = PresencaDeAura.percebida(false, Set.of());

        assertEquals(nuncaDespertou, emZetsu,
                "Quem esta em Zetsu emite um sinal DIFERENTE de quem nao tem Nen."
                        + " Qualquer diferenca aqui e um vazamento: o cliente"
                        + " modificado le o byte e mostra exatamente quem estava"
                        + " tentando se esconder.");
        assertEquals(SinalDeAura.NENHUM, emZetsu);
    }

    @Test
    @DisplayName("Zetsu vence Ten e Ren, mesmo ligados juntos")
    void zetsuVenceTudo() {
        // Hoje Zetsu EXCLUI os dois, entao a combinacao nao acontece em jogo --
        // e por isso a regra precisa de teste. Um Zetsu que perdesse para Ten
        // anunciaria justamente quem esta tentando sumir, e o defeito ficaria
        // dormindo ate a primeira tecnica que combine com ele.
        assertEquals(SinalDeAura.NENHUM,
                PresencaDeAura.percebida(true, Set.of(Ten.ID, Ren.ID, Zetsu.ID)));
    }

    @Test
    @DisplayName("Ren vence Ten: e o estado que se sente de longe")
    void renVenceTen() {
        assertEquals(SinalDeAura.REN, PresencaDeAura.percebida(true, Set.of(Ten.ID, Ren.ID)),
                "Com Ten e Ren ligados, os outros percebem o menor dos dois."
                        + " Ten e Ren convivem de proposito, entao esta disputa"
                        + " acontece o tempo todo.");
        assertEquals(SinalDeAura.TEN, PresencaDeAura.percebida(true, Set.of(Ten.ID)));
        assertEquals(SinalDeAura.REN, PresencaDeAura.percebida(true, Set.of(Ren.ID)));
    }

    @Test
    @DisplayName("Ten e Ren sao distinguiveis entre si")
    void tenERenNaoSeConfundem() {
        // Sem isto, colapsar os dois em "tem aura" passaria em todo o resto
        // deste arquivo -- e o jogador perderia a unica informacao tatica que
        // este canal existe para dar.
        assertNotEquals(PresencaDeAura.percebida(true, Set.of(Ten.ID)),
                PresencaDeAura.percebida(true, Set.of(Ren.ID)));
    }

    @Test
    @DisplayName("desperto sem tecnica nao anuncia nada")
    void despertoQuietoNaoAnuncia() {
        assertEquals(SinalDeAura.NENHUM, PresencaDeAura.percebida(true, Set.of()),
                "Ter Nen nao e estar usando Nen. Anunciar presenca so por o"
                        + " jogador ter despertado contaria a todo mundo quem tem"
                        + " Nen, o tempo todo.");
        assertEquals(SinalDeAura.NENHUM, PresencaDeAura.percebida(true, null));
    }

    @Test
    @DisplayName("nao-desperto com tecnica ativa tambem nao anuncia")
    void naoDespertoNaoAnuncia() {
        // Estado que nao deveria existir. Se existir, o padrao seguro e o
        // silencio: e melhor nao mostrar aura de quem nao devia ter do que
        // mostrar e ninguem entender de onde veio.
        assertEquals(SinalDeAura.NENHUM, PresencaDeAura.percebida(false, Set.of(Ren.ID)));
    }

    @Test
    @DisplayName("tecnica desconhecida nao inventa presenca")
    void desconhecidaNaoAnuncia() {
        assertEquals(SinalDeAura.NENHUM, PresencaDeAura.percebida(true,
                        Set.of(ResourceLocation.fromNamespaceAndPath("outromod", "hatsu_x"))),
                "Uma tecnica que este codigo nao conhece anunciou presenca. O"
                        + " padrao seguro e o silencio: inventar sinal para ela"
                        + " contaria ao mundo algo que ninguem desenhou.");
    }
}
