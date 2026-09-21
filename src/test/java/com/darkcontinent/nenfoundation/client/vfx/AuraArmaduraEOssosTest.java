package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraGeometryLadder;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraGeometryProfile;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraOssosDoInimigo;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraAnchor;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Armadura e ossos: os dois eixos do AV7 que dao para provar sem tela.
 *
 * <p>A metade visual -- "com armadura de ferro completa ainda se le que essa
 * pessoa esta em Ten, a 5 e a 10 blocos" -- e captura com gente olhando, e e o
 * gate #205. O que cabe aqui sao os numeros e a resolucao de nome, e os dois tem
 * modos de falha silenciosos:
 *
 * <ul>
 *   <li>a borda com armadura fora da faixa -- ou ela nao aparece por fora da
 *       armadura, ou a aura cresce e vira uma casca opaca por cima dela;</li>
 *   <li>um osso declarado que o modelo nao tem -- a aura nasce em lugar
 *       nenhum, sem erro.</li>
 * </ul>
 */
class AuraArmaduraEOssosTest {

    private static final String DIRETORIO = "src/main/resources/assets/nenfoundation/nen_vfx";

    private static AuraPerfilVisual ler(String nome) {
        JsonElement json = JsonParser.parseString(Repo.texto(DIRETORIO + "/" + nome));
        return AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(erro -> new AssertionError(nome + " nao le: " + erro));
    }

    // ------------------------------------------------- armadura

    @Test
    @DisplayName("a borda com armadura sobe para a faixa de 0,06 a 0,09")
    void bordaComArmaduraNaFaixa() {
        float ten = ler("ten.json").bordaComArmadura();
        float ren = ler("ren.json").bordaComArmadura();
        assertTrue(ten >= 0.06F && ten <= 0.09F,
                "a borda de Ten com armadura precisa caber em 0,06 a 0,09: " + ten);
        assertTrue(ren >= 0.06F && ren <= 0.09F + 1.0e-5F,
                "a borda de Ren com armadura precisa caber em 0,06 a 0,09: " + ren);
    }

    @Test
    @DisplayName("com armadura a borda ENGROSSA, e nunca afina")
    void armaduraEngrossa() {
        for (String nome : new String[] {"ten.json", "ren.json"}) {
            AuraPerfilVisual p = ler(nome);
            assertTrue(p.bordaComArmadura() > AuraGeometryProfile.ten().espessuraBorda()
                            - 1.0e-6F,
                    nome + ": a borda com armadura precisa ser pelo menos a de Ten -- senao"
                            + " ela some POR DENTRO da peca vestida");
        }
        assertTrue(ler("ren.json").bordaComArmadura() > ler("ten.json").bordaComArmadura(),
                "Ren com armadura continua mais espesso que Ten com armadura");
    }

    @Test
    @DisplayName("Zetsu nao tem borda para engrossar")
    void zetsuNaoEngrossa() {
        assertEquals(0.0F, ler("zetsu.json").bordaComArmadura(),
                "Zetsu e ausencia total, inclusive de espessura");
    }

    @Test
    @DisplayName("a escada ALCANCA a espessura com armadura, nos dois modos")
    void aEscadaAlcancaAArmadura() {
        // SE A ESCADA NAO ALCANCAR, o degrau escolhido satura no topo e a
        // espessura com armadura vira a mesma para Ten e para Ren -- sem erro
        // nenhum, e com a diferenca entre os dois modos desaparecendo justamente
        // quando alguem veste armadura.
        for (String nome : new String[] {"ten.json", "ren.json"}) {
            float pedida = ler(nome).bordaComArmadura();
            int degrau = AuraGeometryLadder.degrauPara(pedida);
            float obtida = AuraGeometryLadder.espessuraDaBordaDe(degrau);
            assertTrue(Math.abs(obtida - pedida) < 0.006F,
                    nome + ": a escada so chega a " + obtida + " para uma borda pedida de "
                            + pedida);
        }
    }

    @Test
    @DisplayName("a borda com armadura respeita o teto de design")
    void tetoDeDesignVale() {
        // POR EXTREMO AUMENTA DENSIDADE, BRILHO, VELOCIDADE E PRESSAO -- NAO
        // TAMANHO. Um personagem dez vezes mais forte nao vira uma esfera, e
        // vestir armadura nao e ficar mais forte.
        assertTrue(AuraPerfilDeBrilhoNaoInterfere());
        assertThrows(IllegalArgumentException.class, () -> new AuraPerfilVisual(
                0.05F, 0.2F, 0.03F, 3.4F, 2.7F, 2.0F, 0.12F, 4.0F, 0.9F, 0.4F, 0.18F,
                new com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraRibbonProfile(
                        8, 0.15F, 0.6F, 0.009F, 1.1F),
                com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilDePressao.NENHUMA,
                com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilDeBrilho.NENHUM,
                0.025F,
                // Meio bloco de borda: acima do teto de 0,25.
                0.5F));
    }

