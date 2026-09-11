package com.darkcontinent.nenfoundation.api.event;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * O jogador SOUBE qual e a sua categoria.
 *
 * <p>Este e o evento do onboarding: e aqui que a Adivinhacao da Agua termina,
 * que a quest completa e que a ficha passa a mostrar a categoria. Quando ele
 * dispara, o perfil ja foi gravado, o marco
 * {@code nenfoundation:categoria_revelada} ja existe, e o snapshot com a
 * categoria ja saiu para o cliente.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. AO CONTRARIO DE {@link CategoriaAtribuidaEvent}, aqui a categoria NAO
 * e segredo: o jogador acabou de descobri-la. Exibir, anunciar no chat e
 * escrever no log sao usos legitimos deste evento, e nao daquele.
 *
 * <p>2. Nao e cancelavel, e nao ha {@code CategoriaRevelandoEvent}. Ver a
 * decisao 2 de {@link CategoriaAtribuidaEvent}: o par cancelavel entra quando
 * houver quem cancele, e acrescentar um evento depois nao quebra listener
 * nenhum.
 *
 * <p>3. Revelar duas vezes NAO dispara duas vezes. O servico e idempotente, e
 * o onboarding que completa duas vezes e um bug classico de quest.
 */
public class CategoriaReveladaEvent extends PlayerEvent {

    private final NenCategory categoria;

    public CategoriaReveladaEvent(ServerPlayer jogador, NenCategory categoria) {
        super(Objects.requireNonNull(jogador, "jogador"));
        this.categoria = Objects.requireNonNull(categoria, "categoria");
    }

    /** O jogador. Sempre server-side. */
    public ServerPlayer jogador() {
        return (ServerPlayer) getEntity();
    }

    /**
     * A categoria que o jogador acabou de descobrir. Nunca
     * {@link NenCategory#UNDETERMINED}: nao se revela o que nao foi atribuido.
     */
    public NenCategory categoria() {
        return this.categoria;
    }
}
