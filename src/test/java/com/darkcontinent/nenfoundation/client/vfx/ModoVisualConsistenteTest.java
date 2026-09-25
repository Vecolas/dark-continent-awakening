package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.client.hud.AparenciaDeTecnica;
import com.darkcontinent.nenfoundation.client.hud.EstadoDeNenNaHud;
import com.darkcontinent.nenfoundation.nen.aura.PresencaDeAura;
import com.darkcontinent.nenfoundation.nen.technique.PrecedenciaDeTecnicas;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * O portao que liga TODAS as tabelas de tecnica, para TODOS os observadores.
 *
 * <p><b>POR QUE ELE EXISTE.</b> A cadeia "tecnica -> o que se ve" passava por
 * tres tabelas independentes, cada uma com a propria lista incompleta:
 *
 * <pre>
 *   PresencaDeAura         servidor   Zetsu, Ken, Ren, Ten
 *   ModoVisualDeTecnica    voce       Zetsu, Ren, Ten
 *   EstadoVisualDeTerceiro os outros  via SinalDeAura
 * </pre>
 *
 * <p>Nenhum teste as comparava. O resultado em jogo: <b>ligar Ken apagava a
 * propria aura</b> -- Ken exclui Ten e Ren, e caia no OFF da segunda tabela --
 * enquanto os outros continuavam vendo, porque a terceira o desenhava como REN.
 * E <b>Ko sozinho nao produzia sinal para ninguem</b>, por falta na primeira.
 *
 * <p>Este arquivo e parametrizado sobre as sete tecnicas e cobra as duas
 * perguntas que importam: cada tabela esta COMPLETA, e as tabelas CONCORDAM.
 */
class ModoVisualConsistenteTest {

    /** A fonte dos casos: a precedencia do dominio, que o proprio teste valida. */
    static List<ResourceLocation> todasAsTecnicas() {
        return PrecedenciaDeTecnicas.ordem();
    }

