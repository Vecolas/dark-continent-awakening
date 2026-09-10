package com.darkcontinent.nenfoundation.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da arvore de comandos.
 *
 * <p>POR QUE ELE NAO PROCURA TEXTO NO FONTE: a versao anterior deste teste
 * conferia a permissao com {@code fonte.contains(".requires(...)")}. Isso
 * quebra num rename inofensivo e, muito pior, **passa** quando alguem
 * acrescenta um segundo registro de {@code /nen} sem permissao — porque o
 * texto que ele procura continua la, no primeiro.
 *
 * <p>Este constroi a arvore DE VERDADE e a inspeciona.
 */
class NenCommandsTest {

    private static CommandDispatcher<CommandSourceStack> arvore() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        NenCommands.registrar(dispatcher);
        return dispatcher;
    }

    @Test
    @DisplayName("o mod registra UMA raiz so, chamada nen")
    void umaRaizSo() {
        CommandDispatcher<CommandSourceStack> dispatcher = arvore();

        assertEquals(1, dispatcher.getRoot().getChildren().size(),
                "O mod registrou mais de uma raiz. Brigadier funde nos de mesmo"
                        + " nome, entao duas raizes /nen funcionariam -- com"
                        + " permissoes possivelmente diferentes, e nada acusaria.");
        assertEquals("nen", dispatcher.getRoot().getChildren().iterator().next().getName());
    }

    @Test
    @DisplayName("a raiz exige permissao, e o portao prova isso contra um controle")
    void raizExigePermissao() {
        LiteralCommandNode<CommandSourceStack> nen =
                (LiteralCommandNode<CommandSourceStack>) arvore().getRoot().getChild("nen");

        // O CONTROLE: um no construido sem .requires() fica com o predicado
        // padrao do Brigadier, que e um lambda sem captura -- e a JVM reusa a
        // MESMA instancia para todos os builders. Entao "tem o mesmo predicado
        // do controle" e exatamente "ninguem chamou .requires()".
        //
        // Sem esse controle, o teste teria de inventar a propria escala.
        CommandNode<CommandSourceStack> controle = Commands.literal("controle").build();

        assertNotSame(controle.getRequirement(), nen.getRequirement(),
                "A raiz /nen esta com o predicado PADRAO: ninguem chamou"
                        + " .requires(). Todo comando do mod ficaria disponivel a"
                        + " qualquer jogador -- e /nen technique unlock daria a"
                        + " qualquer um o que a progressao existe para controlar.");

        assertEquals(2, NenCommands.NIVEL_DE_OPERADOR,
                "O nivel exigido mudou. Nivel 2 e o de operador.");
    }

    @Test
    @DisplayName("o controle do teste anterior de fato compartilha o predicado padrao")
    void oControleEValido() {
        // Se esta premissa deixar de valer numa versao futura do Brigadier, o
        // teste de cima passa a aprovar qualquer coisa em silencio. Entao ela
        // e verificada, e nao suposta.
        CommandNode<CommandSourceStack> a = Commands.literal("a").build();
        CommandNode<CommandSourceStack> b = Commands.literal("b").build();
        assertSame(a.getRequirement(), b.getRequirement(),
                "Dois nos sem .requires() deixaram de compartilhar o predicado"
                        + " padrao. O controle de raizExigePermissao nao vale mais,"
                        + " e aquele teste precisa de outro criterio.");
    }

    @Test
    @DisplayName("todo comando executavel esta sob a raiz permissionada")
    void nenhumComandoEscapaDaRaiz() {
        CommandDispatcher<CommandSourceStack> dispatcher = arvore();

        List<String> foraDaRaiz = new ArrayList<>();
        for (CommandNode<CommandSourceStack> filho : dispatcher.getRoot().getChildren()) {
            if (!"nen".equals(filho.getName())) {
                coletarExecutaveis(filho, filho.getName(), foraDaRaiz);
            }
        }
        assertTrue(foraDaRaiz.isEmpty(),
                "Comandos executaveis fora da raiz permissionada: " + foraDaRaiz);

        List<String> dentro = new ArrayList<>();
        coletarExecutaveis(dispatcher.getRoot().getChild("nen"), "nen", dentro);
        assertFalse(dentro.isEmpty(),
                "Nenhum comando executavel encontrado. Um laco que nao acha nada"
                        + " imprime aprovacao com zero verificacoes.");
    }

    @Test
    @DisplayName("os caminhos prometidos existem, e reset exige a palavra confirmar")
    void caminhosPrometidos() {
        List<String> caminhos = new ArrayList<>();
        coletarExecutaveis(arvore().getRoot().getChild("nen"), "nen", caminhos);

        assertTrue(caminhos.contains("nen debug profile"), caminhos.toString());
        assertTrue(caminhos.contains("nen debug dump"), caminhos.toString());
        assertTrue(caminhos.contains("nen technique unlock <tecnica>"), caminhos.toString());
        assertTrue(caminhos.contains("nen technique lock <tecnica>"), caminhos.toString());

        assertTrue(caminhos.contains("nen reset confirmar"), caminhos.toString());
        assertFalse(caminhos.contains("nen reset"),
                "/nen reset ficou executavel sem a palavra 'confirmar'. Apagar o"
                        + " progresso de alguem nao tem desfazer, e um literal"
                        + " obrigatorio e o que impede um erro de digitacao de"
                        + " completar para algo destrutivo.");
    }

    @Test
    @DisplayName("toda acao destrutiva ou mutante aceita um alvo explicito")
    void mutacoesAceitamAlvo() {
        List<String> caminhos = new ArrayList<>();
        coletarExecutaveis(arvore().getRoot().getChild("nen"), "nen", caminhos);

        for (String base : List.of("nen technique unlock <tecnica>",
                                   "nen technique lock <tecnica>",
                                   "nen reset confirmar")) {
            assertTrue(caminhos.contains(base + " <alvo>"),
                    "Falta a variante com alvo explicito de '" + base
                            + "'. Sem ela, diagnosticar o perfil de outra pessoa"
                            + " exige entrar na conta dela.");
        }
    }

    /** Varre a arvore e coleta o caminho de todo no que executa alguma coisa. */
    private static void coletarExecutaveis(
            CommandNode<CommandSourceStack> no, String caminho, List<String> destino) {
        if (no.getCommand() != null) {
            destino.add(caminho);
        }
        for (CommandNode<CommandSourceStack> filho : no.getChildren()) {
            String nome = filho instanceof LiteralCommandNode
                    ? filho.getName()
                    : "<" + filho.getName() + ">";
            coletarExecutaveis(filho, caminho + " " + nome, destino);
        }
    }

    @Test
    @DisplayName("id de tecnica fora do namespace do mod e recusado")
    void namespaceErradoERecusado() {
        ResourceLocation forasteiro = ResourceLocation.fromNamespaceAndPath("minecraft", "ten");

        CommandSyntaxException e = org.junit.jupiter.api.Assertions.assertThrows(
                CommandSyntaxException.class,
                () -> NenCommands.validarTecnica(forasteiro));
        assertTrue(e.getMessage().contains("minecraft:ten"),
                "A mensagem precisa nomear o id recebido; sem isso o operador nao"
                        + " sabe o que digitou errado.");
    }

    @Test
    @DisplayName("id no namespace do mod passa, e volta igual")
    void namespaceCertoPassa() throws CommandSyntaxException {
        ResourceLocation ten = ResourceLocation.fromNamespaceAndPath("nenfoundation", "ten");
        assertSame(ten, NenCommands.validarTecnica(ten));
    }
}
