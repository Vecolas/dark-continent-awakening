package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A parte do VFX que da para provar sem o jogo de pe.
 *
 * <p>O QUE ESTES TESTES NAO PROVAM: que alguma coisa aparece na tela. Isso e
 * {@code runClient} a mao, e esta dito no relato e em
 * {@code o-que-nao-provamos.md}. O que eles cobrem e a aritmetica -- e e nela
 * que moram os defeitos que nao dao erro: transicao que estala, tecnica
 * discreta que nunca aparece, e aura do mundo anterior que fica na tela.
 */
class VfxDeAuraLigadoTest {

    // ------------------------------------------------------- precedencia

    @Test
    @DisplayName("Ren ganha de Ten, porque os dois convivem")
    void renGanhaDeTen() {
        assertEquals(AuraVisualMode.REN, ModoVisualDeTecnica.de(Set.of(Ten.ID, Ren.ID)),
                "Com Ten e Ren ligados a tela mostrou o menor dos dois. Esta"
                        + " disputa acontece o tempo todo em jogo: Ten e Ren"
                        + " convivem de proposito.");
        assertEquals(AuraVisualMode.TEN, ModoVisualDeTecnica.de(Set.of(Ten.ID)));
        assertEquals(AuraVisualMode.REN, ModoVisualDeTecnica.de(Set.of(Ren.ID)));
    }

    @Test
    @DisplayName("Zetsu ganha de todo mundo: supressao que perde nao suprime")
    void zetsuGanhaDeTodos() {
        // Hoje Zetsu EXCLUI Ten e Ren, entao esta combinacao nao acontece em
        // jogo -- e por isso a regra precisa de teste: ela ficaria dormindo ate
        // a primeira tecnica que combine com ele, e ai apareceria como "as
        // vezes Zetsu brilha".
        assertEquals(AuraVisualMode.ZETSU,
                ModoVisualDeTecnica.de(Set.of(Ten.ID, Ren.ID, Zetsu.ID)));
    }

    @Test
    @DisplayName("nada ligado, e tecnica desconhecida, nao acendem nada")
    void desconhecidaNaoAcende() {
        assertEquals(AuraVisualMode.OFF, ModoVisualDeTecnica.de(Set.of()));
        assertEquals(AuraVisualMode.OFF, ModoVisualDeTecnica.de(null));
        assertEquals(AuraVisualMode.OFF, ModoVisualDeTecnica.de(
                        Set.of(ResourceLocation.fromNamespaceAndPath("outromod", "hatsu_x"))),
                "Uma tecnica que este codigo nao conhece ganhou um visual."
                        + " Inventar efeito para ela e mostrar ao jogador algo que"
                        + " nao significa nada.");
    }

    // -------------------------------------------------------- transicao

    @Test
    @DisplayName("na metade da transicao, a aura esta na metade")
    void transicaoSegueACurvaNominal() {
        // ESTE TESTE ACHOU UM DEFEITO DE VERDADE -- e a PRIMEIRA versao dele
        // nao achava nada, o que vale registrar.
        //
        // O controlador interpolava de `atual` para `alvo` e reescrevia
        // `atual`, entao cada passo saia de um ponto ja movido. A primeira
        // tentativa de teste procurou um "estalo" no ultimo tick e passou
        // mesmo com o defeito: o erro nao e um salto no fim.
        //
        // O erro e a CURVA INTEIRA. Interpolando do valor movido, cada passo
        // cobre uma fracao do que RESTA, e a aura chega perto do alvo em
        // metade do tempo pedido -- uma transicao de 20 ticks termina em 12, e
        // o que sobra rasteja. Com a origem congelada, o smoothstep vale o que
        // diz: na metade dos passos, exatamente metade do caminho.
        AuraVisualController c = new AuraVisualController();
        c.receber(AuraVisualMode.REN, 1.0F);

        // A CONTAGEM SAI DA TABELA, e nao de um 10 escrito a mao: cada troca
        // tem duracao propria desde o AV3, e um numero fixo aqui voltaria a ser
        // a segunda fonte de verdade que a tabela acabou de eliminar.
        int metade = AuraTransicao.de(AuraVisualMode.OFF, AuraVisualMode.REN).ticks() / 2;
        float anterior = 0.0F;
        for (int i = 0; i < metade; i++) {
            float agora = c.avancar(1.0F).intensity();
            assertTrue(agora >= anterior, "a intensidade andou para tras no passo " + i);
            anterior = agora;
        }

        assertEquals(0.5F, anterior, 0.02F,
                "Na metade dos passos a aura estava em " + anterior + ", e nao em"
                        + " 0.5. A curva nao e a que o smoothstep promete: a"
                        + " transicao corre demais no comeco e rasteja no fim.");

        for (int i = 0; i < 40; i++) {
            anterior = c.avancar(1.0F).intensity();
        }
        assertEquals(1.0F, anterior, 1.0e-5F, "a transicao nao chegou ao alvo");
    }

