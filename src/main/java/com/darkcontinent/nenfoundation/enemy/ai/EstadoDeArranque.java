package com.darkcontinent.nenfoundation.enemy.ai;

import java.util.Objects;

/**
 * O relogio do arranque de UM bicho: a fase de agora e ha quantos ticks.
 *
 * <p><b>Uma instancia por ENTIDADE, e nunca em campo estatico.</b> Este e o erro
 * numero dois da lista do CLAUDE.md -- estado de jogador num campo de classe. Um
 * {@code EstadoDeArranque} estatico faria os guepardos de um servidor inteiro
 * dividirem uma fadiga: matar um deixaria os outros cansados, e ninguem ligaria
 * uma coisa a outra.</p>
 *
 * <p><b>O avanco e o pedido sao operacoes SEPARADAS, de proposito.</b>
 * {@link #tick()} roda todo tick, sempre, mesmo quando o bicho nao esta
 * perseguindo ninguem -- e o que faz a fadiga acabar e a recarga correr enquanto
 * o alvo se esconde. {@link #tentar} so roda quando a IA quer o arranque. Fundir
 * os dois num metodo so criaria a armadilha classica: um bicho que perde o alvo
 * deixaria de chamar o metodo, e a fase ficaria congelada em FATIGADA para
 * sempre. Isso nao daria erro nenhum -- daria um guepardo permanentemente lento,
 * e a causa estaria a uma hora de distancia do sintoma.</p>
 *
 * <p><b>Nada aqui conhece Minecraft.</b> Quem tem o mundo le
 * {@link #multiplicadorDeVelocidade()} e o entrega a navegacao; quem tem o
 * cliente le a fase sincronizada e pede o mesmo multiplicador as
 * {@link RegrasDeArranque}. Sao duas leituras da MESMA fonte, e nao duas
 * fontes.</p>
 */
public final class EstadoDeArranque {

    private final RegrasDeArranque regras;
    private FaseDoArranque fase = FaseDoArranque.PRONTA;
    private int ticksNaFase;

    public EstadoDeArranque(RegrasDeArranque regras) {
        this.regras = Objects.requireNonNull(regras, "regras de arranque ausentes: sem elas nao ha"
                + " duracao, nao ha fadiga e nao ha teto -- e um arranque sem teto e velocidade"
                + " permanente");
    }

    public RegrasDeArranque regras() { return regras; }

    public FaseDoArranque fase() { return fase; }

    /** Ha quantos ticks ele esta nesta fase. Leitura de diagnostico. */
    public int ticksNaFase() { return ticksNaFase; }

    /**
     * O fator de velocidade de AGORA -- lido da fase, nunca guardado.
     *
     * <p>Derivado a cada chamada de proposito. Um campo {@code multiplicador}
     * escrito no instante do arranque continuaria valendo depois que a fase
     * mudasse, e o bicho correria acelerado durante a propria fadiga -- com a
     * fase certa no debug, o clipe certo na tela e a velocidade errada.</p>
     */
    public double multiplicadorDeVelocidade() { return regras.multiplicadorDe(fase); }

    /**
     * Avanca o relogio UM tick, e troca de fase quando o prazo vence.
     *
     * <p>Tem de ser chamado todo tick de servidor, inclusive sem alvo. A
     * transicao acontece aqui e so aqui: espalha-la pelas Goals faria duas delas
     * avancarem a mesma fase no mesmo tick, e o arranque duraria metade do que
     * esta escrito sem que nenhum numero tivesse mudado.</p>
     */
    public void tick() {
        if (fase == FaseDoArranque.PRONTA) {
            // PRONTA nao tem prazo: ela espera uma decisao. Contar ticks aqui
            // faria o contador crescer sem limite ate estourar o int -- e um
            // contador negativo compararia mal contra a duracao, devolvendo o
            // bicho para o arranque no tick seguinte.
            return;
        }
        ticksNaFase++;
        if (ticksNaFase < regras.duracaoDe(fase)) return;
        ticksNaFase = 0;
        fase = switch (fase) {
            case ARRANCANDO -> FaseDoArranque.FATIGADA;
            case FATIGADA -> FaseDoArranque.EM_RECARGA;
            case EM_RECARGA -> FaseDoArranque.PRONTA;
            case PRONTA -> FaseDoArranque.PRONTA;
        };
    }

    /**
     * Pede o arranque. So {@link DecisaoDeArranque#ARRANCAR} muda alguma coisa.
     *
     * <p><b>A recusa nao mexe no relogio, e isso e a parte que importa.</b> Se um
     * pedido recusado zerasse {@code ticksNaFase}, um bicho que tentasse arrancar
     * todo tick nunca sairia da fadiga: ele ficaria lento para sempre, a IA
     * continuaria pedindo, e o log ficaria limpo.</p>
     *
     * @return o que aconteceu, com motivo quando nao aconteceu nada
     */
    public DecisaoDeArranque tentar(double distanciaAoAlvo, double distanciaDoAliadoAoAlvo,
            boolean temAlvo, boolean alvoVisivel) {
        DecisaoDeArranque decisao = regras.decidir(fase, distanciaAoAlvo, distanciaDoAliadoAoAlvo,
                temAlvo, alvoVisivel);
        if (decisao == DecisaoDeArranque.ARRANCAR) {
            fase = FaseDoArranque.ARRANCANDO;
            ticksNaFase = 0;
        }
        return decisao;
    }

    /**
     * Limpeza: morte, remocao, unload, troca de dimensao.
     *
     * <p>Quem liga, desliga. Sem isto, uma entidade reaproveitada pelo pool do
     * servidor voltaria a vida no meio de uma fadiga que ela nunca pagou -- e o
     * jogador veria um guepardo nascer lento, sem causa nenhuma.</p>
     */
    public void limpar() {
        fase = FaseDoArranque.PRONTA;
        ticksNaFase = 0;
    }
}
