package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do agarrao COMPARTILHADO (issue #140).
 *
 * <p>A falha central que ele cobre nao da erro: um ponto de saida esquecido
 * deixa um jogador preso dentro de um bicho ate o servidor reiniciar. Por isso
 * cada saida tem caso proprio aqui -- tempo, dano, morte dos dois lados, vitima
 * sumida por fora do ciclo, unload, dimensao, teleporte e interrupcao.</p>
 */
class GrabControllerTest {

    private static final UUID ALVO = UUID.nameUUIDFromBytes("alvo".getBytes());
    private static final UUID OUTRO = UUID.nameUUIDFromBytes("outro".getBytes());

    /** 100 ticks presos, pulso a cada 20, e 12 de dano no predador compram a soltura. */
    private static GrabController controlador() {
        return new GrabController(new GrabRules(100, 20, 3.0F, 12.0F), 2.0D, 1.0D);
    }

    private static GrabController agarrando() {
        GrabController c = controlador();
        c.agarrar(ALVO);
        return c;
    }

    // ------------------------------------------------------------- recusa

    @Test
    @DisplayName("a recusa diz o MOTIVO, e nao apenas 'nao'")
    void recusaTemMotivo() {
        GrabController c = controlador();
        assertEquals(GrabRefusal.NENHUMA, c.podeAgarrar(1.8D, 0.6D, false, true));
        assertEquals(GrabRefusal.ALVO_GRANDE_DEMAIS, c.podeAgarrar(2.9D, 0.6D, false, true),
                "Sem motivo, o relato de bug vira 'as vezes o bicho me morde e nao me engole'.");
        assertEquals(GrabRefusal.ALVO_GRANDE_DEMAIS, c.podeAgarrar(1.8D, 1.4D, false, true));
        assertEquals(GrabRefusal.ALVO_OCUPADO, c.podeAgarrar(1.8D, 0.6D, true, true));
        assertEquals(GrabRefusal.ALVO_INVALIDO, c.podeAgarrar(1.8D, 0.6D, false, false));
    }

    @Test
    @DisplayName("uma boca, uma vitima: o segundo agarrao e recusado")
    void segundaVitimaERecusada() {
        GrabController c = agarrando();
        assertEquals(GrabRefusal.JA_AGARRANDO, c.podeAgarrar(1.8D, 0.6D, false, true));
        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> c.agarrar(OUTRO));
        assertTrue(erro.getMessage().contains("ja segurando"),
                "Sobrescrever deixaria a primeira vitima presa para sempre, com o relogio"
                        + " dela perdido e nenhum erro no log.");
        assertEquals(ALVO, c.vitima().orElseThrow());
    }

    // -------------------------------------------------------------- saidas

    @Test
    @DisplayName("saida por TEMPO: a vitima sai viva, cuspida e nao digerida")
    void soltaPorTempo() {
        GrabController c = agarrando();
        GrabController.GrabTick ultimo = GrabController.GrabTick.PARADO;
        for (int tick = 0; tick < 100; tick++) ultimo = c.tick(true, true, true);
        assertEquals(GrabRelease.TEMPO, ultimo.soltura());
        assertFalse(c.agarrando());
    }

    @Test
    @DisplayName("saida por DANO: bater no predador tem de ser MELHOR do que esperar")
    void soltaPorDano() {
        GrabController c = agarrando();
        c.tick(true, true, true);
        c.registrarDanoNoPredador(12.0F);
        assertEquals(GrabRelease.DANO, c.tick(true, true, true).soltura(),
                "Se aguentar calado custasse menos que reagir, a janela de escape seria"
                        + " decorativa e o mob voltaria a ser morte sem resposta.");
        assertFalse(c.agarrando());
    }

    @Test
    @DisplayName("dano fora do agarrao NAO conta para o proximo")
    void danoForaDoAgarraoNaoAcumula() {
        GrabController c = controlador();
        c.registrarDanoNoPredador(50.0F);
        c.agarrar(ALVO);
        assertEquals(0.0F, c.danoNoPredador(),
                "Somar sempre faria o predador soltar a proxima vitima na hora por causa de"
                        + " uma briga anterior, e o sintoma seria um agarrao que 'as vezes"
                        + " nao pega'.");
        assertFalse(c.tick(true, true, true).soltou());
    }

    @Test
    @DisplayName("morte dos dois lados solta, e cada uma com o proprio motivo")
    void morteDeQualquerUmSolta() {
        GrabController vitimaMorre = agarrando();
        assertEquals(GrabRelease.VITIMA_MORTA, vitimaMorre.tick(false, true, true).soltura());

        GrabController predadorMorre = agarrando();
        assertEquals(GrabRelease.PREDADOR_MORTO, predadorMorre.tick(true, true, false).soltura(),
                "Morrer com alguem na boca nao pode deixar a vitima presa a um cadaver.");
    }

    @Test
    @DisplayName("vitima que some por fora do ciclo (logout, /kill) solta o agarrao")
    void vitimaSumidaSolta() {
        GrabController c = agarrando();
        assertEquals(GrabRelease.VITIMA_SUMIU, c.tick(true, false, true).soltura(),
                "Sem esta checagem o predador ficaria agarrando um fantasma: o relogio correria"
                        + " sozinho ate o fim e o proximo agarrao nunca aconteceria.");
    }

    @Test
    @DisplayName("unload, dimensao, teleporte e interrupcao passam pela MESMA saida")
    void saidasExternasUsamOMesmoCaminho() {
        for (GrabRelease motivo : new GrabRelease[] { GrabRelease.UNLOAD, GrabRelease.DIMENSAO,
                GrabRelease.TELEPORTE, GrabRelease.INTERROMPIDO }) {
            GrabController c = agarrando();
            assertEquals(motivo, c.soltar(motivo));
            assertFalse(c.agarrando(), "Saida " + motivo + " deixou o agarrao ligado.");
            assertEquals(0, c.ticksAgarrado());
            assertEquals(0.0F, c.danoNoPredador());
        }
    }

    @Test
    @DisplayName("soltar sem motivo e impossivel -- o motivo decide a consequencia")
    void solturaSemMotivoReprova() {
        assertThrows(NullPointerException.class, () -> agarrando().soltar(null));
    }

    // --------------------------------------------------------------- pulso

    @Test
    @DisplayName("o tick 0 nao pulsa: engolir nao machuca no mesmo tick em que agarra")
    void primeiroTickNaoPulsa() {
        GrabController c = agarrando();
        assertFalse(c.tick(true, true, true).pulsoDeDano());
        Set<Integer> pulsos = new HashSet<>();
        for (int tick = 2; tick <= 60; tick++) {
            if (c.tick(true, true, true).pulsoDeDano()) pulsos.add(tick);
        }
        assertEquals(Set.of(20, 40, 60), pulsos,
                "Sem a excecao do tick 0 a vitima levaria o primeiro pulso antes de ter um"
                        + " unico tick para reagir, e a janela comecaria com dano no caixa.");
    }

    @Test
    @DisplayName("tick sem agarrao nenhum nao faz nada e nao explode")
    void tickSemAgarraoEInerte() {
        assertEquals(GrabController.GrabTick.PARADO, controlador().tick(true, true, true));
    }

    // ------------------------------------------------------- soltura segura

    @Test
    @DisplayName("a frente livre e a primeira escolha -- e a soltura que o jogador espera")
    void frenteLivreEAPrimeiraEscolha() {
        Optional<Vec3> onde = SafeReleaseSpot.escolher(Vec3.ZERO, new Vec3(0, 0, -1),
                1.5D, 2.0D, ponto -> true);
        assertEquals(new Vec3(0.0D, 0.0D, -1.5D), onde.orElseThrow());
    }

    @Test
    @DisplayName("encurralado contra a parede, a vitima sai pelo lado -- nunca dentro da pedra")
    void paredeNaFrenteNaoSufoca() {
        // Tudo com z negativo e pedra: e o caso do predador que encurralou o alvo,
        // que e justamente quando o agarrao acontece.
        Optional<Vec3> onde = SafeReleaseSpot.escolher(Vec3.ZERO, new Vec3(0, 0, -1),
                1.5D, 2.0D, ponto -> ponto.z >= 0.0D);
        Vec3 escolhido = onde.orElseThrow();
        assertTrue(escolhido.z >= 0.0D,
                "Soltar dentro do bloco da dano de sufocamento continuo que ninguem consegue"
                        + " explicar, e so acontece encostado na parede.");
        assertFalse(escolhido.equals(Vec3.ZERO),
                "Sobrar a propria posicao do predador so quando nada mais serve; antes disso a"
                        + " vitima nunca veria a boca abrir.");
    }

    @Test
    @DisplayName("sem lugar nenhum livre, devolve VAZIO em vez de inventar um ponto")
    void semLugarLivreNaoInventa() {
        assertTrue(SafeReleaseSpot.escolher(Vec3.ZERO, new Vec3(1, 0, 0), 1.5D, 2.0D,
                ponto -> false).isEmpty(),
                "Parado dentro do predador por um tick e recuperavel; dentro da pedra, nao.");
    }

    @Test
    @DisplayName("direcao de saida nula reprova -- normalizar vetor nulo devolve NaN")
    void direcaoNulaReprova() {
        assertThrows(IllegalArgumentException.class, () -> SafeReleaseSpot.escolher(
                Vec3.ZERO, Vec3.ZERO, 1.5D, 2.0D, ponto -> true));
        assertThrows(IllegalArgumentException.class, () -> SafeReleaseSpot.escolher(
                Vec3.ZERO, new Vec3(1, 0, 0), 0.0D, 2.0D, ponto -> true));
    }
}
