package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.category.SorteioDeCategoria;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.progression.Marcos;
import com.darkcontinent.nenfoundation.server.NenProfileService;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Os comandos de seed, executados como texto pelo dispatcher de verdade.
 *
 * <p>POR QUE PASSAR PELO TEXTO, e nao chamar os metodos: o que este teste
 * precisa provar e que a ARVORE liga o comando digitado ao servico certo.
 * Chamar {@code definirCategoria(...)} direto provaria que o metodo funciona e
 * nao diria nada sobre o caminho -- e o defeito tipico aqui e de ligacao: um
 * {@code .then} pendurado no no errado, um argumento com nome trocado. O
 * portao de arvore pega a FORMA; so a execucao pega o comportamento.
 *
 * <p>TODOS OS COMANDOS AQUI RODAM SEM ALVO EXPLICITO, e isso e uma limitacao
 * do ambiente, nao uma escolha de desenho: {@code makeMockServerPlayerInLevel}
 * cria jogadores que COMPARTILHAM o mesmo nome e nao entram na lista do
 * servidor, entao {@code EntityArgument} resolve sempre o primeiro deles. Na
 * primeira versao deste arquivo isso passou despercebido e quatro testes
 * falharam medindo o perfil do jogador errado. Rodando sem alvo, a fonte do
 * comando E o jogador, e cada teste mede quem pretendia medir.
 *
 * <p>As variantes {@code &lt;alvo&gt;} ficam cobertas pelo portao de arvore, que
 * prova que elas existem. O comportamento DELAS nao e exercitado aqui --
 * ponto cego declarado.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenComandoDeSeedGameTest {

    private static final String TEMPLATE = "empty";

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new GameTestAssertException(mensagem);
        }
    }

    /**
     * A fonte de um operador. {@code withPermission(2)} e deliberado: sem isso
     * o teste passaria a medir tambem a permissao, e uma falha de ligacao viria
     * disfarcada de "sem permissao".
     */
    private static CommandSourceStack comoOperador(ServerPlayer jogador) {
        return jogador.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
    }

    /**
     * Roda um comando que DEVE funcionar, e devolve o codigo.
     *
     * <p>POR QUE ERRO DE SINTAXE VIRA FALHA DE TESTE, e nao "recusa": este
     * arquivo escreve os comandos como TEXTO, entao um erro de digitacao no
     * proprio teste e possivel. Se o helper engolisse
     * {@code CommandSyntaxException} como "recusou", cada teste negativo
     * passaria pelo motivo errado -- inclusive contra um comando que nem
     * existe. O verde diria "recusou corretamente" sobre uma arvore vazia.
     *
     * <p>{@code performPrefixedCommand} devolve {@code void} no 1.21.1, entao
     * o codigo de saida so existe indo ao dispatcher.
     */
    private static int rodar(GameTestHelper helper, ServerPlayer jogador, String comando) {
        try {
            return helper.getLevel().getServer().getCommands().getDispatcher()
                    .execute(comando, comoOperador(jogador));
        } catch (CommandSyntaxException e) {
            throw new GameTestAssertException(
                    "O parser recusou '" + comando + "': " + e.getMessage()
                            + " -- isto e erro do TESTE ou da arvore, e nao a"
                            + " recusa que se queria medir.");
        }
    }

    /**
     * Roda um comando que DEVE ser recusado. Devolve o motivo, ou {@code null}
     * se ele foi aceito.
     *
     * <p>Aceita as duas formas de recusa, porque as duas sao legitimas e o
     * jogador nao distingue: o argumento recusado no parser
     * ({@code CommandSyntaxException}) e a acao recusada pelo servico
     * ({@code return 0} depois de {@code sendFailure}).
     */
    private static String recusa(GameTestHelper helper, ServerPlayer jogador, String comando) {
        try {
            int codigo = helper.getLevel().getServer().getCommands().getDispatcher()
                    .execute(comando, comoOperador(jogador));
            return codigo == 0 ? "o comando devolveu 0" : null;
        } catch (CommandSyntaxException e) {
            return e.getMessage();
        }
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void oCaminhoInteiroPorComando(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();

        exigir(rodar(helper, jogador, "nen awaken") > 0,
                "/nen awaken falhou.");
        PersistentNenData desperto = NenProfileService.ler(jogador);
        exigir(desperto.awakened(), "awaken nao despertou o jogador.");
        exigir(desperto.temMarco(Marcos.DESPERTOU), "awaken nao pos o marco.");

        exigir(rodar(helper, jogador,
                        "nen category set conjuration") > 0,
                "/nen category set falhou.");
        PersistentNenData atribuido = NenProfileService.ler(jogador);
        exigir(atribuido.category() == NenCategory.CONJURATION,
                "set nao gravou a categoria; veio " + atribuido.category());

        // O ITEM DO CRITERIO DE ACEITE: set NAO revela.
        exigir(!atribuido.categoryRevealed(),
                "/nen category set revelou junto. O comando existe justamente"
                        + " para montar o estado do meio -- tem categoria, nao sabe"
                        + " qual e -- que nenhum ritual deixa montar a mao.");
        exigir(atribuido.categoriaVisivel() == NenCategory.UNDETERMINED,
                "a projecao ja entregava a categoria depois do set.");

        exigir(rodar(helper, jogador, "nen category reveal") > 0,
                "/nen category reveal falhou.");
        PersistentNenData revelado = NenProfileService.ler(jogador);
        exigir(revelado.categoryRevealed(), "reveal nao ligou category_revealed.");
        exigir(revelado.temMarco(Marcos.CATEGORIA_REVELADA), "reveal nao pos o marco.");
        exigir(revelado.category() == NenCategory.CONJURATION,
                "reveal trocou a categoria; veio " + revelado.category());

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void rollReproduzOSorteioDaSemente(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        rodar(helper, jogador, "nen awaken");

        long semente = 12345L;
        NenCategory esperada = SorteioDeCategoria.sortear(semente);

        exigir(rodar(helper, jogador,
                        "nen category roll " + semente) > 0,
                "/nen category roll falhou.");

        exigir(NenProfileService.ler(jogador).category() == esperada,
                "A semente " + semente + " devia dar " + esperada + " e deu "
                        + NenProfileService.ler(jogador).category()
                        + ". Se o comando calcular o sorteio por um caminho e"
                        + " atribuir por outro, os dois divergem no primeiro"
                        + " ajuste -- e o QA passa a reproduzir outra coisa.");
        exigir(!NenProfileService.ler(jogador).categoryRevealed(),
                "roll revelou junto.");

        helper.succeed();
    }

    /** O criterio de aceite: da para chegar nas SEIS por comando. */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void daParaChegarNasSeisPorComando(GameTestHelper helper) {
        Set<NenCategory> alcancadas = EnumSet.noneOf(NenCategory.class);

        for (NenCategory categoria : NenCategory.REAIS) {
            ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
            rodar(helper, jogador, "nen awaken");
            int codigo = rodar(helper, jogador,
                    "nen category set " + categoria.getSerializedName());
            exigir(codigo > 0, "o comando falhou para " + categoria.getSerializedName());
            alcancadas.add(NenProfileService.ler(jogador).category());
        }

        exigir(alcancadas.equals(EnumSet.copyOf(NenCategory.REAIS)),
                "Nao deu para chegar nas seis por comando. Alcancadas: "
                        + alcancadas + ". Uma categoria inalcancavel nao da erro:"
                        + " ela so nunca aparece no QA.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void comandoRecusaComMotivoEmVezDeInventarEstado(GameTestHelper helper) {
        ServerPlayer semNen = helper.makeMockServerPlayerInLevel();

        // Sem despertar, set tem de recusar -- e nao gravar categoria escondida.
        exigir(recusa(helper, semNen, "nen category set emission") != null,
                "set aceitou um jogador que nao despertou.");
        exigir(NenProfileService.ler(semNen).category() == NenCategory.UNDETERMINED,
                "set gravou categoria para quem nao despertou.");

        // Sem categoria, reveal tem de recusar -- e NAO sortear uma.
        rodar(helper, semNen, "nen awaken");
        exigir(recusa(helper, semNen, "nen category reveal") != null,
                "reveal aceitou um jogador sem categoria.");
        exigir(NenProfileService.ler(semNen).category() == NenCategory.UNDETERMINED,
                "O reveal INVENTOU uma categoria. Consertar 'quando falta'"
                        + " esconderia para sempre quem chamou fora de ordem.");
        exigir(!NenProfileService.ler(semNen).categoryRevealed(),
                "reveal ligou o booleano sem haver categoria.");

        // Com categoria, set de novo tem de recusar -- e nao TROCAR.
        rodar(helper, semNen, "nen category set emission");
        exigir(recusa(helper, semNen, "nen category set manipulation") != null,
                "set aceitou trocar a categoria de quem ja tinha.");
        exigir(NenProfileService.ler(semNen).category() == NenCategory.EMISSION,
                "a categoria foi TROCADA; veio "
                        + NenProfileService.ler(semNen).category());

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void undeterminedNaoEAtribuivelPorComando(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        rodar(helper, jogador, "nen awaken");

        String motivo = recusa(helper, jogador,
                "nen category set undetermined");
        exigir(motivo != null,
                "O comando aceitou 'undetermined'. Ele e a AUSENCIA de categoria;"
                        + " atribui-lo seria apagar a categoria de alguem por uma"
                        + " porta que ninguem desenhou para isso.");
        exigir(motivo.contains("AUSENCIA"),
                "A recusa nao explica POR QUE: veio \"" + motivo + "\". O neutro EXISTE"
                        + " no enum, e a mensagem generica manda quem o digitou"
                        + " procurar um erro de digitacao que nao ha.");
        exigir(NenProfileService.ler(jogador).category() == NenCategory.UNDETERMINED,
                "estado inesperado depois da recusa.");

        helper.succeed();
    }

    /** Depois de resetar, da para montar um cenario novo. O caminho do QA. */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void resetLiberaUmCenarioNovo(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();

        rodar(helper, jogador, "nen awaken");
        rodar(helper, jogador, "nen category set emission");
        rodar(helper, jogador, "nen category reveal");

        exigir(rodar(helper, jogador, "nen reset confirmar") > 0,
                "/nen reset confirmar falhou.");
        exigir(NenProfileService.ler(jogador).category() == NenCategory.UNDETERMINED,
                "o reset nao limpou a categoria.");

        rodar(helper, jogador, "nen awaken");
        exigir(rodar(helper, jogador,
                        "nen category set specialization") > 0,
                "Depois do reset nao deu para atribuir de novo. Sem este caminho,"
                        + " a recusa de trocar categoria vira um beco sem saida"
                        + " para quem esta reproduzindo um bug.");
        exigir(NenProfileService.ler(jogador).category() == NenCategory.SPECIALIZATION,
                "a categoria nova nao entrou depois do reset.");

        helper.succeed();
    }
}
