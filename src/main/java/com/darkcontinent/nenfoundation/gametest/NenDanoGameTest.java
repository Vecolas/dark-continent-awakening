package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.api.event.OrigemDoDespertar;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
import com.darkcontinent.nenfoundation.nen.technique.Ken;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
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

    // ------------------------------------------------- o lado ofensivo

    /**
     * Quanto dano o jogador CAUSA, pelo evento.
     *
     * <p>A vitima e um mob sem Nen nenhum, e isso e o cenario: quem bate tem
     * aura, quem apanha nao. Enquanto so existia defesa, o handler desistia
     * cedo quando a vitima nao era jogador -- e foi essa saida antecipada que
     * precisou cair para o lado ofensivo existir.
     */
    private static float danoCausado(ServerPlayer jogador, GameTestHelper helper,
            float quantidade) {
        net.minecraft.world.entity.LivingEntity alvo =
                helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,
                        net.minecraft.core.BlockPos.ZERO);
        LivingIncomingDamageEvent evento = new LivingIncomingDamageEvent(alvo,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(
                        jogador.damageSources().playerAttack(jogador), quantidade));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(evento);
        alvo.discard();
        return evento.getAmount();
    }

    /**
     * Ren faz o golpe doer mais -- e esta era a metade que faltava.
     *
     * <p>NO CANONE REN E O AUMENTO DE PODER DE ATAQUE. Ate esta entrega, Ren
     * levantava o teto de Output e cobrava aura, e mais nada: a issue
     * guarda-chuva do M4 pedia "modificadores defensivos <b>e ofensivos</b>", e
     * so a metade defensiva existia. O sintoma era silencioso -- ninguem
     * reclama de um golpe que nao ficou mais forte, porque nao ha com o que
     * comparar.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void renFazOGolpeDoerMais(GameTestHelper helper) {
        ServerPlayer jogador = desperto(helper);

        float semNada = danoCausado(jogador, helper, 6.0F);
        NenTechniqueService.ativar(jogador, Ren.ID);
        float comRen = danoCausado(jogador, helper, 6.0F);

        exigir(comRen > semNada,
                "Ren nao somou nada ao golpe: " + comRen + " contra " + semNada
                        + ". A aura do atacante nao chegou ao dano -- ou o"
                        + " handler nao olha para quem bate, ou olhou e o"
                        + " reforco saiu zero.");

        helper.succeed();
    }

    /**
     * Concentrar longe do punho CUSTA golpe.
     *
     * <p>ESTA E A TROCA INTEIRA, e ela e o que separa concentracao de bonus. Um
     * jogador em Gyo com a aura na cabeca bate MENOS do que um jogador em
     * repouso com a mesma tecnica -- porque o punho ficou quase vazio.
     *
     * <p>Sem este teste, um reforco que ignorasse a alocacao passaria pelo teste
     * acima sem reprovar: Ren sozinho ja daria o numero maior. O defeito so
     * aparece quando se pergunta ONDE a aura estava.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void concentrarLongeDoPunhoCustaGolpe(GameTestHelper helper) {
        ServerPlayer jogador = desperto(helper);
        NenTechniqueService.ativar(jogador, Ren.ID);

        float comAuraEspalhada = danoCausado(jogador, helper, 6.0F);

        // A aura toda na cabeca, sem mexer em tecnica nenhuma: so a alocacao.
        NenRuntimeService.estadoDe(jogador).definirAlocacao(
                com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura
                        .concentrando(RegiaoDoCorpo.CABECA, 0.90F));
        float comAuraNaCabeca = danoCausado(jogador, helper, 6.0F);

        exigir(comAuraNaCabeca < comAuraEspalhada,
                "concentrar na cabeca nao custou golpe: " + comAuraNaCabeca
                        + " contra " + comAuraEspalhada + " com a aura espalhada."
                        + " O reforco esta ignorando ONDE a aura esta, e com isso"
                        + " concentrar vira bonus sem preco -- Ko passaria a ser"
                        + " vantagem pura, sem o risco que o justifica.");

        helper.succeed();
    }

    /**
     * Em Zetsu o golpe vale o que valeria sem Nen nenhum.
     *
     * <p>Zetsu ja tirava a defesa; faltava tirar o ataque. Um Zetsu que ainda
     * reforcasse o golpe seria furtividade com bonus -- exatamente o desenho
     * que a issue de Zetsu foi escrita para impedir.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void zetsuBateComoQuemNaoTemNen(GameTestHelper helper) {
        ServerPlayer jogador = desperto(helper);

        float semNada = danoCausado(jogador, helper, 6.0F);
        NenTechniqueService.ativar(jogador, Zetsu.ID);
        float comZetsu = danoCausado(jogador, helper, 6.0F);

        exigir(comZetsu == semNada,
                "com Zetsu o golpe deu " + comZetsu + " e sem aura nenhuma deu "
                        + semNada + ". Zetsu tem de custar os dois lados.");

        helper.succeed();
    }

    /**
     * Com duas reforcadoras ligadas, vale A MAIOR -- e nao a soma.
     *
     * <p>ELE PRECISOU DE TECNICAS FALSAS, e o motivo vale registrar: a mutacao
     * que troca {@code Math.max} por {@code +=} passou pelos 120 gametests sem
     * reprovar um. Em producao nunca ha duas reforcadoras ativas ao mesmo tempo
     * -- Zetsu exclui todas, Ken exclui Ten e Ren, Ko exclui Gyo -- e com UMA
     * so, somar e pegar a maior dao o mesmo numero.
     *
     * <p>E exatamente o que aconteceu com a protecao quando ela nasceu. Um
     * defeito invisivel em jogo precisa de um cenario que o jogo ainda nao
     * produz; o cenario aqui sao duas tecnicas modestas que CONVIVEM.
     *
     * <p>Por que a regra e essa: somar faz duas tecnicas modestas darem um
     * golpe que nenhuma das duas promete, e ninguem nota -- o numero final e
     * plausivel.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void entreDuasReforcadorasValeAMaior(GameTestHelper helper) {
        ServerPlayer jogador = desperto(helper);
        Reforcadora fraca = new Reforcadora("fraca", 0.20D);
        Reforcadora forte = new Reforcadora("forte", 0.50D);

        NenTecnicaGameTest.comRegistro(java.util.List.of(fraca, forte), () -> {
            NenTechniqueService.ativar(jogador, fraca.id());
            NenTechniqueService.ativar(jogador, forte.id());
            exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().size() == 2,
                    "as duas tinham de ficar ligadas; sem isso o teste mede uma"
                            + " tecnica so e a soma nunca aparece.");

            double reforco = com.darkcontinent.nenfoundation.server.NenDanoService
                    .reforcoDe(NenRuntimeService.estadoDe(jogador).tecnicasAtivas());

            exigir(Math.abs(reforco - 0.50D) < 1.0E-6D,
                    "com 0.20 e 0.50 ligadas o reforco deu " + reforco
                            + ". Somar da 0.70 -- um golpe que nenhuma das duas"
                            + " promete, e plausivel demais para alguem notar"
                            + " sem medir.");
        });

        helper.succeed();
    }

    /** Reforcadora minima que nao exclui ninguem: existe para poder CONVIVER. */
    private static final class Reforcadora implements
            com.darkcontinent.nenfoundation.nen.technique.NenTechnique,
            com.darkcontinent.nenfoundation.nen.technique.ReforcaGolpe {

        private final net.minecraft.resources.ResourceLocation id;
        private final double reforco;

        Reforcadora(String nome, double reforco) {
            this.id = net.minecraft.resources.ResourceLocation
                    .fromNamespaceAndPath(NenFoundation.MOD_ID, "teste_" + nome);
            this.reforco = reforco;
        }

        @Override public net.minecraft.resources.ResourceLocation id() { return this.id; }
        @Override public java.util.Set<net.minecraft.resources.ResourceLocation>
                incompativeisCom() { return java.util.Set.of(); }
        @Override public double reforcoBase() { return this.reforco; }

        @Override
        public com.darkcontinent.nenfoundation.nen.technique.TechniqueActivationResult
                canActivate(ServerPlayer j,
                        com.darkcontinent.nenfoundation.nen.technique.NenContext c) {
            return com.darkcontinent.nenfoundation.nen.technique
                    .TechniqueActivationResult.aceito();
        }

        @Override public void onActivate(ServerPlayer j,
                com.darkcontinent.nenfoundation.nen.technique.NenContext c) { }
        @Override public void serverTick(ServerPlayer j,
                com.darkcontinent.nenfoundation.nen.technique.NenContext c) { }
        @Override public void onDeactivate(ServerPlayer j,
                com.darkcontinent.nenfoundation.nen.technique.NenContext c,
                com.darkcontinent.nenfoundation.nen.technique.StopReason m) { }
    }
}
