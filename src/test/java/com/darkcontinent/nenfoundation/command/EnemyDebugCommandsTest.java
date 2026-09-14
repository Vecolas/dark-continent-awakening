package com.darkcontinent.nenfoundation.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao das ferramentas de debug de inimigo (#113).
 *
 * <p>ELE CONSTROI A ARVORE DE VERDADE e a inspeciona, pelo mesmo motivo que o
 * {@code NenCommandsTest}: um portao que procurasse {@code ".requires("} no
 * texto da fonte passaria no dia em que alguem acrescentasse uma SEGUNDA raiz
 * sem permissao -- o texto procurado continuaria la, na primeira.
 *
 * <p>O QUE ELE NAO PROVA, e que precisa de servidor de pe:
 *
 * <ul>
 *   <li>que {@code spawn} coloca o bicho onde a mira aponta -- a medida usa
 *       {@code Entity#pick}, que exige nivel carregado;</li>
 *   <li>que {@code freeze} de fato para a IA: {@code setNoAi} so tem efeito com
 *       entidade viva, e o proprio {@code CongelamentoDeIa} declara que a flag
 *       sobrevive ao reinicio enquanto o interruptor nao;</li>
 *   <li>que {@code clear} respeita o raio esferico e que {@code discard()}
 *       dispara a limpeza do {@code EnemyRuntime};</li>
 *   <li>que {@code info} encontra a entidade mirada.</li>
 * </ul>
 *
 * <p>Isso e gametest e roteiro manual; ver {@code docs/testing/o-que-nao-provamos.md}.
 */
class EnemyDebugCommandsTest {

    private static final String FONTE_COMANDO =
            "src/main/java/com/darkcontinent/nenfoundation/command/EnemyDebugCommands.java";
    private static final String FONTE_CONGELAMENTO =
            "src/main/java/com/darkcontinent/nenfoundation/command/CongelamentoDeIa.java";
    private static final String FONTE_REGISTRO =
            "src/main/java/com/darkcontinent/nenfoundation/command/NenCommands.java";
    private static final List<String> IDIOMAS = List.of(
            "src/main/resources/assets/nenfoundation/lang/en_us.json",
            "src/main/resources/assets/nenfoundation/lang/pt_br.json");

    /** Toda chave de traducao deste comando, onde quer que ela apareca. */
    private static final Pattern CHAVE =
            Pattern.compile("nenfoundation\\.enemy\\.debug\\.[a-z0-9_]+");

    private static CommandDispatcher<CommandSourceStack> arvore() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        EnemyDebugCommands.registrar(dispatcher);
        return dispatcher;
    }

    private static CommandNode<CommandSourceStack> raiz() {
        return arvore().getRoot().getChild(EnemyDebugCommands.RAIZ);
    }

    // --------------------------------------------------------------- arvore

    @Test
    @DisplayName("a arvore de inimigos e uma raiz PROPRIA, e nao um enxerto em /nen")
    void raizPropria() {
        CommandDispatcher<CommandSourceStack> dispatcher = arvore();

        assertEquals(1, dispatcher.getRoot().getChildren().size(),
                "O registro de /nenenemy criou mais de uma raiz.");
        assertEquals(EnemyDebugCommands.RAIZ,
                dispatcher.getRoot().getChildren().iterator().next().getName());

        // Se um dia isto virar um filho de /nen, a permissao passa a vir da raiz
        // do outro comando -- e mudar a permissao la mudaria esta sem que nada
        // neste arquivo acusasse.
        assertFalse(dispatcher.getRoot().getChildren().stream()
                        .anyMatch(filho -> "nen".equals(filho.getName())),
                "A arvore de inimigos foi fundida em /nen.");
    }

    @Test
    @DisplayName("a raiz exige operador, e o portao prova isso contra um controle")
    void raizExigePermissao() {
        // O MESMO CONTROLE do NenCommandsTest: um no sem .requires() fica com o
        // predicado padrao do Brigadier, que e um lambda sem captura e portanto
        // a MESMA instancia para todos os builders. "Tem o predicado do
        // controle" e exatamente "ninguem chamou .requires()".
        CommandNode<CommandSourceStack> controle = Commands.literal("controle").build();

        assertNotSame(controle.getRequirement(), raiz().getRequirement(),
                "A raiz /nenenemy esta com o predicado PADRAO: ninguem chamou"
                        + " .requires(). Spawn de mob e limpeza de entidade ficariam"
                        + " disponiveis a qualquer jogador, e limpeza de entidade nao"
                        + " tem desfazer.");

        assertEquals(2, NenCommands.NIVEL_DE_OPERADOR,
                "O nivel exigido mudou. Nivel 2 e o de operador.");
    }

    @Test
    @DisplayName("as duas arvores do pacote convivem, e nenhum executavel fica solto")
    void nenhumComandoEscapaDeUmaRaizPermissionada() {
        // As duas nascem da MESMA fila de registro; este teste reproduz o que o
        // RegisterCommandsEvent monta, para que "todo executavel esta sob raiz
        // permissionada" seja afirmado sobre a arvore inteira, e nao sobre
        // metade dela.
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        NenCommands.registrar(dispatcher);
        EnemyDebugCommands.registrar(dispatcher);

        CommandNode<CommandSourceStack> controle = Commands.literal("controle").build();
        List<String> semPermissao = new ArrayList<>();
        List<String> executaveis = new ArrayList<>();

        for (CommandNode<CommandSourceStack> raizDoMod : dispatcher.getRoot().getChildren()) {
            if (controle.getRequirement() == raizDoMod.getRequirement()) {
                semPermissao.add(raizDoMod.getName());
            }
            coletarExecutaveis(raizDoMod, raizDoMod.getName(), executaveis);
        }

        assertTrue(semPermissao.isEmpty(),
                "Raizes sem .requires(): " + semPermissao);
        assertEquals(2, dispatcher.getRoot().getChildren().size(),
                "O pacote command deixou de registrar exatamente duas raizes: "
                        + dispatcher.getRoot().getChildren().stream()
                                .map(CommandNode::getName).toList());
        assertFalse(executaveis.isEmpty(),
                "Nenhum executavel encontrado. Um laco que nao acha nada imprime"
                        + " aprovacao com zero verificacoes.");
    }

    @Test
    @DisplayName("os quatro subcomandos prometidos existem, com as variantes")
    void caminhosPrometidos() {
        List<String> caminhos = new ArrayList<>();
        coletarExecutaveis(raiz(), EnemyDebugCommands.RAIZ, caminhos);

        for (String prometido : List.of(
                "nenenemy spawn <tipo>",
                "nenenemy spawn <tipo> <quantidade>",
                "nenenemy info",
                "nenenemy freeze on",
                "nenenemy freeze off",
                "nenenemy clear",
                "nenenemy clear <raio>")) {
            assertTrue(caminhos.contains(prometido),
                    "Falta o caminho '" + prometido + "'. Caminhos: " + caminhos);
        }
    }

    @Test
    @DisplayName("freeze sao DOIS literais, e nao um booleano que completa sozinho")
    void freezeTemDoisLiterais() {
        CommandNode<CommandSourceStack> freeze = raiz().getChild("freeze");

        Set<String> sentidos = new LinkedHashSet<>();
        for (CommandNode<CommandSourceStack> filho : freeze.getChildren()) {
            assertTrue(filho instanceof LiteralCommandNode,
                    "O sentido do freeze virou argumento ('" + filho.getName() + "')."
                            + " Com um booleano, o tab-complete oferece os dois valores e"
                            + " um engano de digitacao completa para o sentido OPOSTO --"
                            + " descongelando uma arena no meio de uma medicao sem que"
                            + " nada pareca estranho.");
            sentidos.add(filho.getName());
        }
        assertEquals(Set.of("on", "off"), sentidos);

        assertTrue(freeze.getCommand() == null,
                "/nenenemy freeze ficou executavel sem sentido nenhum. Um comando"
                        + " de congelamento que nao diz o sentido teria de escolher um,"
                        + " e a escolha seria invisivel para quem digitou.");
    }

    @Test
    @DisplayName("quantidade e raio tem teto no proprio argumento, e nao num if esquecivel")
    void limitesMoramNoArgumento() {
        CommandNode<CommandSourceStack> quantidade =
                raiz().getChild("spawn").getChild("tipo").getChild("quantidade");
        IntegerArgumentType tipoDaQuantidade = (IntegerArgumentType)
                ((ArgumentCommandNode<CommandSourceStack, ?>) quantidade).getType();

        assertEquals(1, tipoDaQuantidade.getMinimum(),
                "spawn passou a aceitar quantidade zero ou negativa.");
        assertEquals(EnemyDebugCommands.LIMITE_DE_SPAWN, tipoDaQuantidade.getMaximum(),
                "O teto de spawn saiu do argumento. Sem ele, um zero a mais nao da"
                        + " erro: da duzentos mobs, TPS no chao e uma mira que nao"
                        + " consegue mais selecionar nada para remover.");

        CommandNode<CommandSourceStack> raio = raiz().getChild("clear").getChild("raio");
        DoubleArgumentType tipoDoRaio = (DoubleArgumentType)
                ((ArgumentCommandNode<CommandSourceStack, ?>) raio).getType();

        assertTrue(tipoDoRaio.getMinimum() > 0.0D,
                "clear passou a aceitar raio zero ou negativo.");
        assertEquals(EnemyDebugCommands.RAIO_MAXIMO, tipoDoRaio.getMaximum(),
                "O teto do clear saiu do argumento. clear apaga entidade sem drop e"
                        + " sem evento de morte; um raio digitado errado leva junto o"
                        + " bicho que a outra pessoa estava medindo, e entidade apagada"
                        + " nao volta.");
    }

    // ------------------------------------------------------------- recusas

    @Test
    @DisplayName("id fora do namespace do mod e recusado com motivo proprio")
    void namespaceErradoERecusado() {
        Optional<EnemyDebugCommands.Recusa> recusa = EnemyDebugCommands.validarNamespace(
                ResourceLocation.fromNamespaceAndPath("minecraft", "zombie"));

        assertEquals(Optional.of(EnemyDebugCommands.Recusa.TIPO_FORA_DO_MOD), recusa,
                "minecraft:zombie passou pela validacao de namespace.");
    }

    @Test
    @DisplayName("id no namespace do mod passa, mesmo que a fila ainda nao o conheca")
    void namespaceCertoPassa() {
        assertEquals(Optional.empty(), EnemyDebugCommands.validarNamespace(
                        ResourceLocation.fromNamespaceAndPath("nenfoundation", "dummy_enemy")),
                "Um id do proprio mod foi recusado pelo namespace. A ausencia na fila"
                        + " e OUTRA recusa, com outra mensagem, de proposito: o conserto"
                        + " de cada uma e diferente.");
    }

    @Test
    @DisplayName("toda recusa tem chave de traducao nos DOIS idiomas")
    void recusaSempreTemChaveTraduzida() {
        List<String> textos = IDIOMAS.stream().map(Repo::texto).toList();

        assertTrue(EnemyDebugCommands.Recusa.values().length > 0,
                "Nenhuma recusa declarada; a varredura seria vazia.");
        for (EnemyDebugCommands.Recusa recusa : EnemyDebugCommands.Recusa.values()) {
            String procurado = "\"" + recusa.chave() + "\"";
            for (int i = 0; i < IDIOMAS.size(); i++) {
                assertTrue(textos.get(i).contains(procurado),
                        "A recusa " + recusa + " nao tem traducao em " + IDIOMAS.get(i)
                                + ". O operador recebe a chave crua na tela, e chave crua"
                                + " parece defeito do mod em vez de recusa com motivo.");
            }
        }
    }

    @Test
    @DisplayName("o portao das chaves morde dos DOIS lados: nenhuma falta, nenhuma sobra")
    void chavesDeDebugBatemComOsIdiomas() {
        Set<String> naFonte = chavesEm(Repo.texto(FONTE_COMANDO));
        naFonte.addAll(chavesEm(Repo.texto(FONTE_CONGELAMENTO)));

        assertFalse(naFonte.isEmpty(),
                "Nenhuma chave nenfoundation.enemy.debug.* encontrada na fonte."
                        + " Ou o comando parou de falar com o jogador, ou este padrao"
                        + " deixou de casar -- e ai o portao aprova sem olhar nada.");

        for (String idioma : IDIOMAS) {
            Set<String> noIdioma = chavesEm(Repo.texto(idioma));

            Set<String> faltando = new LinkedHashSet<>(naFonte);
            faltando.removeAll(noIdioma);
            assertTrue(faltando.isEmpty(),
                    "Chaves usadas no codigo e ausentes de " + idioma + ": " + faltando
                            + ". Elas aparecem cruas na tela, e so aparecem na hora exata"
                            + " em que alguem esta diagnosticando outra coisa.");

            Set<String> orfas = new LinkedHashSet<>(noIdioma);
            orfas.removeAll(naFonte);
            assertTrue(orfas.isEmpty(),
                    "Chaves em " + idioma + " que ninguem mais usa: " + orfas
                            + ". Traducao orfa nao da erro: ela so faz a proxima pessoa"
                            + " acreditar que aquela mensagem existe em algum lugar.");
        }
    }

    // ---------------------------------------------------- o par liga/desliga

    @Test
    @DisplayName("o congelamento nasce desligado e o par liga/desliga e simetrico")
    void oParLigaDesliga() {
        // O estado e estatico de processo. O teste o devolve ao valor de boot no
        // finally: deixar ligado contaminaria qualquer teste futuro que o leia,
        // e contaminacao de estado estatico nao da erro -- da um teste que falha
        // so quando roda depois de outro.
        try {
            assertFalse(CongelamentoDeIa.ligado(),
                    "O congelamento nasce LIGADO. Um servidor que sobe congelado nao"
                            + " avisa ninguem: os inimigos so nao reagem.");

            assertTrue(CongelamentoDeIa.definir(true), "definir(true) nao ligou.");
            assertTrue(CongelamentoDeIa.ligado(), "o interruptor nao refletiu o ligar.");

            assertTrue(CongelamentoDeIa.definir(true),
                    "Ligar duas vezes mudou o estado. O comando e idempotente de"
                            + " proposito: quem nao sabe se congelou pode repetir.");

            assertFalse(CongelamentoDeIa.definir(false), "definir(false) nao desligou.");
            assertFalse(CongelamentoDeIa.ligado(), "o interruptor nao refletiu o desligar.");

            assertFalse(CongelamentoDeIa.definir(false),
                    "Desligar duas vezes mudou o estado. Desligar de novo E a"
                            + " recuperacao depois de um reinicio congelado; ela precisa"
                            + " funcionar exatamente quando o interruptor ja diz off.");
        } finally {
            CongelamentoDeIa.definir(false);
        }
    }

    // --------------------------------------------------------- uma fila so

    @Test
    @DisplayName("a arvore nasce da fila de registro que ja existia, e nao de uma segunda")
    void umaFilaDeRegistroSo() {
        String registro = Repo.texto(FONTE_REGISTRO);
        assertTrue(registro.contains("EnemyDebugCommands.registrar("),
                "NenCommands parou de registrar a arvore de inimigos. Se ela nao"
                        + " nasce de la, ou ela nao existe em jogo, ou nasceu de um"
                        + " segundo lugar -- e os dois casos passam no compilador.");

        // A varredura olha ANOTACAO, e nao texto solto: a classe EXPLICA no
        // javadoc por que nao tem um subscriber proprio, e um contains() cru
        // reprovaria exatamente a explicacao que ela deve carregar. Portao que
        // proibe falar sobre o que ele procura vira portao que ensina a nao
        // documentar.
        List<String> anotacoes = new ArrayList<>();
        for (String linha : Repo.linhas(FONTE_COMANDO)) {
            String limpa = linha.strip();
            // Comeca com '@' -- o que descarta javadoc (que comeca com '*') --
            // e CONTEM o nome: a forma qualificada inteira
            // (@net.neoforged.bus.api.SubscribeEvent) tambem registra, e uma
            // varredura por startsWith passaria batido por ela. Este caso foi
            // exercitado: com startsWith, o portao aprovou um subscriber de
            // verdade.
            if (limpa.startsWith("@")
                    && (limpa.contains("SubscribeEvent") || limpa.contains("EventBusSubscriber"))) {
                anotacoes.add(limpa);
            }
        }
        assertTrue(anotacoes.isEmpty(),
                "EnemyDebugCommands ganhou um registro proprio de evento " + anotacoes
                        + ". Dois pontos de registro FUNCIONAM -- e e isso que os torna"
                        + " perigosos: cada um pode aplicar uma permissao diferente na"
                        + " raiz, e nada acusa a divergencia.");
    }

    // ------------------------------------------------------------- apoio

    private static Set<String> chavesEm(String texto) {
        Set<String> achadas = new LinkedHashSet<>();
        Matcher m = CHAVE.matcher(texto);
        while (m.find()) {
            achadas.add(m.group());
        }
        return achadas;
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
}
