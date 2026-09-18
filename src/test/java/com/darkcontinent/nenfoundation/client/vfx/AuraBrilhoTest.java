package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilDeBrilho;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.shader.KernelDeBorrao;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O passe de brilho: niveis, raio e os pesos do desfoque.
 *
 * <p><b>O QUE DA PARA PROVAR SEM TELA, E O QUE NAO DA.</b> Que o halo respeita
 * parede e captura com o jogador atras de um bloco -- olho humano, gate do AV5.
 * Que o alvo de brilho nao recebe a skin e por CONSTRUCAO: so os tipos de render
 * de brilho conhecem o {@code OutputStateShard}. O que sobra para JUnit e a
 * aritmetica, e ela tem tres modos de falha silenciosos:
 *
 * <ul>
 *   <li>pesos que nao somam 1 -- mudar o RAIO passaria a mudar tambem o BRILHO,
 *       e "o halo fica mais forte quando eu aumento o raio" e verdade e nao
 *       deveria ser: raio e tamanho, forca e forca;</li>
 *   <li>raio sem teto -- trinta pixels de halo deixa de ser brilho e vira
 *       nevoa, e nevoa apaga a silhueta do personagem;</li>
 *   <li>fallback caindo para {@code OFF} -- rebaixar ate o minimo esconde que
 *       alguma coisa falhou, e ninguem procura um log que nao sabe existir.</li>
 * </ul>
 */
class AuraBrilhoTest {

    private static final String DIRETORIO = "src/main/resources/assets/nenfoundation/nen_vfx";

    private static AuraPerfilVisual ler(String nome) {
        JsonElement json = JsonParser.parseString(Repo.texto(DIRETORIO + "/" + nome));
        return AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(erro -> new AssertionError(nome + " nao le: " + erro));
    }

    // ------------------------------------------------- o perfil

    @Test
    @DisplayName("Ten pede 2 a 3 px de halo; Ren, 4 a 7")
    void raiosDaDirecaoDeArte() {
        float ten = ler("ten.json").brilho().raio();
        float ren = ler("ren.json").brilho().raio();
        assertTrue(ten >= 2.0F && ten <= 3.0F, "raio de Ten fora da faixa: " + ten);
        assertTrue(ren >= 4.0F && ren <= 7.0F, "raio de Ren fora da faixa: " + ren);
    }

    @Test
    @DisplayName("um raio de nevoa e RECUSADO pelo codec, e nao aceito como plausivel")
    void raioDeNevoaReprova() {
        // O CASO QUE DEVE REPROVAR. Trinta pixels nao e "bloom forte": e outro
        // efeito, e um que apaga a silhueta -- que e o item 1 da hierarquia de
        // leitura. O teto e limite de DESENHO, e nenhum ajuste de arte deveria
        // poder atravessa-lo.
        assertTrue(AuraPerfilDeBrilho.CODEC.parse(JsonOps.INSTANCE,
                        JsonParser.parseString("{\"forca\": 0.5, \"raio\": 30.0}"))
                .error().isPresent(), "trinta pixels de halo passaram");
        assertThrows(IllegalArgumentException.class,
                () -> new AuraPerfilDeBrilho(0.5F, 30.0F));
    }

    @Test
    @DisplayName("forca ou raio em zero ja significa NAO HA brilho")
    void zeroEmQualquerUmDesliga() {
        assertTrue(!new AuraPerfilDeBrilho(0.0F, 5.0F).existe(),
                "forca zero nao contribui, por maior que seja o raio");
        assertTrue(!new AuraPerfilDeBrilho(0.5F, 0.0F).existe(),
                "raio zero nao espalha nada, por maior que seja a forca");
        assertTrue(ler("ren.json").brilho().existe());
    }

    @Test
    @DisplayName("interpolar o brilho fecha nas duas pontas")
    void interpolacaoDoBrilho() {
        AuraPerfilDeBrilho ten = ler("ten.json").brilho();
        AuraPerfilDeBrilho ren = ler("ren.json").brilho();
        assertEquals(ten, AuraPerfilDeBrilho.interpolar(ten, ren, 0.0F));
        assertEquals(ren, AuraPerfilDeBrilho.interpolar(ten, ren, 1.0F));
        assertEquals(ren, AuraPerfilDeBrilho.interpolar(ten, ren, 9.0F), "satura");
    }

    // ------------------------------------------------- os niveis

    @Test
    @DisplayName("exatamente UM nivel usa framebuffer")
    void soHighUsaAlvo() {
        int comAlvo = 0;
        for (AuraBloomLevel nivel : AuraBloomLevel.values()) {
            if (nivel.usaFramebuffer()) {
                comAlvo++;
            }
        }
        assertEquals(1, comAlvo, "so HIGH cria alvo de render");
        assertTrue(AuraBloomLevel.HIGH.usaFramebuffer());
        assertTrue(!AuraBloomLevel.FAST.usaFramebuffer(),
                "EM FAST NENHUM RenderTarget E CRIADO, e isso e criterio de aceite --"
                        + " verificado pelo contador de dev, e nao por leitura de codigo");
        assertTrue(!AuraBloomLevel.OFF.usaFramebuffer());
    }

