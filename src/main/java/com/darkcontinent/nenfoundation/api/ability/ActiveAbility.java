package com.darkcontinent.nenfoundation.api.ability;

import net.minecraft.resources.ResourceLocation;

/**
 * Uma instancia VIVA de habilidade: canalizacao, duracao, construct no mundo.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. A instancia e separada da definicao. Duas instancias da mesma
 * habilidade nao compartilham estado. Guardar estado de execucao na
 * implementacao de {@link NenAbility} — que e um singleton registrado — e
 * estado global disfarcado: dois jogadores usando a mesma habilidade passam a
 * escrever no mesmo campo.
 *
 * <p>2. Trabalho iniciado morre com quem o iniciou. Toda instancia tem um dono
 * e some quando o dono some — morte, logout, troca de dimensao. Projetil e
 * construct orfaos sao a causa classica de vazamento de tick em servidor.
 */
public interface ActiveAbility {

    /** Qual habilidade esta instancia executa. */
    ResourceLocation abilityId();

    /** Tick de servidor em que a instancia comecou. */
    long tickDeInicio();

    /** Se a instancia ainda deve receber tick. */
    boolean viva();
}
