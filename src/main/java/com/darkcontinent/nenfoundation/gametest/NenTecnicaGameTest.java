package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.aura.AuraPool;
import com.darkcontinent.nenfoundation.nen.technique.ConsomeAura;
import com.darkcontinent.nenfoundation.nen.technique.ModificaRegeneracao;
import com.darkcontinent.nenfoundation.nen.technique.ModificaTetoDeOutput;
import com.darkcontinent.nenfoundation.nen.technique.LimitaTetoDeOutput;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import com.darkcontinent.nenfoundation.nen.aura.FocoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
import com.darkcontinent.nenfoundation.nen.technique.Gyo;
import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.technique.RedistribuiAura;
import com.darkcontinent.nenfoundation.nen.technique.Ken;
import com.darkcontinent.nenfoundation.nen.technique.Shu;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.NenContext;
import com.darkcontinent.nenfoundation.nen.technique.NenTechnique;
import com.darkcontinent.nenfoundation.nen.technique.RegistroDeTecnicas;
import com.darkcontinent.nenfoundation.nen.technique.StopReason;
import com.darkcontinent.nenfoundation.nen.technique.TechniqueActivationResult;
import com.darkcontinent.nenfoundation.server.NenProfileService;
import com.darkcontinent.nenfoundation.nen.technique.ProtegeComAura;
import com.darkcontinent.nenfoundation.server.NenDanoService;
import com.darkcontinent.nenfoundation.nen.technique.Ko;
import com.darkcontinent.nenfoundation.nen.combat.FaixaDoCorpo;
import com.darkcontinent.nenfoundation.server.NenKoService;
import com.darkcontinent.nenfoundation.server.NenRuntimeService;
import com.darkcontinent.nenfoundation.server.NenTechniqueService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * O ciclo de vida de tecnica, com o jogo de pe.
 *
 * <p>O teste unitario cobre a TABELA -- quem exclui quem, e se as exclusoes
 * fecham. O que SO existe aqui: o servico agindo sobre o runtime de um
 * {@link ServerPlayer} de verdade, a idempotencia sob input repetido, e o
 * desligamento chegando pelo ciclo de vida.
 *
 * <p>As tecnicas aqui sao de MENTIRA de proposito. Ten, Ren, Zetsu e Gyo tem
 * issues proprias (#86, #87, #88); testar o ciclo contra elas misturaria
 * "o motor esta certo" com "Ten esta certo", e um defeito de um apareceria
 * como falha do outro.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenTecnicaGameTest {

    private static final String TEMPLATE = "empty";

    /** Nome distinto do metodo de instancia `id()` das tecnicas, que colidia. */
    private static ResourceLocation idDeTeste(String nome) {
        return NenFoundation.id("teste_" + nome);
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new GameTestAssertException(mensagem);
        }
    }

    /** Uma tecnica que registra tudo que aconteceu com ela. */
    private static final class Espia implements NenTechnique {
        private final ResourceLocation id;
        private final Set<ResourceLocation> incompativeis;
        private final boolean aceita;
        final List<String> chamadas = new ArrayList<>();
        final List<StopReason> motivos = new ArrayList<>();

        Espia(String nome, boolean aceita, String... incompativeis) {
            this.id = idDeTeste(nome);
            this.aceita = aceita;
            this.incompativeis = java.util.Arrays.stream(incompativeis)
                    .map(NenTecnicaGameTest::idDeTeste)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }

        @Override
        public ResourceLocation id() {
            return this.id;
        }

        @Override
        public Set<ResourceLocation> incompativeisCom() {
            return this.incompativeis;
        }

        @Override
        public TechniqueActivationResult canActivate(ServerPlayer jogador, NenContext ctx) {
            this.chamadas.add("canActivate");
            return this.aceita
                    ? TechniqueActivationResult.aceito()
                    : TechniqueActivationResult.negado("nenfoundation.error.indisponivel");
        }

        @Override
        public void onActivate(ServerPlayer jogador, NenContext ctx) {
            this.chamadas.add("onActivate");
        }

        @Override
        public void serverTick(ServerPlayer jogador, NenContext ctx) {
            this.chamadas.add("serverTick");
        }

        @Override
        public void onDeactivate(ServerPlayer jogador, NenContext ctx, StopReason motivo) {
            this.chamadas.add("onDeactivate");
            this.motivos.add(motivo);
        }

        long quantas(String etapa) {
            return this.chamadas.stream().filter(etapa::equals).count();
        }
    }

    /**
     * Instala um registro de teste e GARANTE que o original volta.
     *
     * <p>QUEM LIGA, DESLIGA. Um registro de teste vazado contaminaria todos os
     * gametests seguintes -- e o sintoma seria um teste que passa sozinho e
     * falha na suite.
     */
    private static void comRegistro(List<NenTechnique> tecnicas, Runnable corpo) {
        RegistroDeTecnicas original = NenTechniqueService.registro();
        NenTechniqueService.instalar(RegistroDeTecnicas.selar(tecnicas));
        try {
            corpo.run();
        } finally {
            NenTechniqueService.instalar(original);
        }
    }

    private static ServerPlayer jogadorDesperto(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        com.darkcontinent.nenfoundation.server.NenAwakeningService.despertar(jogador,
                com.darkcontinent.nenfoundation.api.event.OrigemDoDespertar.TREINO);
        return jogador;
    }

    // ------------------------------------------------------- ativacao

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void ativarLigaEGrava(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Espia ten = new Espia("ten", true);

        comRegistro(List.of(ten), () -> {
            NenTechniqueService.Resultado r = NenTechniqueService.ativar(jogador, ten.id());
            exigir(r.estado() == NenTechniqueService.Ativacao.ATIVOU,
                    "Esperava ATIVOU, veio " + r.estado());
        });

        exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().contains(ten.id()),
                "a tecnica nao entrou no runtime.");
        exigir(ten.quantas("onActivate") == 1,
                "onActivate rodou " + ten.quantas("onActivate") + " vez(es).");
        // A ordem e contrato: perguntar antes de ligar.
        exigir(ten.chamadas.indexOf("canActivate") < ten.chamadas.indexOf("onActivate"),
                "onActivate rodou antes de canActivate: " + ten.chamadas);

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void reentrarNaoRecobraNemReinicia(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Espia ten = new Espia("ten", true);

        comRegistro(List.of(ten), () -> {
            NenTechniqueService.ativar(jogador, ten.id());
            for (int i = 0; i < 10; i++) {
                NenTechniqueService.Resultado r = NenTechniqueService.ativar(jogador, ten.id());
                exigir(r.estado() == NenTechniqueService.Ativacao.JA_ATIVA,
                        "reentrada " + i + " devolveu " + r.estado());
            }
        });

        exigir(ten.quantas("onActivate") == 1,
                "onActivate rodou " + ten.quantas("onActivate") + " vezes. O jogador"
                        + " SEGURA a tecla: input repetido e o caso normal, e"
                        + " recobrar custo a cada repeticao nao daria erro nenhum.");
        exigir(ten.quantas("canActivate") == 1,
                "canActivate rodou " + ten.quantas("canActivate") + " vezes; a saida"
                        + " por idempotencia precisa vir ANTES da pergunta.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void recusaNaoLigaENaoLimpaNada(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Espia ten = new Espia("ten", true);
        Espia recusa = new Espia("recusa", false);

        comRegistro(List.of(ten, recusa), () -> {
            NenTechniqueService.ativar(jogador, ten.id());
            NenTechniqueService.Resultado r = NenTechniqueService.ativar(jogador, recusa.id());
            exigir(r.estado() == NenTechniqueService.Ativacao.RECUSADA,
                    "Esperava RECUSADA, veio " + r.estado());
            exigir(r.motivo().isPresent(), "recusa sem motivo para o jogador.");
        });

        exigir(recusa.quantas("onActivate") == 0, "recusada rodou onActivate.");
        exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().contains(ten.id()),
                "A recusa de uma tecnica desligou outra que estava ativa.");

        helper.succeed();
    }

    // ------------------------------------------------------- exclusao

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void incompativelDesligaAOutraAntesDeLigar(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Espia ten = new Espia("ten", true, "zetsu");
        Espia zetsu = new Espia("zetsu", true, "ten");

        comRegistro(List.of(ten, zetsu), () -> {
            NenTechniqueService.ativar(jogador, ten.id());
            NenTechniqueService.Resultado r = NenTechniqueService.ativar(jogador, zetsu.id());

            exigir(r.estado() == NenTechniqueService.Ativacao.ATIVOU, "zetsu nao ligou.");
            exigir(r.desligadasPorConflito().contains(ten.id()),
                    "o resultado nao diz quem foi desligada: " + r.desligadasPorConflito());
        });

        Set<ResourceLocation> ativas = NenRuntimeService.estadoDe(jogador).tecnicasAtivas();
        exigir(!ativas.contains(ten.id()), "ten continuou ativa junto com zetsu.");
        exigir(ativas.contains(zetsu.id()), "zetsu nao ficou ativa.");
        exigir(ten.motivos.equals(List.of(StopReason.REPLACED_BY_INCOMPATIBLE)),
                "motivo errado ao ser substituida: " + ten.motivos);

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void aExclusaoValeNasDuasOrdens(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Espia ten = new Espia("ten", true, "zetsu");
        Espia zetsu = new Espia("zetsu", true, "ten");

        comRegistro(List.of(ten, zetsu), () -> {
            // Zetsu primeiro, Ten depois -- a ordem inversa do teste anterior.
            // Uma exclusao declarada de um lado so passaria num sentido e
            // falharia no outro, e o defeito ficaria escondido no sentido que
            // ninguem testou.
            NenTechniqueService.ativar(jogador, zetsu.id());
            NenTechniqueService.ativar(jogador, ten.id());
        });

        Set<ResourceLocation> ativas = NenRuntimeService.estadoDe(jogador).tecnicasAtivas();
        exigir(ativas.contains(ten.id()) && !ativas.contains(zetsu.id()),
                "as duas ficaram ativas, ou a errada sobrou: " + ativas);

        helper.succeed();
    }

    // ------------------------------------------------------- tick

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void tickRodaSoParaAsAtivas(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Espia ativa = new Espia("ativa", true);
        Espia parada = new Espia("parada", true);

        comRegistro(List.of(ativa, parada), () -> {
            NenTechniqueService.ativar(jogador, ativa.id());
            NenTechniqueService.tick(jogador, NenRuntimeService.estadoDe(jogador));
            NenTechniqueService.tick(jogador, NenRuntimeService.estadoDe(jogador));
        });

        exigir(ativa.quantas("serverTick") == 2,
                "a ativa tickou " + ativa.quantas("serverTick") + " vez(es).");
        exigir(parada.quantas("serverTick") == 0,
                "uma tecnica DESLIGADA tickou. Logica rodando para um estado que"
                        + " nao existe e o tipo de coisa que nao da erro e consome"
                        + " tick para sempre.");

        helper.succeed();
    }

    // ------------------------------------------------------- desligamento

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void desligarTodasUsaOMotivoCerto(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Espia um = new Espia("um", true);
        Espia dois = new Espia("dois", true);

        comRegistro(List.of(um, dois), () -> {
            NenTechniqueService.ativar(jogador, um.id());
            NenTechniqueService.ativar(jogador, dois.id());
            int desligadas = NenTechniqueService.desligarTodas(jogador, StopReason.LOGOUT);
            exigir(desligadas == 2, "desligou " + desligadas + " em vez de 2.");
        });

        exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().isEmpty(),
                "sobrou tecnica ativa depois de desligarTodas.");
        exigir(um.motivos.equals(List.of(StopReason.LOGOUT))
                        && dois.motivos.equals(List.of(StopReason.LOGOUT)),
                "motivo errado: " + um.motivos + " / " + dois.motivos);

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void desligarTecnicaJaParadaNaoEErro(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Espia ten = new Espia("ten", true);

        comRegistro(List.of(ten), () -> {
            NenTechniqueService.ativar(jogador, ten.id());
            exigir(NenTechniqueService.desligar(jogador, ten.id(), StopReason.PLAYER_REQUEST),
                    "o primeiro desligamento devia valer.");
            exigir(!NenTechniqueService.desligar(jogador, ten.id(), StopReason.PLAYER_REQUEST),
                    "o segundo devia devolver false, e nao ser erro.");
        });

        exigir(ten.quantas("onDeactivate") == 1,
                "onDeactivate rodou " + ten.quantas("onDeactivate") + " vezes.");

        helper.succeed();
    }

    /**
     * Uma tecnica mal escrita nao pode derrubar as outras.
     *
     * <p>Sem a protecao, uma excecao em {@code onDeactivate} abortaria o laco
     * de {@code desligarTodas} -- e no logout isso deixaria metade das tecnicas
     * ligadas, com o jogador ja fora.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void tecnicaQueLancaNaoDerrubaAsOutras(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Espia boa = new Espia("boa", true);
        NenTechnique explosiva = new NenTechnique() {
            @Override
            public ResourceLocation id() {
                return idDeTeste("explosiva");
            }

            @Override
            public Set<ResourceLocation> incompativeisCom() {
                return Set.of();
            }

            @Override
            public TechniqueActivationResult canActivate(ServerPlayer j, NenContext c) {
                return TechniqueActivationResult.aceito();
            }

            @Override
            public void onActivate(ServerPlayer j, NenContext c) {
            }

            @Override
            public void serverTick(ServerPlayer j, NenContext c) {
            }

            @Override
            public void onDeactivate(ServerPlayer j, NenContext c, StopReason motivo) {
                throw new IllegalStateException("tecnica mal escrita");
            }
        };

        comRegistro(List.of(explosiva, boa), () -> {
            NenTechniqueService.ativar(jogador, explosiva.id());
            NenTechniqueService.ativar(jogador, boa.id());
            NenTechniqueService.desligarTodas(jogador, StopReason.LOGOUT);
        });

        exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().isEmpty(),
                "A excecao de uma tecnica deixou outra ligada. No logout isso"
                        + " sairia com o jogador ja fora, e sem erro visivel.");
        exigir(boa.quantas("onDeactivate") == 1,
                "a tecnica boa nao foi desligada.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void doisJogadoresNaoCompartilhamTecnica(GameTestHelper helper) {
        ServerPlayer um = jogadorDesperto(helper);
        ServerPlayer outro = jogadorDesperto(helper);
        Espia ten = new Espia("ten", true);

        comRegistro(List.of(ten), () -> NenTechniqueService.ativar(um, ten.id()));

        exigir(NenRuntimeService.estadoDe(um).tecnicasAtivas().contains(ten.id()),
                "o primeiro nao ficou com a tecnica.");
        exigir(NenRuntimeService.estadoDe(outro).tecnicasAtivas().isEmpty(),
                "A tecnica do primeiro apareceu no segundo. E o erro numero 2 do"
                        + " CLAUDE.md: a implementacao e singleton, e dois"
                        + " jogadores escrevem no mesmo campo.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void idDesconhecidoNaoEstoura(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);

        comRegistro(List.of(), () -> {
            NenTechniqueService.Resultado r =
                    NenTechniqueService.ativar(jogador, idDeTeste("nao_existe"));
            exigir(r.estado() == NenTechniqueService.Ativacao.DESCONHECIDA,
                    "Esperava DESCONHECIDA, veio " + r.estado());
        });

        exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().isEmpty(),
                "um id desconhecido entrou no runtime.");

        helper.succeed();
    }

    /** O perfil nunca e tocado por ativar ou desligar tecnica. */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void tecnicaNaoMexeNoPerfilPersistente(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Espia ten = new Espia("ten", true);
        Supplier<String> perfil = () -> NenProfileService.ler(jogador).toString();
        String antes = perfil.get();

        comRegistro(List.of(ten), () -> {
            NenTechniqueService.ativar(jogador, ten.id());
            NenTechniqueService.tick(jogador, NenRuntimeService.estadoDe(jogador));
            NenTechniqueService.desligarTodas(jogador, StopReason.PLAYER_REQUEST);
        });

        exigir(antes.equals(perfil.get()),
                "Ligar e desligar tecnica alterou o perfil PERSISTENTE. Estado de"
                        + " combate e runtime (ADR-002); escrever no attachment"
                        + " aqui gravaria em disco a cada ativacao.");

        helper.succeed();
    }

    /**
     * Ten, Ren e Zetsu SO SELAM JUNTAS, e este helper e a consequencia disso.
     *
     * <p>As tres se excluem, e o selamento do registro RECUSA exclusao que
     * aponte para tecnica de fora. Registrar so uma delas reprova -- e reprovou
     * mesmo, em sete gametests, no momento em que Zetsu entrou. O portao fez o
     * trabalho dele; o que estava errado era o registro parcial.
     *
     * <p>As parceiras entram INERTES: custo zero e multiplicador neutro. Elas
     * existem para o registro fechar, e nao para participar. Se alguma for
     * ativada por engano, ela nao mexe em numero nenhum -- o teste continua
     * medindo o que dizia medir.
     */
    private static List<NenTechnique> comAsParceiras(NenTechnique... tecnicas) {
        List<NenTechnique> todas = new ArrayList<>(List.of(tecnicas));
        Set<ResourceLocation> ids = todas.stream().map(NenTechnique::id)
                .collect(java.util.stream.Collectors.toSet());
        if (!ids.contains(Ten.ID)) {
            todas.add(new Ten(() -> 0.0D, () -> 1.0D, () -> 0.0D));
        }
        if (!ids.contains(Ren.ID)) {
            todas.add(new Ren(() -> 0.0D, () -> AuraPool.OUTPUT_MAXIMO_ABSOLUTO));
        }
        if (!ids.contains(Zetsu.ID)) {
            todas.add(new Zetsu(() -> 0.0D, () -> 1.0D,
                    () -> AuraPool.OUTPUT_MAXIMO_ABSOLUTO));
        }
        if (!ids.contains(Ko.ID)) {
            todas.add(koDeTeste(1));
        }
        if (!ids.contains(Ken.ID)) {
            todas.add(new Ken(() -> 0.0D, () -> AuraPool.OUTPUT_MAXIMO_ABSOLUTO, () -> 0.0D));
        }
        if (!ids.contains(Shu.ID)) {
            todas.add(new Shu(() -> 0.0D, () -> RegiaoDoCorpo.fracaoUniforme()));
        }
        if (!ids.contains(Gyo.ID)) {
            // Gyo entrou no quarteto quando a alocacao nasceu (ADR-014), e o
            // portao de simetria cobrou na hora: Zetsu passou a recusar Gyo, e
            // sem ele aqui a exclusao vira orfa e o registro nao sela.
            todas.add(new Gyo(() -> 0.0D, () -> RegiaoDoCorpo.fracaoUniforme()));
        }
        return List.copyOf(todas);
    }

    // ------------------------------------------------------------ Ten (#86)

    /** Ten de teste, com numeros proprios: os da config nao sao o assunto aqui. */
    private static Ten tenDeTeste(double custoPorSegundo, double multiplicador) {
        return new Ten(() -> custoPorSegundo, () -> multiplicador, () -> 0.0D);
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void tenCobraManutencaoPorTick(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Ten ten = tenDeTeste(20.0D, 1.0D);   // 1.0 de aura por tick, sem ganho

        comRegistro(comAsParceiras(ten), () -> {
            NenRuntimeService.estadoDe(jogador).definirAuraMaxima(100.0D);
            NenRuntimeService.estadoDe(jogador).definirAuraAtual(50.0D);
            NenTechniqueService.ativar(jogador, ten.id());

            double antes = NenRuntimeService.estadoDe(jogador).auraAtual();
            NenTechniqueService.tick(jogador, NenRuntimeService.estadoDe(jogador));
            double depois = NenRuntimeService.estadoDe(jogador).auraAtual();

            exigir(depois < antes,
                    "Ten nao cobrou nada no tick: " + antes + " -> " + depois
                            + ". Usar Nen custa aura; uma tecnica sustentada de"
                            + " graca vira o estado permanente obvio.");
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void tenCaiQuandoAAuraAcaba(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Ten ten = tenDeTeste(20.0D, 1.0D);

        comRegistro(comAsParceiras(ten), () -> {
            NenRuntimeService.estadoDe(jogador).definirAuraMaxima(100.0D);
            NenRuntimeService.estadoDe(jogador).definirAuraAtual(0.0D);
            NenTechniqueService.ativar(jogador, ten.id());

            exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().contains(ten.id()),
                    "Ten nao ligou: ele nao tem custo de ENTRADA, so de manutencao.");

            NenTechniqueService.tick(jogador, NenRuntimeService.estadoDe(jogador));

            exigir(!NenRuntimeService.estadoDe(jogador).tecnicasAtivas().contains(ten.id()),
                    "Com aura zero, Ten continuou ativo -- de graca.");
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void tenMudaOMultiplicadorEODevolveAoSair(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Ten ten = tenDeTeste(0.0D, 2.0D);

        comRegistro(comAsParceiras(ten), () -> {
            exigir(NenRuntimeService.estadoDe(jogador).multiplicadorDeRegeneracao() == 1.0D,
                    "sem tecnica ativa o multiplicador tem de ser neutro.");

            NenTechniqueService.ativar(jogador, ten.id());
            exigir(NenRuntimeService.estadoDe(jogador).multiplicadorDeRegeneracao() == 2.0D,
                    "Ten nao alterou a regeneracao; veio "
                            + NenRuntimeService.estadoDe(jogador).multiplicadorDeRegeneracao());

            NenTechniqueService.desligar(jogador, ten.id(), StopReason.PLAYER_REQUEST);
            exigir(NenRuntimeService.estadoDe(jogador).multiplicadorDeRegeneracao() == 1.0D,
                    "O multiplicador NAO voltou ao desligar. Regeneracao acelerada"
                            + " de uma tecnica que ja parou nao da erro nenhum --"
                            + " ela so fica ligada para sempre.");
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void oMultiplicadorEOProdutoDasAtivas(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        // Duas tecnicas que modificam regeneracao: o efetivo e o PRODUTO.
        Ten um = tenDeTeste(0.0D, 2.0D);
        ModificadorDeTeste dobro = new ModificadorDeTeste("dobro", 3.0D);

        comRegistro(comAsParceiras(um, dobro), () -> {
            NenTechniqueService.ativar(jogador, um.id());
            NenTechniqueService.ativar(jogador, dobro.id());

            double efetivo = NenRuntimeService.estadoDe(jogador).multiplicadorDeRegeneracao();
            exigir(efetivo == 6.0D,
                    "O multiplicador de duas ativas devia ser o produto (2 x 3 = 6)"
                            + " e veio " + efetivo + ". O TETO nao mora aqui: ele e"
                            + " do motor, e aplica-lo duas vezes esconderia um"
                            + " produto estourado atras de um numero plausivel.");

            NenTechniqueService.desligar(jogador, dobro.id(), StopReason.PLAYER_REQUEST);
            exigir(NenRuntimeService.estadoDe(jogador).multiplicadorDeRegeneracao() == 2.0D,
                    "ao sair uma, o produto tem de voltar ao da outra sozinha.");
        });

        helper.succeed();
    }

    /** Tecnica minima que so existe para modificar regeneracao. */
    private static final class ModificadorDeTeste implements NenTechnique, ModificaRegeneracao {
        private final ResourceLocation id;
        private final double multiplicador;

        ModificadorDeTeste(String nome, double multiplicador) {
            this.id = idDeTeste(nome);
            this.multiplicador = multiplicador;
        }

        @Override public ResourceLocation id() { return this.id; }
        @Override public Set<ResourceLocation> incompativeisCom() { return Set.of(); }
        @Override public double multiplicadorDeRegeneracao() { return this.multiplicador; }

        @Override
        public TechniqueActivationResult canActivate(ServerPlayer j, NenContext c) {
            return TechniqueActivationResult.aceito();
        }

        @Override public void onActivate(ServerPlayer j, NenContext c) { }
        @Override public void serverTick(ServerPlayer j, NenContext c) { }
        @Override public void onDeactivate(ServerPlayer j, NenContext c, StopReason m) { }
    }

    // ------------------------------ o limite simultaneo E a Aura (#97)

    /**
     * NAO HA TETO DE SLOTS. Quantas tecnicas ficam ligadas e o que a Aura paga.
     *
     * <p>Esta e a regra que o responsavel definiu, e ela precisa de teste
     * porque senao e so uma frase bonita: um teto de slots poderia ser
     * acrescentado por engano depois, e nada acusaria -- o jogo so ficaria
     * menor.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void aAuraEOLimiteDeTecnicasSimultaneas(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        // Cinco tecnicas baratas: nada no modelo impede as cinco juntas.
        List<NenTechnique> cinco = List.of(
                new Cobradora("a", 20.0D), new Cobradora("b", 20.0D),
                new Cobradora("c", 20.0D), new Cobradora("d", 20.0D),
                new Cobradora("e", 20.0D));

        comRegistro(cinco, () -> {
            NenRuntimeService.estadoDe(jogador).definirAuraMaxima(1000.0D);
            NenRuntimeService.estadoDe(jogador).definirAuraAtual(1000.0D);

            for (NenTechnique t : cinco) {
                exigir(NenTechniqueService.ativar(jogador, t.id()).estado()
                                == NenTechniqueService.Ativacao.ATIVOU,
                        "a tecnica " + t.id() + " foi recusada com aura de sobra."
                                + " Um teto de SLOTS teria entrado sem ninguem pedir.");
            }
            exigir(NenRuntimeService.estadoDe(jogador).tecnicasAtivas().size() == 5,
                    "com aura de sobra, as cinco deviam ficar ligadas; ficaram "
                            + NenRuntimeService.estadoDe(jogador).tecnicasAtivas().size());
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void semAuraAsTecnicasCaemAteSobrarOQueCabe(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        List<NenTechnique> cinco = List.of(
                new Cobradora("a", 20.0D), new Cobradora("b", 20.0D),
                new Cobradora("c", 20.0D), new Cobradora("d", 20.0D),
                new Cobradora("e", 20.0D));

        comRegistro(cinco, () -> {
            NenRuntimeService.estadoDe(jogador).definirAuraMaxima(1000.0D);
            NenRuntimeService.estadoDe(jogador).definirAuraAtual(1000.0D);
            for (NenTechnique t : cinco) {
                NenTechniqueService.ativar(jogador, t.id());
            }

            // Cada uma cobra 1.0 por tick. Com 2.5 de aura, o tick paga duas e
            // recusa as outras tres -- e sao ELAS que caem, nao um numero
            // magico de slots.
            NenRuntimeService.estadoDe(jogador).definirAuraAtual(2.5D);
            NenTechniqueService.tick(jogador, NenRuntimeService.estadoDe(jogador));

            int sobraram = NenRuntimeService.estadoDe(jogador).tecnicasAtivas().size();
            exigir(sobraram > 0 && sobraram < 5,
                    "Com aura para duas, sobraram " + sobraram + " de 5. O limite"
                            + " tem de EMERGIR do custo: nem todas sobrevivem, nem"
                            + " todas caem juntas.");

            // E a aura nao pode ter ficado negativa no processo.
            exigir(NenRuntimeService.estadoDe(jogador).auraAtual() >= 0.0D,
                    "a aura ficou negativa: "
                            + NenRuntimeService.estadoDe(jogador).auraAtual());
        });

        helper.succeed();
    }

    /** Tecnica minima que so cobra manutencao. */
    private static final class Cobradora implements NenTechnique, ConsomeAura {
        private final ResourceLocation id;
        private final double custoPorSegundo;
        /**
         * POR QUE ELA GUARDA O MOTIVO: a versao anterior descartava o
         * argumento de {@code onDeactivate}, e com isso NENHUM teste do mod
         * olhava para o {@link StopReason}. Trocar {@code OUT_OF_AURA} por
         * {@code PLAYER_REQUEST} no servico deixava a suite inteira verde.
         */
        private final java.util.List<StopReason> motivos = new java.util.ArrayList<>();

        Cobradora(String nome, double custoPorSegundo) {
            this.id = idDeTeste(nome);
            this.custoPorSegundo = custoPorSegundo;
        }

        @Override public ResourceLocation id() { return this.id; }
        @Override public Set<ResourceLocation> incompativeisCom() { return Set.of(); }
        @Override public double custoPorTick() { return this.custoPorSegundo / 20.0D; }

        @Override
        public TechniqueActivationResult canActivate(ServerPlayer j, NenContext c) {
            return TechniqueActivationResult.aceito();
        }

        @Override public void onActivate(ServerPlayer j, NenContext c) { }
        @Override public void serverTick(ServerPlayer j, NenContext c) { }
        @Override public void onDeactivate(ServerPlayer j, NenContext c, StopReason m) {
            this.motivos.add(m);
        }
    }

    /**
     * Aura zero encerra COM O MOTIVO CERTO, e nao so encerra.
     *
     * <p>ESTE ERA UM FALSO VERDE DECLARADO NO ROTEIRO. A linha C2 do gate do M4
     * dizia "automatizado -- mesmo teste", apontando para
     * {@code tenCaiQuandoAAuraAcaba}. Aquele teste prova que a tecnica CAI; o
     * motivo com que ela cai nunca foi olhado por ninguem. {@code OUT_OF_AURA}
     * existia so em codigo de producao.
     *
     * <p>O MOTIVO NAO E DETALHE INTERNO: ele e o que decide a mensagem que o
     * jogador le e o que uma habilidade futura vai consultar para saber se deve
     * religar sozinha. Um desligamento por falta de aura anunciado como
     * "o jogador pediu" manda todo mundo procurar no lugar errado -- a mesma
     * familia de defeito da recusa com chave generica.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void auraZeroEncerraComOUT_OF_AURA(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Cobradora cara = new Cobradora("cara", 1000.0D);

        comRegistro(List.of(cara), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirAuraMaxima(100.0D);
            estado.definirAuraAtual(100.0D);

            NenTechniqueService.ativar(jogador, cara.id());
            exigir(estado.tecnicasAtivas().contains(cara.id()),
                    "a tecnica nao ligou; o teste mediria o vazio.");

            // Sem aura nenhuma, o proximo tick nao tem como pagar.
            estado.definirAuraAtual(0.0D);
            NenTechniqueService.tick(jogador, estado);

            exigir(!estado.tecnicasAtivas().contains(cara.id()),
                    "a tecnica sobreviveu sem aura; nao ha desligamento para"
                            + " conferir o motivo.");
            exigir(cara.motivos.equals(List.of(StopReason.OUT_OF_AURA)),
                    "a tecnica caiu com " + cara.motivos + ", e nao com"
                            + " [OUT_OF_AURA]. Ela caiu pela razao certa e"
                            + " ANUNCIOU outra -- o jogador leria a mensagem de"
                            + " um problema que nao teve, e quem consultar o"
                            + " motivo depois decide errado.");
        });

        helper.succeed();
    }

    // ------------------------------------------------------------ Ren (#87)

    /** Uma tecnica que so levanta o teto, para medir o teto sem medir custo. */
    private static final class Levantadora
            implements NenTechnique, ModificaTetoDeOutput {
        private final ResourceLocation id;
        private final float teto;

        Levantadora(String nome, float teto) {
            this.id = idDeTeste(nome);
            this.teto = teto;
        }

        @Override public ResourceLocation id() { return this.id; }
        @Override public Set<ResourceLocation> incompativeisCom() { return Set.of(); }
        @Override public float tetoDeOutput() { return this.teto; }

        @Override
        public TechniqueActivationResult canActivate(ServerPlayer j, NenContext c) {
            return TechniqueActivationResult.aceito();
        }

        @Override public void onActivate(ServerPlayer j, NenContext c) { }
        @Override public void serverTick(ServerPlayer j, NenContext c) { }
        @Override public void onDeactivate(ServerPlayer j, NenContext c, StopReason m) { }
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void levantarOTetoMudaOOutputEfetivo(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Levantadora ren = new Levantadora("ren", 1.0F);

        comRegistro(List.of(ren), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirOutputSelecionado(1.0F);
            estado.definirOutputMaximo(0.5F);

            exigir(estado.outputEfetivo() == 0.5F,
                    "o teto de repouso nao estava limitando; veio " + estado.outputEfetivo());

            NenTechniqueService.ativar(jogador, ren.id());
            exigir(estado.outputEfetivo() == 1.0F,
                    "Ren nao levantou o teto: efetivo " + estado.outputEfetivo()
                            + ". E este o ponto cego que o PR #80 declarou -- nada"
                            + " mexia no maximo, e o `min` nunca mordia.");

            NenTechniqueService.desligar(jogador, ren.id(), StopReason.PLAYER_REQUEST);
            exigir(estado.outputEfetivo() < 1.0F,
                    "O teto NAO voltou ao desligar: efetivo " + estado.outputEfetivo()
                            + ". Teto elevado de uma tecnica que ja parou nao da"
                            + " erro nenhum -- ele so fica ligado para sempre.");
            exigir(estado.outputSelecionado() == 1.0F,
                    "a escolha do jogador foi rebaixada junto com o teto.");
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void oTetoEOMaiorDasAtivasENaoASoma(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Levantadora baixa = new Levantadora("baixa", 0.6F);
        Levantadora alta = new Levantadora("alta", 0.9F);

        comRegistro(List.of(baixa, alta), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirOutputSelecionado(1.0F);

            NenTechniqueService.ativar(jogador, baixa.id());
            NenTechniqueService.ativar(jogador, alta.id());

            exigir(estado.outputMaximo() == 0.9F,
                    "Com duas ativas o teto devia ser o MAIOR (0.9) e veio "
                            + estado.outputMaximo() + ". Somar daria 1.5 -- duas"
                            + " tecnicas modestas estourando o limite absoluto.");

            NenTechniqueService.desligar(jogador, alta.id(), StopReason.PLAYER_REQUEST);
            exigir(estado.outputMaximo() == 0.6F,
                    "ao sair a maior, o teto devia cair para a que sobrou; veio "
                            + estado.outputMaximo());
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void sessaoNovaNasceComOTetoDeRepouso(GameTestHelper helper) {
        // Sem o recalculo no inicio de sessao, o runtime nasceria com o teto
        // ABSOLUTO -- o jogador comecaria a partida com o teto de quem esta em
        // Ren, e so voltaria ao normal depois de ligar e desligar algo.
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        var estado = NenRuntimeService.estadoDe(jogador);

        exigir(estado.outputMaximo() > 0.0F,
                "sessao nova nasceu com teto zero: o jogador nao libera nada.");
        exigir(estado.multiplicadorDeRegeneracao() == 1.0D,
                "sessao nova nasceu com multiplicador de regeneracao diferente de"
                        + " neutro: " + estado.multiplicadorDeRegeneracao());

        helper.succeed();
    }

    // ----------------------------------------------------------- Zetsu (#88)

    /** So ABAIXA o teto. Sem custo, sem regeneracao: mede uma coisa de cada vez. */
    private static final class Limitadora
            implements NenTechnique, LimitaTetoDeOutput {
        private final ResourceLocation id;
        private final float teto;

        Limitadora(String nome, float teto) {
            this.id = idDeTeste(nome);
            this.teto = teto;
        }

        @Override public ResourceLocation id() { return this.id; }
        @Override public Set<ResourceLocation> incompativeisCom() { return Set.of(); }
        @Override public float tetoMaximoPermitido() { return this.teto; }

        @Override
        public TechniqueActivationResult canActivate(ServerPlayer j, NenContext c) {
            return TechniqueActivationResult.aceito();
        }

        @Override public void onActivate(ServerPlayer j, NenContext c) { }
        @Override public void serverTick(ServerPlayer j, NenContext c) { }
        @Override public void onDeactivate(ServerPlayer j, NenContext c, StopReason m) { }
    }

    /**
     * QUEM ABAIXA VENCE QUEM LEVANTA, mesmo com a que levanta pedindo mais.
     *
     * <p>ESTE TESTE EXISTE PORQUE O DEFEITO SERIA INVISIVEL. A unica limitadora
     * de verdade hoje e Zetsu, e ela EXCLUI Ren -- entao as duas nunca ficam
     * ativas juntas em jogo, e uma ordem errada em {@code tetoDe} nao mudaria
     * nada que alguem pudesse ver. Ficaria dormindo ate a primeira tecnica que
     * combinasse com as duas, e ai apareceria como uma supressao que nao
     * suprime.
     *
     * <p>Por isso as tecnicas aqui sao falsas e NAO se excluem: e o unico jeito
     * de exercitar as duas passagens no mesmo tick.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void quemAbaixaVenceQuemLevanta(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Levantadora sobe = new Levantadora("sobe", 1.0F);
        Limitadora desce = new Limitadora("desce", 0.1F);

        comRegistro(List.of(sobe, desce), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirOutputSelecionado(1.0F);

            NenTechniqueService.ativar(jogador, sobe.id());
            exigir(estado.outputMaximo() == 1.0F,
                    "a levantadora nao levantou; veio " + estado.outputMaximo());

            NenTechniqueService.ativar(jogador, desce.id());
            exigir(estado.outputMaximo() == 0.1F,
                    "A limitadora nao venceu a levantadora: teto " + estado.outputMaximo()
                            + ". Se a ordem das duas passagens se inverter, limitar"
                            + " deixa de limitar -- e como Zetsu exclui Ren, nada"
                            + " em jogo acusaria isso.");

            NenTechniqueService.desligar(jogador, desce.id(), StopReason.PLAYER_REQUEST);
            exigir(estado.outputMaximo() == 1.0F,
                    "ao sair a limitadora, o teto devia voltar ao que a levantadora"
                            + " permite; veio " + estado.outputMaximo());
        });

        helper.succeed();
    }

    /** Duas restricoes nao se cancelam: vale a MENOR. */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void entreDoisLimitadoresValeOMenor(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Limitadora frouxa = new Limitadora("frouxa", 0.4F);
        Limitadora dura = new Limitadora("dura", 0.1F);

        comRegistro(List.of(frouxa, dura), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirOutputSelecionado(1.0F);

            NenTechniqueService.ativar(jogador, frouxa.id());
            NenTechniqueService.ativar(jogador, dura.id());

            exigir(estado.outputMaximo() == 0.1F,
                    "Com dois limitadores o teto devia ser o MENOR (0.1) e veio "
                            + estado.outputMaximo() + ". Pegar o maior faria a"
                            + " restricao mais fraca APAGAR a mais forte.");

            NenTechniqueService.desligar(jogador, dura.id(), StopReason.PLAYER_REQUEST);
            exigir(estado.outputMaximo() == 0.4F,
                    "ao sair o limitador duro, devia sobrar o frouxo; veio "
                            + estado.outputMaximo());
        });

        helper.succeed();
    }

    /** A Zetsu DE VERDADE: o Output efetivo vai a zero, e volta ao desligar. */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void zetsuFechaOOutputEDevolveAoDesligar(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Zetsu zetsu = new Zetsu(() -> 1.2D, () -> 3.0D, () -> 0.0D);

        comRegistro(comAsParceiras(zetsu), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirOutputSelecionado(1.0F);
            float antes = estado.outputEfetivo();
            exigir(antes > 0.0F, "o jogador ja nao liberava nada antes de Zetsu");

            NenTechniqueService.ativar(jogador, Zetsu.ID);
            exigir(estado.outputEfetivo() == 0.0F,
                    "Zetsu nao fechou o Output: efetivo " + estado.outputEfetivo()
                            + ". Sem isso ele e furtividade com bonus de"
                            + " regeneracao, e nenhuma desvantagem.");
            exigir(estado.outputSelecionado() == 1.0F,
                    "Zetsu rebaixou a ESCOLHA do jogador, e nao so o teto. Ao"
                            + " desligar, o jogador acharia que perdeu o ajuste.");

            NenTechniqueService.desligar(jogador, Zetsu.ID, StopReason.PLAYER_REQUEST);
            exigir(estado.outputEfetivo() == antes,
                    "o Output nao voltou ao sair de Zetsu: " + estado.outputEfetivo()
                            + " contra " + antes + ". Teto rebaixado por tecnica"
                            + " que ja parou nao da erro: o jogador so nunca mais"
                            + " libera aura.");

            exigir(estado.multiplicadorDeRegeneracao() == 1.0D,
                    "o multiplicador de Zetsu sobreviveu ao desligamento: "
                            + estado.multiplicadorDeRegeneracao());
        });

        helper.succeed();
    }

    /** Zetsu recupera melhor -- e o multiplicador entra enquanto ele esta ligado. */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void zetsuAceleraARegeneracao(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Zetsu zetsu = new Zetsu(() -> 1.2D, () -> 3.0D, () -> 0.0D);

        comRegistro(comAsParceiras(zetsu), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            NenTechniqueService.ativar(jogador, Zetsu.ID);
            exigir(estado.multiplicadorDeRegeneracao() == 3.0D,
                    "Zetsu nao acelerou a regeneracao: multiplicador "
                            + estado.multiplicadorDeRegeneracao());
        });

        helper.succeed();
    }

    /**
     * Zetsu AINDA CUSTA. Item 6 do ADR-010: usar Nen gasta.
     *
     * <p>Se o custo sumisse, Zetsu viraria o estado permanente obvio -- ninguem
     * teria motivo para desligar, e o sintoma seria "todo mundo anda em Zetsu",
     * e nao um erro.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void zetsuDrenaAuraEnquantoLigado(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Zetsu zetsu = new Zetsu(() -> 1.2D, () -> 3.0D, () -> 0.0D);

        comRegistro(comAsParceiras(zetsu), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirAuraMaxima(1000.0D);
            estado.definirAuraAtual(500.0D);

            NenTechniqueService.ativar(jogador, Zetsu.ID);
            double antes = estado.auraAtual();
            NenTechniqueService.tick(jogador, estado);

            exigir(estado.auraAtual() < antes,
                    "Zetsu nao cobrou nada no tick: aura " + estado.auraAtual()
                            + " igual a " + antes + ". Estado sustentado de graca"
                            + " contraria o item 6 do ADR-010.");
        });

        helper.succeed();
    }

    /**
     * A EXCLUSAO FUNCIONA NAS DUAS ORDENS.
     *
     * <p>O erro previsto no CLAUDE.md e exatamente este: "Ten sabe de Zetsu,
     * Zetsu esquece de Ren -- a combinacao ilegal funciona". Uma exclusao
     * declarada pela metade passa numa ordem e falha na outra, e ninguem repara
     * porque a ordem em que se testa costuma ser sempre a mesma.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void zetsuETenSeExcluemNasDuasOrdens(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Ten ten = new Ten(() -> 3.0D, () -> 2.0D, () -> 0.0D);
        Ren ren = new Ren(() -> 10.0D, () -> 1.0D);
        Zetsu zetsu = new Zetsu(() -> 1.2D, () -> 3.0D, () -> 0.0D);

        comRegistro(comAsParceiras(ten, ren, zetsu), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirAuraMaxima(1000.0D);
            estado.definirAuraAtual(1000.0D);

            // Ordem 1: Ten e Ren primeiro, Zetsu depois. As duas caem.
            NenTechniqueService.ativar(jogador, Ten.ID);
            NenTechniqueService.ativar(jogador, Ren.ID);
            NenTechniqueService.ativar(jogador, Zetsu.ID);
            exigir(estado.tecnicasAtivas().equals(Set.of(Zetsu.ID)),
                    "Zetsu nao derrubou Ten e Ren; sobrou " + estado.tecnicasAtivas());

            // Ordem 2: Zetsu primeiro, Ten depois. Zetsu cai.
            NenTechniqueService.ativar(jogador, Ten.ID);
            exigir(estado.tecnicasAtivas().equals(Set.of(Ten.ID)),
                    "Ten nao derrubou Zetsu; ativas " + estado.tecnicasAtivas()
                            + ". A exclusao vale nos DOIS sentidos ou nao vale.");

            // E o teto voltou a ser o de quem ficou, e nao o zero que saiu.
            exigir(estado.outputMaximo() > 0.0F,
                    "o teto zero de Zetsu ficou para tras depois de ele ser"
                            + " derrubado por conflito: " + estado.outputMaximo()
                            + ". Derrubar por conflito e um ponto de saida como"
                            + " outro qualquer, e ele tambem precisa limpar.");
        });

        helper.succeed();
    }

    // ------------------------------------------------- alocacao (ADR-014)

    /** So redistribui. Sem custo, para medir a alocacao sem medir aura. */
    private static final class Concentradora
            implements NenTechnique, RedistribuiAura {
        private final ResourceLocation id;
        private final RegiaoDoCorpo regiao;
        private final float fracao;

        Concentradora(String nome, RegiaoDoCorpo regiao, float fracao) {
            this.id = idDeTeste(nome);
            this.regiao = regiao;
            this.fracao = fracao;
        }

        @Override public ResourceLocation id() { return this.id; }
        @Override public Set<ResourceLocation> incompativeisCom() { return Set.of(); }

        @Override
        public AlocacaoDeAura alocacaoDesejada(FocoDeAura foco) {
            // IGNORA O FOCO de proposito: este duble representa uma tecnica de
            // regiao fixa, como Ko num ponto escolhido antes. E e por isso que
            // o foco e argumento e nao campo -- Gyo le a regiao escolhida, Shu
            // le o braco dominante, e esta aqui nao le nada.
            return AlocacaoDeAura.concentrando(this.regiao, this.fracao);
        }

        @Override
        public TechniqueActivationResult canActivate(ServerPlayer j, NenContext c) {
            return TechniqueActivationResult.aceito();
        }

        @Override public void onActivate(ServerPlayer j, NenContext c) { }
        @Override public void serverTick(ServerPlayer j, NenContext c) { }
        @Override public void onDeactivate(ServerPlayer j, NenContext c, StopReason m) { }
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void aAlocacaoNasceUniformeEVoltaAoDesligar(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Concentradora gyo = new Concentradora("gyo", RegiaoDoCorpo.CABECA, 0.45F);

        comRegistro(List.of(gyo), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            exigir(estado.alocacao().equals(AlocacaoDeAura.uniforme()),
                    "sessao nova nao nasceu com a aura espalhada: " + estado.alocacao()
                            + ". Repouso uniforme e estado DEFINIDO, e nao ausencia"
                            + " de estado.");

            NenTechniqueService.ativar(jogador, gyo.id());
            exigir(estado.alocacao().em(RegiaoDoCorpo.CABECA) > 0.4F,
                    "concentrar nao mudou a alocacao: " + estado.alocacao());
            exigir(estado.alocacao().em(RegiaoDoCorpo.PERNA_DIREITA)
                            < AlocacaoDeAura.uniforme().em(RegiaoDoCorpo.PERNA_DIREITA),
                    "concentrar na cabeca nao TIROU das pernas. Sem a troca, Gyo"
                            + " vira bonus em vez de escolha.");

            NenTechniqueService.desligar(jogador, gyo.id(), StopReason.PLAYER_REQUEST);
            exigir(estado.alocacao().equals(AlocacaoDeAura.uniforme()),
                    "A ALOCACAO FICOU PRESA depois de desligar: " + estado.alocacao()
                            + ". Isto nao da erro nenhum -- o jogador so continua"
                            + " com a aura concentrada para sempre.");
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void entreDuasAlocacoesValeAMaisConcentrada(GameTestHelper helper) {
        // NEM SOMA NEM MEDIA. Somar estouraria a invariante de 1.0 na primeira
        // combinacao. A media seria pior de um jeito mais sutil: Ko com Ken
        // viraria uma concentracao morna -- nem a defesa do corpo inteiro, nem
        // o punho devastador -- que e o oposto do que as duas fazem.
        ServerPlayer jogador = jogadorDesperto(helper);
        Concentradora leve = new Concentradora("leve", RegiaoDoCorpo.CABECA, 0.4F);
        Concentradora ko = new Concentradora("ko", RegiaoDoCorpo.BRACO_DIREITO, 0.9F);

        comRegistro(List.of(leve, ko), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            NenTechniqueService.ativar(jogador, leve.id());
            NenTechniqueService.ativar(jogador, ko.id());

            exigir(estado.alocacao().maisConcentrada() == RegiaoDoCorpo.BRACO_DIREITO,
                    "com as duas ativas, venceu " + estado.alocacao().maisConcentrada()
                            + " em vez do braco. A intencao mais extrema vence.");
            exigir(estado.alocacao().em(RegiaoDoCorpo.BRACO_DIREITO) > 0.85F,
                    "a concentracao foi diluida: " + estado.alocacao());
            exigir(estado.alocacao().soma(),
                    "a alocacao combinada nao fecha em 1.0: " + estado.alocacao());
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void aAlocacaoNaoAtravessaAMorte(GameTestHelper helper) {
        // O terceiro derivado, e o mais facil de esquecer: teto e multiplicador
        // ja tem teste de ponto de saida; a alocacao nasceu depois deles.
        ServerPlayer jogador = jogadorDesperto(helper);
        Concentradora ko = new Concentradora("morte", RegiaoDoCorpo.BRACO_DIREITO, 0.95F);

        comRegistro(List.of(ko), () -> {
            NenTechniqueService.ativar(jogador, ko.id());
            exigir(NenRuntimeService.estadoDe(jogador).alocacao()
                            .em(RegiaoDoCorpo.BRACO_DIREITO) > 0.9F,
                    "a concentracao nao aconteceu; o teste mediria o vazio.");

            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(
                            jogador, jogador, true));

            exigir(NenRuntimeService.estadoDe(jogador).alocacao()
                            .equals(AlocacaoDeAura.uniforme()),
                    "A ALOCACAO ATRAVESSOU A MORTE: "
                            + NenRuntimeService.estadoDe(jogador).alocacao()
                            + ". O jogador renasceria com quase toda a aura num"
                            + " braco, sem nada na tela dizendo por que.");
        });

        helper.succeed();
    }

    // ----------------------------------------------------------- Shu

    private static Shu shuDeTeste() {
        return new Shu(() -> 0.0D, () -> 0.30D);
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void shuRecusaMaoVaziaComMotivoProprio(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        jogador.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                net.minecraft.world.item.ItemStack.EMPTY);

        comRegistro(comAsParceiras(shuDeTeste()), () -> {
            NenTechniqueService.Resultado r = NenTechniqueService.ativar(jogador, Shu.ID);
            exigir(r.estado() == NenTechniqueService.Ativacao.RECUSADA,
                    "Shu ligou com a mao vazia; nao ha o que envolver.");
            exigir(r.motivo().isPresent(),
                    "A recusa veio SEM MOTIVO. 'Aperto a tecla e nao acontece"
                            + " nada' e o pior relato de bug que existe.");
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void shuConcentraNoBracoDaMaoDominante(GameTestHelper helper) {
        // O JOGADOR E CANHOTO DE PROPOSITO, e este detalhe e o teste inteiro.
        //
        // A primeira versao usava o jogador padrao, que e destro -- e ai fixar
        // BRACO_DIREITO no codigo passava igual. Alimentar o portao com esse
        // defeito mostrou: ele aprovava a versao que ignora a mao dominante, e
        // o erro so apareceria para quem joga canhoto, sem dar erro nenhum.
        ServerPlayer jogador = jogadorDesperto(helper);
        // PELO CAMINHO DE VERDADE: a mao dominante e opcao do cliente, e
        // `updateOptions` e por onde o servidor a recebe. Mexer no EntityData
        // direto nao compila -- o campo e protegido -- e tambem seria escrever
        // por fora do caminho que o jogo usa.
        jogador.updateOptions(new net.minecraft.server.level.ClientInformation(
                "en_us", 8, net.minecraft.world.entity.player.ChatVisiblity.FULL,
                true, 0, net.minecraft.world.entity.HumanoidArm.LEFT, false, false));
        exigir(jogador.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT,
                "nao consegui tornar o jogador canhoto; o teste mediria o destro"
                        + " de novo e aprovaria qualquer coisa.");

        jogador.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));

        comRegistro(comAsParceiras(shuDeTeste()), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            NenTechniqueService.ativar(jogador, Shu.ID);

            exigir(estado.alocacao().maisConcentrada() == RegiaoDoCorpo.BRACO_ESQUERDO,
                    "Shu concentrou em " + estado.alocacao().maisConcentrada()
                            + " num jogador CANHOTO. O braco sai da mao"
                            + " dominante; assumir o direito poe a aura no braco"
                            + " errado de parte dos jogadores, e isso nao da erro"
                            + " nenhum.");
            exigir(estado.alocacao().soma(), "a alocacao de Shu nao fecha");
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void shuCaiQuandoAMaoEsvazia(GameTestHelper helper) {
        // A MAO E LIDA A CADA TICK, e nao na ativacao. Guardar o item de quando
        // ligou faria Shu continuar cobrindo uma espada ja guardada -- erro
        // numero 1 da lista do CLAUDE.md, congelar na ativacao o que devia ser
        // consultado depois.
        ServerPlayer jogador = jogadorDesperto(helper);
        jogador.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));

        comRegistro(comAsParceiras(shuDeTeste()), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirAuraMaxima(1000.0D);
            estado.definirAuraAtual(1000.0D);
            NenTechniqueService.ativar(jogador, Shu.ID);
            exigir(estado.tecnicasAtivas().contains(Shu.ID), "Shu nao ligou");

            jogador.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    net.minecraft.world.item.ItemStack.EMPTY);
            NenTechniqueService.tick(jogador, estado);

            exigir(!estado.tecnicasAtivas().contains(Shu.ID),
                    "Shu continuou ligada com a mao vazia. Ela estaria cobrindo"
                            + " um item que nao esta mais ali, cobrando aura por"
                            + " isso.");
            exigir(estado.alocacao().equals(AlocacaoDeAura.uniforme()),
                    "a alocacao ficou presa no braco depois de Shu cair: "
                            + estado.alocacao());
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void trocarDeItemNaoDerrubaShu(GameTestHelper helper) {
        // O outro lado da mesma regra: trocar de item com Shu ligada muda o que
        // esta coberto, e nao desliga nada. Se o item fosse congelado na
        // ativacao, esta troca teria de derrubar a tecnica para nao mentir.
        ServerPlayer jogador = jogadorDesperto(helper);
        jogador.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));

        comRegistro(comAsParceiras(shuDeTeste()), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirAuraMaxima(1000.0D);
            estado.definirAuraAtual(1000.0D);
            NenTechniqueService.ativar(jogador, Shu.ID);

            jogador.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.item.ItemStack(
                            net.minecraft.world.item.Items.IRON_SWORD));
            NenTechniqueService.tick(jogador, estado);

            exigir(estado.tecnicasAtivas().contains(Shu.ID),
                    "trocar de item derrubou Shu; ela cobre o que esta na mao"
                            + " AGORA, e nao o que estava quando ligou.");
        });

        helper.succeed();
    }

    // ------------------------------------------------- defesa (#213)

    /** So protege. Sem custo, sem teto: mede a protecao sem medir o resto. */
    private static final class Protetora
            implements NenTechnique, ProtegeComAura {
        private final ResourceLocation id;
        private final double protecao;

        Protetora(String nome, double protecao) {
            this.id = idDeTeste(nome);
            this.protecao = protecao;
        }

        @Override public ResourceLocation id() { return this.id; }
        @Override public Set<ResourceLocation> incompativeisCom() { return Set.of(); }
        @Override public double protecaoBase() { return this.protecao; }

        @Override
        public TechniqueActivationResult canActivate(ServerPlayer j, NenContext c) {
            return TechniqueActivationResult.aceito();
        }

        @Override public void onActivate(ServerPlayer j, NenContext c) { }
        @Override public void serverTick(ServerPlayer j, NenContext c) { }
        @Override public void onDeactivate(ServerPlayer j, NenContext c, StopReason m) { }
    }

    /**
     * ENTRE DUAS PROTECOES, VALE A MAIOR -- e nao a soma.
     *
     * <p>ESTE TESTE EXISTE PORQUE O DEFEITO E INVISIVEL EM JOGO. Zetsu exclui
     * todas as outras tecnicas que protegem, entao nunca ha duas ativas ao
     * mesmo tempo -- e com uma so, somar e pegar a maior dao o mesmo resultado.
     * A mutacao que troca `max` por `+=` passou por todos os 92 gametests.
     *
     * <p>Somar faria duas tecnicas modestas darem uma protecao que nenhuma das
     * duas promete, e o defeito ficaria dormindo ate a primeira combinacao
     * legitima. Por isso as tecnicas aqui sao falsas e CONVIVEM.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void entreDuasProtecoesValeAMaior(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        Protetora fraca = new Protetora("fraca", 0.2D);
        Protetora forte = new Protetora("forte", 0.6D);

        comRegistro(List.of(fraca, forte), () -> {
            NenTechniqueService.ativar(jogador, fraca.id());
            NenTechniqueService.ativar(jogador, forte.id());

            double protecao = NenDanoService.protecaoDe(
                    NenRuntimeService.estadoDe(jogador).tecnicasAtivas());

            exigir(Math.abs(protecao - 0.6D) < 1.0e-6D,
                    "com 0.2 e 0.6 ativas, a protecao virou " + protecao
                            + ". Somar da 0.8 -- uma protecao que nenhuma das duas"
                            + " promete.");
        });

        helper.succeed();
    }

    /** Uma protecao ZERO explicita apaga a das outras, e nao e ignorada. */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void protecaoZeroExplicitaNaoEhIgnorada(GameTestHelper helper) {
        // Zetsu implementa a interface devolvendo ZERO, e isso significa "eu
        // apago a protecao" -- diferente de nao implementar, que seria "eu nao
        // mexo nisso". Hoje ele exclui as outras, entao a diferenca nao aparece
        // em jogo; ela aparecera na primeira tecnica que combine com ele.
        ServerPlayer jogador = jogadorDesperto(helper);
        Protetora zero = new Protetora("zero", 0.0D);

        comRegistro(List.of(zero), () -> {
            NenTechniqueService.ativar(jogador, zero.id());
            double protecao = NenDanoService.protecaoDe(
                    NenRuntimeService.estadoDe(jogador).tecnicasAtivas());
            exigir(protecao == 0.0D,
                    "uma tecnica de protecao zero devolveu " + protecao);
        });

        helper.succeed();
    }

    // ----------------------------------------------------------- Ko (#213)

    /** Ko de teste, com relogio proprio: o do servidor e compartilhado. */
    private static Ko koDeTeste(int ticks) {
        return new Ko(() -> 0.0D, () -> 0.95D, () -> ticks, NenKoService.INSTANCIA);
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void koExpiraSozinhoComEXPIRED(GameTestHelper helper) {
        // O PRAZO E O QUE SEPARA KO DE GYO. Sem ele, os dois seriam a mesma
        // tecnica com constantes diferentes, e a mais forte tornaria a outra
        // inutil. `StopReason.EXPIRED` existia desde sempre e ninguem podia
        // usa-lo -- Ko e a primeira tecnica com prazo.
        ServerPlayer jogador = jogadorDesperto(helper);

        comRegistro(comAsParceiras(koDeTeste(3)), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirAuraMaxima(1000.0D);
            estado.definirAuraAtual(1000.0D);

            NenTechniqueService.ativar(jogador, Ko.ID);
            exigir(estado.tecnicasAtivas().contains(Ko.ID), "Ko nao ligou");

            NenTechniqueService.tick(jogador, estado);
            NenTechniqueService.tick(jogador, estado);
            exigir(estado.tecnicasAtivas().contains(Ko.ID),
                    "Ko caiu antes do prazo; a janela de acerto some.");

            NenTechniqueService.tick(jogador, estado);
            exigir(!estado.tecnicasAtivas().contains(Ko.ID),
                    "Ko NAO expirou depois do prazo. Sem prazo ele e Gyo com um"
                            + " numero maior, e o jogador poderia ficar com o"
                            + " corpo desprotegido para sempre de graca.");
            exigir(estado.alocacao().equals(AlocacaoDeAura.uniforme()),
                    "a alocacao ficou presa depois de Ko expirar: "
                            + estado.alocacao());
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void koDeixaORestoDoCorpoQuaseNu(GameTestHelper helper) {
        // O RISCO E A TECNICA. Com quase tudo num ponto, um golpe em qualquer
        // outro lugar encontra quase nada -- e e por isso que errar e
        // catastrofico.
        ServerPlayer jogador = jogadorDesperto(helper);

        comRegistro(comAsParceiras(koDeTeste(100)), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirAuraMaxima(1000.0D);
            estado.definirAuraAtual(1000.0D);
            NenTechniqueService.ativar(jogador, Ko.ID);

            float noFoco = FaixaDoCorpo.CABECA.auraDefendendo(estado.alocacao());
            float longe = FaixaDoCorpo.PERNAS.auraDefendendo(estado.alocacao());

            exigir(noFoco > 5.0F,
                    "a regiao concentrada defende " + noFoco + "; com 95% ali,"
                            + " ela devia estar muito acima do repouso (1.0).");
            exigir(longe < 0.1F,
                    "as pernas ainda defendem " + longe + " com o jogador em Ko."
                            + " Sem essa exposicao, Ko nao tem risco nenhum.");
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void koInterrompidoNaoDeixaRelogioVivo(GameTestHelper helper) {
        // QUEM LIGA, DESLIGA -- e aqui vale para o relogio. Um Ko derrubado por
        // conflito deixaria a contagem viva, e o PROXIMO Ko duraria so o que
        // sobrou do anterior. Isso nao da erro: da uma tecnica que as vezes
        // dura menos, e ninguem sabe por que.
        ServerPlayer jogador = jogadorDesperto(helper);

        comRegistro(comAsParceiras(koDeTeste(100)), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirAuraMaxima(1000.0D);
            estado.definirAuraAtual(1000.0D);

            NenTechniqueService.ativar(jogador, Ko.ID);
            NenTechniqueService.tick(jogador, estado);
            exigir(NenKoService.restanteDe(jogador) > 0, "o relogio nao comecou");

            NenTechniqueService.desligar(jogador, Ko.ID, StopReason.PLAYER_REQUEST);
            exigir(NenKoService.restanteDe(jogador) == 0,
                    "o relogio de Ko sobreviveu ao desligamento: "
                            + NenKoService.restanteDe(jogador) + " ticks.");
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void koEGyoNaoConvivem(GameTestHelper helper) {
        // As duas concentram. Juntas, a regra "vence a mais concentrada"
        // decidiria em silencio qual das escolhas do jogador vale.
        Gyo gyo = new Gyo(() -> 0.0D, () -> 0.45D);
        ServerPlayer jogador = jogadorDesperto(helper);

        comRegistro(comAsParceiras(koDeTeste(100), gyo), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirAuraMaxima(1000.0D);
            estado.definirAuraAtual(1000.0D);

            NenTechniqueService.ativar(jogador, Gyo.ID);
            NenTechniqueService.ativar(jogador, Ko.ID);
            exigir(!estado.tecnicasAtivas().contains(Gyo.ID),
                    "Gyo continuou ligado com Ko; ativas: " + estado.tecnicasAtivas());

            NenTechniqueService.ativar(jogador, Gyo.ID);
            exigir(!estado.tecnicasAtivas().contains(Ko.ID),
                    "na ordem inversa, Ko sobreviveu a Gyo. A exclusao vale nos"
                            + " dois sentidos ou nao vale.");
        });

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void koSemContagemEncerraEmVezDeFicarLigado(GameTestHelper helper) {
        // O RAMO DEFENSIVO, e ele nao tinha prova. Um Ko ativo sem prazo
        // registrado e estado quebrado -- pode acontecer se a contagem for
        // limpa por outro caminho. O padrao seguro e ENCERRAR: deixar ligado
        // para sempre uma tecnica que devia durar um golpe daria ao jogador
        // concentracao permanente de graca, e o corpo desprotegido junto.
        ServerPlayer jogador = jogadorDesperto(helper);

        comRegistro(comAsParceiras(koDeTeste(100)), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirAuraMaxima(1000.0D);
            estado.definirAuraAtual(1000.0D);
            NenTechniqueService.ativar(jogador, Ko.ID);

            // O relogio some debaixo da tecnica, e ela continua ativa.
            NenKoService.INSTANCIA.limpar(jogador);
            exigir(estado.tecnicasAtivas().contains(Ko.ID),
                    "Ko caiu antes do tick; o teste mediria outra coisa.");

            NenTechniqueService.tick(jogador, estado);

            exigir(!estado.tecnicasAtivas().contains(Ko.ID),
                    "Ko ficou ligado sem contagem nenhuma. Sem o padrao seguro,"
                            + " um estado quebrado vira concentracao permanente"
                            + " -- e ninguem consegue desligar o que nao tem"
                            + " prazo.");
        });

        helper.succeed();
    }

    // ------------------------------------------ a recusa chega ao jogador

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void aRecusaDaTecnicaChegaComAChaveDela(GameTestHelper helper) {
        // FALSO VERDE EXATO, e ele viveu duas entregas.
        //
        // Shu recusa mao vazia com "nenfoundation.error.shu_sem_item" -- a
        // chave existe, ha traducao nos dois idiomas, e havia gametest
        // conferindo o Resultado. So que `NenPedidoService` traduzia TODA
        // recusa de tecnica em ESTADO_INVALIDO e jogava o texto fora: o jogador
        // de mao vazia lia "seu estado atual nao permite esse pedido", uma
        // mensagem sobre outro problema.
        //
        // O gametest antigo media o Resultado; este mede o que SAI para a rede.
        // A diferenca entre os dois e onde o defeito morava.
        ServerPlayer jogador = jogadorDesperto(helper);
        jogador.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                net.minecraft.world.item.ItemStack.EMPTY);

        comRegistro(comAsParceiras(new Shu(() -> 0.0D, () -> 0.30D)), () -> {
            var recusa = com.darkcontinent.nenfoundation.server.NenPedidoService.validar(
                    jogador,
                    new com.darkcontinent.nenfoundation.network.payload.AtivarTecnicaC2S(Shu.ID));

            exigir(recusa != null, "Shu foi aceita com a mao vazia");
            exigir("nenfoundation.error.shu_sem_item".equals(recusa.chave()),
                    "a recusa chegou com a chave '" + recusa.chave() + "'. A chave"
                            + " da tecnica foi trocada por um motivo generico, e o"
                            + " jogador le uma mensagem sobre outro problema --"
                            + " recusa com motivo errado e pior que recusa sem"
                            + " motivo, porque manda procurar no lugar errado.");
        });

        helper.succeed();
    }

    /**
     * A CLASSE {@code Gyo} DE VERDADE, e nao um duble parecido com ela.
     *
     * <p>POR QUE ESTE TESTE PRECISOU EXISTIR. Toda a prova de alocacao deste
     * arquivo roda sobre {@code Concentradora}, um duble que recebe regiao e
     * fracao prontas no construtor. Ele prova que o SERVICO redistribui -- e
     * nao prova nada sobre Gyo: trocar {@code foco.regiaoEscolhida()} por uma
     * regiao fixa dentro de {@code Gyo.java}, ou ignorar a fracao da config,
     * passava pelos 116 gametests sem reprovar um.
     *
     * <p>As duas coisas que so a classe real pode errar sao exatamente estas:
     * <b>concentrar onde o jogador escolheu</b> e <b>concentrar o quanto a
     * config manda</b>. Uma regiao fixa poria a aura na cabeca de quem pediu a
     * perna, e o sintoma em jogo seria "Gyo nao faz nada" -- porque a defesa
     * melhoraria no lugar errado.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void gyoConcentraOndeOJogadorEscolheuEQuantoAConfigManda(
            GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        final float fracao = 0.62F;
        Gyo gyo = new Gyo(() -> 0.0D, () -> fracao);

        // UMA REGIAO QUE NAO E O PADRAO. Com a CABECA, um Gyo que ignorasse a
        // escolha daria o mesmo resultado e o teste passaria sem tocar no
        // defeito -- o padrao e justamente a cabeca.
        com.darkcontinent.nenfoundation.server.NenGyoService.escolher(
                jogador, RegiaoDoCorpo.PERNA_ESQUERDA);

        comRegistro(comAsParceiras(gyo), () -> {
            var estado = NenRuntimeService.estadoDe(jogador);
            estado.definirAuraMaxima(1000.0D);
            estado.definirAuraAtual(1000.0D);

            NenTechniqueService.ativar(jogador, Gyo.ID);
            exigir(estado.tecnicasAtivas().contains(Gyo.ID),
                    "Gyo nao ligou; o teste mediria o vazio.");

            var alocacao = estado.alocacao();
            exigir(Math.abs(alocacao.em(RegiaoDoCorpo.PERNA_ESQUERDA) - fracao) < 1.0E-4F,
                    "Gyo concentrou " + alocacao.em(RegiaoDoCorpo.PERNA_ESQUERDA)
                            + " na perna escolhida, e a config pediu " + fracao
                            + ". Ou ele ignorou a escolha do jogador, ou ignorou"
                            + " o numero da config -- e nenhuma das duas da erro:"
                            + " a aura so vai parar no lugar errado.");
            exigir(alocacao.em(RegiaoDoCorpo.CABECA) < fracao,
                    "sobrou mais aura na CABECA (" + alocacao.em(RegiaoDoCorpo.CABECA)
                            + ") do que na regiao escolhida. Gyo esta"
                            + " concentrando na regiao PADRAO e nao na pedida.");
        });

        helper.succeed();
    }
}
