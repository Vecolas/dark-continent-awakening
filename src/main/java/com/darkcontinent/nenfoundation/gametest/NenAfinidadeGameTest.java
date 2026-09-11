package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.api.event.OrigemDoDespertar;
import com.darkcontinent.nenfoundation.nen.category.Afinidade;
import com.darkcontinent.nenfoundation.nen.category.MatrizDeAfinidade;
import com.darkcontinent.nenfoundation.nen.category.NenAffinity;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.server.NenAwakeningService;
import com.darkcontinent.nenfoundation.server.NenCategoryService;
import com.darkcontinent.nenfoundation.server.NenProfileService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A matriz de afinidade com o jogo de pe.
 *
 * <p>O teste unitario le o JSON do disco e o valida. O que SO existe aqui: que
 * o carregador esta REGISTRADO e que o datapack embutido do mod passou pelo
 * pipeline de verdade do Minecraft. Um arquivo perfeito num carregador que
 * ninguem registrou produz exatamente o mesmo verde no JUnit -- e afinidade
 * zero no jogo.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenAfinidadeGameTest {

    private static final String TEMPLATE = "empty";

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new GameTestAssertException(mensagem);
        }
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void oDatapackEmbutidoCarregouDeVerdade(GameTestHelper helper) {
        exigir(NenAffinity.carregada(),
                "Nenhuma matriz de afinidade foi carregada ao subir o servidor."
                        + " O arquivo pode estar certo e o carregador nao estar"
                        + " registrado -- e o JUnit nao ve diferenca entre os dois"
                        + " casos.");

        // Valores da tabela classica, conferidos contra o dado distribuido.
        // Nao sao numeros inventados aqui: se o datapack mudar, este teste
        // reprova e obriga a decisao a ser consciente.
        Afinidade propria = NenAffinity.entre(NenCategory.EMISSION, NenCategory.EMISSION);
        exigir(propria.effectiveness() == 1.0D,
                "a propria categoria devia render 1.0, veio " + propria.effectiveness());

        Afinidade adjacente =
                NenAffinity.entre(NenCategory.EMISSION, NenCategory.ENHANCEMENT);
        exigir(adjacente.effectiveness() == 0.8D,
                "adjacente devia render 0.8, veio " + adjacente.effectiveness());

        Afinidade oposta = NenAffinity.entre(NenCategory.EMISSION, NenCategory.CONJURATION);
        exigir(oposta.effectiveness() == 0.4D,
                "oposta devia render 0.4, veio " + oposta.effectiveness());

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void especializacaoNaoSaiDaMatriz(GameTestHelper helper) {
        exigir(NenAffinity.carregada(), "sem matriz carregada.");

        MatrizDeAfinidade matriz = NenAffinity.matriz().orElseThrow();
        exigir(!matriz.comuns().containsKey(NenCategory.SPECIALIZATION),
                "Specialization apareceu como LINHA da matriz distribuida.");
        for (Map.Entry<NenCategory, Map<NenCategory, Afinidade>> linha
                : matriz.comuns().entrySet()) {
            exigir(!linha.getValue().containsKey(NenCategory.SPECIALIZATION),
                    "Specialization apareceu como ALVO na linha de "
                            + linha.getKey().getSerializedName() + ".");
        }

        // E ela continua respondendo -- pela regra propria, e nao pela matriz.
        exigir(NenAffinity.entre(NenCategory.SPECIALIZATION, NenCategory.SPECIALIZATION)
                        .effectiveness() == 1.0D,
                "Specialist na propria categoria nao respondeu 1.0.");

        helper.succeed();
    }

    /**
     * O criterio de aceite da issue: trocar a matriz nao mexe em jogador.
     *
     * <p>ATE ONDE ISTO PROVA: ele exercita a troca de matriz, que e o que uma
     * recarga de datapack faz ao estado do mod. Ele NAO executa um
     * {@code /reload} de verdade -- isso fica na QA manual, e esta declarado
     * como tal no PR.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void trocarAMatrizNaoMexeNoPerfil(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);
        NenCategoryService.atribuir(jogador, NenCategory.CONJURATION);
        NenCategoryService.revelar(jogador);

        PersistentNenData antes = NenProfileService.ler(jogador);
        MatrizDeAfinidade original = NenAffinity.matriz().orElseThrow();

        try {
            NenAffinity.instalar(outraMatriz());

            PersistentNenData depois = NenProfileService.ler(jogador);
            exigir(antes.equals(depois),
                    "O perfil do jogador mudou ao trocar a matriz de afinidade."
                            + " ANTES: " + antes + " DEPOIS: " + depois);
            exigir(depois.category() == NenCategory.CONJURATION,
                    "a categoria mudou: " + depois.category());
            exigir(depois.categoryRevealed(), "a revelacao foi perdida.");

            // E a matriz nova REALMENTE entrou -- sem isto, o teste passaria
            // tambem se `instalar` nao fizesse nada, que e o jeito mais facil
            // de "nao mexer no perfil".
            exigir(NenAffinity.entre(NenCategory.EMISSION, NenCategory.EMISSION)
                            .effectiveness() == 0.25D,
                    "a matriz nova nao entrou; o teste estaria medindo o nada.");
        } finally {
            // QUEM LIGA, DESLIGA. Deixar a matriz de teste instalada
            // contaminaria os outros gametests do mesmo servidor.
            NenAffinity.instalar(original);
        }

        helper.succeed();
    }

    private static MatrizDeAfinidade outraMatriz() {
        Map<NenCategory, Map<NenCategory, Afinidade>> comuns = new LinkedHashMap<>();
        for (NenCategory origem : MatrizDeAfinidade.COMUNS) {
            Map<NenCategory, Afinidade> linha = new LinkedHashMap<>();
            for (NenCategory alvo : MatrizDeAfinidade.COMUNS) {
                linha.put(alvo, new Afinidade(0.25D, 0.25D, 0.25D));
            }
            comuns.put(origem, linha);
        }
        return new MatrizDeAfinidade(comuns,
                new MatrizDeAfinidade.RegraDeEspecializacao(
                        new Afinidade(1.0D, 1.0D, 1.0D),
                        new Afinidade(0.0D, 0.0D, 0.0D),
                        new Afinidade(0.5D, 0.5D, 0.5D)),
                Optional.empty());
    }
}
