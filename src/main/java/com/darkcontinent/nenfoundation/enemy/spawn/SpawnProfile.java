package com.darkcontinent.nenfoundation.enemy.spawn;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * COMO uma criatura chega ao mundo -- e, principalmente, se ela chega sozinha.
 *
 * <p><b>A pergunta que este enum responde nao e "onde ela pisa".</b> Isso ja e
 * trabalho de {@link SpawnRule#permite(SpawnContext)}. A pergunta aqui e outra,
 * e e a que a issue #112 chama de nao-negociavel: <em>esta criatura pode entrar
 * na lista de spawn de bioma?</em></p>
 *
 * <p><b>Por que isso merece um tipo proprio.</b> Natural, structure-only e
 * encounter-only caindo no mesmo caminho de registro nao da erro nenhum -- da um
 * chefe de encontro nascendo sozinho no meio do mato, um monstro de Greed Island
 * fora de Greed Island, e nenhuma das duas coisas aparece em log ou em build. O
 * unico sintoma e alguem abrir o jogo e encontrar algo que nao deveria estar
 * ali. Um {@code boolean} espalhado pelos pontos de registro seria esquecido no
 * decimo mob; um enum obrigatorio na regra nao pode ser esquecido, porque nao
 * existe regra de spawn sem ele.</p>
 *
 * <p><b>Ele tambem escolhe o placement vanilla.</b> Registrar um peixe com
 * placement de chao reprovaria todo ponto de agua funda e o mob simplesmente
 * nunca nasceria, sem nada acusar -- foi o que quase aconteceu com o Master of
 * the Swamp. Aqui o par placement/heightmap vem do perfil, e nao de uma escolha
 * repetida em cada linha de registro.</p>
 */
public enum SpawnProfile {

    /** Fauna terrestre comum: o mundo a coloca, de pe, em bloco solido. */
    ON_GROUND(true, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),

    /** Vida aquatica: precisa de coluna de agua, e chao solido a reprovaria. */
    IN_WATER(true, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),

    /**
     * Voa, mas NASCE POUSADA.
     *
     * <p>Spawnar no ar parece o obvio para um bicho que voa e e o erro: sem
     * superficie, o ninho ancora no vazio e toda distancia medida a partir dele
     * passa a sair de um ponto que ninguem alcanca. A ave sobe depois; ela nao
     * comeca la em cima.</p>
     */
    FLYING_SURFACE_ANCHOR(true, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),

    /**
     * So dentro de estrutura. Nao entra em lista de bioma.
     *
     * <p>Ainda ganha placement registrado, porque a estrutura precisa de um
     * predicado valido para colocar a entidade -- o que ela NAO ganha e a entrada
     * no pool do bioma.</p>
     */
    STRUCTURE_ONLY(false, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),

    /**
     * So por controller de encontro. Nao entra em lista de bioma e nao ganha
     * placement natural nenhum.
     *
     * <p>E o perfil dos chefes e dos alvos de encontro. Se ele vazasse para o
     * pool, o jogo geraria um encontro unico varias vezes pelo mundo e a
     * recompensa dele viraria farm -- sem duplicata aparente, porque cada bicho e
     * uma entidade legitima. Ver {@link #entraNaListaDeBioma()}.</p>
     */
    ENCOUNTER_ONLY(false, null, null);

    /** Serializacao por nome minusculo; o nome do enum e o contrato do datapack. */
    public static final Codec<SpawnProfile> CODEC = Codec.STRING.xmap(
            texto -> valueOf(texto.toUpperCase(Locale.ROOT)),
            perfil -> perfil.name().toLowerCase(Locale.ROOT));

    private final boolean listaDeBioma;
    private final SpawnPlacementType placement;
    private final Heightmap.Types heightmap;

    SpawnProfile(boolean listaDeBioma, SpawnPlacementType placement, Heightmap.Types heightmap) {
        this.listaDeBioma = listaDeBioma;
        this.placement = placement;
        this.heightmap = heightmap;
    }

    /**
     * A UNICA resposta que importa para o biome modifier.
     *
     * <p>Quem gera ou audita {@code neoforge:add_spawns} pergunta aqui. Perguntar
     * a outra coisa -- ao {@code MobCategory}, a um comentario, a memoria de quem
     * escreveu -- e como o chefe acaba no pool.</p>
     */
    public boolean entraNaListaDeBioma() { return listaDeBioma; }

    /** Este perfil registra {@code SpawnPlacements}? ENCOUNTER_ONLY nao registra. */
    public boolean registraPlacement() { return placement != null; }

    public SpawnPlacementType placement() {
        if (placement == null) {
            throw new IllegalStateException(name() + " nao tem placement natural: ele nasce por"
                    + " controller de encontro. Pedir placement aqui significa que alguem esta"
                    + " prestes a registra-lo no caminho natural, que e justamente o vazamento"
                    + " que este perfil existe para impedir.");
        }
        return placement;
    }

    public Heightmap.Types heightmap() {
        if (heightmap == null) throw new IllegalStateException(name() + " nao tem heightmap natural");
        return heightmap;
    }
}