    @Test
    @DisplayName("a cor da tecnica atravessa ate o estado visual")
    void aCorChegaAoEstado() {
        // Antes, o controlador gravava 0xFFFFFFFF fixo e ignorava qualquer cor.
        // As particulas sairiam todas brancas, e Ten, Ren e Zetsu ficariam
        // indistinguiveis -- sem nenhum erro em lugar nenhum.
        AuraVisualController c = new AuraVisualController();
        int laranja = 0xFF_F0_8A_30;
        c.receber(AuraVisualMode.REN, 1.0F, AuraDistribution.uniforme(), laranja, laranja);

        assertEquals(laranja, concluir(c).primaryColor(),
                "a cor passada no comando nao chegou ao estado visual.");
    }

    // ---------------------------------------------------------- sessao

    @Test
    @DisplayName("tick sem mudanca NAO reinicia a transicao")
    void tickSemMudancaNaoReinicia() {
        // Se a sessao reenviasse o comando todo tick, a transicao voltaria a
        // zero a cada tick e a aura ficaria congelada no primeiro quadro --
        // para sempre, e sem erro nenhum.
        SessaoDeVfxDeAura sessao = new SessaoDeVfxDeAura();
        for (int i = 0; i < 10; i++) {
            sessao.aoTick(Set.of(Ren.ID), 1.0F, 0xFF112233, 1.0F);
        }
        assertEquals(1.0F, sessao.estado().intensity(), 1.0e-5F,
                "Depois de dez ticks com passo de 0.2 a transicao devia ter"
                        + " terminado. Ela reiniciou a cada tick: a aura ficaria"
                        + " presa no comeco da animacao.");
    }

    @Test
    @DisplayName("limpar apaga tudo, e e o que impede a aura do mundo anterior")
    void limparApagaTudo() {
        SessaoDeVfxDeAura sessao = new SessaoDeVfxDeAura();
        // O MODO SO TROCA QUANDO A TRANSICAO TERMINA, e desde o AV3 ela leva
        // varios ticks. Um tick so deixa o estado ainda em OFF -- o que e
        // correto, e nao era o que este teste queria exercitar.
        for (int i = 0; i < 40; i++) {
            sessao.aoTick(Set.of(Ren.ID), 1.0F, 0xFF112233, 1.0F);
        }
        assertTrue(sessao.estado().enabled(), "a aura nao ligou; nao ha o que limpar");

        sessao.limpar();
        assertEquals(AuraVisualMode.OFF, sessao.estado().mode());
        assertTrue(!sessao.estado().enabled(),
                "A aura sobreviveu ao limpar. Ao trocar de servidor, o jogador"
                        + " continuaria com o efeito do mundo anterior ate o"
                        + " primeiro delta novo -- e para sempre, se ele nao"
                        + " despertar no servidor novo.");
    }

    @Test
    @DisplayName("NaN vindo do output nao explode o tick do cliente")
    void nanNaoExplode() {
        // O output ja atravessou o protocolo como NaN uma vez neste projeto. O
        // controlador valida e LANCA -- e uma excecao no tick do cliente leva a
        // tela junto.
        SessaoDeVfxDeAura sessao = new SessaoDeVfxDeAura();
        sessao.aoTick(Set.of(Ren.ID), Float.NaN, 0xFF112233, Float.NaN);
        assertTrue(Float.isFinite(sessao.estado().intensity()),
                "NaN atravessou ate o estado visual.");
    }

    // -------------------------------------------------------- emissao

