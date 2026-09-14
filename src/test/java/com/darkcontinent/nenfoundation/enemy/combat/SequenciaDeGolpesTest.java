package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A FORMA de um combo, provada com numeros do teste e nao com os de producao.
 *
 * <p>Os valores que de fato vao para o jogo sao medidos em
 * {@code MultiarmCentipedeTuningTest}. Aqui o que se prova e a REGRA: que um
 * combo cujo ultimo golpe nao e o mais telegrafado, ou cuja emenda nao cabe na
 * recuperacao, ou cujo alcance encolhe, e RECUSADO na construcao -- e nao aceito
 * para falhar em silencio meses depois.</p>
 *
 * <p>Cada recusa aqui existe por causa de uma falha que nao levanta excecao em
 * lugar nenhum do jogo: o mob continua nascendo, atacando e passando em todo
 * portao. So o que ele ENSINA deixa de funcionar.</p>
 */
class SequenciaDeGolpesTest {

    private static GolpeEncadeado golpe(String id, int windup, int active, int recovery,
            double alcance) {
        return new GolpeEncadeado(
                new AttackDefinition(id, windup, active, recovery, 5.0F, 0.2F, true, false, true),
                new AttackHitbox(-1.0D, 0.0D, 0.5D, 1.0D, 2.0D, alcance));
    }

    /** Tres golpes validos: dois curtos e um final longo, com alcance crescente. */
    private static List<GolpeEncadeado> tresGolpesValidos() {
        return List.of(golpe("um", 12, 4, 8, 1.10D),
                golpe("dois", 11, 4, 8, 1.40D),
                golpe("tres", 24, 5, 26, 1.70D));
    }

    // ------------------------------------------------------------- o normal

    @Test
    @DisplayName("o caso normal monta, e o ultimo golpe e o que a sequencia aponta")
    void oCasoNormalMonta() {
        SequenciaDeGolpes sequencia = new SequenciaDeGolpes(tresGolpesValidos(), 4);

        assertEquals(3, sequencia.quantidade());
        assertTrue(sequencia.ehOUltimo(2));
        assertTrue(!sequencia.ehOUltimo(1), "o golpe do meio nao pode ser tratado como final:"
                + " a recuperacao dele e cortada pela emenda e ela nao e janela de punicao");
        assertSame(sequencia.golpe(2), sequencia.ultimo());
        assertNotNull(sequencia.golpe(0).caixa());
    }

    @Test
    @DisplayName("o alcance de DECISAO e o do primeiro golpe, e nao o maior da sequencia")
    void oAlcanceDeDecisaoEODoPrimeiroGolpe() {
        SequenciaDeGolpes sequencia = new SequenciaDeGolpes(tresGolpesValidos(), 4);
        assertEquals(1.10D, sequencia.alcanceParaComecarEmBlocos(),
                "comecar o combo a uma distancia que so o ultimo golpe alcanca faz os dois"
                        + " primeiros baterem no ar, sempre -- e como a navegacao trava durante o"
                        + " combo, o mob gasta a sequencia inteira errando");
    }

    @Test
    @DisplayName("os ticks totais contam a EMENDA dos encadeados, nao a recuperacao inteira")
    void osTicksTotaisContamAEmenda() {
        SequenciaDeGolpes sequencia = new SequenciaDeGolpes(tresGolpesValidos(), 4);
        // (12+4+4) + (11+4+4) + (24+5+26)
        assertEquals(94, sequencia.ticksTotais(),
                "contar a recuperacao completa dos encadeados daria um combo mais longo do que ele"
                        + " de fato dura, e quem comparasse esse numero com a recarga acharia folga"
                        + " que nao existe");
    }

    @Test
    @DisplayName("o dano total e a soma dos golpes, para poder ser medido contra a ficha")
    void oDanoTotalEASoma() {
        SequenciaDeGolpes sequencia = new SequenciaDeGolpes(tresGolpesValidos(), 4);
        assertEquals(15.0F, sequencia.danoTotal(), 1.0E-4F);
    }

    // ------------------------------------------------------------ o recusado

    @Test
    @DisplayName("REPROVA: o ultimo golpe sem o telegrafo mais longo")
    void reprovaUltimoTelegrafoCurto() {
        // Este e o caso que a regua existe para pegar. Com o ultimo avisando tao
        // pouco quanto os encadeados, o jogador perde a marca de onde o combo
        // termina e passa a punir no meio dele -- onde nao ha janela. Em jogo isso
        // nao levanta nada: o mob ataca tres vezes, o dano sai, o log fica limpo.
        var invalida = List.of(golpe("um", 12, 4, 8, 1.1D),
                golpe("dois", 11, 4, 8, 1.4D),
                golpe("tres", 12, 5, 26, 1.7D));
        IllegalArgumentException erro =
                assertThrows(IllegalArgumentException.class, () -> new SequenciaDeGolpes(invalida, 4));
        assertTrue(erro.getMessage().contains("telegrafo mais"),
                "a recusa tem de dizer QUE o telegrafo do ultimo e o que ensina; uma mensagem que"
                        + " so acusa nao ensina nada a quem a recebeu: " + erro.getMessage());
    }

