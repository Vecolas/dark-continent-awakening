package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellPass;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraRibbonProfile;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os perfis de aura, que ate aqui eram constantes no codigo.
 *
 * <p>ESTE ARQUIVO LE OS JSON DE VERDADE, e nao um texto colado aqui. Um teste
 * que validasse uma copia provaria que a copia esta certa -- e a copia nao e o
 * que o jogo carrega.
 */
class AuraPerfilVisualTest {

    private static final String DIRETORIO = "src/main/resources/assets/nenfoundation/nen_vfx";

    /** O bloco obrigatorio, para os JSON inline que testam OUTRA coisa. */
    private static final String FILAMENTOS =
            "\"filamentos\": {\"quantidade\": 8, \"comprimento_min\": 0.15,"
                    + " \"comprimento_max\": 0.6, \"largura\": 0.009,"
                    + " \"ciclo_segundos\": 1.1}";

    private static List<Path> arquivos() {
        List<Path> encontrados = new ArrayList<>(Repo.varrer(DIRETORIO, ".json"));
        return encontrados;
    }

    private static AuraPerfilVisual ler(String nome) {
        JsonElement json = JsonParser.parseString(Repo.texto(DIRETORIO + "/" + nome));
        return AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(erro -> new AssertionError(nome + " nao le: " + erro));
    }

    @Test
    @DisplayName("ha perfil no disco, e todos leem")
    void todosOsArquivosLeem() {
        List<Path> todos = arquivos();
        assertTrue(!todos.isEmpty(),
                "nenhum perfil em " + DIRETORIO + ". Portao com zero verificacoes reprova:"
                        + " diretorio vazio nao e aprovacao.");
        for (Path p : todos) {
            ler(p.getFileName().toString());
        }
    }

    @Test
    @DisplayName("Ten e Ren existem, e Ren e a MESMA shell mais densa")
    void renEATenComABordaAberta() {
        AuraPerfilVisual ten = ler("ten.json");
        AuraPerfilVisual ren = ler("ren.json");

        for (AuraShellPass passe : AuraShellPass.values()) {
            assertTrue(ren.alphaDe(passe) > ten.alphaDe(passe),
                    "Ren tem de ser mais forte que Ten em " + passe);
            // Expoente MENOR = borda mais espessa. Ren abre a borda.
            assertTrue(ren.fresnelDe(passe) < ten.fresnelDe(passe),
                    "Ren tem de ter a borda mais aberta que Ten em " + passe);
        }
        assertTrue(ren.velocidadeDeFluxo() > ten.velocidadeDeFluxo());
        assertTrue(ren.reforcoDaBorda() > ten.reforcoDaBorda());
    }

    @Test
    @DisplayName("a borda e a camada que carrega a leitura, nos dois perfis")
    void bordaEAMaisForte() {
        for (String nome : new String[] {"ten.json", "ren.json"}) {
            AuraPerfilVisual p = ler(nome);
            assertTrue(p.alphaDe(AuraShellPass.BORDA) > p.alphaDe(AuraShellPass.INTERNA), nome);
            assertTrue(p.alphaDe(AuraShellPass.BORDA) > p.alphaDe(AuraShellPass.EXTERNA), nome);
        }
    }

    @Test
    @DisplayName("Fresnel chapado e recusado PELO CODEC, e nao por excecao")
    void fresnelPrecisaDiminuirParaFora() {
        // A CHECAGEM MUDOU DE LUGAR, e o motivo importa: no construtor ela
        // LANCAVA durante a leitura do dado, e excecao dentro do reload de
        // recursos derruba o carregamento inteiro por causa de um resource pack
        // torto. Em `validate`, o mesmo arquivo vira erro com motivo.
        assertTrue(chapado("2.0, 2.0, 2.0").error().isPresent(),
                "tres expoentes iguais passaram: as camadas virariam uma so mais opaca");
        assertTrue(chapado("1.0, 2.0, 3.0").error().isPresent(),
                "invertido tambem: a borda ficaria mais estreita para fora");
        assertTrue(chapado("3.4, 2.7, 2.0").error().isEmpty(), "o perfil correto foi recusado");
    }