    @Test
    @DisplayName("Ten e discreto, mas nao invisivel")
    void tenAparecePorSorteio() {
        // Ten pede uma fracao de particula por tick. Arredondar daria ZERO para
        // sempre: a tecnica mais basica do jogo nao teria efeito nenhum, e o
        // sintoma seria "Ten nao faz nada".
        AuraVisualState ten = estadoDe(AuraVisualMode.TEN, 1.0F);
        int comSorteioBaixo = EmissorDeParticulasDeAura.quantasEmitir(
                PerfilDoDisco.de(AuraVisualMode.TEN), ten, 1.0D, 0.0F);
        assertTrue(comSorteioBaixo >= 1,
                "Com o sorteio favoravel Ten devia emitir ao menos uma particula;"
                        + " emitiu " + comSorteioBaixo + ".");
        assertEquals(0, EmissorDeParticulasDeAura.quantasEmitir(
                        PerfilDoDisco.de(AuraVisualMode.TEN), ten, 1.0D, 0.999F),
                "Com o sorteio desfavoravel Ten deve ficar quieto -- e o que o"
                        + " torna discreto em vez de constante.");
    }

    @Test
    @DisplayName("Ren emite mais que Ten -- medido na TAXA, e nao num sorteio")
    void renEmiteMaisQueTen() {
        // ESTE TESTE MUDOU DE FORMA NO AV3, e vale dizer por que.
        //
        // Ele perguntava se Ren emitia mais que Ten NUM sorteio fixo (0.5), com
        // a justificativa de que "se os dois saem iguais, o jogador nao
        // distingue o estado". Naquela epoca a particula ERA a aura, e a
        // justificativa estava certa.
        //
        // Com a shell e os filamentos carregando a identidade, a particula virou
        // ACABAMENTO (#186) e a contagem caiu: nos dois estados ela agora e
        // fracionaria, e num sorteio especifico os dois podem dar zero. Isso nao
        // e regressao -- e o efeito pretendido.
        //
        // A PROPRIEDADE QUE CONTINUA VALENDO e sobre a TAXA, e nao sobre um
        // sorteio: ao longo do tempo, Ren precisa emitir estritamente mais. Esta
        // forma e mais forte que a anterior, porque cobre o intervalo inteiro em
        // vez de um ponto.
        int ten = 0;
        int ren = 0;
        for (int i = 0; i < 100; i++) {
            float sorteio = i / 100.0F;
            ten += EmissorDeParticulasDeAura.quantasEmitir(
                    PerfilDoDisco.de(AuraVisualMode.TEN),
                    estadoDe(AuraVisualMode.TEN, 1.0F), 1.0D, sorteio);
            ren += EmissorDeParticulasDeAura.quantasEmitir(
                    PerfilDoDisco.de(AuraVisualMode.REN),
                    estadoDe(AuraVisualMode.REN, 1.0F), 1.0D, sorteio);
        }
        assertTrue(ren > ten,
                "Ren (" + ren + " em 100 ticks) nao emite mais que Ten (" + ten + ")."
                        + " A particula e acabamento, mas acabamento de Ren ainda tem"
                        + " de ser mais denso que o de Ten.");
        assertTrue(ten > 0,
                "Ten parou de emitir por completo. Acabamento discreto nao e"
                        + " acabamento ausente -- e a faisca ocasional continua sendo"
                        + " parte da linguagem visual.");
    }

    @Test
    @DisplayName("Zetsu e densidade zero nao emitem nada")
    void zetsuESilencioNaoEmitem() {
        assertEquals(0, EmissorDeParticulasDeAura.quantasEmitir(
                        PerfilDoDisco.de(AuraVisualMode.ZETSU),
                        estadoDe(AuraVisualMode.ZETSU, 1.0F), 1.0D, 0.0F),
                "Zetsu emitiu particula. Ele e o estado em que o jogador SOME do"
                        + " radar; brilhar seria o contrario do que ele faz.");
        assertEquals(0, EmissorDeParticulasDeAura.quantasEmitir(
                        PerfilDoDisco.de(AuraVisualMode.REN),
                        estadoDe(AuraVisualMode.REN, 1.0F), 0.0D, 0.0F),
                "densidade zero na config devia desligar o desenho por completo.");
        assertEquals(0, EmissorDeParticulasDeAura.quantasEmitir(
                        PerfilDoDisco.de(AuraVisualMode.TEN), null, 1.0D, 0.0F));
        assertEquals(0, EmissorDeParticulasDeAura.quantasEmitir(
                        null, estadoDe(AuraVisualMode.REN, 1.0F), 1.0D, 0.0F),
                "sem perfil carregado o emissor tem de ficar quieto, e nao"
                        + " assumir um numero de dentro do codigo -- assumir e"
                        + " como o AuraVisualPreset voltaria pela porta dos fundos.");
    }

