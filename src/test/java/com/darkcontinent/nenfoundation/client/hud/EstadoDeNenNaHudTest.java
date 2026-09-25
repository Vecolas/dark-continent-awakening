package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.technique.Gyo;
import com.darkcontinent.nenfoundation.nen.technique.Ken;
import com.darkcontinent.nenfoundation.nen.technique.Ko;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Shu;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao do chip de estado.
 *
 * <p>O TESTE QUE IMPORTA E O DA COMPLETUDE, e ele existe por causa de um
 * defeito real e vivo no repositorio: {@code ModoVisualDeTecnica} tem uma lista
 * de tres tecnicas com a regra "desconhecida nao acende nada", e essa regra
 * engoliu o Ken em silencio -- ligar Ken apaga a aura do proprio jogador
 * enquanto os outros continuam vendo. Nenhum portao ligava as duas tabelas.
 *
 * <p>Este liga. Se alguem registrar uma tecnica nova e der aparencia a ela sem
 * dar nome de chip, o build reprova nomeando a tecnica que ficou de fora.
 */
class EstadoDeNenNaHudTest {

    @Test
    @DisplayName("o chip sabe nomear EXATAMENTE as tecnicas que tem aparencia")
    void asDuasTabelasDaHudCobremAsMesmasTecnicas() {
        Set<ResourceLocation> comAparencia = AparenciaDeTecnica.conhecidas();
        Set<ResourceLocation> nomeaveis = EstadoDeNenNaHud.nomeaveis();

        Set<ResourceLocation> semNome = new HashSet<>(comAparencia);
        semNome.removeAll(nomeaveis);
        Set<ResourceLocation> semAparencia = new HashSet<>(nomeaveis);
        semAparencia.removeAll(comAparencia);

        assertTrue(semNome.isEmpty(),
                "tecnica desenhada na fila e sem nome no chip -- o jogador ve a forma "
                        + "e o chip fica vazio, sem erro nenhum: " + semNome);
        assertTrue(semAparencia.isEmpty(),
                "tecnica nomeada no chip e sem forma na fila: " + semAparencia);
    }

    @Test
    @DisplayName("a precedencia nao tem repetida -- uma duplicata esconde a seguinte")
    void aPrecedenciaNaoRepete() {
        assertEquals(EstadoDeNenNaHud.precedencia().size(),
                EstadoDeNenNaHud.nomeaveis().size(),
                "id repetido na precedencia");
    }

    @Test
    @DisplayName("Zetsu ganha o chip de qualquer combinacao")
    void zetsuVenceTudo() {
        assertEquals(Optional.of(Zetsu.ID),
                EstadoDeNenNaHud.dominante(Set.of(Zetsu.ID, Ten.ID, Ren.ID, Ken.ID)));
    }

    @Test
    @DisplayName("Ko vence Ken, que vence Ren, que vence Ten")
    void aEscadaDeCompromisso() {
        assertEquals(Optional.of(Ko.ID),
                EstadoDeNenNaHud.dominante(Set.of(Ko.ID, Ken.ID, Ren.ID, Ten.ID)));
        assertEquals(Optional.of(Ken.ID),
                EstadoDeNenNaHud.dominante(Set.of(Ken.ID, Ren.ID, Ten.ID)));
        assertEquals(Optional.of(Ren.ID),
                EstadoDeNenNaHud.dominante(Set.of(Ren.ID, Ten.ID)));
        assertEquals(Optional.of(Ten.ID), EstadoDeNenNaHud.dominante(Set.of(Ten.ID)));
    }

