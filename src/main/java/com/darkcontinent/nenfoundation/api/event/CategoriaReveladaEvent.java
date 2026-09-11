package com.darkcontinent.nenfoundation.api.event;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Disparado DEPOIS de o jogador descobrir a propria categoria. Nao cancelavel.
 *
 * <p>Quando este evento roda, o perfil ja foi gravado, o marco
 * {@code nenfoundation:categoria_revelada} ja existe, e o snapshot novo ja
 * saiu -- entao o cliente deste jogador ja recebeu a categoria. Ao contrario
 * do {@link CategoriaAtribuidaEvent}, aqui {@link #categoria()} NAO e segredo:
 * o dono acabou de descobri-la.
 *
 * <p>E este o evento do onboarding: mensagem da Water Divination, conclusao de
 * quest, cutscene, som, o que for. E o unico jeito de o mundo externo saber
 * que isso aconteceu -- nada de fora le o attachment direto.
 *
 * <p>Ele dispara UMA vez por jogador. Revelar de novo e no-op e nao reanuncia:
 * quest que completa duas vezes e clique duplo existem, e o desenho que trata
 * isso como excecao e o que toca a cutscene duas vezes.
 */
public class CategoriaReveladaEvent extends PlayerEvent {

    private final NenCategory categoria;

    public CategoriaReveladaEvent(ServerPlayer jogador, NenCategory categoria) {
        super(Objects.requireNonNull(jogador, "jogador"));
        this.categoria = Objects.requireNonNull(categoria, "categoria");
    }

    /** O jogador que descobriu a propria categoria. Sempre server-side. */
    public ServerPlayer jogador() {
        return (ServerPlayer) getEntity();
    }

    /** A categoria revelada. Sempre uma das seis reais, nunca UNDETERMINED. */
    public NenCategory categoria() {
        return this.categoria;
    }
}
