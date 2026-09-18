package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilDePressao;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraAnchor;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Colunas, anel de chao e detritos -- o que so existe quando a aura e liberada.
 *
 * <p><b>O QUE ESTE ARQUIVO PROTEGE.</b> Os tres componentes de Ren tem tetos que
 * nao sao tuning: oito colunas, quarenta e oito segmentos, doze detritos. Passar
 * deles nao produz erro -- produz uma fogueira sob o jogador, e a hierarquia de
 * leitura invertida. Um numero absurdo num resource pack de terceiro chega pelo
 * mesmo caminho que o nosso, e o teto e a unica defesa.
 *
 * <p>O outro modo de falha e mais fino: {@code pressao} ausente num perfil. Se
 * ela fosse opcional com padrao zero, "esqueceu" e "escreveu zero de proposito"
 * seriam indistinguiveis -- e o dia em que Ren nao desenhar coluna nenhuma
 * ninguem saberia onde procurar.
 */
class AuraPressaoDeRenTest {

    private static final String DIRETORIO = "src/main/resources/assets/nenfoundation/nen_vfx";

    private static AuraPerfilVisual ler(String nome) {
        JsonElement json = JsonParser.parseString(Repo.texto(DIRETORIO + "/" + nome));
        return AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(erro -> new AssertionError(nome + " nao le: " + erro));
    }

    // ------------------------------------------------- o dado

    @Test
    @DisplayName("Ten NAO toca o chao: o bloco existe e e todo zero")
    void tenNaoTocaOChao() {
        AuraPerfilDePressao p = ler("ten.json").pressao();
        assertEquals(0, p.colunas(), "Ten nao tem coluna vertical");
        assertEquals(0.0F, p.anel(), "Ten nao tem anel de pressao");
        assertEquals(0, p.detritos(), "Ten nao levanta fragmento");
        assertTrue(!p.existe(),
                "A AUSENCIA PRECISA ESTAR ESCRITA NO DADO, e nao numa guarda do renderer:"
                        + " e assim que o alvo da interpolacao TEN->REN vira um objeto"
                        + " legitimo em vez de um caso especial");
    }

    @Test
    @DisplayName("Ren tem os tres componentes, dentro dos tetos de design")
    void renTemOsTres() {
        AuraPerfilDePressao p = ler("ren.json").pressao();
        assertTrue(p.existe());
        assertTrue(p.colunas() >= 4 && p.colunas() <= AuraPerfilDePressao.TETO_DE_COLUNAS,
                "A DIRECAO DE ARTE PEDE DE 4 A 8 COLUNAS. Passar de oito transforma"
                        + " pressao em fogueira e inverte a hierarquia de leitura: "
                        + p.colunas());
        assertTrue(p.anelSegmentos() >= 24
                        && p.anelSegmentos() <= AuraPerfilDePressao.TETO_DE_SEGMENTOS,
                "de 24 a 48 segmentos: " + p.anelSegmentos());
        assertTrue(p.detritos() > 0 && p.detritos() <= AuraPerfilDePressao.TETO_DE_DETRITOS,
                "ate 12 detritos: " + p.detritos());
        assertTrue(p.alturaMaxima() <= AuraPerfilDePressao.ALTURA_MAXIMA);
        assertTrue(p.alturaMinima() >= 0.8F - 1.0e-5F,
                "a coluna mais curta sobe 0,8 bloco: " + p.alturaMinima());
    }

    @Test
    @DisplayName("Ren e mais forte que Ten em bloom e em pulso, e Ten NAO pisca")
    void brilhoEPulso() {
        AuraPerfilVisual ten = ler("ten.json");
        AuraPerfilVisual ren = ler("ren.json");
        assertTrue(ren.bloom() > ten.bloom(), "Ren alimenta mais o passe de brilho");
        assertTrue(ren.amplitudeDePulso() > ten.amplitudeDePulso());
        assertTrue(ten.amplitudeDePulso() >= 0.02F && ten.amplitudeDePulso() <= 0.05F,
                "TEN NAO PISCA: a amplitude fica entre 0,02 e 0,05, e veio "
                        + ten.amplitudeDePulso());
    }