    @Test
    @DisplayName("densidade absurda na config nao trava o cliente")
    void densidadeAbsurdaTemTeto() {
        int quantas = EmissorDeParticulasDeAura.quantasEmitir(
                PerfilDoDisco.de(AuraVisualMode.REN),
                estadoDe(AuraVisualMode.REN, 1.0F), 1_000_000.0D, 0.5F);
        assertTrue(quantas <= 12 && quantas > 0,
                "Sem teto, um numero errado na config emitiria " + quantas
                        + " particulas por tick e travaria o cliente -- e config e"
                        + " arquivo que qualquer um edita a mao.");
    }

    @Test
    @DisplayName("o tamanho da faisca vem do PERFIL, e Ren tem a maior")
    void tamanhoVemDoPerfil() {
        // ESTE NUMERO MORAVA NO CODIGO. Era `AuraVisualPreset.shellOpacity()`,
        // um campo com nome de shell usado para dimensionar particula -- e por
        // isso ninguem o encontrava ao procurar por que a faisca tinha aquele
        // tamanho. Hoje ele e `tamanho_de_particula`, no JSON.
        float ten = EmissorDeParticulasDeAura.tamanhoDe(
                PerfilDoDisco.de(AuraVisualMode.TEN), estadoDe(AuraVisualMode.TEN, 1.0F));
        float ren = EmissorDeParticulasDeAura.tamanhoDe(
                PerfilDoDisco.de(AuraVisualMode.REN), estadoDe(AuraVisualMode.REN, 1.0F));
        assertTrue(ren > ten, "a faisca de Ren (" + ren + ") nao e maior que a de Ten ("
                + ten + ")");
        assertTrue(ten >= 0.6F && ren <= 1.5F,
                "fora de 0,6..1,5 a poeira vanilla ou some ou vira mancha; saiu "
                        + ten + " e " + ren);
    }

    @Test
    @DisplayName("a cor sai inteira para o vetor da poeira")
    void corViraVetor() {
        var v = EmissorDeParticulasDeAura.corComo(0xFF_FF_80_00);
        assertEquals(1.0F, v.x(), 1.0e-3F);
        assertEquals(0.502F, v.y(), 1.0e-2F);
        assertEquals(0.0F, v.z(), 1.0e-3F);
        assertNotEquals(v.x(), v.z(), "o canal vermelho e o azul sairam iguais");
    }

    private static AuraVisualState estadoDe(AuraVisualMode modo, float intensidade) {
        AuraVisualController c = new AuraVisualController();
        c.receber(modo, intensidade);
        return concluir(c);
    }

    /**
     * Leva a transicao ate o fim, seja qual for a duracao dela.
     *
     * <p>AS DURACOES DEIXARAM DE SER UM NUMERO SO no AV3: cada troca tem o
     * proprio tempo, e {@code avancar} recebe uma ESCALA, e nao um passo. Os
     * testes que diziam {@code avancar(1.0F)} para "terminar agora" nao
     * terminavam mais nada -- eles pediam um tick de duracao normal.
     *
     * <p>O teto de cem ticks nao e paciencia: e a garantia de que um erro de
     * escala nunca vire um teste que nao termina.
     */
    private static AuraVisualState concluir(AuraVisualController c) {
        // AVANCO FIXO, e nao "ate o progresso chegar a 1". A primeira versao
        // disto olhava `transitionProgress() < 1`, e o estado inicial
        // (`desligado()`) ja NASCE com progresso 1 -- entao o laco nunca rodava
        // e todo teste via OFF. Quarenta ticks e mais que o dobro da transicao
        // mais longa da tabela.
        AuraVisualState estado = c.atual();
        for (int i = 0; i < 40; i++) {
            estado = c.avancar(1.0F);
        }
        return estado;
    }
}
