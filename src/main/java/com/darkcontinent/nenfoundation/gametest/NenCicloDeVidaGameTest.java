package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.api.event.OrigemDoDespertar;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import com.darkcontinent.nenfoundation.server.NenAwakeningService;
import com.darkcontinent.nenfoundation.server.NenRuntimeService;
import com.darkcontinent.nenfoundation.server.NenTechniqueService;
import com.darkcontinent.nenfoundation.nen.technique.NenContext;
import com.darkcontinent.nenfoundation.nen.technique.NenTechnique;
import com.darkcontinent.nenfoundation.nen.technique.RegistroDeTecnicas;
import com.darkcontinent.nenfoundation.nen.technique.StopReason;
import com.darkcontinent.nenfoundation.nen.technique.TechniqueActivationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Os PONTOS DE SAIDA, exercitados pelo evento de verdade.
 *
 * <p>POR QUE ESTE ARQUIVO EXISTE. Os gametests de tecnica cobrem o ciclo de
 * vida chamando {@code NenTechniqueService.desligarTodas} <b>direto</b>. Isso
 * prova que o servico desliga; nao prova que ALGUEM o chama quando o jogador
 * morre, desloga ou troca de dimensao. O elo que faltava era justamente o do
 * meio: {@link com.darkcontinent.nenfoundation.server.NenPlayerLifecycle}
 * estar inscrito no barramento e reagir.
 *
 * <p>E o erro numero 3 da lista do CLAUDE.md -- "limpeza espalhada pelos
 * pontos de saida; um deles vai faltar, e o buff fica ligado para sempre".
 * Ele foi escrito como previsivel, e ate aqui nenhum teste o pegaria.
 *
 * <p>POR ISSO OS TESTES POSTAM O EVENTO NO BARRAMENTO, em vez de chamar o
 * handler. Chamar {@code NenPlayerLifecycle.aoSair(evento)} testaria o corpo
 * do metodo e nao a inscricao: alguem poderia apagar a anotacao
 * {@code @SubscribeEvent} e o teste continuaria verde, com o jogo quebrado.
 *
 * <p>O QUE ELES NAO PROVAM: que o Minecraft dispara esses eventos nos momentos
 * certos. Isso e contrato do NeoForge, e testa-lo aqui seria testar a
 * plataforma. O que se prova e que, disparado o evento, o mod limpa tudo.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenCicloDeVidaGameTest {

    private static final String TEMPLATE = "empty";

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new GameTestAssertException(mensagem);
        }
    }

    /**
     * Um jogador desperto, com Ren ligado e o teto ja levantado.
     *
     * <p>REN DE PROPOSITO, e nao Ten: Ren e a unica das tres que levanta o teto
     * de Output, e teto elevado e exatamente o estado derivado que sobrevive em
     * silencio quando a limpeza falha. Com Ten, o teste passaria mesmo que o
     * teto ficasse preso.
     */
    private static ServerPlayer comRenLigado(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);
        NenRuntimeService.estadoDe(jogador).definirAuraMaxima(1000.0D);
        NenRuntimeService.estadoDe(jogador).definirAuraAtual(1000.0D);
        NenRuntimeService.estadoDe(jogador).definirOutputSelecionado(1.0F);

        NenTechniqueService.ativar(jogador, Ren.ID);
        exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().contains(Ren.ID),
                "Ren nao ligou; o cenario deste teste nao existe.");
        exigir(NenRuntimeService.estadoDe(jogador).outputMaximo() > tetoDeRepouso(helper),
                "o teto nao subiu com Ren ativo, entao nao ha estado derivado"
                        + " para sobreviver -- o teste mediria o vazio.");
        return jogador;
    }

    /**
     * O teto de quem nao tem nada ligado.
     *
     * <p>Vem de um jogador NOVO, e nao de uma constante: assim o valor sai do
     * mesmo caminho que o jogo usa no inicio de sessao. Uma constante escrita
     * aqui viraria a segunda fonte da mesma verdade, e passaria a concordar com
     * o codigo so por coincidencia.
     */
    private static float tetoDeRepouso(GameTestHelper helper) {
        return NenRuntimeService.estadoDe(helper.makeMockServerPlayerInLevel())
                .outputMaximo();
    }


    /**
     * Uma tecnica que anota como foi desligada.
     *
     * <p>ELA E A DIFERENCA ENTRE ESTE ARQUIVO MORDER E NAO MORDER. A primeira
     * versao do teste de logout so conferia que a sessao de runtime tinha
     * acabado -- e isso quem faz e {@code encerrarSessao}, que e uma chamada
     * SEPARADA no mesmo handler. Apagar o desligamento das tecnicas deixava o
     * teste verde.
     *
     * <p>Descoberto alimentando o portao com o defeito. Sem essa checagem, uma
     * tecnica que segura estado externo -- projetil, buff, construct -- nunca
     * receberia o aviso de que acabou, e o teste juraria que tudo foi limpo.
     */
    private static final class Espia implements NenTechnique {
        private final ResourceLocation id = NenFoundation.id("teste_espia_ciclo");
        final List<StopReason> motivos = new ArrayList<>();

        @Override public ResourceLocation id() { return this.id; }
        @Override public Set<ResourceLocation> incompativeisCom() { return Set.of(); }

        @Override
        public TechniqueActivationResult canActivate(ServerPlayer j, NenContext c) {
            return TechniqueActivationResult.aceito();
        }

        @Override public void onActivate(ServerPlayer j, NenContext c) { }
        @Override public void serverTick(ServerPlayer j, NenContext c) { }

        @Override
        public void onDeactivate(ServerPlayer j, NenContext c, StopReason motivo) {
            this.motivos.add(motivo);
        }
    }

    /** Instala um registro de teste e GARANTE que o original volta. */
    private static void comEspia(Espia espia, Runnable corpo) {
        RegistroDeTecnicas original = NenTechniqueService.registro();
        NenTechniqueService.instalar(RegistroDeTecnicas.selar(List.of(espia)));
        try {
            corpo.run();
        } finally {
            NenTechniqueService.instalar(original);
        }
    }

    // ----------------------------------------------------------- logout

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void logoutDesligaTudoPeloEventoDeVerdade(GameTestHelper helper) {
        Espia espia = new Espia();

        comEspia(espia, () -> {
            ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
            NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);
            NenRuntimeService.estadoDe(jogador).definirAuraMaxima(1000.0D);
            NenRuntimeService.estadoDe(jogador).definirAuraAtual(1000.0D);
            NenTechniqueService.ativar(jogador, espia.id());
            exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().contains(espia.id()),
                    "a espia nao ligou; o cenario deste teste nao existe.");

            // PELO CAMINHO DE VERDADE, e nao postando o evento a mao:
            // `PlayerList.remove` dispara o logout E tira o jogador da lista.
            // Postar so o evento encerrava a sessao e DEIXAVA o jogador
            // tickando -- foi assim que este teste derrubou o servidor na
            // primeira tentativa. (O tick tambem ficou mais duro por causa
            // disso; ver NenTickScheduler.)
            jogador.serverLevel().getServer().getPlayerList().remove(jogador);

            exigir(espia.motivos.equals(List.of(StopReason.LOGOUT)),
                    "A tecnica nao foi avisada do logout; motivos: " + espia.motivos
                            + ". Encerrar a sessao apaga o registro do estado, mas"
                            + " nao avisa quem tinha algo ligado -- projetil, buff"
                            + " ou construct ficariam orfaos no mundo.");

            boolean sessaoEncerrada;
            try {
                NenRuntimeService.estadoDe(jogador);
                sessaoEncerrada = false;
            } catch (IllegalStateException semSessao) {
                sessaoEncerrada = true;
            }
            exigir(sessaoEncerrada,
                    "A sessao de runtime sobreviveu ao logout. Aura e tecnicas"
                            + " ativas ficariam na memoria do servidor para quem"
                            + " saiu, contra o ADR-002.");
        });

        helper.succeed();
    }

    // ------------------------------------------------------------ morte

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void morteDesligaTudoEDevolveOTeto(GameTestHelper helper) {
        ServerPlayer jogador = comRenLigado(helper);
        float tetoComRen = NenRuntimeService.estadoDe(jogador).outputMaximo();
        var antes = NenRuntimeService.estadoDe(jogador);

        // `Clone` com wasDeath=true e o evento que o NeoForge dispara no
        // respawn. O mesmo jogador dos dois lados e proposital: o runtime e
        // indexado por UUID, e a morte nao troca o UUID -- e por isso que o
        // handler nunca precisa da entidade antiga.
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(jogador, jogador, true));

        exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().isEmpty(),
                "Sobrou tecnica ativa depois da morte: "
                        + NenRuntimeService.estadoDe(jogador).tecnicasAtivas());

        // O RUNTIME TEM DE SER OUTRO, e nao o mesmo objeto limpo. Desligar as
        // tecnicas ja derrubaria o teto sozinho, entao so olhar o teto nao
        // distingue "reiniciou" de "desligou" -- e a mutacao que apaga o
        // `reiniciar` passaria batida. O que so o reinicio entrega e uma
        // sessao NOVA: aura, cooldowns e tudo o mais zerados de uma vez.
        exigir(NenRuntimeService.estadoDe(jogador) != antes,
                "A morte reaproveitou o MESMO RuntimeNenState. Qualquer coisa que"
                        + " o desligamento de tecnica nao limpe -- cooldown, aura,"
                        + " campo futuro -- atravessa a morte junto com ele.");

        float tetoDepois = NenRuntimeService.estadoDe(jogador).outputMaximo();
        exigir(tetoDepois < tetoComRen,
                "O TETO DE REN SOBREVIVEU A MORTE: " + tetoDepois + ", igual ao de"
                        + " quem esta em Ren (" + tetoComRen + "). Isto nao da erro"
                        + " nenhum -- o jogador so passa a liberar aura acima do"
                        + " limite dele para sempre, sem nada acusar.");

        helper.succeed();
    }

    // ------------------------------------------------------- dimensao

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void trocarDeDimensaoDesligaTudoEDevolveOTeto(GameTestHelper helper) {
        ServerPlayer jogador = comRenLigado(helper);
        float tetoComRen = NenRuntimeService.estadoDe(jogador).outputMaximo();
        var antes = NenRuntimeService.estadoDe(jogador);

        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(
                jogador, Level.OVERWORLD, Level.NETHER));

        exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().isEmpty(),
                "Sobrou tecnica ativa depois da troca de dimensao: "
                        + NenRuntimeService.estadoDe(jogador).tecnicasAtivas());
        exigir(NenRuntimeService.estadoDe(jogador).outputMaximo() < tetoComRen,
                "o teto de Ren atravessou o portal junto com o jogador: "
                        + NenRuntimeService.estadoDe(jogador).outputMaximo());
        exigir(NenRuntimeService.estadoDe(jogador) != antes,
                "a troca de dimensao nao reiniciou o runtime; ver o mesmo motivo"
                        + " no teste de morte.");

        helper.succeed();
    }

    // ------------------------------------------- o multiplicador tambem

    /**
     * O outro estado derivado, e ele e mais facil de esquecer que o teto.
     *
     * <p>Ten e Zetsu multiplicam a REGENERACAO. Um multiplicador preso depois
     * da morte nao muda nada na tela: a aura so volta mais rapido do que
     * deveria, para sempre, e ninguem liga uma coisa na outra.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void oMultiplicadorNaoAtravessaAMorte(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);
        NenRuntimeService.estadoDe(jogador).definirAuraMaxima(1000.0D);
        NenRuntimeService.estadoDe(jogador).definirAuraAtual(1000.0D);

        NenTechniqueService.ativar(jogador, Zetsu.ID);
        double comZetsu = NenRuntimeService.estadoDe(jogador).multiplicadorDeRegeneracao();
        exigir(comZetsu != 1.0D,
                "Zetsu nao mexeu no multiplicador; nao ha o que sobreviver.");

        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(jogador, jogador, true));

        exigir(NenRuntimeService.estadoDe(jogador).multiplicadorDeRegeneracao() == 1.0D,
                "O multiplicador de Zetsu (" + comZetsu + ") atravessou a morte e"
                        + " ficou em "
                        + NenRuntimeService.estadoDe(jogador).multiplicadorDeRegeneracao()
                        + ". A aura passaria a voltar mais rapido para sempre, sem"
                        + " nenhum sintoma visivel.");

        helper.succeed();
    }

    // ------------------------------------------------- varias de uma vez

    /**
     * Nenhuma tecnica fica para tras quando ha mais de uma ligada.
     *
     * <p>Uma limpeza que itera sobre a colecao VIVA enquanto a modifica derruba
     * a primeira e pula a segunda, e o sintoma seria "as vezes uma tecnica fica
     * ligada depois de morrer" -- o relato de bug que ninguem consegue
     * reproduzir.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void duasTecnicasCaemJuntasNaMorte(GameTestHelper helper) {
        ServerPlayer jogador = comRenLigado(helper);
        NenTechniqueService.ativar(jogador, Ten.ID);
        exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().size() == 2,
                "Ten e Ren deviam conviver; ativas: "
                        + NenRuntimeService.estadoDe(jogador).tecnicasAtivas());

        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(jogador, jogador, true));

        exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().isEmpty(),
                "Sobrou tecnica depois da morte com duas ligadas: "
                        + NenRuntimeService.estadoDe(jogador).tecnicasAtivas()
                        + ". Uma limpeza que itera a colecao viva enquanto a"
                        + " modifica derruba uma e pula a outra.");

        helper.succeed();
    }

    // ------------------------------------------- o tick sobrevive ao quebrado

    /**
     * UM JOGADOR SEM SESSAO NAO PODE MATAR O SERVIDOR.
     *
     * <p>Este teste existe porque o defeito aconteceu: a primeira versao do
     * teste de logout deixou um jogador na lista sem runtime, e o servidor de
     * gametest morreu com "Exception in server tick loop". Nao foi uma funcao
     * de Nen que parou -- foi o mundo inteiro, para todos os jogadores.
     *
     * <p>O ERRO CONTINUA ALTO, no log, com o nome de quem falhou. O que mudou e
     * que ele deixou de ser fatal. Engolir em silencio esconderia um erro de
     * integracao real; deixar subir troca um jogador quebrado por um servidor
     * caido.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    @PrefixGameTestTemplate(false)
    public static void tickSobreviveAJogadorSemSessao(GameTestHelper helper) {
        ServerPlayer quebrado = helper.makeMockServerPlayerInLevel();
        ServerPlayer saudavel = comRenLigado(helper);

        // O jogador continua na lista, e o runtime some debaixo dele.
        NenRuntimeService.encerrarSessao(quebrado);

        // Se o tick nao aguentar, o servidor cai antes de chegar aqui e o
        // gametest nunca reporta sucesso.
        helper.runAfterDelay(5L, () -> {
            exigir(NenRuntimeService.estadoDe(saudavel).tecnicasAtivas().contains(Ren.ID),
                    "o jogador saudavel perdeu Ren junto com o quebrado; a falha de"
                            + " um contaminou o outro.");
            helper.succeed();
        });
    }
}