    @Test
    @DisplayName("um perfil SEM o bloco de pressao e recusado, e nao assume zero")
    void blocoAusenteReprova() {
        // O CASO QUE DEVE REPROVAR, alimentado de proposito. Com um bloco
        // opcional de padrao zero, este JSON passaria -- e "esqueceu a pressao"
        // ficaria indistinguivel de "Ten, que nao tem pressao".
        JsonElement semPressao = JsonParser.parseString("""
                {"alpha_interno": 0.05, "alpha_borda": 0.2, "alpha_externo": 0.03,
                 "fresnel_interno": 3.4, "fresnel_borda": 2.7, "fresnel_externo": 2.0,
                 "velocidade_de_fluxo": 0.12, "escala_de_ruido": 4.0, "reforco_da_borda": 0.9,
                 "taxa_de_faiscas": 0.4, "tamanho_de_particula": 0.18,
                 "filamentos": {"quantidade": 8, "comprimento_min": 0.15,
                   "comprimento_max": 0.6, "largura": 0.009, "ciclo_segundos": 1.1},
                 "bloom": 0.2, "amplitude_de_pulso": 0.025}
                """);
        assertTrue(AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, semPressao).error().isPresent(),
                "o bloco de pressao e OBRIGATORIO");
    }

    @Test
    @DisplayName("colunas acima do teto sao recusadas PELO CODEC, e nao por excecao")
    void tetoDeColunasNoCodec() {
        // O construtor LANCA, e o codec constroi para depois validar -- por isso
        // a faixa precisa estar no codec. Uma excecao dentro do reload de
        // recursos derruba o carregamento inteiro por causa de um pack torto.
        assertTrue(pressao("\"colunas\": 30").error().isPresent(),
                "trinta colunas passaram: pressao viraria fogueira");
        assertTrue(pressao("\"anel_segmentos\": 500").error().isPresent());
        assertTrue(pressao("\"detritos\": 200").error().isPresent());
    }

    @Test
    @DisplayName("minimo maior que maximo e recusado, nas duas faixas")
    void ordemDasFaixas() {
        assertTrue(pressao("\"altura_minima\": 2.0, \"altura_maxima\": 1.0").error().isPresent());
        assertTrue(pressao("\"anel_raio_minimo\": 1.8, \"anel_raio_maximo\": 0.9")
                .error().isPresent());
    }

    private static com.mojang.serialization.DataResult<AuraPerfilDePressao> pressao(
            String sobrescrito) {
        String json = """
                {"colunas": 6, "altura_minima": 0.8, "altura_maxima": 2.5,
                 "anel": 0.65, "anel_raio_minimo": 0.8, "anel_raio_maximo": 2.0,
                 "anel_segmentos": 32, "detritos": 8, %s}
                """.formatted(sobrescrito);
        // A chave sobrescrita aparece DEPOIS: em JSON, a ultima ocorrencia vence
        // no parser do Gson, e e assim que o caso torto substitui o correto sem
        // precisar de um template por campo.
        return AuraPerfilDePressao.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    // ------------------------------------------------- a aritmetica

    @Test
    @DisplayName("o raio do anel cresce com a intensidade, dentro da faixa do perfil")
    void raioSegueAIntensidade() {
        AuraPerfilDePressao p = ler("ren.json").pressao();
        assertEquals(p.anelRaioMinimo(), p.raioPara(0.0F), 1.0e-5F);
        assertEquals(p.anelRaioMaximo(), p.raioPara(1.0F), 1.0e-5F);
        assertTrue(p.raioPara(0.5F) > p.raioPara(0.2F));
        // SATURA, e nao extrapola: uma intensidade fora de faixa nao pode
        // produzir um anel de dez blocos.
        assertEquals(p.anelRaioMaximo(), p.raioPara(9.0F), 1.0e-5F);
        assertEquals(p.anelRaioMinimo(), p.raioPara(-3.0F), 1.0e-5F);
    }

    @Test
    @DisplayName("interpolar entre Ten e Ren ARREDONDA as contagens, e nao trunca")
    void interpolacaoArredonda() {
        AuraPerfilDePressao ten = AuraPerfilDePressao.NENHUMA;
        AuraPerfilDePressao ren = ler("ren.json").pressao();

        // COM TRUNCAMENTO, o ultimo passo antes do alvo desenharia cinco colunas
        // de seis -- e a sexta apareceria de uma vez no quadro final. E a troca
        // seca que a transicao em fases existe para nao produzir.
        assertEquals(ren.colunas(), AuraPerfilDePressao.interpolar(ten, ren, 0.95F).colunas(),
                "a 95% do caminho, a contagem ja precisa ser a de Ren");
        assertEquals(0, AuraPerfilDePressao.interpolar(ten, ren, 0.0F).colunas());
        assertEquals(ren.colunas(), AuraPerfilDePressao.interpolar(ten, ren, 1.0F).colunas());
    }

    @Test
    @DisplayName("interpolar satura: t fora de 0..1 nao inventa perfil")
    void interpolacaoSatura() {
        AuraPerfilDePressao ren = ler("ren.json").pressao();
        assertEquals(ren, AuraPerfilDePressao.interpolar(AuraPerfilDePressao.NENHUMA, ren, 5.0F));
        assertEquals(AuraPerfilDePressao.NENHUMA,
                AuraPerfilDePressao.interpolar(AuraPerfilDePressao.NENHUMA, ren, -2.0F));
    }

    @Test
    @DisplayName("a altura de uma coluna cai dentro da faixa, para qualquer semente")
    void alturaDentroDaFaixa() {
        AuraPerfilDePressao p = ler("ren.json").pressao();
        for (int i = 0; i < 2000; i++) {
            float h = p.alturaDe(com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraCurve
                    .semente(i * 31L, AuraAnchor.SHOULDER_LEFT, i, i % 7));
            assertTrue(h >= p.alturaMinima() - 1.0e-4F && h <= p.alturaMaxima() + 1.0e-4F,
                    "altura fora da faixa do perfil: " + h);
        }
    }

    @Test
    @DisplayName("o construtor recusa numero fora do teto, para quem construir em Java")
    void construtorTambemRecusa() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuraPerfilDePressao(99, 0.8F, 2.5F, 0.6F, 0.8F, 2.0F, 32, 8));
        assertThrows(IllegalArgumentException.class,
                () -> new AuraPerfilDePressao(6, 0.8F, 2.5F, 0.6F, 0.8F, 2.0F, 32, 99));
    }

    // ------------------------------------------------- as ancoras de coluna

    @Test
    @DisplayName("ha exatamente um lugar de coluna por unidade do teto de design")
    void lugaresDeColunaBatemComOTeto() {
        // SE ELES SE SEPARAREM, uma das duas coisas acontece em silencio: ou o
        // perfil pede oito colunas e so seis nascem, ou duas colunas saem do
        // mesmo ponto e sao lidas como uma coluna grossa.
        assertEquals(AuraPerfilDePressao.TETO_DE_COLUNAS, AuraAnchor.lugaresDeColuna(),
                "o teto de colunas e a lista de ancoras precisam concordar");
    }

    @Test
    @DisplayName("nenhum lugar de coluna se repete: duas no mesmo ponto viram uma grossa")
    void lugaresDeColunaSaoDistintos() {
        java.util.Set<AuraAnchor> vistas = new java.util.HashSet<>();
        for (int i = 0; i < AuraAnchor.lugaresDeColuna(); i++) {
            assertTrue(vistas.add(AuraAnchor.coluna(i)),
                    "a ancora " + AuraAnchor.coluna(i) + " aparece duas vezes");
        }
    }

    @Test
    @DisplayName("as colunas nascem em ombros, costas, pernas e cabeca -- e nao nas maos")
    void lugaresDeColunaSaoOsDaDirecaoDeArte() {
        java.util.Set<AuraAnchor> esperadas = java.util.Set.of(
                AuraAnchor.SHOULDER_LEFT, AuraAnchor.SHOULDER_RIGHT, AuraAnchor.BACK_CENTER,
                AuraAnchor.THIGH_LEFT, AuraAnchor.THIGH_RIGHT,
                AuraAnchor.HEAD_TOP, AuraAnchor.HEAD_LEFT, AuraAnchor.HEAD_RIGHT);
        for (int i = 0; i < AuraAnchor.lugaresDeColuna(); i++) {
            assertTrue(esperadas.contains(AuraAnchor.coluna(i)),
                    "coluna nascendo em " + AuraAnchor.coluna(i) + ", que nao esta na"
                            + " direcao de arte");
        }
    }

    @Test
    @DisplayName("o indice de coluna satura em vez de estourar o vetor")
    void indiceDeColunaSatura() {
        // Quem chama e o caminho de desenho, e uma excecao no tick de render
        // derruba o mundo inteiro -- nao so a aura.
        assertEquals(AuraAnchor.coluna(0), AuraAnchor.coluna(AuraAnchor.lugaresDeColuna()));
        assertEquals(AuraAnchor.coluna(1), AuraAnchor.coluna(-AuraAnchor.lugaresDeColuna() + 1));
    }
}