    @Test
    @DisplayName("o Ken tem chip -- e este e o caso que a outra tabela perde")
    void oKenNaoEEngolido() {
        assertEquals(Optional.of(Ken.ID), EstadoDeNenNaHud.dominante(Set.of(Ken.ID)),
                "Ken ligado sozinho precisa nomear o chip. `ModoVisualDeTecnica` "
                        + "devolve OFF nesse mesmo caso, e e o defeito conhecido.");
        assertEquals(Optional.of(Shu.ID), EstadoDeNenNaHud.dominante(Set.of(Shu.ID)));
        assertEquals(Optional.of(Gyo.ID), EstadoDeNenNaHud.dominante(Set.of(Gyo.ID)));
    }

    @Test
    @DisplayName("ausencia e tecnica desconhecida nao ganham chip, e nao lancam")
    void silencioERespostaValida() {
        assertEquals(Optional.empty(), EstadoDeNenNaHud.dominante(Set.of()));
        assertEquals(Optional.empty(), EstadoDeNenNaHud.dominante(null));
        assertEquals(Optional.empty(), EstadoDeNenNaHud.dominante(
                Set.of(ResourceLocation.fromNamespaceAndPath("outromod", "tecnica_estranha"))));
    }

    @Test
    void aChaveDeNomeSegueAConvencaoDaRoda() {
        assertEquals("nenfoundation.tecnica.ken", EstadoDeNenNaHud.chaveDeNome(Ken.ID));
        assertEquals("nenfoundation.tecnica.zetsu", EstadoDeNenNaHud.chaveDeNome(Zetsu.ID));
    }

    @Test
    @DisplayName("Zetsu dessatura a Aura; escurecer seria a informacao errada")
    void zetsuDessaturaEmVezDeEscurecer() {
        int tratada = EstadoDeNenNaHud.tratamentoDe(Optional.of(Zetsu.ID))
                .aplicar(PaletaDaHud.AURA, 1.0F);

        assertNotEquals(PaletaDaHud.AURA, tratada, "Zetsu precisa mudar a cor");
        assertTrue(luminancia(tratada) > luminancia(PaletaDaHud.escurecer(PaletaDaHud.AURA, 0.8F)),
                "dessaturar tem de preservar o brilho; escurecer leria como reserva baixa, "
                        + "que e justamente o que Zetsu NAO faz com a reserva");
    }

    @Test
    @DisplayName("Ren e Ken pulsam; Ten, Gyo, Shu e Ko nao")
    void soAAuraLiberadaESustentadaPulsa() {
        assertEquals(EstadoDeNenNaHud.Tratamento.PRESSIONADO,
                EstadoDeNenNaHud.tratamentoDe(Optional.of(Ren.ID)));
        assertEquals(EstadoDeNenNaHud.Tratamento.PRESSIONADO,
                EstadoDeNenNaHud.tratamentoDe(Optional.of(Ken.ID)));
        for (ResourceLocation id : new ResourceLocation[] {Ten.ID, Gyo.ID, Shu.ID, Ko.ID}) {
            assertEquals(EstadoDeNenNaHud.Tratamento.NEUTRO,
                    EstadoDeNenNaHud.tratamentoDe(Optional.of(id)), id.toString());
        }
        assertEquals(EstadoDeNenNaHud.Tratamento.NEUTRO,
                EstadoDeNenNaHud.tratamentoDe(Optional.empty()));
    }

    @Test
    @DisplayName("o pulso no fundo do ciclo devolve a cor intacta")
    void oPulsoVaiEVolta() {
        var pressionado = EstadoDeNenNaHud.Tratamento.PRESSIONADO;
        assertEquals(PaletaDaHud.AURA, pressionado.aplicar(PaletaDaHud.AURA, 0.0F),
                "com pulso zero a barra tem de voltar exatamente a cor de repouso");
        assertNotEquals(PaletaDaHud.AURA, pressionado.aplicar(PaletaDaHud.AURA, 1.0F));
    }

    private static int luminancia(int argb) {
        return (int) Math.round(0.2126 * ((argb >> 16) & 0xFF)
                + 0.7152 * ((argb >> 8) & 0xFF) + 0.0722 * (argb & 0xFF));
    }
}
