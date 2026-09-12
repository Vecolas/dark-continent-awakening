package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.entity.ManFacedApeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * O Man-faced Ape com o jogo de pe.
 *
 * <p>{@code DisguiseRules} responde "pode aproximar?"; so aqui se responde
 * "aproximou?". A diferenca importa porque quem pode tirar o macaco do lugar
 * nao e a regra: e o {@code goalSelector}, que tem outras Goals disputando o
 * mesmo MOVE. Um passeio aleatorio herdando o MOVE enquanto o jogador encara o
 * macaco apagaria a unica pista do mob -- e nao apareceria como erro nenhum.
 *
 * <p>O OBSERVADOR E UM GOLEM DE FERRO, pelo mesmo motivo dos outros dois mobs:
 * {@code makeMockServerPlayerInLevel()} devolve jogador criativo, e dano contra
 * jogador e escalado pela dificuldade. O golem tambem serve melhor aqui porque
 * a direcao do olhar dele e um campo que o teste escreve, em vez de depender de
 * um cliente.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class ManFacedApeGameTest {

    private static final String ARENA = "arena";

    /**
     * CADA CENARIO DE MACACO NO SEU PROPRIO LOTE. E precaucao, nao diagnostico.
     *
     * <p>Cenarios do mesmo lote rodam AO MESMO TEMPO, em arenas vizinhas do mesmo
     * mundo. O raio do bando e de 12 blocos e nao sabe o que e uma arena: um macaco
     * ferido no cenario do bando pode alcancar o macaco do cenario ao lado e derrubar
     * o disfarce dele sem que ninguem tenha chegado perto. Nao foi o que aconteceu na
     * falha que este arquivo investigou -- aquilo era sufocamento por coordenada --
     * mas o alcance existe, e separar por lote e o unico jeito de dois cenarios que
     * criam a MESMA especie nao conversarem entre si.</p>
     */
    private static final String LOTE_OBSERVACAO = "man_faced_ape_observacao";
    private static final String LOTE_REVELACAO = "man_faced_ape_revelacao";
    private static final String LOTE_BANDO = "man_faced_ape_bando";

    /** Olhar para -X. {@code getLookAngle()} vira (-sen(yRot), ..., cos(yRot)). */
    private static final float OLHANDO_PARA_MENOS_X = 90.0F;
    /** Olhar para +X: de costas para quem esta em -X. */
    private static final float OLHANDO_PARA_MAIS_X = -90.0F;

    private ManFacedApeGameTest() { }

    /**
     * A PISTA DO MOB, medida em jogo: encarado, ele nao chega mais perto.
     *
     * <p>O teste faz as duas metades no mesmo cenario, porque so a segunda prova
     * que a primeira nao passou por acidente: um macaco que nunca anda tambem
     * ficaria parado enquanto encarado, e o teste aprovaria um mob quebrado.
     */
    @GameTest(template = ARENA, timeoutTicks = 300, batch = LOTE_OBSERVACAO)
    @PrefixGameTestTemplate(false)
    public static void encaradoEleNaoSeAproximaESemOlharEleChega(GameTestHelper helper) {
        ManFacedApeEntity macaco = helper.spawn(
                ManFacedApeEntity.registeredType(), new BlockPos(2, 2, 5));
        IronGolem observador = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(8, 2, 5));
        macaco.setTarget(observador);
        // O macaco esta em -X em relacao ao golem; olhar para -X e encarar o macaco.
        encarar(observador, OLHANDO_PARA_MENOS_X);

        double[] distanciaEncarado = new double[1];

        helper.startSequence()
                .thenExecuteAfter(20, () -> distanciaEncarado[0] = macaco.distanceTo(observador))
                .thenExecuteAfter(60, () -> {
                    double agora = macaco.distanceTo(observador);
                    helper.assertTrue(agora >= distanciaEncarado[0] - 0.5D,
                            "encarado por 60 ticks, o macaco avancou de " + distanciaEncarado[0]
                                    + " para " + agora + " blocos. A pista do mob e justamente"
                                    + " que ele NAO avanca enquanto alguem olha.");
                    helper.assertTrue(macaco.estaDisfarcado(),
                            "o macaco largou o disfarce a " + agora + " blocos do alvo, longe da"
                                    + " distancia de revelacao, com " + macaco.getHealth() + " de "
                                    + macaco.getMaxHealth() + " de vida. Ultima fonte de dano: "
                                    + macaco.getLastDamageSource() + "; ultimo agressor: "
                                    + macaco.getLastHurtByMob() + ".");
                    // Agora o golem vira as costas: a mesma cena, so que sem ninguem olhando.
                    encarar(observador, OLHANDO_PARA_MAIS_X);
                    distanciaEncarado[0] = macaco.distanceTo(observador);
                })
                .thenExecuteAfter(80, () -> {
                    double agora = macaco.distanceTo(observador);
                    helper.assertTrue(agora < distanciaEncarado[0] - 1.0D,
                            "com o observador de costas o macaco saiu de " + distanciaEncarado[0]
                                    + " e chegou a " + agora + ": ele nao se aproximou. Um macaco"
                                    + " que nunca anda passaria na primeira metade deste teste e"
                                    + " ainda assim seria um mob quebrado.");
                })
                .thenSucceed();
    }

    /**
     * Chegar perto derruba o disfarce, e o unico aviso e a fase WINDUP.
     *
     * <p>Sem o telegrafo publicado, o cliente nao tem o que desenhar e o bote
     * chega junto com o dano -- que e o que a secao 14 do plano proibe.
     */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = LOTE_REVELACAO)
    @PrefixGameTestTemplate(false)
    public static void chegarPertoDerrubaODisfarceComAviso(GameTestHelper helper) {
        ManFacedApeEntity macaco = helper.spawn(
                ManFacedApeEntity.registeredType(), new BlockPos(5, 2, 5));
        IronGolem alvo = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(5, 2, 7));
        macaco.setTarget(alvo);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertFalse(macaco.estaDisfarcado(),
                        "o alvo esta a dois blocos, dentro da distancia de revelacao do perfil,"
                                + " e o disfarce continua de pe."))
                .thenExecute(() -> helper.assertTrue(macaco.faseDeAtaque() == AttackPhase.WINDUP,
                        "o disfarce caiu publicando a fase " + macaco.faseDeAtaque()
                                + " em vez de WINDUP: o cliente fica sem telegrafo para desenhar,"
                                + " e o bote chega junto com o dano."))
                .thenSucceed();
    }

    /**
     * Revelar chama o BANDO -- e e isso que faz a forca do mob ser numeros.
     *
     * <p>Se o aviso nao saisse, o jogador enfrentaria um macaco de cada vez, o
     * encontro pareceria so facil demais, e nada acusaria a diferenca.
     */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = LOTE_BANDO)
    @PrefixGameTestTemplate(false)
    public static void revelarUmRevelaOBandoInteiro(GameTestHelper helper) {
        ManFacedApeEntity primeiro = helper.spawn(
                ManFacedApeEntity.registeredType(), new BlockPos(2, 2, 2));
        ManFacedApeEntity segundo = helper.spawn(
                ManFacedApeEntity.registeredType(), new BlockPos(5, 2, 4));
        ManFacedApeEntity terceiro = helper.spawn(
                ManFacedApeEntity.registeredType(), new BlockPos(8, 2, 6));

        helper.startSequence()
                .thenExecute(() -> {
                    helper.assertTrue(segundo.estaDisfarcado() && terceiro.estaDisfarcado(),
                            "o bando ja nasceu revelado; o teste nao chegou a medir o aviso.");
                    primeiro.hurt(helper.getLevel().damageSources().generic(), 1.0F);
                })
                .thenExecuteAfter(5, () -> {
                    helper.assertFalse(primeiro.estaDisfarcado(),
                            "quem levou o golpe continuou disfarcado.");
                    helper.assertFalse(segundo.estaDisfarcado(),
                            "o segundo macaco do bando, a menos de 12 blocos, nao foi avisado.");
                    helper.assertFalse(terceiro.estaDisfarcado(),
                            "o terceiro macaco do bando, a menos de 12 blocos, nao foi avisado.");
                })
                .thenSucceed();
    }

    /** Escreve a direcao do olhar do observador; e a unica entrada do sensor do macaco. */
    private static void encarar(LivingEntity observador, float yRot) {
        observador.setYRot(yRot);
        observador.setYHeadRot(yRot);
        observador.yBodyRot = yRot;
        observador.setXRot(0.0F);
    }
}