    // ------------------------------------------------------------------
    // 1. COMPLETUDE -- nenhuma tabela pode esquecer uma tecnica
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a precedencia do dominio conhece TODA classe de tecnica do codigo")
    void nenhumaTecnicaFicaDeForaDaPrecedencia() {
        Set<String> noCodigo = idsDeclaradosEmNenTechnique();
        Set<String> naLista = new TreeSet<>();
        PrecedenciaDeTecnicas.ordem().forEach(id -> naLista.add(id.getPath()));

        Set<String> esquecidas = new TreeSet<>(noCodigo);
        esquecidas.removeAll(naLista);
        assertTrue(esquecidas.isEmpty(),
                "tecnica com classe em nen/technique e fora da precedencia: " + esquecidas
                        + ". Ela nao desenha shell, nao anuncia presenca e nao ganha chip "
                        + "-- exatamente o que aconteceu com o Ken.");

        Set<String> fantasmas = new TreeSet<>(naLista);
        fantasmas.removeAll(noCodigo);
        assertTrue(fantasmas.isEmpty(),
                "id na precedencia sem classe correspondente: " + fantasmas);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasAsTecnicas")
    @DisplayName("toda tecnica tem modo de shell canonico")
    void todaTecnicaDesenhaAlgumaCoisa(ResourceLocation id) {
        assertTrue(ModoVisualCanonico.modoDe(id).isPresent(),
                id + " nao tem modo visual. Ligada sozinha, ela apagaria a aura.");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasAsTecnicas")
    @DisplayName("toda tecnica tem cor e forma, e nome no chip")
    void todaTecnicaTemIdentidade(ResourceLocation id) {
        assertTrue(AparenciaDeTecnica.conhecidas().contains(id),
                id + " cairia na cor derivada do hash e na forma neutra");
        assertTrue(EstadoDeNenNaHud.nomeaveis().contains(id),
                id + " desenharia a forma na fila e deixaria o chip vazio");
    }

    // ------------------------------------------------------------------
    // 2. O DEFEITO DO KEN -- ligada sozinha, a aura NAO pode apagar
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasAsTecnicas")
    @DisplayName("tecnica ligada SOZINHA nunca cai em OFF para o proprio jogador")
    void nenhumaTecnicaApagaAPropriaAura(ResourceLocation id) {
        AuraVisualMode modo = ModoVisualDeTecnica.de(Set.of(id));

        assertNotEquals(AuraVisualMode.OFF, modo,
                id + " ligada sozinha devolve OFF: o jogador paga aura, o HUD acusa a "
                        + "tecnica, e a aura dele some da tela. Foi o defeito do Ken, e "
                        + "as exclusoes permitem este caso para Ken, Gyo, Shu e Ko.");

        if (Zetsu.ID.equals(id)) {
            assertEquals(AuraVisualMode.ZETSU, modo,
                    "Zetsu precisa parecer SUPRIMIDO, e nao ausente");
        }
    }

    @Test
    @DisplayName("o caso concreto do Ken, escrito por extenso")
    void oKenNaoApagaMaisAAura() {
        // Ken.excluidas() devolve Set.of(Ten, Ren, Zetsu): ligar Ken DESLIGA a
        // tecnica que estava acesa. Antes, o conjunto resultante {ken} nao
        // casava com List.of(Zetsu, Ren, Ten) e a shell ia a zero.
        assertEquals(AuraVisualMode.REN, ModoVisualDeTecnica.de(
                        Set.of(com.darkcontinent.nenfoundation.nen.technique.Ken.ID)),
                "Ken e envelope grande de aura liberada, como Ren");
    }

    // ------------------------------------------------------------------
    // 3. ACORDO ENTRE OBSERVADORES
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasAsTecnicas")
    @DisplayName("o que VOCE ve e o que os OUTROS veem nao se contradizem")
    void osDoisObservadoresConcordam(ResourceLocation id) {
        AuraVisualMode proprio = ModoVisualDeTecnica.de(Set.of(id));
        SinalDeAura sinal = PresencaDeAura.percebida(true, Set.of(id));
        AuraVisualMode terceiro = EstadoVisualDeTerceiro
                .de(sinal, AuraRenderLod.FULL).mode();

        if (Zetsu.ID.equals(id)) {
            // A UNICA ASSIMETRIA LEGITIMA, e ela e a mecanica: quem esta em
            // Zetsu ve a propria aura suprimida; os outros nao veem nada.
            assertEquals(AuraVisualMode.ZETSU, proprio);
            assertEquals(AuraVisualMode.OFF, terceiro,
                    "se terceiros vissem Zetsu, a tecnica deixaria de existir");
            return;
        }

        assertNotEquals(AuraVisualMode.OFF, terceiro,
                id + " e visivel para o proprio jogador e INVISIVEL para os outros. "
                        + "Fora de Zetsu, essa assimetria e defeito: foi o caso de Ko, "
                        + "que ninguem percebia.");
        assertEquals(proprio, terceiro,
                id + ": os dois caminhos desenham shells diferentes para a MESMA "
                        + "tecnica. E a divergencia que o canonico existe para impedir.");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasAsTecnicas")
    @DisplayName("a COR e a mesma nos dois caminhos, ou a diferenca e do sinal pobre")
    void aCorNaoDivergeSemMotivo(ResourceLocation id) {
        if (Zetsu.ID.equals(id)) {
            return;
        }
        SinalDeAura sinal = PresencaDeAura.percebida(true, Set.of(id));
        int corDeTerceiro = EstadoVisualDeTerceiro.de(sinal, AuraRenderLod.FULL)
                .primaryColor();
        int corPropria = AparenciaDeTecnica.de(id).cor();

        // Gyo, Shu e Ko chegam empacotados em TEN/REN de proposito: SinalDeAura
        // tem quatro valores porque atravessa a rede, e cada valor a mais conta
        // ao cliente do vizinho algo sobre voce. A cor do terceiro sera a do
        // representante, e isso e a perda funcionando -- nao um erro.
        boolean representadaPorOutra = corDeTerceiro != corPropria;
        if (representadaPorOutra) {
            assertTrue(Set.of(SinalDeAura.TEN, SinalDeAura.REN).contains(sinal),
                    id + " diverge de cor sem ser por empacotamento de sinal");
        }
    }

    // ------------------------------------------------------------------
    // 4. COMBINACOES LEGAIS -- a precedencia nao pode apagar ninguem
    // ------------------------------------------------------------------

    @Test
    @DisplayName("combinacoes permitidas pelas exclusoes nunca apagam a aura")
    void asCombinacoesLegaisDesenhamAlgo() {
        var ten = com.darkcontinent.nenfoundation.nen.technique.Ten.ID;
        var ren = com.darkcontinent.nenfoundation.nen.technique.Ren.ID;
        var ken = com.darkcontinent.nenfoundation.nen.technique.Ken.ID;
        var gyo = com.darkcontinent.nenfoundation.nen.technique.Gyo.ID;
        var shu = com.darkcontinent.nenfoundation.nen.technique.Shu.ID;
        var ko = com.darkcontinent.nenfoundation.nen.technique.Ko.ID;

        List<Set<ResourceLocation>> legais = List.of(
                Set.of(ten, gyo), Set.of(ten, shu), Set.of(ten, gyo, shu),
                Set.of(ten, ko), Set.of(ten, ko, shu),
                Set.of(ren, gyo), Set.of(ren, ko, shu),
                Set.of(ken, gyo), Set.of(ken, ko, shu),
                Set.of(gyo, shu), Set.of(ko, shu));

        for (Set<ResourceLocation> combinacao : legais) {
            assertNotEquals(AuraVisualMode.OFF, ModoVisualDeTecnica.de(combinacao),
                    "combinacao legal sem shell: " + combinacao);
            assertNotEquals(SinalDeAura.NENHUM,
                    PresencaDeAura.percebida(true, combinacao),
                    "combinacao legal invisivel para terceiros: " + combinacao);
        }
    }

    @Test
    @DisplayName("tecnica desconhecida nao apaga a aura de quem tem outra ligada")
    void umIdDeDatapackNaoDerrubaAShell() {
        var ten = com.darkcontinent.nenfoundation.nen.technique.Ten.ID;
        var estranha = ResourceLocation.fromNamespaceAndPath("outromod", "tecnica_nova");

        assertEquals(AuraVisualMode.TEN, ModoVisualDeTecnica.de(Set.of(ten, estranha)));
        assertEquals(AuraVisualMode.OFF, ModoVisualDeTecnica.de(Set.of(estranha)),
                "sozinha, uma tecnica que ninguem sabe desenhar nao inventa visual");
    }

    // ------------------------------------------------------------------

    /** Varre {@code nen/technique} atras de todo {@code ID} publico declarado. */
    private static Set<String> idsDeclaradosEmNenTechnique() {
        Pattern padrao = Pattern.compile(
                "public\\s+static\\s+final\\s+ResourceLocation\\s+ID\\s*=\\s*"
                        + "NenFoundation\\.id\\(\"([a-z_]+)\"\\)");
        Set<String> achados = new TreeSet<>();
        for (var arquivo : Repo.varrer(
                "src/main/java/com/darkcontinent/nenfoundation/nen/technique", ".java")) {
            String texto;
            try {
                texto = Files.readString(arquivo);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            Matcher m = padrao.matcher(texto);
            while (m.find()) {
                achados.add(m.group(1));
            }
        }
        assertTrue(achados.size() >= 7,
                "a varredura achou " + achados.size() + " tecnicas; o regex provavelmente "
                        + "parou de casar e este portao viraria carimbo");
        return achados;
    }
}
