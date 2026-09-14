package com.darkcontinent.nenfoundation.enemy.encounter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coloca no mundo o que a receita manda -- e devolve TODOS os uuids.
 *
 * <p><b>A lista de retorno e a parte que nao pode falhar.</b> Um spawn que nao
 * volta nela fica invisivel para o controlador: ao reiniciar o servidor ele
 * conta zero entidades vivas, decide que o episodio nao terminou, e spawna outra
 * leva por cima da primeira. Nao ha erro nisso -- ha dois chefes.</p>
 *
 * <p><b>Lista vazia e resposta legitima, e significa "nao comecou".</b> Chunk
 * fechando, receita ausente, tipo sem lugar para caber: em qualquer um desses o
 * controlador falha o episodio em vez de deixa-lo ACTIVE sem entidade -- um
 * encontro que nunca conclui e nunca falha deixaria o ancoradouro morto para
 * sempre.</p>
 *
 * <p><b>A altura e MEDIDA, e nao a do ancoradouro.</b> O ancoradouro e salvo uma
 * vez e o terreno muda: um bicho colocado na altura gravada pode nascer dentro
 * da pedra (dano de sufocamento contínuo que ninguem explica) ou vinte blocos no
 * ar. O heightmap responde onde e a superficie AGORA.</p>
 */
public final class EncounterSpawnerPadrao implements EncounterSpawner {

    private static final Logger LOG = LoggerFactory.getLogger(EncounterSpawnerPadrao.class);

    /**
     * Quantas posicoes sao tentadas por criatura antes de desistir dela.
     *
     * <p>NAO e botao de balanceamento: e o limite entre "procurou" e "travou o
     * tick". Uma busca sem teto num lugar cercado de parede varreria o chunk
     * inteiro por bicho, e o sintoma seria um engasgo no instante em que o
     * jogador chega -- exatamente quando ele esta olhando.</p>
     */
    private static final int TENTATIVAS_POR_CRIATURA = 8;

    @Override
    public List<UUID> spawnar(ServerLevel nivel, EncounterInstance instancia) {
        var receita = EncounterBlueprints.de(instancia.definitionId()).orElse(null);
        if (receita == null) {
            LOG.warn("Encontro {} pede a definicao '{}', que nao tem receita: nada foi criado.",
                    instancia.id(), instancia.definitionId());
            return List.of();
        }

        EntityType<?> tipo = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getOptional(receita.tipo()).orElse(null);
        if (tipo == null) {
            // A receita guarda um ID, e o registro e consultado AQUI. Um id que
            // nao existe no registro nao pode derrubar o tick: ele significa
            // "este encontro nao tem o que colocar", e o controlador falha o
            // episodio -- que e recuperavel.
            LOG.warn("Encontro {} pede a entidade '{}', que nao esta registrada.",
                    instancia.id(), receita.tipo());
            return List.of();
        }
        List<UUID> criados = new ArrayList<>(receita.quantidade());
        for (int i = 0; i < receita.quantidade(); i++) {
            UUID id = criarUm(nivel, instancia, tipo, receita.raioDeEspalhamento(), i);
            if (id != null) criados.add(id);
        }

        if (criados.size() < receita.quantidade()) {
            LOG.warn("Encontro {} criou {} de {} criaturas: faltou lugar. O episodio segue com"
                    + " o que coube.", instancia.id(), criados.size(), receita.quantidade());
        }
        return List.copyOf(criados);
    }

    private UUID criarUm(ServerLevel nivel, EncounterInstance instancia, EntityType<?> tipo,
            double raio, int indice) {
        BlockPos ancora = instancia.ancora();
        for (int tentativa = 0; tentativa < TENTATIVAS_POR_CRIATURA; tentativa++) {
            BlockPos posicao = espalhar(nivel, ancora, raio, indice, tentativa);
            if (!nivel.isLoaded(posicao)) continue;

            Entity entidade = tipo.spawn(nivel, posicao, MobSpawnType.EVENT);
            if (entidade != null) return entidade.getUUID();
        }
        return null;
    }

    /**
     * Uma posicao ao redor do ancoradouro, DETERMINISTICA.
     *
     * <p>O angulo sai do indice e da tentativa, e nao de sorteio: dois servidores
     * com o mesmo save tem de colocar o mesmo encontro no mesmo lugar, senao a
     * unica forma de reproduzir um relato de bug some.</p>
     */
    private BlockPos espalhar(ServerLevel nivel, BlockPos ancora, double raio,
            int indice, int tentativa) {
        double angulo = (indice * 2.399963D) + tentativa * 0.7D;
        double distancia = raio * (0.4D + 0.6D * ((tentativa % 4) / 3.0D));
        int x = ancora.getX() + (int) Math.round(Math.cos(angulo) * distancia);
        int z = ancora.getZ() + (int) Math.round(Math.sin(angulo) * distancia);
        // A altura e medida AGORA: o ancoradouro foi salvo uma vez e o terreno muda.
        int y = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }
}
