package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A maquina do combo, provada SEM servidor e SEM relogio proprio.
 *
 * <p>Todo teste aqui alimenta a decisao com a fase e os ticks restantes que o
 * {@link AttackController} teria publicado. Isso e deliberado: se esta classe
 * precisasse de um {@code AttackController} de verdade para ser testada, ela ja
 * teria virado o segundo relogio de ataque que este mob nao pode ter.</p>
 *
 * <p>O teste mais importante do arquivo e
 * {@link #staggerNoMeioCancelaORestoDoCombo}: cortar a sequencia e a jogada mais
 * cara que o jogador tem contra este mob, e ela e a mais facil de perder em
 * silencio -- basta o indice sobreviver a interrupcao para o combo seguinte
 * comecar pelo golpe do meio, com o telegrafo curto e sem o golpe que ensina.</p>
 */
class EstadoDaSequenciaTest {

    private static final int EMENDA = 4;
    private static final int RECUPERACAO_ENCADEADA = 8;

    private static GolpeEncadeado golpe(String id, int windup, int recovery, double alcance) {
        return new GolpeEncadeado(
                new AttackDefinition(id, windup, 4, recovery, 5.0F, 0.2F, true, false, true),
                new AttackHitbox(-1.0D, 0.0D, 0.5D, 1.0D, 2.0D, alcance));
    }

    private static EstadoDaSequencia novo() {
        return new EstadoDaSequencia(new SequenciaDeGolpes(List.of(
                golpe("um", 12, RECUPERACAO_ENCADEADA, 1.10D),
                golpe("dois", 11, RECUPERACAO_ENCADEADA, 1.40D),
                golpe("tres", 24, 26, 1.70D)), EMENDA));
    }

    private static EntradaDaSequencia em(AttackPhase fase, int restantes) {
        return new EntradaDaSequencia(fase, restantes, false, true, false, false);
    }

    /** Leva a sequencia ate o inicio do golpe de indice `alvo`, um encadeamento por vez. */
    private static EstadoDaSequencia ateOGolpe(int alvo) {
        EstadoDaSequencia estado = novo();
        assertEquals(DecisaoDeSequencia.COMECAR, estado.decidir(EntradaDaSequencia.pronta()));
        for (int i = 0; i < alvo; i++) {
            assertEquals(DecisaoDeSequencia.ENCADEAR,
                    estado.decidir(em(AttackPhase.RECOVERY, RECUPERACAO_ENCADEADA - EMENDA)));
        }
        assertEquals(alvo, estado.indice());
        return estado;
    }

    // ------------------------------------------------------------- o normal

    @Test
    @DisplayName("parada, ela comeca pelo PRIMEIRO golpe")
    void comecaPeloPrimeiro() {
        EstadoDaSequencia estado = novo();
        assertFalse(estado.emCurso());
        assertEquals(0, estado.golpesDados());

        assertEquals(DecisaoDeSequencia.COMECAR, estado.decidir(EntradaDaSequencia.pronta()));
        assertTrue(estado.emCurso());
        assertEquals(0, estado.indice());
        assertEquals("um", estado.golpeAtual().definicao().id());
        assertEquals(1, estado.golpesDados());
    }

    @Test
    @DisplayName("enquanto o golpe corre, ela espera -- o relogio e do AttackController")
    void esperaEnquantoOGolpeCorre() {
        EstadoDaSequencia estado = ateOGolpe(0);
        assertEquals(DecisaoDeSequencia.ESPERAR, estado.decidir(em(AttackPhase.WINDUP, 7)));
        assertEquals(DecisaoDeSequencia.ESPERAR, estado.decidir(em(AttackPhase.ACTIVE, 2)));
        assertEquals(0, estado.indice(), "esperar nao pode avancar o indice: o golpe seguinte"
                + " sairia com a caixa do anterior e o jogador apanharia de um alcance que nao"
                + " corresponde ao braco que se mexeu");
    }

    @Test
    @DisplayName("a emenda so encadeia DEPOIS de cumpridos os ticks declarados")
    void aEmendaEsperaOsTicksDeclarados() {
        EstadoDaSequencia estado = ateOGolpe(0);
        // Um tick de recuperacao gasto: ainda faltam tres para a emenda.
        assertEquals(DecisaoDeSequencia.ESPERAR,
                estado.decidir(em(AttackPhase.RECOVERY, RECUPERACAO_ENCADEADA - 1)));
        assertEquals(0, estado.indice());
        assertEquals(DecisaoDeSequencia.ENCADEAR,
                estado.decidir(em(AttackPhase.RECOVERY, RECUPERACAO_ENCADEADA - EMENDA)));
        assertEquals(1, estado.indice());
        assertEquals("dois", estado.golpeAtual().definicao().id());
    }

    @Test
    @DisplayName("a recuperacao do ULTIMO golpe toca inteira: ela e a janela de punicao")
    void oUltimoGolpeNaoEncadeia() {
        EstadoDaSequencia estado = ateOGolpe(2);
        assertEquals(DecisaoDeSequencia.ESPERAR, estado.decidir(em(AttackPhase.RECOVERY, 1)));
        assertEquals(DecisaoDeSequencia.ESPERAR, estado.decidir(em(AttackPhase.RECOVERY, 0)));
        assertEquals(2, estado.indice(), "cortar a recuperacao do ultimo golpe devolveria o mob ao"
                + " ataque no meio da unica janela que o encontro oferece");
    }

    @Test
    @DisplayName("COMPLETE encerra a sequencia, e o indice NAO sobrevive")
    void completeEncerra() {
        EstadoDaSequencia estado = ateOGolpe(2);
        assertEquals(DecisaoDeSequencia.ENCERRADA_POR_FIM,
                estado.decidir(em(AttackPhase.COMPLETE, 0)));
        assertFalse(estado.emCurso());
        assertEquals(0, estado.golpesDados());
    }

    // ------------------------------------------- a interrupcao, que e a ficha

    @Test
    @DisplayName("stagger no MEIO do combo cancela o resto, e o proximo comeca do zero")
    void staggerNoMeioCancelaORestoDoCombo() {
        EstadoDaSequencia estado = ateOGolpe(1);
        assertEquals(2, estado.golpesDados());

        EntradaDaSequencia cambaleando =
                new EntradaDaSequencia(AttackPhase.WINDUP, 6, true, true, false, false);
        assertEquals(DecisaoDeSequencia.INTERROMPIDA, estado.decidir(cambaleando),
                "interromper tem de ter nome proprio: cortar dois golpes e cortar zero nao podem"
                        + " ter a mesma leitura no codigo");
        assertFalse(estado.emCurso());

        // E o proximo combo comeca pelo PRIMEIRO golpe. Um indice sobrevivente daria
        // um combo que comeca pelo golpe do meio -- telegrafo curto, alcance errado,
        // e sem o golpe que ensina. Nao daria erro nenhum.
        assertEquals(DecisaoDeSequencia.COMECAR, estado.decidir(EntradaDaSequencia.pronta()));
        assertEquals("um", estado.golpeAtual().definicao().id());
    }

    @Test
    @DisplayName("stagger sem combo em curso nao inventa interrupcao nenhuma")
    void staggerSemComboNaoInterrompeNada() {
        EstadoDaSequencia estado = novo();
        EntradaDaSequencia cambaleando =
                new EntradaDaSequencia(AttackPhase.IDLE, 0, true, true, true, false);
        assertEquals(DecisaoDeSequencia.SEGUE_PARADA, estado.decidir(cambaleando));
    }

    // ------------------------------------------------------------ o recusado

    @Test
    @DisplayName("alvo que morreu encerra o combo, e com motivo proprio")
    void alvoQueSumiuEncerra() {
        EstadoDaSequencia estado = ateOGolpe(0);
        EntradaDaSequencia semAlvo =
                new EntradaDaSequencia(AttackPhase.WINDUP, 6, false, false, false, false);
        assertEquals(DecisaoDeSequencia.ENCERRADA_SEM_ALVO, estado.decidir(semAlvo));
        assertFalse(estado.emCurso());
    }

    @Test
    @DisplayName("recarga recusa o COMECO, e a recusa tem motivo")
    void recargaRecusaOComeco() {
        EstadoDaSequencia estado = novo();
        EntradaDaSequencia recarregando =
                new EntradaDaSequencia(AttackPhase.COMPLETE, 0, false, true, false, false);
        assertEquals(DecisaoDeSequencia.RECUSADA_POR_RECARGA, estado.decidir(recarregando));
        assertFalse(estado.emCurso());
    }

    @Test
    @DisplayName("quem recua nao COMECA combo -- mas um combo em curso termina")
    void recuoBarraOComecoENaoOMeio() {
        EstadoDaSequencia estado = novo();
        EntradaDaSequencia recuando =
                new EntradaDaSequencia(AttackPhase.IDLE, 0, false, true, true, true);
        assertEquals(DecisaoDeSequencia.RECUSADA_POR_RECUO, estado.decidir(recuando));
        assertFalse(estado.emCurso());

        // Ja em curso, o recuo NAO cancela: o jogador ja viu o primeiro golpe sair, e
        // desarmar o resto em silencio faria o telegrafo mentir.
        EstadoDaSequencia emCombo = ateOGolpe(0);
        EntradaDaSequencia recuandoNoMeio =
                new EntradaDaSequencia(AttackPhase.RECOVERY, RECUPERACAO_ENCADEADA - EMENDA,
                        false, true, false, true);
        assertEquals(DecisaoDeSequencia.ENCADEAR, emCombo.decidir(recuandoNoMeio));
    }

    @Test
    @DisplayName("REPROVA: perguntar o golpe corrente sem combo em curso")
    void reprovaGolpeAtualSemCombo() {
        // Devolver o primeiro golpe por conveniencia poria a caixa de dano no mundo
        // fora de qualquer janela de ataque, e alguem apanharia de um golpe que nao
        // existe. O erro alto e a unica defesa contra isso.
        EstadoDaSequencia estado = novo();
        IllegalStateException erro = assertThrows(IllegalStateException.class, estado::golpeAtual);
        assertTrue(erro.getMessage().contains("emCurso()"), erro.getMessage());
    }

    @Test
    @DisplayName("REPROVA: sequencia ausente e entrada com ticks negativos")
    void reprovaEntradasInvalidas() {
        assertThrows(NullPointerException.class, () -> new EstadoDaSequencia(null));
        assertThrows(NullPointerException.class, () -> novo().decidir(null));
        assertThrows(IllegalArgumentException.class,
                () -> new EntradaDaSequencia(AttackPhase.RECOVERY, -1, false, true, false, false));
        assertThrows(NullPointerException.class,
                () -> new EntradaDaSequencia(null, 0, false, true, false, false));
    }

    @Test
    @DisplayName("limpar zera o indice de UMA entidade, e e o ponto unico de saida")
    void limparZeraOIndice() {
        EstadoDaSequencia estado = ateOGolpe(1);
        estado.limpar();
        assertFalse(estado.emCurso());
        assertEquals(EstadoDaSequencia.PARADA, estado.indice());
    }
}
