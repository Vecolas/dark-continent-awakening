package com.darkcontinent.nenfoundation.enemy.combat;

import java.util.Objects;

/**
 * Em que golpe da sequencia UMA entidade esta -- e so isso.
 *
 * <p><b>Ele nao conta tempo.</b> O relogio continua sendo o
 * {@link AttackController}: ele e quem sabe a fase, quem decrementa os ticks e
 * quem arma a recarga. Esta classe guarda um INDICE e responde uma pergunta por
 * tick -- "e hora de comecar o proximo golpe?". Um contador de ticks aqui seria o
 * segundo relogio de ataque do mesmo mob; os dois andariam juntos ate a primeira
 * interrupcao e depois discordariam por um tick, e um tick de diferenca e
 * invisivel em teste e visivel na tela.</p>
 *
 * <p><b>Estado de instancia, nunca de classe.</b> Este projeto ja sabe que vai
 * cometer o erro de guardar estado de jogador num campo da classe da tecnica; a
 * versao dele aqui seria um indice estatico, e o sintoma seria um oficial pulando
 * para o terceiro golpe porque o irmao do outro lado do mapa chegou la primeiro.
 * Uma instancia por mob, criada no construtor da entidade e apagada no
 * {@code limpar()} dela.</p>
 *
 * <p><b>Ele nao sobrevive a save.</b> Um oficial salvo no meio do segundo golpe
 * volta do zero. Isso e a separacao do ADR-002 -- janela de combate e runtime, e
 * runtime nao sobrevive a nada. Vale dizer em voz alta porque o contrario tambem
 * seria plausivel, e escolher por acidente e o que produz save estranho meses
 * depois.</p>
 *
 * <p><b>O stagger tem prioridade sobre tudo, e e de proposito.</b> Interromper e,
 * por definicao, o que ganha do golpe: cambaleou, o resto da sequencia nao
 * acontece. Essa e a recompensa mais cara que o jogador pode cobrar deste mob, e
 * se ela ficasse atras de qualquer outra condicao aqui, ela sumiria em silencio
 * -- o mob continuaria encadeando depois de apanhar, e o jogador aprenderia a nao
 * interromper.</p>
 */
public final class EstadoDaSequencia {

    /** Nenhuma sequencia em curso. Nao e erro: e o estado de quase todo tick. */
    public static final int PARADA = -1;

    private final SequenciaDeGolpes sequencia;
    private int indice = PARADA;

    public EstadoDaSequencia(SequenciaDeGolpes sequencia) {
        this.sequencia = Objects.requireNonNull(sequencia, "sequencia de golpes ausente: sem ela"
                + " nao ha o que encadear, e o mob ficaria com um controlador de ataque que"
                + " ninguem manda comecar");
    }

    public SequenciaDeGolpes sequencia() { return sequencia; }

    public boolean emCurso() { return indice != PARADA; }

    /** Indice do golpe corrente, ou {@link #PARADA}. */
    public int indice() { return indice; }

    /** Quantos golpes ja SAIRAM, contando o que esta em curso. Zero quando parada. */
    public int golpesDados() { return emCurso() ? indice + 1 : 0; }

    /**
     * O golpe que o controlador de ataque deve estar tocando agora.
     *
     * @throws IllegalStateException se nao ha sequencia em curso -- e o erro e
     *         alto de proposito: devolver o primeiro golpe por conveniencia faria
     *         a caixa do golpe 1 ser usada fora de qualquer golpe, e alguem
     *         apanharia de um ataque que nao existe
     */
    public GolpeEncadeado golpeAtual() {
        if (!emCurso()) {
            throw new IllegalStateException("nao ha golpe em curso: quem pergunta precisa checar"
                    + " emCurso() antes, porque devolver o primeiro golpe aqui poria a caixa de"
                    + " dano no mundo fora de qualquer janela de ataque");
        }
        return sequencia.golpe(indice);
    }

