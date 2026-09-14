package com.darkcontinent.nenfoundation.enemy.registry;

import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.entity.FoxbearEntity;
import com.darkcontinent.nenfoundation.enemy.entity.FrogInWaitingEntity;
import com.darkcontinent.nenfoundation.enemy.entity.GreatStampEntity;
import com.darkcontinent.nenfoundation.enemy.entity.KirikoEntity;
import com.darkcontinent.nenfoundation.enemy.entity.ManFacedApeEntity;
import com.darkcontinent.nenfoundation.enemy.entity.MasterOfTheSwampEntity;
import com.darkcontinent.nenfoundation.enemy.entity.SpiderEagleEntity;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnProfile;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnRule;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Listeners de lifecycle mantidos fora do ponto de entrada do mod. */
public final class EnemyEntityEvents {
    private EnemyEntityEvents() { }

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(EnemyEntityTypes.GREAT_STAMP.get(), GreatStampEntity.createAttributes().build());
        event.put(EnemyEntityTypes.FROG_IN_WAITING.get(), FrogInWaitingEntity.createAttributes().build());
        event.put(EnemyEntityTypes.MAN_FACED_APE.get(), ManFacedApeEntity.createAttributes().build());
        event.put(EnemyEntityTypes.SPIDER_EAGLE.get(), SpiderEagleEntity.createAttributes().build());
        event.put(EnemyEntityTypes.MASTER_OF_THE_SWAMP.get(),
                MasterOfTheSwampEntity.createAttributes().build());
        event.put(EnemyEntityTypes.KIRIKO.get(), KirikoEntity.createAttributes().build());
        event.put(EnemyEntityTypes.FOXBEAR.get(), FoxbearEntity.createAttributes().build());
    }

    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        registrarPeloPerfil(event, EnemyEntityTypes.GREAT_STAMP.get(),
                HunterExamProfiles.greatStamp().spawnRule(), noChaoComLuzDoPerfil(HunterExamProfiles.greatStamp().spawnRule()));

        // O sapo tambem nasce no chao: ele emboscada ENTERRADO, e nao ha "enterrar"
        // sem um bloco solido embaixo. A faixa de luz e a do perfil dele, que vai ate
        // 15 justamente porque exigir escuridao faria a emboscada nunca nascer.
        registrarPeloPerfil(event, EnemyEntityTypes.FROG_IN_WAITING.get(),
                HunterExamProfiles.frogInWaiting().spawnRule(), noChaoComLuzDoPerfil(HunterExamProfiles.frogInWaiting().spawnRule()));

        // O macaco tambem nasce no chao, e com a MESMA forma de predicado: a faixa de
        // luz dele vai ate 15 porque ele se disfarca de gente, e ninguem e enganado no
        // escuro. Copiar o corpo do predicado aqui faria o terceiro lugar onde a mesma
        // regra pode divergir do perfil -- por isso ele e lido, nao repetido.
        registrarPeloPerfil(event, EnemyEntityTypes.MAN_FACED_APE.get(),
                HunterExamProfiles.manFacedApe().spawnRule(), noChaoComLuzDoPerfil(HunterExamProfiles.manFacedApe().spawnRule()));

        // A ave VOA, e mesmo assim nasce no chao: o ninho e o lugar dela, e ela nasce
        // POUSADA nele. Registrar um placement de ar aqui faria a ave aparecer
        // pairando sobre o canyon e ancorar o ninho no vazio -- e o ninho no ar nao
        // da erro nenhum, so faz a coleira medir a partir de um ponto que ninguem
        // consegue alcancar. Chao solido continua valendo, e a faixa de luz e a do
        // perfil, lida e nao repetida.
        registrarPeloPerfil(event, EnemyEntityTypes.SPIDER_EAGLE.get(),
                HunterExamProfiles.spiderEagle().spawnRule(), noChaoComLuzDoPerfil(HunterExamProfiles.spiderEagle().spawnRule()));

        // O PRIMEIRO PLACEMENT DE AGUA DO REPOSITORIO. O predicado de chao nao serve
        // aqui: ele exige face solida embaixo, e isso reprovaria todo ponto de agua
        // funda -- o mob simplesmente nunca nasceria, e nada acusaria. A faixa de luz
        // continua sendo a DO PERFIL, lida e nao repetida, pelo mesmo motivo de sempre.
        registrarPeloPerfil(event, EnemyEntityTypes.MASTER_OF_THE_SWAMP.get(),
                HunterExamProfiles.masterOfTheSwamp().spawnRule(), naAguaComLuzDoPerfil(HunterExamProfiles.masterOfTheSwamp().spawnRule()));

        // O kiriko nasce no chao e de dia, com a MESMA forma de predicado dos outros
        // tres terrestres -- reusada, e nao copiada, pelo motivo de sempre: numero de
        // luz repetido aqui vira botao morto na SpawnRule. A faixa dele vai ate 15
        // porque o mob inteiro depende de ser VISTO: um disfarce de gente no escuro nao
        // engana ninguem, e o encontro simplesmente nao aconteceria. Nada acusaria --
        // apareceria como um bioma vazio.
        registrarPeloPerfil(event, EnemyEntityTypes.KIRIKO.get(),
                HunterExamProfiles.kiriko().spawnRule(), noChaoComLuzDoPerfil(HunterExamProfiles.kiriko().spawnRule()));

        // O foxbear fecha a fila, e chega aqui trocando de predicado. Ele usava
        // Animal::checkAnimalSpawnRules -- bloco da lista de spawn de animais embaixo
        // e luz BRUTA maior que 8 -- e o perfil dele nao entrava na conta: a faixa
        // 0..12 de HunterExamProfiles.foxbear() nao chegava a lugar nenhum. Botao
        // morto nao da erro; ele custa a tarde de quem gira o numero esperando ver
        // diferenca. Agora ele le do perfil como os outros quatro terrestres, e a
        // faixa entra em vigor COMO ESTA ESCRITA: 12, e nao 15 como os seis irmaos.
        // Se 12 nao era a intencao, e uma linha no perfil -- e uma decisao de
        // balanceamento, que nao e desta entrega.
        //
        // Operation.OR, e nao REPLACE: REPLACE so faz sentido para derrubar um
        // placement que ja existe, e um tipo autoral nao tem nenhum. As duas formas
        // se comportam igual aqui, e a divergencia de forma e o que faz a proxima
        // pessoa achar que ha um motivo escondido.
        registrarPeloPerfil(event, EnemyEntityTypes.FOXBEAR.get(),
                HunterExamProfiles.foxbear().spawnRule(), noChaoComLuzDoPerfil(HunterExamProfiles.foxbear().spawnRule()));
    }


    /**
     * Registra o placement LENDO o perfil -- placement, heightmap e a pergunta
     * "isto pode nascer sozinho?" saem todos de {@link SpawnProfile}.
     *
     * <p>Antes cada linha repetia o par {@code SpawnPlacementTypes.X} +
     * {@code Heightmap.Types.Y}, e repetir e como o peixe quase ganhou placement
     * de chao: a linha compila, o registro carrega, e o mob simplesmente nunca
     * nasce. Aqui a escolha vem do dado, e a divergencia deixa de ser possivel.</p>
     *
     * <p>ENCOUNTER_ONLY e RECUSADO com todas as letras. Um chefe que chegue aqui
     * por engano ganharia placement natural e passaria a nascer no meio do mato
     * -- e nada acusaria, porque cada bicho seria uma entidade legitima. A
     * excecao no carregamento e barata; o encontro unico virando farm, nao.</p>
     */
    private static <T extends net.minecraft.world.entity.Mob> void registrarPeloPerfil(
            RegisterSpawnPlacementsEvent event, net.minecraft.world.entity.EntityType<T> tipo,
            SpawnRule regra, SpawnPlacements.SpawnPredicate<T> predicado) {
        SpawnProfile perfil = regra.profile();
        if (!perfil.registraPlacement()) {
            throw new IllegalStateException(tipo.getDescriptionId() + " usa o perfil " + perfil
                    + ", que nao nasce pelo caminho natural. Registrar placement para ele e"
                    + " exatamente o vazamento que o perfil existe para impedir.");
        }
        event.register(tipo, perfil.placement(), perfil.heightmap(), predicado,
                // Operation.OR, e nao REPLACE: REPLACE so faz sentido para derrubar
                // um placement que ja existe, e tipo autoral nao tem nenhum. As duas
                // se comportam igual aqui, e a divergencia de forma e o que faz a
                // proxima pessoa achar que ha motivo escondido.
                RegisterSpawnPlacementsEvent.Operation.OR);
    }

    /**
     * Predicado unico de "chao solido, dentro da faixa de luz DO PERFIL, e nao
     * vindo de spawner".
     *
     * <p>Os limites de luz sao LIDOS do perfil, nao repetidos aqui. Antes o 10
     * do great stamp estava nos dois lugares; girar o numero na {@link SpawnRule}
     * nao mudava nada em jogo, e o botao morto so aparece depois de uma tarde de
     * balanceamento perdida. Com dois mobs usando a mesma forma, copiar o corpo
     * do predicado faria a mesma divergencia nascer de novo -- em dobro.</p>
     */
    private static <T extends Entity> SpawnPlacements.SpawnPredicate<T> noChaoComLuzDoPerfil(SpawnRule regra) {
        int luzMinima = regra.minLight();
        int luzMaxima = regra.maxLight();
        return (type, level, spawnType, pos, random) -> {
            int luz = level.getMaxLocalRawBrightness(pos);
            return spawnType != MobSpawnType.SPAWNER
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    && luz >= luzMinima && luz <= luzMaxima;
        };
    }

    /**
     * Irmao AQUATICO do predicado acima: "agua funda, dentro da faixa de luz DO
     * PERFIL, e nao vindo de spawner".
     *
     * <p>A forma e a mesma de proposito -- inclusive a leitura da faixa de luz --
     * porque o erro que ela evita e o mesmo: numero de luz repetido aqui vira botao
     * morto na {@link SpawnRule}, e a sessao de balanceamento gira o botao sem que
     * nada mude em jogo.</p>
     *
     * <p>DOIS blocos de agua, e nao um. A caixa deste mob tem 1.6 de altura: nascido
     * numa poca de um bloco ele apareceria com metade do corpo para fora, encalhado,
     * e o unico sinal disso seria alguem ver a cena. Exigir o bloco de cima tambem com
     * agua e o que transforma "bioma de pantano" em "agua que comporta o bicho".</p>
     */
    private static <T extends Entity> SpawnPlacements.SpawnPredicate<T> naAguaComLuzDoPerfil(SpawnRule regra) {
        int luzMinima = regra.minLight();
        int luzMaxima = regra.maxLight();
        return (type, level, spawnType, pos, random) -> {
            int luz = level.getMaxLocalRawBrightness(pos);
            return spawnType != MobSpawnType.SPAWNER
                    && level.getFluidState(pos).is(FluidTags.WATER)
                    && level.getFluidState(pos.above()).is(FluidTags.WATER)
                    && luz >= luzMinima && luz <= luzMaxima;
        };
    }
}