    @Test
    @DisplayName("o fallback e FAST, e NUNCA OFF")
    void fallbackNuncaVaiParaOff() {
        assertSame(AuraBloomLevel.FAST, AuraBloomLevel.HIGH.rebaixado(),
                "REBAIXAR DIRETO AO MINIMO ESCONDE QUE ALGO FALHOU. OFF e uma escolha"
                        + " legitima de quem quer o jogo mais leve; cair nele apagaria a"
                        + " diferenca entre 'eu escolhi' e 'o shader nao compilou'");
        // Quem ja esta no chao nao cai mais: nem FAST nem OFF criam alvo, entao
        // nao ha de onde rebaixar.
        assertSame(AuraBloomLevel.FAST, AuraBloomLevel.FAST.rebaixado());
        assertSame(AuraBloomLevel.OFF, AuraBloomLevel.OFF.rebaixado());
    }

    @Test
    @DisplayName("OFF nao desenha nada a mais; FAST e HIGH desenham")
    void offEOUnicoQueNaoDesenha() {
        assertTrue(!AuraBloomLevel.OFF.desenhaAlgo());
        assertTrue(AuraBloomLevel.FAST.desenhaAlgo());
        assertTrue(AuraBloomLevel.HIGH.desenhaAlgo());
    }

    // ------------------------------------------------- o kernel

    @Test
    @DisplayName("os pesos somam 1, em todo raio -- raio e tamanho, forca e forca")
    void pesosSomamUm() {
        for (float raio = 0.0F; raio <= AuraPerfilDeBrilho.RAIO_MAXIMO; raio += 0.25F) {
            float[] pesos = KernelDeBorrao.pesos(raio);
            assertEquals(1.0F, KernelDeBorrao.soma(pesos), 1.0e-4F,
                    "SEM NORMALIZACAO, MUDAR O RAIO MUDA O BRILHO -- e o sintoma seria"
                            + " 'o halo fica mais forte quando eu aumento o raio', que"
                            + " seria verdade e nao deveria ser. Raio " + raio);
        }
    }

    @Test
    @DisplayName("raio zero deixa o centro com tudo, e nao a imagem preta")
    void raioZeroNaoApaga() {
        float[] pesos = KernelDeBorrao.pesos(0.0F);
        assertEquals(1.0F, pesos[0], 1.0e-6F,
                "DEVOLVER ZEROS DEIXARIA A IMAGEM PRETA em vez de apenas nao borrada, e"
                        + " 'a aura sumiu' e um relato muito pior que 'a aura nao borrou'");
        for (int i = 1; i < pesos.length; i++) {
            assertEquals(0.0F, pesos[i], 1.0e-6F);
        }
        assertEquals(1.0F, pesos[0], 1.0e-6F);
        assertEquals(1.0F, KernelDeBorrao.pesos(Float.NaN)[0], 1.0e-6F,
                "NaN precisa cair no caminho seguro: toda comparacao com ele e falsa");
        assertEquals(1.0F, KernelDeBorrao.pesos(-4.0F)[0], 1.0e-6F);
    }

    @Test
    @DisplayName("os pesos CAEM do centro para fora: e uma gaussiana, e nao uma caixa")
    void pesosDecrescem() {
        float[] pesos = KernelDeBorrao.pesos(5.0F);
        for (int i = 1; i < pesos.length; i++) {
            assertTrue(pesos[i] <= pesos[i - 1],
                    "o peso subiu do tap " + (i - 1) + " para o " + i + ": um desfoque de"
                            + " caixa produz bandas visiveis, e nao um halo");
        }
    }

    @Test
    @DisplayName("alem do raio, peso ZERO -- e nao um numero pequeno")
    void tapsForaDoRaioSaoZero() {
        // UM TAP COM PESO 1e-6 AINDA CUSTA DUAS AMOSTRAS DE TEXTURA por pixel, e
        // o shader pula o tap justamente quando o peso e zero. Deixar residuo
        // aqui e pagar quinze amostras para somar nada.
        float[] pesos = KernelDeBorrao.pesos(2.0F);
        for (int i = 3; i < pesos.length; i++) {
            assertEquals(0.0F, pesos[i], 0.0F,
                    "o tap " + i + " sobreviveu a um raio de 2 texels");
        }
    }

    @Test
    @DisplayName("raio maior espalha mais: o centro perde peso conforme o raio cresce")
    void raioMaiorEspalhaMais() {
        float centroEstreito = KernelDeBorrao.pesos(1.0F)[0];
        float centroLargo = KernelDeBorrao.pesos(6.0F)[0];
        assertTrue(centroLargo < centroEstreito,
                "com raio maior o centro precisa ceder peso para as laterais;"
                        + " estreito=" + centroEstreito + " largo=" + centroLargo);
    }

    @Test
    @DisplayName("o kernel cabe nos dois vec4 que o uniform do Minecraft aceita")
    void kernelCabeNoUniform() {
        // O `Uniform` do Minecraft para em quatro componentes, e por isso os
        // pesos viajam como DOIS vec4. Um array de oito floats seria mais
        // natural e nao existe -- e um kernel que crescesse alem de oito taps
        // passaria a ser truncado em silencio no caminho para a GPU.
        assertEquals(8, KernelDeBorrao.TAPS);
        assertEquals(KernelDeBorrao.TAPS, KernelDeBorrao.pesos(4.0F).length);
    }
}
