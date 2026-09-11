package com.darkcontinent.nenfoundation.api.event;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Disparado DEPOIS de um jogador receber uma categoria. Nao cancelavel.
 *
 * <p>Neste instante o jogador TEM categoria e NAO SABE qual e. Os dois fatos
 * sao separados no schema v1 de proposito: a Water Divination e o teste que
 * DESCOBRE a categoria. Se ela so passasse a existir na revelacao, o teste nao
 * descobriria nada -- ele criaria.
 *
 * <p><b>CUIDADO, E ESTE E O PONTO INTEIRO DESTE ARQUIVO:</b>
 * {@link #categoria()} devolve a categoria REAL, antes da revelacao. Um
 * listener que a mande para o cliente -- num chat, num toast, num payload
 * proprio, num log que o jogador leia -- destroi a revelacao inteira, e nao ha
 * teste automatico que pegue isso. O nucleo so envia
 * {@code PersistentNenData.categoriaVisivel()}; quem escuta este evento esta
 * fora dessa protecao e responde por ela.
 *
 * <p>Se o que voce quer e reagir ao momento em que o JOGADOR descobre, o
 * evento certo e {@link CategoriaReveladaEvent}.
 *
 * <p>Nome no PASSADO, como todo evento deste mod: ele relata, nao pede
 * permissao. Nao ha versao cancelavel porque nao ha sorteio a impedir --
 * quem quer decidir a categoria chama a atribuicao explicita com a categoria
 * que quiser.
 */
public class CategoriaAtribuidaEvent extends PlayerEvent {

    private final NenCategory categoria;

    public CategoriaAtribuidaEvent(ServerPlayer jogador, NenCategory categoria) {
        super(Objects.requireNonNull(jogador, "jogador"));
        this.categoria = Objects.requireNonNull(categoria, "categoria");
    }

    /** O jogador que recebeu a categoria. Sempre server-side. */
    public ServerPlayer jogador() {
        return (ServerPlayer) getEntity();
    }

    /**
     * A categoria real, ainda escondida do jogador.
     *
     * <p>Ver o aviso no topo da classe antes de repassar isto para qualquer
     * lugar que o jogador consiga ler.
     */
    public NenCategory categoria() {
        return this.categoria;
    }
}
