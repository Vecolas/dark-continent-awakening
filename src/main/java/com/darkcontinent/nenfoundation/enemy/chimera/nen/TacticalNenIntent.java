package com.darkcontinent.nenfoundation.enemy.chimera.nen;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.resources.ResourceLocation;

/**
 * O que a formiga QUER fazer com Nen neste instante.
 *
 * <p><b>Intencao, e nao acao.</b> O controlador nao ativa nada: ele responde uma
 * pergunta. Quem ativa e o lado que tem o registro de tecnicas e a aura -- e essa
 * separacao e o que impede este pacote de virar uma segunda autoridade sobre
 * Nen, exatamente o que o CLAUDE.md proibe.</p>
 *
 * <p>Os ids apontam para as tecnicas que JA existem no nucleo. Escrever um id
 * novo aqui nao criaria tecnica nenhuma: criaria uma intencao que nunca encontra
 * consumidor, e o sintoma seria uma formiga que "decide" algo e nao faz nada.</p>
 */
public enum TacticalNenIntent {
    /** Nao usar Nen. E o padrao, e a maioria das formigas fica aqui para sempre. */
    NENHUMA(null),
    /** Defesa passiva constante. A primeira coisa que uma formiga desperta aprende. */
    MANTER_TEN(NenFoundation.id("ten")),
    /** Ofensiva: aura elevada, dreno continuo. So quando ha alvo e vale a pena. */
    ELEVAR_REN(NenFoundation.id("ren")),
    /** Perceber: enxergar o que esta escondido. */
    USAR_GYO(NenFoundation.id("gyo")),
    /** Sumir: aura recolhida para nao ser sentida. E a intencao de quem FOGE. */
    ENTRAR_EM_ZETSU(NenFoundation.id("zetsu")),
    /** Defesa total, cara. Reservada a quem tem treino. */
    MANTER_KEN(NenFoundation.id("ken"));

    private final ResourceLocation tecnica;

    TacticalNenIntent(ResourceLocation tecnica) { this.tecnica = tecnica; }

    /** O id da tecnica correspondente; nulo apenas em {@link #NENHUMA}. */
    public ResourceLocation tecnica() { return tecnica; }

    public boolean usaNen() { return tecnica != null; }
}
