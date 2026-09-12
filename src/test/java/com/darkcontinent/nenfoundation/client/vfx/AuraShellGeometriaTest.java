package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.client.vfx.model.AuraGeometryProfile;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellOpacity;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellPass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O que da para provar da shell SEM subir o jogo.
 *
 * <p>E POUCO, e isso esta declarado. Render nao e testavel em unidade: que a
 * shell ACOMPANHE a animacao so a captura em movimento responde, e isso e o
 * gate do AV0 (#169). O que estes testes cobrem e a aritmetica que, errada,
 * produz uma aura invisivel ou de cor trocada sem lancar nada.
 */
class AuraShellGeometriaTest {

    @Test
    @DisplayName("a espessura em blocos vira unidade de modelo, e nao passa crua")
    void conversaoDeBlocoParaUnidade() {
        AuraGeometryProfile ten = AuraGeometryProfile.ten();

        // 0,032 bloco = 0,512 unidade. Passar 0,032F cru para CubeDeformation
        // infla DOIS MILESIMOS de bloco: a shell some, e nada acusa.
        assertEquals(0.512F, ten.unidadesDe(AuraShellPass.INTERNA), 1.0e-5F,
                "sem o x16 a shell fica invisivel, sem erro nenhum");

        // E precisa passar por FORA do overlay da skin, que usa 0,25 unidade
        // (jaqueta e mangas) e 0,5 (chapeu).
        assertTrue(ten.unidadesDe(AuraShellPass.INTERNA) > 0.5F,
                "a camada interna tem de passar por fora do overlay da skin");
    }

    @Test
    @DisplayName("espessuras coincidentes sao recusadas: e z-fighting garantido")
    void espessurasPrecisamSerCrescentes() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuraGeometryProfile(0.05F, 0.05F, 0.08F),
                "duas superficies na mesma posicao cintilam, e ninguem liga a"
                        + " cintilacao a este numero meses depois");
        assertThrows(IllegalArgumentException.class,
                () -> new AuraGeometryProfile(0.08F, 0.05F, 0.03F));
    }

    @Test
    @DisplayName("o teto de design recusa a aura-esfera")
    void espessuraTemTeto() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuraGeometryProfile(0.01F, 0.02F, 0.30F),
                "poder extremo aumenta densidade e brilho, nao TAMANHO");
    }

    @Test
    @DisplayName("nao-finito e nao-positivo sao recusados nos dois perfis")
    void valoresInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuraGeometryProfile(Float.NaN, 0.05F, 0.08F));
        assertThrows(IllegalArgumentException.class,
                () -> new AuraGeometryProfile(0.0F, 0.05F, 0.08F));
        assertThrows(IllegalArgumentException.class,
                () -> new AuraShellOpacity(Float.NaN, 0.2F, 0.03F));
        assertThrows(IllegalArgumentException.class,
                () -> new AuraShellOpacity(0.05F, 1.5F, 0.03F));
    }

    @Test
    @DisplayName("Ren e a MESMA shell mais densa, e nao outro efeito")
    void renEMaisDensoQueTen() {
        AuraGeometryProfile ten = AuraGeometryProfile.ten();
        AuraGeometryProfile ren = AuraGeometryProfile.ren();
        for (AuraShellPass passe : AuraShellPass.values()) {
            assertTrue(ren.espessuraDe(passe) > ten.espessuraDe(passe),
                    "Ren tem de ser mais espesso que Ten em " + passe);
            assertTrue(AuraShellOpacity.ren().alphaDe(passe)
                            > AuraShellOpacity.ten().alphaDe(passe),
                    "Ren tem de ser mais forte que Ten em " + passe);
        }
    }

    @Test
    @DisplayName("a borda e a camada que carrega a leitura")
    void bordaEAMaisForte() {
        AuraShellOpacity ten = AuraShellOpacity.ten();
        assertTrue(ten.alphaDe(AuraShellPass.BORDA) > ten.alphaDe(AuraShellPass.INTERNA));
        assertTrue(ten.alphaDe(AuraShellPass.BORDA) > ten.alphaDe(AuraShellPass.EXTERNA));
    }

    @Test
    @DisplayName("Zetsu e zero em todas as camadas: a ausencia e a informacao")
    void zetsuEZero() {
        for (AuraShellPass passe : AuraShellPass.values()) {
            assertEquals(0.0F, AuraShellOpacity.zero().alphaDe(passe));
        }
    }
}
