package com.darkcontinent.nenfoundation.api.event;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * A categoria do jogador foi definida. Ele AINDA NAO SABE qual e.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. ESTE EVENTO CARREGA A CATEGORIA REAL, e por isso ele e perigoso. Quem
 * o escuta esta lendo informacao que o jogador nao tem. Um listener que
 * escreva isto numa mensagem de chat, num placar ou num log visivel destroi a
 * Adivinhacao da Agua inteira -- e nao ha erro nenhum para acusar isso. Se o
 * seu listener so precisa saber que a atribuicao aconteceu, ignore
 * {@link #categoria()}.
 *
 * <p>2. ELE NAO E CANCELAVEL, e nao ha {@code CategoriaAtribuindoEvent}. Hoje
 * ninguem precisa IMPEDIR uma atribuicao: ela e o passo interno que existe
 * para que a revelacao tenha o que revelar. Um evento cancelavel sem ninguem
 * que cancele e um botao que nunca foi apertado -- e o dia em que alguem
 * cancelar sera o primeiro dia em que o caminho roda. O par cancelavel entra
 * quando houver quem cancele; acrescentar um evento depois nao quebra
 * listener nenhum.
 *
 * <p>3. ATRIBUIR E REVELAR SAO DOIS FATOS, e por isso sao dois eventos. Se
 * fossem um so, a Adivinhacao da Agua nao DESCOBRIRIA a categoria: ela a
 * criaria, e o ritual viraria um sorteio com animacao. O schema v1 ja separa
 * os dois campos.
 */
public class CategoriaAtribuidaEvent extends PlayerEvent {

    private final NenCategory categoria;

    public CategoriaAtribuidaEvent(ServerPlayer jogador, NenCategory categoria) {
        super(Objects.requireNonNull(jogador, "jogador"));
        this.categoria = Objects.requireNonNull(categoria, "categoria");
    }

    /** O jogador. Sempre server-side. */
    public ServerPlayer jogador() {
        return (ServerPlayer) getEntity();
    }

    /**
     * A categoria REAL, que o jogador ainda nao conhece. Nunca
     * {@link NenCategory#UNDETERMINED}.
     *
     * <p>Ver a decisao 1 no topo desta classe antes de exibir este valor em
     * qualquer lugar.
     */
    public NenCategory categoria() {
        return this.categoria;
    }
}
