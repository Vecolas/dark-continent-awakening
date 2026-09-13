package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraAnchor;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraCurve;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraRibbonProfile;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A curva do filamento e pura, entao da para prova-la sem subir o jogo.
 *
 * <p>O que NAO se prova aqui: que ela PARECA certa. Isso e o gate #181, e so
 * captura em movimento responde.
 */
class AuraRibbonTest {

    private static final float FOLGA = AuraCurve.folgaBase(0.052F);

    private static float[] curvaDe(AuraAnchor ancora, boolean slim, long semente, float comp) {
        float[] destino = new float[9 * 3];
        int n = AuraCurve.pontos(destino, ancora, slim, semente, FOLGA, comp);
        float[] usado = new float[n * 3];
        System.arraycopy(destino, 0, usado, 0, n * 3);
        return usado;
    }

    @Test
    @DisplayName("a mesma semente da a mesma curva -- nada e sorteado por quadro")
    void deterministica() {
        long s = AuraCurve.semente(0x1234_5678_9ABC_DEF0L, AuraAnchor.FOREARM_RIGHT, 3, 7);
        assertArrayEquals(curvaDe(AuraAnchor.FOREARM_RIGHT, false, s, 0.35F),
                curvaDe(AuraAnchor.FOREARM_RIGHT, false, s, 0.35F),
                "sorteio a 60 Hz e ruido branco, e ruido branco e a assinatura do"
                        + " raio eletrico que a direcao de arte reprova");
    }

    @Test
    @DisplayName("trocar de ciclo troca a curva: o filamento renasce sem alocar")
    void cicloTrocaACurva() {
        long a = AuraCurve.semente(42L, AuraAnchor.CHEST_LEFT, 0, 1);
        long b = AuraCurve.semente(42L, AuraAnchor.CHEST_LEFT, 0, 2);
        assertFalse(java.util.Arrays.equals(
                curvaDe(AuraAnchor.CHEST_LEFT, false, a, 0.35F),
                curvaDe(AuraAnchor.CHEST_LEFT, false, b, 0.35F)));
    }

    @Test
    @DisplayName("jogadores diferentes nao desenham o mesmo filamento")
    void sementePorJogador() {
        Set<Float> primeiros = new HashSet<>();
        for (long uuid : new long[] {1L, 2L, 3L, 99999L, -7L}) {
            long s = AuraCurve.semente(uuid, AuraAnchor.HIP_LEFT, 0, 0);
            primeiros.add(curvaDe(AuraAnchor.HIP_LEFT, false, s, 0.35F)[0]);
        }
        assertTrue(primeiros.size() >= 4, "sincronia acidental e o que mais denuncia efeito falso");
    }

    @Test
    @DisplayName("o no zero nasce NA superficie, e nao no ar")
    void noZeroPousaNaSuperficie() {
        // A ondulacao e a expansao sao multiplicadas por `s`, entao em s=0 o
        // ponto fica exatamente a `folgaBase` do eixo. E o criterio de
        // aprovacao "os filamentos nascem na superficie" virando aritmetica.
        for (AuraAnchor ancora : AuraAnchor.values()) {
            if (ancora.familia() == AuraAnchor.Familia.AXIAL) {
                continue;
            }
            long s = AuraCurve.semente(7L, ancora, 0, 0);
            float[] c = curvaDe(ancora, false, s, 0.35F);
            float dx = c[0] - ancora.centroX(false);
            float dz = c[2];
            float rx = ancora.raioX(false) + FOLGA;
            float rz = ancora.raioZ() + FOLGA;
            // O ponto tem de estar SOBRE a elipse de raio (rx, rz).
            float naElipse = (dx * dx) / (rx * rx) + (dz * dz) / (rz * rz);
            assertEquals(1.0F, naElipse, 0.02F, "no zero fora da superficie em " + ancora);
        }
    }

    @Test
    @DisplayName("o filamento SOBE, e +Y aponta para baixo nesta pilha")
    void sobeComYNegativo() {
        for (AuraAnchor ancora : AuraAnchor.values()) {
            long s = AuraCurve.semente(11L, ancora, 0, 0);
            float[] c = curvaDe(ancora, false, s, 0.60F);
            float yInicial = c[1];
            float yFinal = c[c.length - 2];
            assertTrue(yFinal < yInicial,
                    "com o sinal trocado a ribbon entra no chao, e o chao nao reclama; "
                            + ancora);
        }
    }

    @Test
    @DisplayName("o braco slim estreita as seis ancoras de braco, e so elas")
    void slimMudaSoOsBracos() {
        for (AuraAnchor ancora : AuraAnchor.values()) {
            boolean ehBraco = ancora.regiao() == AuraBodyRegion.LEFT_ARM
                    || ancora.regiao() == AuraBodyRegion.RIGHT_ARM;
            boolean mudou = ancora.raioX(true) != ancora.raioX(false)
                    || ancora.centroX(true) != ancora.centroX(false);
            assertEquals(ehBraco, mudou,
                    "usar o modelo errado so aparece quando alguem com skin Alex entra; " + ancora);
        }
    }

    @Test
    @DisplayName("sao vinte ancoras, e cada regiao tem pelo menos uma")
    void cobreOCorpoInteiro() {
        assertEquals(20, AuraAnchor.values().length);
        for (AuraBodyRegion regiao : AuraBodyRegion.values()) {
            boolean tem = false;
            for (AuraAnchor a : AuraAnchor.values()) {
                tem |= a.regiao() == regiao;
            }
            assertTrue(tem, "regiao sem nenhum filamento: " + regiao);
        }
    }

    @Test
    @DisplayName("a folga sai da espessura da borda, e nao de uma constante")
    void folgaDerivaDaShell() {
        assertTrue(AuraCurve.folgaBase(0.072F) > AuraCurve.folgaBase(0.052F),
                "engordar a shell de Ren tem de afastar o filamento, senao ele some dentro dela");
    }

    @Test
    @DisplayName("Ren e mais denso que Ten em todos os eixos")
    void renEMaisDenso() {
        AuraRibbonProfile ten = AuraRibbonProfile.ten();
        AuraRibbonProfile ren = AuraRibbonProfile.ren();
        assertTrue(ren.quantidade() > ten.quantidade());
        assertTrue(ren.comprimentoMax() > ten.comprimentoMax());
        assertTrue(ren.largura() > ten.largura());
        assertTrue(ren.cicloSegundos() < ten.cicloSegundos(), "Ren troca mais rapido");
    }

    @Test
    @DisplayName("largura de tubo neon e recusada, e o teto de ribbons tambem")
    void perfisInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuraRibbonProfile(8, 0.15F, 0.6F, 0.2F, 1.0F),
                "brilhante nao e grosso: acima de 0,05 bloco vira tubo neon");
        assertThrows(IllegalArgumentException.class,
                () -> new AuraRibbonProfile(200, 0.15F, 0.6F, 0.009F, 1.0F));
        assertThrows(IllegalArgumentException.class,
                () -> new AuraRibbonProfile(8, 0.8F, 0.2F, 0.009F, 1.0F));
    }

    @Test
    @DisplayName("a contagem de nos fica entre cinco e nove")
    void numeroDeNos() {
        assertEquals(5, AuraCurve.nos(0.15F));
        assertEquals(9, AuraCurve.nos(9.0F), "filamento longo nao explode em vertices");
        assertTrue(AuraCurve.nos(1.60F) <= 9);
    }
}