    /**
     * Decide o que a sequencia faz neste tick, e AVANCA se for o caso.
     *
     * <p>A ordem das perguntas e a personalidade do combo, e inverter qualquer par
     * nao produz erro. Stagger vem antes de tudo porque interromper ganha do
     * golpe; alvo vem antes de comecar porque encadear no vazio gasta a sequencia
     * inteira batendo no ar; recuo vem antes da recarga porque quem desistiu nao
     * esta esperando recarga nenhuma.</p>
     *
     * <p>Quem chama e OBRIGADO a honrar {@link DecisaoDeSequencia#COMECAR} e
     * {@link DecisaoDeSequencia#ENCADEAR} iniciando o golpe que
     * {@link #golpeAtual()} passa a apontar. Ignorar a decisao deixa o indice
     * andando sem que golpe nenhum saia, e o mob termina a "sequencia" sem ter
     * atacado -- sem erro, e com o jogador vendo um oficial que so posa.</p>
     */
    public DecisaoDeSequencia decidir(EntradaDaSequencia entrada) {
        Objects.requireNonNull(entrada, "entrada de sequencia ausente");

        if (entrada.cambaleando()) {
            if (!emCurso()) return DecisaoDeSequencia.SEGUE_PARADA;
            limpar();
            return DecisaoDeSequencia.INTERROMPIDA;
        }
        if (!entrada.alvoValido()) {
            if (!emCurso()) return DecisaoDeSequencia.SEGUE_PARADA;
            limpar();
            return DecisaoDeSequencia.ENCERRADA_SEM_ALVO;
        }
        if (!emCurso()) {
            // Recuo so barra o COMECO. Uma sequencia ja em curso termina: o
            // jogador ja viu o primeiro golpe sair, e desarmar o resto em silencio
            // faria o telegrafo mentir.
            if (entrada.recuando()) return DecisaoDeSequencia.RECUSADA_POR_RECUO;
            if (!entrada.podeComecar()) return DecisaoDeSequencia.RECUSADA_POR_RECARGA;
            indice = 0;
            return DecisaoDeSequencia.COMECAR;
        }
        return switch (entrada.fase()) {
            case WINDUP, ACTIVE -> DecisaoDeSequencia.ESPERAR;
            case RECOVERY -> naRecuperacao(entrada);
            // IDLE ou COMPLETE com sequencia em curso significa que o relogio
            // acabou ou foi zerado por fora (morte, unload, comando). Encerrar
            // aqui e o que impede um indice orfao de sobreviver ao proximo combo
            // e faze-lo comecar pelo golpe do meio.
            case IDLE, COMPLETE -> {
                limpar();
                yield DecisaoDeSequencia.ENCERRADA_POR_FIM;
            }
        };
    }

    private DecisaoDeSequencia naRecuperacao(EntradaDaSequencia entrada) {
        // A recuperacao do ULTIMO golpe toca inteira: ela E a janela de punicao.
        // Cortar aqui devolveria o mob ao ataque no meio da unica janela que o
        // encontro oferece, e a sequencia deixaria de ter resposta.
        if (sequencia.ehOUltimo(indice)) return DecisaoDeSequencia.ESPERAR;

        int recuperacao = sequencia.golpe(indice).definicao().recoveryTicks();
        int gastos = recuperacao - entrada.ticksRestantesDaFase();
        if (gastos < sequencia.ticksDeEmenda()) return DecisaoDeSequencia.ESPERAR;
        indice++;
        return DecisaoDeSequencia.ENCADEAR;
    }

    /**
     * Limpeza: morte, remocao, unload, troca de dimensao, fim de sequencia.
     *
     * <p>Quem liga, desliga. Um indice deixado para tras faz o combo SEGUINTE
     * comecar pelo golpe do meio -- com o telegrafo curto, o alcance errado e sem
     * o golpe que ensina. Nao da erro: da um oficial que "as vezes" ataca so uma
     * vez.</p>
     */
    public void limpar() {
        indice = PARADA;
    }
}