    private static boolean AuraPerfilDeBrilhoNaoInterfere() {
        return AuraGeometryProfile.ESPESSURA_MAXIMA > 0.0F;
    }

    // ------------------------------------------------- ossos

    @Test
    @DisplayName("o humanoide padrao declara as seis regioes e as vinte ancoras")
    void humanoidePadraoEstaCompleto() {
        AuraOssosDoInimigo ossos = AuraOssosDoInimigo.humanoidePadrao();
        assertEquals(AuraBodyRegion.values().length, ossos.regioesDeclaradas());
        for (AuraAnchor ancora : AuraAnchor.values()) {
            assertTrue(ossos.ossoDe(ancora) != null,
                    "a ancora " + ancora + " ficou sem osso no esqueleto padrao");
        }
        assertTrue(ossos.temAncoras());
    }

    @Test
    @DisplayName("a ancora herda o osso da propria regiao, no corpo humanoide")
    void ancoraSegueARegiao() {
        // NUM HUMANOIDE AS DUAS COINCIDEM -- e e por isso que `AuraAnchor` tem
        // um campo so para regiao e parte. Num corpo que nao coincida, e o
        // adaptador daquele corpo que resolve.
        AuraOssosDoInimigo ossos = AuraOssosDoInimigo.humanoidePadrao();
        for (AuraAnchor ancora : AuraAnchor.values()) {
            assertEquals(ossos.ossoDe(ancora.regiao()), ossos.ossoDe(ancora),
                    "a ancora " + ancora + " aponta para um osso diferente do da regiao dela");
        }
    }

    @Test
    @DisplayName("um corpo sem bracos simplesmente nao os declara -- e isso NAO e erro")
    void corpoSemBracoEValido() {
        // UM QUADRUPEDE NAO TEM BRACO. Exigir que ele declare um seria obrigar
        // todo corpo a fingir ser humanoide, e o resultado seria uma aura
        // nascendo num osso inventado.
        AuraOssosDoInimigo quadrupede = AuraOssosDoInimigo.declarar()
                .regiao(AuraBodyRegion.HEAD, "head")
                .regiao(AuraBodyRegion.TORSO, "body")
                .ancora(AuraAnchor.HEAD_TOP, "head")
                .ancora(AuraAnchor.BACK_CENTER, "body")
                .montar();
        assertNull(quadrupede.ossoDe(AuraBodyRegion.LEFT_ARM));
        assertNull(quadrupede.ossoDe(AuraAnchor.SHOULDER_LEFT));
        assertEquals("head", quadrupede.ossoDe(AuraAnchor.HEAD_TOP));
        assertEquals(2, quadrupede.regioesDeclaradas());
        assertTrue(quadrupede.temAncoras());
    }

    @Test
    @DisplayName("sem ancora declarada, os filamentos nao nascem -- e a shell sobra")
    void semAncoraNaoHaFilamento() {
        assertTrue(!AuraOssosDoInimigo.NENHUM.temAncoras());
        assertNull(AuraOssosDoInimigo.NENHUM.ossoDe(AuraAnchor.HEAD_TOP));
        assertEquals(0, AuraOssosDoInimigo.NENHUM.regioesDeclaradas());
    }

    @Test
    @DisplayName("declarar um osso vazio e RECUSADO na declaracao, e nao em runtime")
    void ossoVazioReprovaCedo() {
        // O CASO QUE DEVE REPROVAR. Um nome vazio passaria pela declaracao,
        // falharia na busca do bone em pleno desenho, e o sintoma seria uma
        // linha de log por quadro -- ou, pior, um silencio.
        assertThrows(IllegalArgumentException.class, () -> AuraOssosDoInimigo.declarar()
                .ancora(AuraAnchor.HEAD_TOP, "  "));
        assertThrows(IllegalArgumentException.class, () -> AuraOssosDoInimigo.declarar()
                .regiao(AuraBodyRegion.HEAD, ""));
        assertThrows(NullPointerException.class, () -> AuraOssosDoInimigo.declarar()
                .ancora(null, "head"));
    }

    @Test
    @DisplayName("a declaracao e uma COPIA: mexer no construtor depois nao muda o resultado")
    void declaracaoEImutavel() {
        AuraOssosDoInimigo.Construtor construtor = AuraOssosDoInimigo.declarar()
                .regiao(AuraBodyRegion.HEAD, "head");
        AuraOssosDoInimigo fechado = construtor.montar();
        construtor.regiao(AuraBodyRegion.TORSO, "body");
        assertEquals(1, fechado.regioesDeclaradas(),
                "a declaracao fechada mudou depois de montada; um adaptador guardado por"
                        + " um renderer passaria a ver ossos que ninguem declarou para ele");
    }
}
