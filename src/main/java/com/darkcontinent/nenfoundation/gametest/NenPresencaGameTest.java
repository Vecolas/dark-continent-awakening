package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.api.event.OrigemDoDespertar;
import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.command.NenCommands;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import com.darkcontinent.nenfoundation.server.NenAwakeningService;
import com.darkcontinent.nenfoundation.server.NenPresencaService;
import com.darkcontinent.nenfoundation.server.NenRuntimeService;
import com.darkcontinent.nenfoundation.server.NenTechniqueService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A presenca de aura, com jogadores de verdade.
 *
 * <p>O TESTE PURO COBRE A TABELA -- quem emite o que. O que so existe aqui e o
 * servico decidindo em cima de um {@link ServerPlayer} de verdade, com perfil e
 * runtime reais, e o anuncio acontecendo uma vez por mudanca em vez de por
 * tick.
 *
 * <p>O QUE NEM AQUI SE PROVA: que o pacote chega ao cliente e vira desenho.
 * Isso precisa de dois clientes reais, e esta no roteiro do gate do M4.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenPresencaGameTest {

    private static final String TEMPLATE = "empty";

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new GameTestAssertException(mensagem);
        }
    }

    private static ServerPlayer despertoComAura(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);
        NenRuntimeService.estadoDe(jogador).definirAuraMaxima(1000.0D);
        NenRuntimeService.estadoDe(jogador).definirAuraAtual(1000.0D);
        return jogador;
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void oSinalAcompanhaAsTecnicasDeVerdade(GameTestHelper helper) {
        ServerPlayer jogador = despertoComAura(helper);

        exigir(NenPresencaService.sinalDe(jogador) == SinalDeAura.NENHUM,
                "desperto e parado ja estava anunciando presenca.");

        NenTechniqueService.ativar(jogador, Ten.ID);
        exigir(NenPresencaService.sinalDe(jogador) == SinalDeAura.TEN,
                "Ten ligado nao virou sinal TEN; veio "
                        + NenPresencaService.sinalDe(jogador));

        NenTechniqueService.ativar(jogador, Ren.ID);
        exigir(NenPresencaService.sinalDe(jogador) == SinalDeAura.REN,
                "com Ten e Ren ligados o sinal devia ser REN; veio "
                        + NenPresencaService.sinalDe(jogador));

        // Zetsu derruba os dois por exclusao, e o sinal tem de sumir junto.
        NenTechniqueService.ativar(jogador, Zetsu.ID);
        exigir(NenPresencaService.sinalDe(jogador) == SinalDeAura.NENHUM,
                "ZETSU CONTINUOU ANUNCIANDO: " + NenPresencaService.sinalDe(jogador)
                        + ". O estado que existe para sumir do radar estaria"
                        + " gritando a propria posicao.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void anunciaSoQuandoMuda(GameTestHelper helper) {
        // Enviar por tick repetiria o mesmo byte sessenta vezes por segundo
        // para cada observador. Nao daria erro nenhum -- apareceria como
        // trafego que ninguem sabe explicar, e so num servidor cheio.
        ServerPlayer jogador = despertoComAura(helper);
        NenPresencaService.limpar();

        exigir(NenPresencaService.anunciarSeMudou(jogador),
                "o primeiro anuncio nao aconteceu; um observador que entrasse"
                        + " agora nao saberia de nada.");
        exigir(!NenPresencaService.anunciarSeMudou(jogador),
                "O sinal foi reanunciado sem ter mudado. Repetido a cada tick,"
                        + " isso vira trafego constante por jogador por observador.");

        NenTechniqueService.ativar(jogador, Ren.ID);
        exigir(NenPresencaService.anunciarSeMudou(jogador),
                "ligar Ren nao gerou anuncio: os outros continuariam vendo o"
                        + " estado antigo ate alguma outra coisa mudar.");
        exigir(!NenPresencaService.anunciarSeMudou(jogador),
                "reanunciou de novo sem mudanca.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void doisJogadoresNaoSeMisturam(GameTestHelper helper) {
        // O erro numero 2 da lista do CLAUDE.md com outra roupa: estado de
        // jogador guardado no lugar errado faz dois jogadores escreverem no
        // mesmo campo. Nao da erro -- da estado trocado.
        ServerPlayer gon = despertoComAura(helper);
        ServerPlayer kurapika = despertoComAura(helper);

        NenTechniqueService.ativar(gon, Ren.ID);
        NenTechniqueService.ativar(kurapika, Zetsu.ID);

        exigir(NenPresencaService.sinalDe(gon) == SinalDeAura.REN,
                "o sinal de Gon virou " + NenPresencaService.sinalDe(gon));
        exigir(NenPresencaService.sinalDe(kurapika) == SinalDeAura.NENHUM,
                "Kurapika esta em Zetsu e anunciou "
                        + NenPresencaService.sinalDe(kurapika)
                        + ". O sinal de um vazou para o outro.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void oLogoutEsqueceOJogador(GameTestHelper helper) {
        // QUEM LIGA, DESLIGA. O mapa de "ultimo sinal anunciado" e memoria de
        // rede, e sem limpeza ele cresce por toda a vida do servidor -- um
        // vazamento lento que aparece como memoria subindo, e nunca como erro.
        ServerPlayer jogador = despertoComAura(helper);
        NenTechniqueService.ativar(jogador, Ren.ID);
        NenPresencaService.anunciarSeMudou(jogador);

        jogador.serverLevel().getServer().getPlayerList().remove(jogador);

        // Depois de esquecido, o proximo anuncio volta a contar como novidade.
        exigir(NenPresencaService.anunciarSeMudou(jogador),
                "o jogador nao foi esquecido no logout: o servico ainda acha que"
                        + " ja contou o sinal dele a alguem.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void semSessaoNaoAnunciaNemQuebra(GameTestHelper helper) {
        // Existe uma janela entre entrar na lista de jogadores e a sessao de
        // runtime comecar. Lancar ali derrubaria o laco de tick inteiro -- que e
        // exatamente o crash consertado no PR #153.
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenRuntimeService.encerrarSessao(jogador);

        exigir(NenPresencaService.sinalDe(jogador) == SinalDeAura.NENHUM,
                "jogador sem sessao anunciou " + NenPresencaService.sinalDe(jogador));

        helper.succeed();
    }

    /**
     * O DUMP MOSTRA O TETO DE OUTPUT.
     *
     * <p>Este teste existe por um buraco achado no teste manual do gate: o
     * roteiro manda conferir, depois de morrer, se o teto voltou ao de repouso
     * -- e avisa que um teto elevado sobrevivente NAO da erro nenhum. So que
     * ele mandava medir "com /nen dump ou o overlay de debug", e nenhum dos
     * dois mostrava o teto. A instrucao existia e era impossivel de seguir.
     *
     * <p>Uma regua que nao mede nada e pior que nenhuma: ela da a impressao de
     * que alguem conferiu.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void oDumpMostraOTetoDeOutput(GameTestHelper helper) {
        ServerPlayer jogador = despertoComAura(helper);
        NenTechniqueService.ativar(jogador, Ren.ID);

        String texto = String.join(" | ", NenCommands.linhasDeRuntimeParaTeste(jogador));

        exigir(texto.contains("teto="),
                "o dump nao mostra o teto de Output. Sem ele, o item mais"
                        + " perigoso do gate do M4 nao tem como ser conferido por"
                        + " ninguem: " + texto);
        exigir(texto.contains("presenca para os outros: REN"),
                "o dump nao mostra o que os outros percebem; veio: " + texto);

        helper.succeed();
    }
}
