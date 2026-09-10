package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * O estado de leitura que a interface precisa. Enviado SO ao dono do perfil.
 *
 * <p>DECISAO: o campo se chama {@code categoriaVisivel}, e nao
 * {@code categoria}, porque o nome e o que impede o erro. Enviar a categoria
 * real antes da revelacao entrega a informacao a qualquer cliente modificado,
 * e a Water Divination vira teatro. Quem preenche este record chama
 * {@code PersistentNenData.categoriaVisivel()}.
 *
 * <p>DECISAO: o snapshot NAO carrega aura. Aura muda a cada tick e viaja no
 * delta; um snapshot com aura convida alguem a reenvia-lo com frequencia.
 */
public record SnapshotDePerfilS2C(NenCategory categoriaVisivel,
        Set<ResourceLocation> tecnicasDesbloqueadas,
        Set<ResourceLocation> habilidadesDesbloqueadas,
        Set<ResourceLocation> marcos)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SnapshotDePerfilS2C> TYPE =
            new CustomPacketPayload.Type<>(NenFoundation.id("nen_profile_snapshot"));

    public static final StreamCodec<ByteBuf, SnapshotDePerfilS2C> STREAM_CODEC =
            StreamCodec.composite(
                    CodecsDePayload.CATEGORIA, SnapshotDePerfilS2C::categoriaVisivel,
                    CodecsDePayload.CONJUNTO_DE_IDS, SnapshotDePerfilS2C::tecnicasDesbloqueadas,
                    CodecsDePayload.CONJUNTO_DE_IDS, SnapshotDePerfilS2C::habilidadesDesbloqueadas,
                    CodecsDePayload.CONJUNTO_DE_IDS, SnapshotDePerfilS2C::marcos,
                    SnapshotDePerfilS2C::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