    private static com.mojang.serialization.DataResult<AuraPerfilVisual> chapado(String tres) {
        String[] f = tres.split(",\s*");
        JsonElement json = JsonParser.parseString("""
                {"alpha_interno": 0.05, "alpha_borda": 0.2, "alpha_externo": 0.03,
                 "fresnel_interno": %s, "fresnel_borda": %s, "fresnel_externo": %s,
                 "velocidade_de_fluxo": 0.12, "escala_de_ruido": 4.0, "reforco_da_borda": 0.9,
                 "taxa_de_faiscas": 0.4, "tamanho_de_particula": 0.18, %s}
                """.formatted(f[0], f[1], f[2], FILAMENTOS));
        return AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, json);
    }

    @Test
    @DisplayName("o codec RECUSA dado torto em vez de aceitar numero plausivel")
    void codecRecusaDadoInvalido() {
        // Alpha acima de 1, que um pack pode escrever por engano.
        JsonElement torto = JsonParser.parseString("""
                {"alpha_interno": 5.0, "alpha_borda": 0.2, "alpha_externo": 0.03,
                 "fresnel_interno": 3.4, "fresnel_borda": 2.7, "fresnel_externo": 2.0,
                 "velocidade_de_fluxo": 0.12, "escala_de_ruido": 4.0, "reforco_da_borda": 0.9,
                 "taxa_de_faiscas": 0.4, "tamanho_de_particula": 0.18, %s}
                """.formatted(FILAMENTOS));
        assertTrue(AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, torto).error().isPresent(),
                "alpha acima de 1 passou. Corrigir em silencio esconderia um pack quebrado"
                        + " atras de numeros plausiveis.");

        JsonElement faltando = JsonParser.parseString("{\"alpha_interno\": 0.05}");
        assertTrue(AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, faltando).error().isPresent(),
                "campo faltando passou");
    }

    @Test
    @DisplayName("ida e volta pelo codec preserva o perfil")
    void idaEVolta() {
        AuraPerfilVisual original = ler("ten.json");
        JsonElement json = AuraPerfilVisual.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow(erro -> new AssertionError("nao escreve: " + erro));
        AuraPerfilVisual volta = AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(erro -> new AssertionError("nao le de volta: " + erro));
        assertEquals(original, volta);
    }

    @Test
    @DisplayName("o perfil de emergencia e visivelmente mais fraco que os reais")
    void emergenciaEPerceptivel() {
        AuraPerfilVisual ten = ler("ten.json");
        // ELE NAO PODE SER COPIA de nenhum arquivo: se fosse, "nao carregou"
        // seria indistinguivel de "carregou", e ninguem iria procurar o motivo.
        assertTrue(AuraPerfilVisual.SEGURO.alphaDe(AuraShellPass.BORDA)
                        < ten.alphaDe(AuraShellPass.BORDA),
                "o perfil de emergencia precisa ser PERCEPTIVELMENTE mais fraco");
        assertTrue(!AuraPerfilVisual.SEGURO.equals(ten));
    }

    @Test
    @DisplayName("o perfil apagado zera os alphas E a particula")
    void apagadoEZero() {
        AuraPerfilVisual apagado = ler("ren.json").apagado();
        for (AuraShellPass passe : AuraShellPass.values()) {
            assertEquals(0.0F, apagado.alphaDe(passe), "a ausencia e a informacao, em " + passe);
        }
        // A PARTICULA ENTROU NESTA CONTA no AV0, quando os dois numeros dela
        // sairam do codigo. Zerar so os alphas apagaria a shell e deixaria a
        // nuvem de poeira acesa -- ou seja, Zetsu vira "poeira desligada", que e
        // exatamente a leitura que o ADR-015 existe para nao produzir.
        assertEquals(0.0F, apagado.taxaDeFaiscas(),
                "Zetsu com faisca nao e supressao");
        assertEquals(0, apagado.filamentos().quantidade(),
                "Zetsu com filamento tambem nao");
    }

    @Test
    @DisplayName("os numeros de filamento estao no DADO, e Ren e a mesma linguagem mais densa")
    void filamentoVemDoArquivo() {
        // ELES MORAVAM EM `AuraRibbonProfile.ten()` e `.ren()`, com um javadoc
        // que prometia tira-los "quando o perfil existir". O perfil existia
        // havia dois gates. Esta e a regua que impede a promessa de voltar:
        // rebaixar Ren aqui reprova, e reescrever a contagem como constante no
        // renderer faz mexer no JSON parar de mudar a tela.
        AuraRibbonProfile ten = ler("ten.json").filamentos();
        AuraRibbonProfile ren = ler("ren.json").filamentos();

        assertTrue(ten.quantidade() > 0, "Ten sem filamento nenhum");
        assertTrue(ren.quantidade() > ten.quantidade(), "Ren nao tem mais filamentos que Ten");
        assertTrue(ren.comprimentoMax() > ten.comprimentoMax(), "os de Ren nao sao mais longos");
        assertTrue(ren.largura() > ten.largura(), "os de Ren nao sao mais grossos");
        assertTrue(ren.cicloSegundos() < ten.cicloSegundos(), "Ren nao troca mais rapido");
        assertTrue(ren.quantidade() <= AuraRibbonProfile.TETO,
                "acima do teto o custo por jogador deixa de ter limite");
    }

    @Test
    @DisplayName("bloco de filamento faltando ou torto e RECUSADO, e nao assumido")
    void filamentoTortoERecusado() {
        // SEM O BLOCO, o perfil inteiro tem de ser recusado. Assumir um padrao
        // aqui daria um Ren que carrega e desenha filamento de Ten -- carregou
        // e errado e pior que nao carregou, porque nao tem sintoma.
        JsonElement semBloco = JsonParser.parseString("""
                {"alpha_interno": 0.05, "alpha_borda": 0.2, "alpha_externo": 0.03,
                 "fresnel_interno": 3.4, "fresnel_borda": 2.7, "fresnel_externo": 2.0,
                 "velocidade_de_fluxo": 0.12, "escala_de_ruido": 4.0, "reforco_da_borda": 0.9,
                 "taxa_de_faiscas": 0.4, "tamanho_de_particula": 0.18}
                """);
        assertTrue(AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, semBloco).error().isPresent(),
                "perfil sem o bloco `filamentos` passou");

        assertTrue(filamento("8, 0.15, 0.6, 0.2, 1.1").error().isPresent(),
                "largura de tubo neon passou pelo codec");
        assertTrue(filamento("8, 0.8, 0.2, 0.009, 1.1").error().isPresent(),
                "comprimento minimo maior que o maximo passou");
        assertTrue(filamento("200, 0.15, 0.6, 0.009, 1.1").error().isPresent(),
                "contagem acima do teto passou");
        assertTrue(filamento("8, 0.15, 0.6, 0.009, 1.1").error().isEmpty(),
                "o bloco correto foi recusado; um portao que reprova tudo nao mede nada");
    }

    private static com.mojang.serialization.DataResult<AuraRibbonProfile> filamento(String cinco) {
        String[] f = cinco.split(",\s*");
        JsonElement json = JsonParser.parseString("""
                {"quantidade": %s, "comprimento_min": %s, "comprimento_max": %s,
                 "largura": %s, "ciclo_segundos": %s}
                """.formatted(f[0], f[1], f[2], f[3], f[4]));
        return AuraRibbonProfile.CODEC.parse(JsonOps.INSTANCE, json);
    }

    @Test
    @DisplayName("os numeros de particula estao no DADO, e Ren e mais denso que Ten")
    void particulaEAcabamentoEVemDoArquivo() {
        // ELES MORAVAM EM `AuraVisualPreset.tenBasic()` e `renBasic()`, no
        // codigo, ao lado de cinco campos que nao tinham leitor nenhum. Este
        // teste e a regua que impede a volta: se alguem reescrever a densidade
        // como constante no emissor, mexer no JSON deixa de mudar o resultado --
        // e e este arquivo, e nao a tela, que acusa primeiro.
        AuraPerfilVisual ten = ler("ten.json");
        AuraPerfilVisual ren = ler("ren.json");

        assertEquals(0.4F, ten.taxaDeFaiscas(), 1.0e-6F,
                "Ten sem faisca nenhuma; acabamento discreto nao e acabamento ausente");
        assertTrue(ren.taxaDeFaiscas() > ten.taxaDeFaiscas(),
                "Ren emite " + ren.taxaDeFaiscas() + " contra " + ten.taxaDeFaiscas()
                        + " de Ten -- deixou de ser mais denso");
        assertTrue(ren.tamanhoDeParticula() > ten.tamanhoDeParticula(),
                "a faisca de Ren deixou de ser maior que a de Ten");

        assertEquals(4.0F, ren.taxaDeFaiscas(), 1.0e-6F,
                "a taxa de Ren saiu do perfil aprovado");
    }
}
