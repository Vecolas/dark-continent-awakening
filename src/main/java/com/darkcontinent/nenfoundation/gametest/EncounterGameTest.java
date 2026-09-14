package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.encounter.EncounterController;
import com.darkcontinent.nenfoundation.enemy.encounter.EncounterInstance;
import com.darkcontinent.nenfoundation.enemy.encounter.EncounterRules;
import com.darkcontinent.nenfoundation.enemy.encounter.EncounterSavedData;
import com.darkcontinent.nenfoundation.enemy.encounter.EncounterSpawnerPadrao;
import com.darkcontinent.nenfoundation.enemy.encounter.EncounterState;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A fundacao de encontro com o jogo de pe (EN4 / issue #141).
 *
 * <p><b>O QUE O TESTE PURO JA COBRE:</b> a tabela de transicoes, o ledger de
 * recompensa, a histerese e a limpeza de episodio. Tudo isso e regra, e roda sem
 * servidor.</p>
 *
 * <p><b>O QUE SO EXISTE AQUI:</b> o spawner colocando entidade DE VERDADE no
 * mundo, a reconciliacao perguntando ao nivel quem sobreviveu, e a conclusao
 * acontecendo porque o ultimo bicho morreu -- e nao porque alguem chamou um
 * metodo. A diferenca entre as duas coisas e o defeito que a issue #141 existe
 * para impedir: um encontro que conclui sozinho paga recompensa sem combate.</p>
 *
 * <p><b>O QUE ESTE ARQUIVO NAO PROVA, e e o gate da issue:</b> o RESTART. Um
 * GameTest roda dentro de um servidor que ja esta de pe; ele nao consegue
 * derrubar e subir o processo. A reconciliacao e exercitada aqui com a lista de
 * entidades manipulada a mao -- o que prova a REGRA e nao o restart. O gate
 * continua sendo humano: reiniciar um servidor no meio de um encontro.</p>
 *
 * <p>A arena e {@code nenfoundation:arena} e os spawns usam y=2, pelo mesmo
 * motivo documentado em {@code GreatStampGameTest}: a camada 0 do template chega
 * como y=1, e quem nasce em y=1 nasce dentro da pedra.</p>
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class EncounterGameTest {

    private static final String ARENA = "arena";

    private EncounterGameTest() { }

    /** Um controlador isolado, com o save do proprio servidor de teste. */
    private static EncounterController controlador(GameTestHelper helper, EncounterRules regras) {
        return new EncounterController(
                EncounterSavedData.de(helper.getLevel().getServer()), regras);
    }

    private static EncounterInstance armado(GameTestHelper helper, String definicao, BlockPos local) {
        EncounterInstance instancia = new EncounterInstance(UUID.randomUUID(),
                definicao, helper.getLevel().dimension(), helper.absolutePos(local));
        instancia.estado(EncounterState.ARMED);
        return instancia;
    }

    /** Jogador conectado de verdade: o controlador consulta a lista do servidor. */
    private static ServerPlayer jogadorNoAncoradouro(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        jogador.setGameMode(GameType.SURVIVAL);
        jogador.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(5, 2, 5)));
        return jogador;
    }

    /**
     * Armado + jogador perto = criaturas NO MUNDO.
     *
     * <p>O teste puro sabe que o estado vira ACTIVE; so aqui se descobre se o
     * spawner realmente achou lugar. Um spawner que nao acha lugar devolve lista
     * vazia, e o controlador FALHA o episodio -- sem erro nenhum, e com o
     * ancoradouro parecendo funcionar.</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    @PrefixGameTestTemplate(false)
    public static void oEncontroArmadoColocaCriaturaNoMundo(GameTestHelper helper) {
        EncounterController controlador = controlador(helper, EncounterRules.campo());
        EncounterInstance encontro = armado(helper, "nenfoundation:dummy_enemy",
                new BlockPos(5, 2, 5));
        controlador.dados().registrar(encontro);

        jogadorNoAncoradouro(helper);

        controlador.tick(helper.getLevel().getServer(), new EncounterSpawnerPadrao());

        helper.succeedWhen(() -> {
            helper.assertTrue(encontro.estado() == EncounterState.ACTIVE,
                    "o encontro nao ativou com um jogador em cima do ancoradouro; estado="
                            + encontro.estado());
            helper.assertTrue(!encontro.entidades().isEmpty(),
                    "o encontro ficou ACTIVE e nao colocou nada no mundo. Um spawner que nao"
                            + " acha lugar devolve lista vazia, e o ancoradouro fica parecendo"
                            + " que funciona.");
        });
    }

    /**
     * Matar TUDO conclui o encontro -- e so entao a recompensa pode sair.
     *
     * <p>Aqui esta a diferenca que o teste puro nao consegue medir: a conclusao
     * acontece porque a ultima entidade MORREU, e nao porque alguem chamou um
     * metodo. Um encontro que conclui sozinho paga recompensa sem combate.</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void matarTudoConcluiEDestravaARecompensa(GameTestHelper helper) {
        EncounterController controlador = controlador(helper, EncounterRules.campo());
        EncounterInstance encontro = armado(helper, "nenfoundation:dummy_enemy",
                new BlockPos(5, 2, 5));
        controlador.dados().registrar(encontro);

        jogadorNoAncoradouro(helper);
        controlador.tick(helper.getLevel().getServer(), new EncounterSpawnerPadrao());

        helper.assertTrue(encontro.estado() == EncounterState.ACTIVE, "o encontro nao ativou");
        for (UUID id : encontro.entidades()) {
            Entity entidade = helper.getLevel().getEntity(id);
            helper.assertTrue(entidade != null, "o uuid registrado nao corresponde a entidade"
                    + " nenhuma: um spawn que nao volta na lista fica invisivel para o"
                    + " controlador, e o restart seguinte spawna outra leva por cima.");
            entidade.kill();
        }

        helper.succeedWhen(() -> {
            controlador.tick(helper.getLevel().getServer(), new EncounterSpawnerPadrao());
            helper.assertTrue(encontro.estado() == EncounterState.COMPLETED,
                    "todas as criaturas morreram e o encontro nao concluiu; estado="
                            + encontro.estado());
            helper.assertTrue(controlador.travarRecompensa(encontro, "teste"),
                    "o encontro concluiu e a recompensa continuou travada");
            helper.assertTrue(!controlador.travarRecompensa(encontro, "teste"),
                    "a MESMA recompensa saiu duas vezes: a trava e o unico ponto de exclusao,"
                            + " e ela decide e grava na mesma chamada justamente para nao ter"
                            + " meio onde o segundo jogador caiba.");
        });
    }

    /**
     * Com as entidades vivas, a reconciliacao NAO spawna outra leva.
     *
     * <p>E o coracao da issue #141. Aqui a lista de entidades e a real, e o
     * controlador pergunta ao nivel quem sobreviveu -- que e exatamente o que ele
     * fara depois de um restart de verdade.</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void reconciliarComBichoVivoNaoDuplica(GameTestHelper helper) {
        EncounterController controlador = controlador(helper, EncounterRules.campo());
        EncounterInstance encontro = armado(helper, "nenfoundation:dummy_enemy",
                new BlockPos(5, 2, 5));
        controlador.dados().registrar(encontro);

        jogadorNoAncoradouro(helper);
        controlador.tick(helper.getLevel().getServer(), new EncounterSpawnerPadrao());

        int antes = encontro.entidades().size();
        helper.assertTrue(antes > 0, "nada foi criado: este teste nao esta medindo duplicacao.");

        controlador.reconciliarAoIniciar(helper.getLevel().getServer());

        helper.succeedIf(() -> {
            helper.assertTrue(encontro.estado() == EncounterState.ACTIVE,
                    "a reconciliacao derrubou um episodio cujas entidades estao VIVAS; estado="
                            + encontro.estado());
            helper.assertTrue(encontro.entidades().size() == antes,
                    "a reconciliacao mudou a lista de entidades de " + antes + " para "
                            + encontro.entidades().size() + ". Ela nao pode spawnar nada: e"
                            + " exatamente isso que produz dois chefes depois de um restart.");
        });
    }

    /**
     * Sem ninguem por perto, o episodio FALHA -- e nao fica ativo para sempre.
     *
     * <p>Um encontro que nunca conclui e nunca falha deixa o ancoradouro morto, e
     * o unico sintoma e um lugar do mapa onde nada mais acontece.</p>
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    @PrefixGameTestTemplate(false)
    public static void encontroAbandonadoFalhaEmVezDeFicarPreso(GameTestHelper helper) {
        // Um segundo de tolerancia: o teste nao pode esperar os dez da regra de
        // campo, e o que ele mede e a REGRA de abandono, nao o numero dela.
        EncounterController controlador = controlador(helper,
                new EncounterRules(4.0D, 16.0D, 20, 0));
        EncounterInstance encontro = armado(helper, "nenfoundation:dummy_enemy",
                new BlockPos(5, 2, 5));
        controlador.dados().registrar(encontro);

        ServerPlayer jogador = jogadorNoAncoradouro(helper);
        controlador.tick(helper.getLevel().getServer(), new EncounterSpawnerPadrao());
        helper.assertTrue(encontro.estado() == EncounterState.ACTIVE, "o encontro nao ativou");

        // O jogador vai embora: para alem do raio de ABANDONO, e nao so do de
        // ativacao -- a histerese existe para a borda nao piscar.
        jogador.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(5, 2, 5))
                .add(0, 0, 40));

        helper.succeedWhen(() -> {
            controlador.tick(helper.getLevel().getServer(), new EncounterSpawnerPadrao());
            helper.assertTrue(encontro.estado() == EncounterState.FAILED,
                    "o encontro ficou " + encontro.estado() + " depois de todos irem embora."
                            + " Um episodio que nunca conclui e nunca falha deixa o"
                            + " ancoradouro morto, e o sintoma e um lugar do mapa onde nada"
                            + " mais acontece.");
        });
    }
}