    @Test
    @DisplayName("REPROVA: o ultimo golpe sem a recuperacao mais longa")
    void reprovaUltimaRecuperacaoCurta() {
        var invalida = List.of(golpe("um", 12, 4, 26, 1.1D),
                golpe("dois", 11, 4, 8, 1.4D),
                golpe("tres", 24, 5, 10, 1.7D));
        IllegalArgumentException erro =
                assertThrows(IllegalArgumentException.class, () -> new SequenciaDeGolpes(invalida, 4));
        assertTrue(erro.getMessage().contains("janela de punicao"), erro.getMessage());
    }

    @Test
    @DisplayName("REPROVA: emenda que nao cabe na recuperacao do golpe encadeado")
    void reprovaEmendaMaiorQueARecuperacao() {
        // A falha mais silenciosa da lista: a linha do tempo chega a COMPLETE antes
        // de a emenda acontecer, a recarga e armada, e o mob da UM golpe e fica
        // parado. Nenhum erro, nenhum log -- so um combo que nunca acontece.
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new SequenciaDeGolpes(tresGolpesValidos(), 8));
        assertTrue(erro.getMessage().contains("COMPLETE"), erro.getMessage());
    }

    @Test
    @DisplayName("REPROVA: emenda zero, que cola dois golpes num movimento so")
    void reprovaEmendaZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new SequenciaDeGolpes(tresGolpesValidos(), 0));
    }

    @Test
    @DisplayName("REPROVA: alcance que encolhe ao longo da sequencia")
    void reprovaAlcanceQueEncolhe() {
        var invalida = List.of(golpe("um", 12, 4, 8, 1.7D),
                golpe("dois", 11, 4, 8, 1.4D),
                golpe("tres", 24, 5, 26, 1.1D));
        IllegalArgumentException erro =
                assertThrows(IllegalArgumentException.class, () -> new SequenciaDeGolpes(invalida, 4));
        assertTrue(erro.getMessage().contains("recuar um passo"), erro.getMessage());
    }

    @Test
    @DisplayName("REPROVA: telegrafo abaixo do piso de leitura")
    void reprovaTelegrafoCurtoDemais() {
        var invalida = List.of(golpe("um", SequenciaDeGolpes.WINDUP_MINIMO - 1, 4, 8, 1.1D),
                golpe("tres", 24, 5, 26, 1.7D));
        assertThrows(IllegalArgumentException.class, () -> new SequenciaDeGolpes(invalida, 4));
    }

    @Test
    @DisplayName("REPROVA: um golpe so nao e sequencia, e cinco e stunlock")
    void reprovaTamanhoForaDaFaixa() {
        assertThrows(IllegalArgumentException.class,
                () -> new SequenciaDeGolpes(List.of(golpe("um", 24, 5, 26, 1.1D)), 4));
        var cinco = List.of(golpe("a", 12, 4, 8, 1.0D), golpe("b", 12, 4, 8, 1.0D),
                golpe("c", 12, 4, 8, 1.0D), golpe("d", 12, 4, 8, 1.0D),
                golpe("e", 24, 5, 26, 1.0D));
        assertThrows(IllegalArgumentException.class, () -> new SequenciaDeGolpes(cinco, 4));
    }

    @Test
    @DisplayName("REPROVA: dois golpes com o mesmo id, que apagam 'qual me acertou'")
    void reprovaIdsRepetidos() {
        var invalida = List.of(golpe("braco", 12, 4, 8, 1.1D),
                golpe("braco", 24, 5, 26, 1.7D));
        assertThrows(IllegalArgumentException.class, () -> new SequenciaDeGolpes(invalida, 4));
    }

    @Test
    @DisplayName("REPROVA: indice fora da sequencia, em vez de um golpe por conveniencia")
    void reprovaIndiceForaDaSequencia() {
        SequenciaDeGolpes sequencia = new SequenciaDeGolpes(tresGolpesValidos(), 4);
        assertThrows(IndexOutOfBoundsException.class, () -> sequencia.golpe(3));
        assertThrows(IndexOutOfBoundsException.class, () -> sequencia.golpe(-1));
    }

    @Test
    @DisplayName("REPROVA: golpe sem caixa ou sem definicao")
    void reprovaGolpeIncompleto() {
        assertThrows(NullPointerException.class, () -> new GolpeEncadeado(null,
                new AttackHitbox(-1.0D, 0.0D, 0.5D, 1.0D, 2.0D, 1.0D)));
        assertThrows(NullPointerException.class, () -> new GolpeEncadeado(
                new AttackDefinition("x", 12, 4, 8, 5.0F, 0.2F, true, false, true), null));
    }
}
