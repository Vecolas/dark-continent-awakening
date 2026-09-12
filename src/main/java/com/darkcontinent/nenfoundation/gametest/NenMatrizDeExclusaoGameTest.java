package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.api.event.OrigemDoDespertar;
import com.darkcontinent.nenfoundation.nen.technique.NenTechnique;
import com.darkcontinent.nenfoundation.nen.technique.StopReason;
import com.darkcontinent.nenfoundation.server.NenAwakeningService;
import com.darkcontinent.nenfoundation.server.NenRuntimeService;
import com.darkcontinent.nenfoundation.server.NenTechniqueService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A MATRIZ INTEIRA de exclusao, nas duas ordens, sem lista escrita a mao.
 *
 * <p>POR QUE ISTO EXISTE. A secao B do gate do M4
 * ({@code docs/testing/m4-tecnicas.md}) manda testar as combinacoes invalidas
 * uma a uma, a mao, e avisa que <b>nenhuma linha pode ser pulada</b> -- uma
 * exclusao declarada pela metade passa numa ordem e falha na outra.
 *
 * <p>Aquele roteiro foi escrito quando havia TRES tecnicas, e listava seis
 * casos. Hoje sao SETE tecnicas e <b>quarenta e dois pares ordenados</b>.
 * Pedir isso a mao e pedir que alguem erre: ninguem confere quarenta e dois
 * pares sem pular um, e o pulado e justamente o que ninguem testa de novo.
 *
 * <p>ELE NAO TEM LISTA. A matriz sai do REGISTRO DE PRODUCAO, entao a oitava
 * tecnica entra nesta prova sozinha, no dia em que for registrada -- sem
 * ninguem lembrar de vir aqui. Uma lista escrita a mao viraria a segunda fonte
 * da mesma verdade, e envelheceria igual ao roteiro.
 *
 * <p>O que ele NAO substitui: ver a roda piscar e a tecnica cair na tela. Isso
 * continua sendo olho humano, e continua no roteiro.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenMatrizDeExclusaoGameTest {

    private static final String TEMPLATE = "empty";

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new GameTestAssertException(mensagem);
        }
    }

    private static ServerPlayer prontoParaTudo(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);
        var estado = NenRuntimeService.estadoDe(jogador);
        // AURA DE SOBRA: o assunto aqui e exclusao, e nao custo. Sem isto, uma
        // tecnica cara cairia por falta de aura e o teste culparia a matriz.
        estado.definirAuraMaxima(100_000.0D);
        estado.definirAuraAtual(100_000.0D);

        // UM ITEM NA MAO, e isto foi a matriz cobrando na primeira execucao:
        // ela reprovou dizendo que Gyo e Shu "nao se recusam e mesmo assim nao
        // ficaram as duas ligadas". Nao era exclusao -- era Shu recusando mao
        // vazia, com o motivo proprio dela.
        //
        // Uma tecnica pode ter pre-condicao propria, e um jogador de teste que
        // nao as satisfaz faz a matriz acusar exclusao onde ha recusa. O
        // cenario tem de ser de alguem que consegue ligar TUDO.
        jogador.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(
                        net.minecraft.world.item.Items.IRON_SWORD));
        return jogador;
    }

    /** Todas as tecnicas registradas em producao, em ordem estavel. */
    private static List<ResourceLocation> todas() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (NenTechnique tecnica : NenTechniqueService.registro().todas()) {
            ids.add(tecnica.id());
        }
        ids.sort(java.util.Comparator.comparing(ResourceLocation::toString));
        return ids;
    }

    private static Set<ResourceLocation> recusadasPor(ResourceLocation id) {
        return NenTechniqueService.registro().porId(id)
                .map(NenTechnique::incompativeisCom)
                .orElse(Set.of());
    }

    private static void desligarTudo(ServerPlayer jogador) {
        NenTechniqueService.desligarTodas(jogador, StopReason.PLAYER_REQUEST);
    }

    /**
     * Para cada par ordenado: ligar A, depois B, e conferir quem sobrou.
     *
     * <p>A REGRA: se B recusa A, entao A tem de ter caido e B ficado. Se nao se
     * recusam, os dois ficam. Nao ha terceiro caso -- e se houvesse, ele seria
     * um estado que ninguem desenhou.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    @PrefixGameTestTemplate(false)
    public static void aMatrizInteiraNasDuasOrdens(GameTestHelper helper) {
        ServerPlayer jogador = prontoParaTudo(helper);
        var estado = NenRuntimeService.estadoDe(jogador);
        List<ResourceLocation> ids = todas();

        exigir(ids.size() >= 2,
                "o registro de producao tem " + ids.size() + " tecnica(s); nao ha"
                        + " matriz nenhuma para provar.");

        int pares = 0;
        for (ResourceLocation primeira : ids) {
            for (ResourceLocation segunda : ids) {
                if (primeira.equals(segunda)) {
                    continue;
                }
                pares++;
                desligarTudo(jogador);

                NenTechniqueService.ativar(jogador, primeira);
                exigir(estado.tecnicasAtivas().contains(primeira),
                        "a tecnica " + primeira + " nao ligou sozinha, com aura de"
                                + " sobra. O teste nao chega nem a medir exclusao.");

                NenTechniqueService.ativar(jogador, segunda);
                Set<ResourceLocation> ativas = estado.tecnicasAtivas();

                boolean segundaRecusaPrimeira = recusadasPor(segunda).contains(primeira);
                boolean primeiraRecusaSegunda = recusadasPor(primeira).contains(segunda);

                if (segundaRecusaPrimeira || primeiraRecusaSegunda) {
                    exigir(!ativas.contains(primeira),
                            "COMBINACAO ILEGAL ATIVA: " + primeira + " sobreviveu a"
                                    + " ativacao de " + segunda + ". Ativas: " + ativas
                                    + ". Uma exclusao que so vale numa das ordens"
                                    + " deixa a combinacao proibida acontecer para"
                                    + " quem apertar os botoes na ordem errada.");
                    exigir(ativas.contains(segunda),
                            "a ultima escolha do jogador (" + segunda + ") nao ficou"
                                    + " ligada. Ativas: " + ativas);
                } else {
                    exigir(ativas.contains(primeira) && ativas.contains(segunda),
                            primeira + " e " + segunda + " nao se recusam, e mesmo"
                                    + " assim nao ficaram as duas ligadas. Ativas: "
                                    + ativas);
                }
            }
        }

        desligarTudo(jogador);
        exigir(pares == ids.size() * (ids.size() - 1),
                "conferi " + pares + " pares, e a matriz tem "
                        + (ids.size() * (ids.size() - 1)) + ".");

        helper.succeed();
    }

    // ------------------------------------------------------------------
    //
    // AQUI HAVIA MAIS DOIS TESTES, e eles foram removidos por serem CARIMBO.
    //
    // Um conferia que a exclusao e simetrica; o outro, que ninguem recusa a si
    // mesmo nem aponta para tecnica inexistente. Alimentei os dois com defeito,
    // e NENHUM dos dois chegou a rodar: o selamento do registro recusa o
    // servidor na subida, antes de qualquer gametest.
    //
    // Regua que nao pode reprovar e carimbo. A propriedade ja tem dono --
    // `RegistroDeTecnicas.selar` -- e ele falha mais cedo e com mensagem
    // melhor. Duplicar aqui seria a segunda fonte da mesma verdade.
    //
    // O que ficou e o teste de COMPORTAMENTO: declaracao simetrica nao garante
    // que o servico derrube a tecnica certa na ordem certa, e e isso que o
    // selamento nao ve.
}
