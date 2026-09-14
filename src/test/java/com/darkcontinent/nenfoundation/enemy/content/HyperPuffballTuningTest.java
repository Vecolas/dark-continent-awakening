package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.DecisaoDeEstouro;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeEstouro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros que o Hyper Puffball vai levar para o jogo, medidos contra a ficha
 * dele -- e nao contra si mesmos.
 *
 * <p>{@code RegrasDeEstouroTest} prova a REGRA com numeros proprios. Este arquivo
 * prova os VALORES DE PRODUCAO: que a combinacao declarada em
 * {@link HyperPuffballTuning} e construivel, que ela ainda entrega a licao do
 * bicho contra a vida que o perfil da, e que o estufo de aviso continua sendo um
 * ataque que nao machuca ninguem. Nenhuma dessas coisas levanta erro quando
 * quebra: o mod carrega, o fungo nasce, e so falha o que ele ensina.</p>
 */
class HyperPuffballTuningTest {

    /** O literal de GreedIslandProfiles.hyperPuffball(): new EnemyAttributes(18, 0.0F, ...). */
    private static final float VIDA_MAXIMA = 18.0F;
    /** O alcance de ataque de um jogador em 1.21.1. Do jogo, nao deste bicho. */
    private static final double ALCANCE_DE_ATAQUE_DO_JOGADOR = 3.0D;

    @Test
    @DisplayName("os numeros declarados formam regras validas")
    void osNumerosDeProducaoSaoConstruiveis() {
        RegrasDeEstouro regras = HyperPuffballTuning.regrasDeEstouro();
        assertEquals(HyperPuffballTuning.RAIO_DO_ESTOURO, regras.raio());
        assertEquals(HyperPuffballTuning.DANO_DO_ESTOURO, regras.dano());
        // O construtor ja cobra aviso >= gatilho. Esta chamada e o que garante que
        // a cobranca de fato roda sobre os numeros de producao, e nao so sobre os
        // do teste da regra.
    }

    @Test
    @DisplayName("um jogador no alcance do proprio soco dispara o estouro")
    void quemBateDeEspadaEstaDentroDoToque() {
        assertTrue(HyperPuffballTuning.DISTANCIA_DO_GATILHO > ALCANCE_DE_ATAQUE_DO_JOGADOR,
                "com o toque mais curto que o alcance de ataque, dar um passo atras e continuar"
                        + " batendo mataria o fungo sem estouro nenhum -- e a ameaca do bicho"
                        + " viraria um detalhe que ninguem encontra");
        assertEquals(DecisaoDeEstouro.ESTOURA,
                HyperPuffballTuning.regrasDeEstouro()
                        .decidir(1.0F, VIDA_MAXIMA, ALCANCE_DE_ATAQUE_DO_JOGADOR, false));
    }

    @Test
    @DisplayName("o fungo sobrevive ao primeiro golpe pesado -- senao ele nao ensina nada")
    void oPrimeiroGolpeNaoPodeJaEstourar() {
        float golpeDeEspadaDeFerro = 6.0F;
        assertEquals(DecisaoDeEstouro.VIDA_ACIMA_DO_LIMIAR,
                HyperPuffballTuning.regrasDeEstouro()
                        .decidir(VIDA_MAXIMA - golpeDeEspadaDeFerro, VIDA_MAXIMA, 2.0D, false),
                "estourando no primeiro golpe, o jogador nunca ve o estufo de aviso acontecer"
                        + " duas vezes, e a leitura que o bicho existe para ensinar nao chega");
    }

    @Test
    @DisplayName("o estufo de aviso nao machuca, e nao empurra")
    void oAvisoEUmAtaqueQueNaoFazNada() {
        AttackDefinition aviso = HyperPuffballTuning.aviso();
        assertEquals(0.0F, aviso.damage(),
                "dano diferente de zero aqui daria um bicho de ficha 'dano 0' machucando por"
                        + " contato, e o unico sinal seria a barra de vida do jogador");
        assertEquals(0.0F, aviso.knockback());
        assertTrue(aviso.interruptibleWindup() && aviso.interruptibleActive()
                        && aviso.interruptibleRecovery(),
                "um estufo que nao pode ser interrompido continua na tela depois do golpe que o"
                        + " cortou, e o jogador le que o ataque dele nao chegou");
    }

    @Test
    @DisplayName("o orcamento do estufo e o que a animacao copiou")
    void osTicksDoAvisoSaoOsDoArquivoDeAnimacao() {
        AttackDefinition aviso = HyperPuffballTuning.aviso();
        // Estes tres numeros estao escritos tambem em
        // art-source/enemies/hyper_puffball/hyper_puffball_animacoes.py, no dicionario
        // ATAQUES, com o nome do campo ao lado. Duplicacao DECLARADA: a outra ponta
        // e o portao valida_duracao_de_ataque, que reprova quando os clipes ficam
        // mais curtos que o orcamento. Sem esta asercao, mexer aqui deixaria o
        // gerador de arte medindo um orcamento que o servidor nao usa mais.
        assertEquals(12, aviso.windupTicks());
        assertEquals(4, aviso.activeTicks());
        assertEquals(10, aviso.recoveryTicks());
    }
}
