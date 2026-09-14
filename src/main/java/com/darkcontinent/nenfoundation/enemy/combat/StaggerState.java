package com.darkcontinent.nenfoundation.enemy.combat;

import java.util.Objects;

/**
 * Acumulador de stagger de UMA entidade, com decaimento e janela de interrupcao.
 *
 * <p><b>Estado de instancia, nunca de classe.</b> Este projeto ja sabe que vai
 * cometer o erro de guardar estado de jogador num campo da classe da tecnica; a
 * versao dele aqui seria um acumulador estatico -- e o sintoma seria a manada
 * inteira cambaleando quando um irmao apanha. Uma instancia por mob.</p>
 *
 * <p><b>O mesmo ataque nao conta duas vezes.</b> Um golpe que atinge em dois
 * ticks por lag, ou um evento de dano processado por dois handlers, somaria duas
 * vezes e o mob cambalearia com metade do esforco. O id de instancia do ataque
 * fecha essa porta -- e e a MESMA defesa que {@link AttackController} usa contra
 * dano dobrado, pelo mesmo motivo.</p>
 */
public final class StaggerState {
    /** Nenhum ataque real usa este id; ele marca "ainda nao contei nada". */
    public static final long SEM_ATAQUE = Long.MIN_VALUE;

    private final StaggerRules regras;
    private float acumulado;
    private int ticksRestantes;
    private long ultimaInstanciaContada = SEM_ATAQUE;

    public StaggerState(StaggerRules regras) {
        this.regras = Objects.requireNonNull(regras, "regras de stagger ausentes");
    }

    public StaggerRules regras() { return regras; }
    public float acumulado() { return acumulado; }
    public int ticksRestantes() { return ticksRestantes; }
    public boolean cambaleando() { return ticksRestantes > 0; }

    /**
     * Soma o resultado REAL de um golpe.
     *
     * <p>Quem chama e o lado que ja confirmou o acerto -- nunca um pacote do
     * cliente, nunca uma animacao. Stagger vindo de keyframe transformaria o
     * arquivo de animacao em autoridade de combate.</p>
     *
     * @param instanciaDeAtaque id unico do ataque que gerou este golpe
     * @param valorBruto quanto de stagger o golpe vale antes da resistencia
     */
    public StaggerResult acumular(long instanciaDeAtaque, float valorBruto) {
        if (instanciaDeAtaque != SEM_ATAQUE && instanciaDeAtaque == ultimaInstanciaContada) {
            return StaggerResult.REPETIDO;
        }
        float efetivo = regras.efetivo(valorBruto);
        if (efetivo <= 0.0F) return StaggerResult.ABSORVIDO;

        ultimaInstanciaContada = instanciaDeAtaque;
        acumulado += efetivo;
        if (acumulado < regras.limiar()) return StaggerResult.ACUMULOU;

        // Dispara e ZERA. Sem zerar, o excedente ficaria guardado e o golpe
        // seguinte dispararia de graca -- um mob que cambaleia em cadeia e nao
        // sai mais do estado, sem nenhum erro no log.
        acumulado = 0.0F;
        ticksRestantes = regras.ticksDeStagger();
        return StaggerResult.DISPAROU;
    }

    /**
     * Um tick: a janela encurta e o acumulado esquece.
     *
     * @return true no tick EXATO em que a interrupcao termina, para quem precisa
     *         devolver o mob ao estado anterior sem vigiar o contador
     */
    public boolean tick() {
        if (acumulado > 0.0F) {
            acumulado = Math.max(0.0F, acumulado - regras.decaimentoPorTick());
            // Zerado o acumulado, o proximo golpe pode ser do mesmo ataque sem
            // que isso seja repeticao: a rodada anterior acabou.
            if (acumulado == 0.0F) ultimaInstanciaContada = SEM_ATAQUE;
        }
        if (ticksRestantes == 0) return false;
        ticksRestantes--;
        return ticksRestantes == 0;
    }

    /**
     * Limpeza simetrica: morte, unload, troca de dimensao.
     *
     * <p>Os tres campos saem JUNTOS. Zerar so a janela deixaria o acumulado
     * pronto para disparar assim que o mob voltasse; zerar so o acumulado
     * deixaria um mob cambaleando sem motivo do outro lado do portal. Estado
     * pela metade nao da erro -- so aparece na tela de quem joga.</p>
     */
    public void limpar() {
        acumulado = 0.0F;
        ticksRestantes = 0;
        ultimaInstanciaContada = SEM_ATAQUE;
    }
}
