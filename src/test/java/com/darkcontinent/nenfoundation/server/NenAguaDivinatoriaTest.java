package com.darkcontinent.nenfoundation.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da condicao de entrada do ritual.
 *
 * <p>POR QUE ESTE ARQUIVO EXISTE, e a resposta e desconfortavel:
 *
 * <p>O gametest da Water Divination ja afirmava que o teste "nao funciona sem
 * Nen desperto". Ao alimentar esse portao com o defeito -- apagar a condicao do
 * ritual --, ele <b>passou</b>. A razao e que o servico de categoria recusa
 * de novo, mais abaixo: sem despertar, {@code atribuirPorSorteio} devolve
 * NAO_DESPERTO e o perfil nao muda de qualquer jeito.
 *
 * <p>Isso e defesa em profundidade, e e bom -- mas significa que aquele
 * gametest mede a rede de baixo, e nao a condicao do ritual. A diferenca e
 * visivel para o jogador: com a condicao, ele le "voce ainda nao despertou o
 * Nen"; sem ela, a agua tambem nao reage, mas por um motivo que a mensagem nao
 * explica.
 *
 * <p>A condicao e uma funcao pura de perfil, entao ela pode ser medida
 * diretamente. E e aqui que <b>Ren entra no M4</b>: quando a exigencia mudar,
 * este arquivo e o primeiro a reprovar.
 */
class NenAguaDivinatoriaTest {

    private static PersistentNenData perfil(boolean desperto, NenCategory categoria) {
        return new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, desperto, categoria, false,
                0.0D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(), Set.of());
    }

    @Test
    @DisplayName("quem nao despertou nao pode fazer o teste")
    void semNenNaoPodeFazer() {
        assertFalse(NenAguaDivinatoria.podeFazerOTeste(PersistentNenData.NAO_DESPERTADO),
                "Sem esta condicao o ritual segue adiante e so e barrado la"
                        + " embaixo, pelo servico de categoria. A agua tambem nao"
                        + " reage -- mas por um motivo que a mensagem na tela nao"
                        + " explica, e o relato de bug vira 'cliquei e nao"
                        + " aconteceu nada'.");
        assertFalse(NenAguaDivinatoria.podeFazerOTeste(
                        perfil(false, NenCategory.EMISSION)),
                "Um perfil com categoria mas sem despertar tambem nao pode: ter"
                        + " categoria nao e ter Nen.");
    }

    @Test
    @DisplayName("quem despertou pode fazer o teste, com ou sem categoria")
    void despertoPodeFazer() {
        assertTrue(NenAguaDivinatoria.podeFazerOTeste(
                        perfil(true, NenCategory.UNDETERMINED)),
                "quem despertou e ainda nao tem categoria e exatamente quem o"
                        + " ritual existe para atender.");
        assertTrue(NenAguaDivinatoria.podeFazerOTeste(
                        perfil(true, NenCategory.CONJURATION)),
                "quem ja tem categoria pode refazer o teste; refazer e no-op,"
                        + " e nao recusa.");
    }

    @Test
    @DisplayName("PONTO CEGO: a condicao ainda NAO exige Ren")
    void aCondicaoAindaNaoExigeRen() {
        // Este teste nao mede uma virtude: ele FIXA uma divida, para que ela
        // nao passe despercebida.
        //
        // O cânone e a issue #61 pedem que a Water Divination seja disparada por
        // REN sobre o copo. Ren e do M4 e nao existe. O portao de hoje e so
        // "despertou", o que torna o teste mais facil do que deveria ser.
        //
        // Quando Ren chegar, este teste REPROVA -- e quem estiver implementando
        // Ren e obrigado a passar por aqui e decidir conscientemente. E esse o
        // ponto: uma divida silenciosa nao cobra ninguem.
        assertTrue(NenAguaDivinatoria.podeFazerOTeste(
                        perfil(true, NenCategory.UNDETERMINED)),
                "Se este teste reprovou, a condicao de entrada mudou -- provavelmente"
                        + " porque Ren chegou. Atualize a condicao, atualize este"
                        + " teste, e apague o ponto cego declarado no javadoc de"
                        + " NenAguaDivinatoria e no PR da issue #61.");
    }
}
