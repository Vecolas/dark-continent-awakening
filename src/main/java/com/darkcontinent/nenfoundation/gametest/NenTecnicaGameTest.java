package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.technique.NenContext;
import com.darkcontinent.nenfoundation.nen.technique.NenTechnique;
import com.darkcontinent.nenfoundation.nen.technique.RegistroDeTecnicas;
import com.darkcontinent.nenfoundation.nen.technique.StopReason;
import com.darkcontinent.nenfoundation.nen.technique.TechniqueActivationResult;
import com.darkcontinent.nenfoundation.server.NenProfileService;
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
}
