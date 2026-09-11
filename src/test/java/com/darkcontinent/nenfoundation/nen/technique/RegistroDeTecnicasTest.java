package com.darkcontinent.nenfoundation.nen.technique;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do registro de tecnicas, e da simetria das exclusoes.
 *
 * <p>A SIMETRIA E O PONTO. Uma exclusao declarada de um lado so nao produz
 * erro: produz uma combinacao ilegal que FUNCIONA, e que aparece no dia em que
 * alguem tentar o par na ordem que ninguem testou. E o erro numero 4 da lista
 * do CLAUDE.md, e a razao de a conferencia acontecer no selamento.
 */
class RegistroDeTecnicasTest {

    private static ResourceLocation id(String nome) {
        return ResourceLocation.fromNamespaceAndPath("nenfoundation", nome);
    }

    /**
     * Uma tecnica de teste que so carrega id e exclusoes.
     *
     * <p>O ciclo de vida devolve nada de proposito: este portao mede a TABELA,
     * e nao o comportamento. Misturar os dois faria um defeito de exclusao
     * aparecer como falha de ativacao, no lugar errado.
     */
    private record Falsa(ResourceLocation id, Set<ResourceLocation> incompativeisCom)
            implements NenTechnique {

        @Override
        public TechniqueActivationResult canActivate(ServerPlayer jogador, NenContext ctx) {
            return TechniqueActivationResult.aceito();
        }

        @Override
        public void onActivate(ServerPlayer jogador, NenContext ctx) {
        }

        @Override
        public void serverTick(ServerPlayer jogador, NenContext ctx) {
        }

        @Override
        public void onDeactivate(ServerPlayer jogador, NenContext ctx, StopReason motivo) {
        }
    }

    private static Falsa tecnica(String nome, String... incompativeis) {
        return new Falsa(id(nome),
                java.util.Arrays.stream(incompativeis)
                        .map(RegistroDeTecnicasTest::id)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }

    // ---------------------------------------------------------- simetria

    @Test
    @DisplayName("exclusao declarada dos DOIS lados sela sem problema")
    void simetricaPassa() {
        RegistroDeTecnicas registro = RegistroDeTecnicas.selar(List.of(
                tecnica("ten", "zetsu"),
                tecnica("zetsu", "ten")));

        assertEquals(2, registro.tamanho());
        assertTrue(registro.porId(id("ten")).isPresent());
    }

    @Test
    @DisplayName("exclusao PELA METADE reprova o selamento")
    void assimetricaReprova() {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> RegistroDeTecnicas.selar(List.of(
                        tecnica("ten", "zetsu"),
                        tecnica("zetsu"))));

        assertTrue(erro.getMessage().contains("pela metade"),
                "a mensagem nao nomeia o problema: " + erro.getMessage());
        assertTrue(erro.getMessage().contains("ten") && erro.getMessage().contains("zetsu"),
                "a mensagem precisa nomear OS DOIS lados; sem isso o operador"
                        + " nao sabe qual arquivo abrir. Veio: " + erro.getMessage());
    }

    @Test
    @DisplayName("a assimetria e pega nos dois sentidos, e nao so num")
    void assimetriaNosDoisSentidos() {
        // Inverter a ordem do registro nao pode mudar o veredito. Se mudasse,
        // o portao dependeria de quem foi registrado primeiro -- e passaria
        // metade das vezes.
        assertThrows(IllegalStateException.class,
                () -> RegistroDeTecnicas.selar(List.of(
                        tecnica("zetsu"),
                        tecnica("ten", "zetsu"))));
    }

    @Test
    @DisplayName("exclusao apontando para tecnica inexistente reprova")
    void exclusaoOrfaReprova() {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> RegistroDeTecnicas.selar(List.of(tecnica("ten", "ken"))));

