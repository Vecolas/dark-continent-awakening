package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A alocacao atravessando ate a tela.
 *
 * <p>ELA EXISTIA E ERA INVISIVEL. Gyo e Shu ja mudavam a alocacao no servidor
 * ha uma sessao inteira, e nada chegava ao cliente -- {@code definirAlocacao}
 * era o unico derivado que nao chamava {@code marcarAlterado}, entao o delta
 * nunca saia por causa dela. Nao dava erro: dava uma tecnica que nao aparece.
 */
class AlocacaoAteATelaTest {

    // ------------------------------------------------- ida e volta pela rede

    @Test
    @DisplayName("a alocacao sobrevive a ida e volta, regiao por regiao")
    void idaEVoltaPreservaCadaRegiao() {
        AlocacaoDeAura original = AlocacaoDeAura.concentrando(RegiaoDoCorpo.BRACO_DIREITO, 0.7F);

        float[] fracoes = new float[RegiaoDoCorpo.values().length];
        for (int i = 0; i < fracoes.length; i++) {
            fracoes[i] = original.em(RegiaoDoCorpo.values()[i]);
        }
        Optional<AlocacaoDeAura> voltou = AlocacaoDeAura.deFracoes(fracoes);

        assertTrue(voltou.isPresent(), "a alocacao nao sobreviveu a leitura");
        for (RegiaoDoCorpo r : RegiaoDoCorpo.values()) {
            assertEquals(original.em(r), voltou.get().em(r), 1.0e-6F,
                    "a regiao " + r + " chegou diferente. Um deslocamento de uma"
                            + " posicao poria a aura do braco na perna, e nada"
                            + " acusaria isso.");
        }
    }

    @Test
    @DisplayName("fracoes que nao fecham sao recusadas, e nao corrigidas")
    void pacoteTortoNaoVira() {
        // A LEITURA E DEFENSIVA, mas nao inventiva: ela recusa, e quem chama
        // decide o padrao. Normalizar aqui esconderia um emissor quebrado
        // atras de numeros plausiveis.
        assertTrue(AlocacaoDeAura.deFracoes(new float[] {0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F})
                .isEmpty(), "aceitou fracoes que somam 3.0");
        assertTrue(AlocacaoDeAura.deFracoes(new float[] {1.0F, 0.0F, 0.0F}).isEmpty(),
                "aceitou um vetor com menos regioes que o enum");
        assertTrue(AlocacaoDeAura.deFracoes(
                new float[] {Float.NaN, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F}).isEmpty(),
                "NaN atravessou a leitura de rede");
        assertTrue(AlocacaoDeAura.deFracoes(null).isEmpty());
    }

    // ----------------------------------------------------- virar desenho

    @Test
    @DisplayName("a projecao normaliza pelo MAIOR, e nao usa a fracao crua")
    void projecaoNormaliza() {
        // A alocacao soma 1.0, entao em repouso cada regiao vale 0,167. Usar
        // isso cru como intensidade desenharia uma aura fraquissima onde ela
        // deveria estar normal -- e o sintoma seria "a aura sumiu", sem que
        // nada tivesse mudado no servidor.
        AuraDistribution repouso = AuraDistribution.daAlocacao(AlocacaoDeAura.uniforme());
        for (AuraBodyRegion r : AuraBodyRegion.values()) {
            assertEquals(1.0F, repouso.intensidade(r), 1.0e-5F,
                    "em repouso a regiao " + r + " saiu com intensidade "
                            + repouso.intensidade(r));
        }
    }

    @Test
    @DisplayName("concentrar aparece na projecao: a regiao forte vence as outras")
    void concentracaoApareceNaTela() {
        AuraDistribution gyo = AuraDistribution.daAlocacao(
                AlocacaoDeAura.concentrando(RegiaoDoCorpo.CABECA, 0.45F));

        assertEquals(1.0F, gyo.head(), 1.0e-5F, "a regiao concentrada nao virou a mais forte");
        assertTrue(gyo.leftLeg() < gyo.head(),
                "a perna ficou tao forte quanto a cabeca concentrada; a"
                        + " concentracao nao apareceria na tela.");
        assertTrue(gyo.leftLeg() > 0.0F,
                "as outras regioes zeraram. Gyo tira aura delas, nao apaga.");
    }

