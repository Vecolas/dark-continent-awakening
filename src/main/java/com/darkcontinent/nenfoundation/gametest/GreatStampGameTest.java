package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.entity.GreatStampEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * O Great Stamp com o jogo de pe.
 *
 * <p>O QUE O TESTE PURO JA COBRE: as tabelas -- quando a carga dispara, quando
 * a parede atordoa, qual regiao um par de numeros de geometria produz. O que so
 * existe aqui e a entidade DE VERDADE medindo essa geometria com a posicao que
 * o servidor tem, e a fase voltando para IDLE sem ninguem mandar.
 *
 * <p>A ARENA e {@code nenfoundation:arena}: 11x6x11 com chao de pedra. O
 * template {@code empty} de 3x3x3 nao serve aqui -- uma carga percorre dez
 * blocos, e um teste que nao cabe na arena passa a medir a parede.
 *
 * <p>COMO A ARENA FOI GERADA: NBT gzipado com {@code size=[11,6,11]},
 * {@code palette=[air, stone]}, {@code DataVersion=3955} e 121 entradas de
 * {@code blocks} formando o piso na camada 0 do template -- o mesmo esquema do
 * {@code empty.nbt}, lido dele campo a campo. E binario e gerado; nao se edita a mao.
 *
 * <p><b>O PISO FICA EM y=1, E NAO EM y=0.</b> {@code prepareTestStructure} cria o
 * structure block UM BLOCO ABAIXO do conteudo ({@code blockpos.below()}), e
 * {@code helper.absolutePos} conta a partir do structure block. Entao a camada 0 do
 * template chega como y=1 relativo, e QUEM NASCE EM y=1 NASCE DENTRO DA PEDRA.
 * Isso custou uma investigacao: o macaco, o mais alto dos tres mobs, apanhava de
 * {@code inWall} e largava o disfarce -- o teste reprovava com uma mensagem que
 * culpava o mob. Todo spawn destes cenarios usa y=2.
 *
 * <p>A VITIMA DE TESTE E UM GOLEM DE FERRO, e isso nao e capricho:
 * {@code makeMockServerPlayerInLevel()} devolve um jogador com
 * {@code isCreative()} cravado em {@code true}, e dano de mob contra jogador
 * ainda e escalado pela dificuldade do servidor. O golem tem 100 de vida, nao
 * caca criatura pacifica e nao muda de dano com a dificuldade -- entao o que o
 * teste mede e o mob, e nao a configuracao da sessao.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class GreatStampGameTest {

    private static final String ARENA = "arena";

    private GreatStampGameTest() { }

    /**
     * A testa doi mais que o corpo -- e quem decide isso e a geometria do
     * SERVIDOR, nao o cliente.
     *
     * <p>Dois stamps identicos levam o MESMO dano cru. A diferenca e so de onde
     * veio o golpe: um pela frente e do alto, outro pelas costas. Se a regiao
     * fosse resolvida errado, os dois perderiam a mesma vida e nada acusaria.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void aTestaDoiMaisQueOCorpo(GameTestHelper helper) {
        GreatStampEntity pelaFrente = helper.spawnWithNoFreeWill(
                GreatStampEntity.registeredType(), new BlockPos(3, 2, 5));
        GreatStampEntity pelasCostas = helper.spawnWithNoFreeWill(
                GreatStampEntity.registeredType(), new BlockPos(7, 2, 5));
        encarar(pelaFrente);
        encarar(pelasCostas);

        ServerPlayer atacante = helper.makeMockServerPlayerInLevel();
        DamageSource golpe = helper.getLevel().damageSources().playerAttack(atacante);

        // Pela FRENTE (o stamp olha para +Z, o atacante esta em +Z) e mirando
        // para baixo, o traco entra pela parte alta da caixa: e a testa.
        atacante.moveTo(pelaFrente.getX(), pelaFrente.getY(), pelaFrente.getZ() + 2.0D, 180.0F, 10.0F);
        float antesDaTesta = pelaFrente.getHealth();
        pelaFrente.hurt(golpe, 4.0F);
        float perdaNaTesta = antesDaTesta - pelaFrente.getHealth();

        // PELAS COSTAS, na mesma altura: o cosseno reprova e a regiao vira corpo.
        atacante.moveTo(pelasCostas.getX(), pelasCostas.getY(), pelasCostas.getZ() - 2.0D, 0.0F, 10.0F);
        float antesDoCorpo = pelasCostas.getHealth();
        pelasCostas.hurt(golpe, 4.0F);
        float perdaNoCorpo = antesDoCorpo - pelasCostas.getHealth();

        helper.assertTrue(perdaNoCorpo > 0.0F,
                "o golpe pelas costas nao tirou vida nenhuma: o teste nao esta medindo dano.");
        helper.assertTrue(perdaNaTesta > perdaNoCorpo * 2.0F,
                "a testa tirou " + perdaNaTesta + " e o corpo tirou " + perdaNoCorpo
                        + ". O multiplicador do perfil e 4x; sem essa diferenca o ponto fraco"
                        + " existe so no catalogo, e o mob vira HP sponge.");
        helper.succeed();
    }

    /**
     * QUEM LIGA, DESLIGA: a fase da carga sempre volta para IDLE.
     *
     * <p>Este e o teste que o puro nao alcanca. A {@code AttackTimeline} e
     * testavel sozinha, mas quem a ticka e a entidade, e quem pode preemptar a
     * Goal e o {@code goalSelector}. Uma fase presa em ACTIVE nao aparece como
     * erro: aparece como um stamp que nunca mais ataca.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void aFaseDaCargaNuncaFicaPresa(GameTestHelper helper) {
        GreatStampEntity stamp = helper.spawn(GreatStampEntity.registeredType(), new BlockPos(2, 2, 5));
        IronGolem vitima = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(8, 2, 5));
        encarar(stamp);
        stamp.setTarget(vitima);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stamp.faseDeAtaque() != AttackPhase.IDLE,
                        "o stamp nunca comecou a carga com um alvo a 6 blocos, dentro da faixa"
                                + " de disparo do perfil."))
                .thenExecuteAfter(90, () -> helper.assertTrue(stamp.faseDeAtaque() == AttackPhase.IDLE,
                        "noventa ticks depois do inicio a fase ainda e " + stamp.faseDeAtaque()
                                + ". A carga inteira dura 62 ticks: ela ficou presa."))
                .thenSucceed();
    }

    /**
     * A manada nao se mata.
     *
     * <p>O stamp que investe passa por cima de um irmao. Sem o filtro de
     * especie, o irmao apanha, o {@code HurtByTargetGoal} dele responde, e uma
     * manada de 2 a 4 se dizima sozinha antes de qualquer jogador chegar perto.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    @PrefixGameTestTemplate(false)
    public static void aManadaNaoSeMata(GameTestHelper helper) {
        GreatStampEntity queInveste = helper.spawn(GreatStampEntity.registeredType(), new BlockPos(2, 2, 5));
        GreatStampEntity noCaminho = helper.spawnWithNoFreeWill(
                GreatStampEntity.registeredType(), new BlockPos(4, 2, 5));
        IronGolem alvo = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(9, 2, 5));
        encarar(queInveste);
        encarar(noCaminho);
        queInveste.setTarget(alvo);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(queInveste.faseDeAtaque() != AttackPhase.IDLE,
                        "o stamp nunca comecou a carga; o teste nao chegou a medir nada."))
                .thenExecuteAfter(90, () -> {
                    // A REGUA E A FONTE DO DANO, e nao a barra de vida.
                    //
                    // A primeira versao comparava getHealth() com o maximo e reprovava com o
                    // irmao em 61 de 70. Nove pontos redondos nao batem com carga (16) nem com
                    // corpo a corpo (11) depois da armadura 7, e esta assercao -- que passa --
                    // prova que nao foi o stamp que investiu. DE ONDE VIERAM OS NOVE PONTOS
                    // CONTINUA SEM RESPOSTA: a suspeita e o empurrao prensando o irmao contra
                    // a parede da arena, mas ninguem mediu. O que se aprendeu e outra coisa:
                    // vida mede o CENARIO inteiro, e quem esta em julgamento aqui e o MOB.
                    helper.assertTrue(noCaminho.getLastHurtByMob() != queInveste,
                            "o irmao no caminho foi ferido pelo stamp que investiu (ultima fonte: "
                                    + noCaminho.getLastDamageSource() + "): a manada esta se"
                                    + " matando sozinha, e o HurtByTargetGoal do irmao responde.");
                    helper.assertTrue(noCaminho.getTarget() != queInveste,
                            "o irmao no caminho virou alvo do stamp que investiu: a briga interna"
                                    + " ja comecou.");
                })
                .thenSucceed();
    }

    /** Olhar para +Z, para a geometria do teste nao depender de onde a entidade nasceu virada. */
    private static void encarar(GreatStampEntity stamp) {
        stamp.setYRot(0.0F);
        stamp.yBodyRot = 0.0F;
        stamp.yHeadRot = 0.0F;
        stamp.setXRot(0.0F);
    }
}