        assertTrue(erro.getMessage().contains("nao esta registrada"),
                "Uma exclusao que aponta para o vazio nao e so id errado: a"
                        + " maquina de estados nunca teria com quem conferir."
                        + " Veio: " + erro.getMessage());
    }

    @Test
    @DisplayName("tecnica incompativel consigo mesma reprova")
    void autoExclusaoReprova() {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> RegistroDeTecnicas.selar(List.of(tecnica("ten", "ten"))));

        assertTrue(erro.getMessage().contains("consigo mesma"), erro.getMessage());
    }

    @Test
    @DisplayName("id repetido reprova")
    void idRepetidoReprova() {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> RegistroDeTecnicas.selar(List.of(tecnica("ten"), tecnica("ten"))));

        assertTrue(erro.getMessage().contains("id repetido"),
                "Duas tecnicas com o mesmo id nao dao erro sozinhas: a segunda"
                        + " simplesmente vence, e a primeira nunca roda."
                        + " Veio: " + erro.getMessage());
    }

    @Test
    @DisplayName("o selamento relata TODOS os problemas, e nao so o primeiro")
    void relataTudoDeUmaVez() {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> RegistroDeTecnicas.selar(List.of(
                        tecnica("ten", "zetsu"),
                        tecnica("zetsu"),
                        tecnica("ren", "ken"))));

        assertTrue(erro.getMessage().contains("pela metade")
                        && erro.getMessage().contains("nao esta registrada"),
                "Quem esta acrescentando uma tecnica quer os erros juntos, e nao"
                        + " uma reinicializacao por erro. Veio: " + erro.getMessage());
    }

    // ---------------------------------------------------------- conflitos

    @Test
    @DisplayName("conflitosDe nomeia com quem o conflito e")
    void conflitosNomeiam() {
        RegistroDeTecnicas registro = RegistroDeTecnicas.selar(List.of(
                tecnica("ten", "zetsu"),
                tecnica("ren", "zetsu"),
                tecnica("zetsu", "ten", "ren")));

        assertEquals(Set.of(id("ten"), id("ren")),
                registro.conflitosDe(id("zetsu"), Set.of(id("ten"), id("ren"))),
                "Devolver um booleano faria a recusa dizer 'nao da' sem nome, e"
                        + " o jogador teria de adivinhar qual desligar.");

        assertEquals(Set.of(),
                registro.conflitosDe(id("ten"), Set.of(id("ren"))),
                "ten e ren nao se excluem; nao pode haver conflito entre eles.");
    }

    @Test
    @DisplayName("uma tecnica nao conflita consigo mesma ao ja estar ativa")
    void naoConflitaConsigoMesma() {
        RegistroDeTecnicas registro = RegistroDeTecnicas.selar(List.of(
                tecnica("ten", "zetsu"), tecnica("zetsu", "ten")));

        assertEquals(Set.of(), registro.conflitosDe(id("ten"), Set.of(id("ten"))),
                "Reativar uma tecnica ja ativa nao pode aparecer como conflito:"
                        + " isso a faria desligar a si mesma ao ser religada.");
    }

    @Test
    @DisplayName("id desconhecido nao tem conflito, e nao estoura")
    void desconhecidoNaoEstoura() {
        RegistroDeTecnicas registro = RegistroDeTecnicas.selar(List.of(tecnica("ten")));
        assertEquals(Set.of(), registro.conflitosDe(id("ko"), Set.of(id("ten"))));
    }

    // ---------------------------------------------------------- registro vazio

    @Test
    @DisplayName("registro vazio sela, e nao finge ter tecnica")
    void vazioSela() {
        RegistroDeTecnicas registro = RegistroDeTecnicas.selar(List.of());

        assertEquals(0, registro.tamanho());
        assertTrue(registro.porId(id("ten")).isEmpty());
        assertTrue(registro.ids().isEmpty());
    }

    @Test
    @DisplayName("o registro nao muda depois de selado")
    void seladoEImutavel() {
        RegistroDeTecnicas registro = RegistroDeTecnicas.selar(List.of(tecnica("ten")));

        // Tecnica acrescentada depois escaparia da conferencia de simetria --
        // e a conferencia so vale se ninguem puder entrar depois dela.
        assertThrows(UnsupportedOperationException.class,
                () -> registro.todas().add(tecnica("zetsu")));
    }
}