    @Test
    @DisplayName("Zetsu nao vira NaN na projecao")
    void zetsuNaoQuebraAProjecao() {
        // A distribuicao de Zetsu e toda zero, e normalizar pelo maior
        // dividiria por zero. NaN numa coordenada de particula nao lanca: ela
        // so nao aparece, em lugar nenhum, para sempre.
        AuraDistribution z = AuraDistribution.daAlocacao(
                AlocacaoDeAura.concentrando(RegiaoDoCorpo.CABECA, 0.0F));
        assertTrue(Float.isFinite(z.head()) && Float.isFinite(z.torso()),
                "NaN vazou para a projecao");
    }

    // ------------------------------------------ a particula segue a aura

    @Test
    @DisplayName("com a cabeca concentrada, as particulas sobem para a cabeca")
    void particulaSegueAConcentracao() {
        AuraDistribution naCabeca = AuraDistribution.daAlocacao(
                AlocacaoDeAura.concentrando(RegiaoDoCorpo.CABECA, 0.9F));

        var ancora = EmissorDeParticulasDeAura.ancoraSorteada(naCabeca, 0.1F, 0);
        assertEquals(AuraBodyRegion.HEAD, ancora.regiao(),
                "a ancora " + ancora + " nao pertence a cabeca com a aura concentrada nela.");
        assertTrue(EmissorDeParticulasDeAura.pontoLocalDa(ancora, false).y() > 1.45D,
                "a faisca da cabeca nao nasceu no alto do modelo");
    }

    @Test
    @DisplayName("com as pernas concentradas, as particulas ficam embaixo")
    void particulaSegueAsPernas() {
        AuraDistribution naPerna = AuraDistribution.daAlocacao(
                AlocacaoDeAura.concentrando(RegiaoDoCorpo.PERNA_DIREITA, 0.9F));

        var ancora = EmissorDeParticulasDeAura.ancoraSorteada(naPerna, 0.95F, 0);
        assertEquals(AuraBodyRegion.RIGHT_LEG, ancora.regiao(),
                "a ancora " + ancora + " nao pertence a perna direita");
        assertTrue(EmissorDeParticulasDeAura.pontoLocalDa(ancora, false).y() < 0.8D,
                "a faisca da perna nao nasceu embaixo no modelo");
    }

    @Test
    @DisplayName("distribuicao toda zero usa ancora segura")
    void distribuicaoZeroTemAncoraSegura() {
        var ancora = EmissorDeParticulasDeAura.ancoraSorteada(
                AuraDistribution.zetsu(), 0.5F, 0);
        assertEquals(AuraBodyRegion.TORSO, ancora.regiao());
    }


    // -------------------------------------- o defeito que estava vivo

    @Test
    @DisplayName("mudar a alocacao SUJA o runtime, senao ela nunca e enviada")
    void mudarAlocacaoSujaORuntime() {
        // ESTE E O DEFEITO QUE ESTAVA VIVO NA MAIN. `definirAlocacao` era o
        // unico derivado que nao chamava `marcarAlterado` -- teto, multiplicador,
        // aura, tecnicas ativas, todos chamavam. Sem a marca, o delta nunca sai
        // por causa da alocacao, e Gyo e Shu mudavam algo que nao chegava a
        // lugar nenhum.
        //
        // Nao dava erro. Dava uma tecnica que nao aparece.
        var estado = new com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState();
        estado.confirmarSincronizacao(estado.revisao());
        assertTrue(!estado.auraSuja(), "o estado ja nasceu sujo; o teste mediria o vazio");

        estado.definirAlocacao(AlocacaoDeAura.concentrando(RegiaoDoCorpo.CABECA, 0.45F));

        assertTrue(estado.auraSuja(),
                "mudar a alocacao NAO sujou o runtime. O delta so sai quando o"
                        + " estado esta sujo, entao a concentracao ficaria so no"
                        + " servidor -- exatamente o que acontecia antes deste"
                        + " conserto.");
    }

    @Test
    @DisplayName("gravar a MESMA alocacao nao suja nada")
    void alocacaoIgualNaoSuja() {
        // O recalculo roda a cada ativacao e a cada tick de mudanca. Marcar
        // sempre faria o delta sair por nada, e o custo apareceria como
        // trafego que ninguem sabe explicar -- so num servidor cheio.
        var estado = new com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState();
        estado.definirAlocacao(AlocacaoDeAura.concentrando(RegiaoDoCorpo.TRONCO, 0.5F));
        estado.confirmarSincronizacao(estado.revisao());

        estado.definirAlocacao(AlocacaoDeAura.concentrando(RegiaoDoCorpo.TRONCO, 0.5F));

        assertTrue(!estado.auraSuja(),
                "regravar a mesma alocacao sujou o runtime; o delta sairia a cada"
                        + " recalculo sem nada ter mudado.");
    }
}
