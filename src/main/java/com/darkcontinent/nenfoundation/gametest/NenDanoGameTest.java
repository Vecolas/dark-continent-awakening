package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.api.event.OrigemDoDespertar;
import com.darkcontinent.nenfoundation.nen.technique.Ken;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import com.darkcontinent.nenfoundation.server.NenAwakeningService;
import com.darkcontinent.nenfoundation.server.NenRuntimeService;
import com.darkcontinent.nenfoundation.server.NenTechniqueService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A aura segurando dano DE VERDADE.
 *
 * <p>O teste puro prova a CONTA. O que so existe aqui e o handler estar
 * inscrito no barramento e o dano chegando reduzido do outro lado -- sem isso,
 * a conta certa fica num arquivo que ninguem chama, que e o falso verde que
 * este projeto ja cometeu.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenDanoGameTest {

    private static final String TEMPLATE = "empty";

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new GameTestAssertException(mensagem);
        }
    }

    private static ServerPlayer desperto(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);
        NenRuntimeService.estadoDe(jogador).definirAuraMaxima(1000.0D);
        NenRuntimeService.estadoDe(jogador).definirAuraAtual(1000.0D);
        jogador.setHealth(jogador.getMaxHealth());
        return jogador;
    }

    /**
     * Quanto dano sobra depois da aura, pelo EVENTO.
     *
     * <p>PELO BARRAMENTO, e nao por {@code hurt}. A primeira versao deste
     * arquivo batia no jogador de verdade e media a vida perdida -- e todos os
     * golpes deram ZERO, porque o jogador de teste nao toma dano (mock sem
     * conexao real, invulneravel). O teste reprovava dizendo que a aura nao
     * segurava nada, quando na verdade nada estava batendo.
     *
     * <p>Postar o evento prova o que so o gametest pode provar: que o handler
     * esta INSCRITO no barramento e reage. Quanto de vida sai depois disso e
     * contrato do Minecraft, nao deste mod.
     */
    private static float danoSofrido(ServerPlayer jogador, float quantidade) {
        LivingIncomingDamageEvent evento = new LivingIncomingDamageEvent(jogador,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(
                        jogador.damageSources().source(DamageTypes.GENERIC), quantidade));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(evento);
        return evento.getAmount();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void tenSeguraParteDoGolpe(GameTestHelper helper) {
        ServerPlayer jogador = desperto(helper);

        float semNada = danoSofrido(jogador, 8.0F);
        NenTechniqueService.ativar(jogador, Ten.ID);
        float comTen = danoSofrido(jogador, 8.0F);

        exigir(comTen < semNada,
                "Ten nao segurou nada: " + comTen + " contra " + semNada
                        + ". A defesa basica de Nen e o que Ten promete desde o"
                        + " M4, e ate a camada de dano existir ela era so texto.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void kenSeguraMaisQueTen(GameTestHelper helper) {
        ServerPlayer jogador = desperto(helper);

        NenTechniqueService.ativar(jogador, Ten.ID);
        float comTen = danoSofrido(jogador, 8.0F);

        // Ken exclui Ten, entao ativar ja derruba o outro.
        NenTechniqueService.ativar(jogador, Ken.ID);
        exigir(!NenRuntimeService.estadoDe(jogador).tecnicasAtivas().contains(Ten.ID),
                "Ken nao derrubou Ten; o teste estaria medindo os dois juntos.");
        float comKen = danoSofrido(jogador, 8.0F);

        exigir(comKen < comTen,
                "Ken (" + comKen + ") nao segurou mais que Ten (" + comTen + ")."
                        + " Ken e a principal defesa geral do canone; igual a Ten,"
                        + " ele e so um Ten caro.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void zetsuDoiTantoQuantoNaoTerNada(GameTestHelper helper) {
        // O PRECO DE ZETSU, e ele nao existia ate agora (#127). Quem esta em
        // Zetsu desligou a armadura para sumir do radar.
        ServerPlayer jogador = desperto(helper);

        float semNada = danoSofrido(jogador, 8.0F);
        NenTechniqueService.ativar(jogador, Zetsu.ID);
        float comZetsu = danoSofrido(jogador, 8.0F);

        exigir(comZetsu >= semNada,
                "Zetsu protegeu alguma coisa: " + comZetsu + " contra " + semNada
                        + " sem tecnica. Ele e o estado em que a defesa some, e"
                        + " protecao ali o transformaria em furtividade de graca.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void desligarDevolveODanoAoNormal(GameTestHelper helper) {
        // QUEM LIGA, DESLIGA. Uma reducao que sobrevive a tecnica desligada nao
        // da erro nenhum: o jogador so passa a apanhar menos para sempre.
        ServerPlayer jogador = desperto(helper);

        float semNada = danoSofrido(jogador, 8.0F);
        NenTechniqueService.ativar(jogador, Ken.ID);
        float comKen = danoSofrido(jogador, 8.0F);
        exigir(comKen < semNada, "Ken nao reduziu nada; o teste mediria o vazio.");

        NenTechniqueService.desligar(jogador, Ken.ID,
                com.darkcontinent.nenfoundation.nen.technique.StopReason.PLAYER_REQUEST);
        float depois = danoSofrido(jogador, 8.0F);

        exigir(Math.abs(depois - semNada) < 0.01F,
                "depois de desligar Ken o dano ficou em " + depois + ", e nao"
                        + " voltou ao normal (" + semNada + ").");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void jogadorSemNenNaoEhAfetado(GameTestHelper helper) {
        // Quem nunca despertou toma o dano inteiro. Se a camada mexesse nele,
        // ela estaria protegendo quem nao tem aura nenhuma.
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        jogador.setHealth(jogador.getMaxHealth());

        float sofrido = danoSofrido(jogador, 8.0F);
        exigir(sofrido >= 8.0F - 0.01F,
                "um jogador sem Nen ficou com " + sofrido + " de um golpe de 8.");

        helper.succeed();
    }
}
