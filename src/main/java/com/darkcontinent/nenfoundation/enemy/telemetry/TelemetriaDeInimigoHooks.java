package com.darkcontinent.nenfoundation.enemy.telemetry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.enemy.api.HxHEnemy;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Onde a telemetria de combate GANHA produtor.
 *
 * <p><b>Ela existia testada e sem ninguem a alimentar</b>, que e o mesmo estado
 * em que a audicao dos inimigos passou meses: um sistema completo, coberto por
 * teste, e desligado na pratica -- ligar a chave produzia um arquivo vazio. Um
 * sentido desligado que parece ligado e pior do que um que nao existe, porque
 * ninguem vai procurar.</p>
 *
 * <p><b>Um combate comeca no primeiro dano e termina na morte.</b> Nao ha
 * "entrou em aggro" aqui de proposito: aggro e estado de IA e oscila, e um
 * cronometro que reinicia a cada oscilacao mediria o ultimo trecho do combate em
 * vez do combate. Primeiro dano e um evento, e eventos nao oscilam.</p>
 *
 * <p><b>Combate que nao termina em morte e DESCARTADO.</b> O jogador que
 * desistiu e foi embora nao produziu uma medida de "quanto tempo o bicho dura" --
 * produziu uma medida de quanto tempo ele teve paciencia. Guardar os dois juntos
 * puxaria a media para cima e faria todo mob parecer mais duro do que e.</p>
 */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class TelemetriaDeInimigoHooks {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetriaDeInimigoHooks.class);

    /** O arquivo, dentro do diretorio do mundo. Nada sai da maquina. */
    private static final String ARQUIVO = "nenfoundation-telemetria-inimigos.csv";

    private static final TelemetriaDeCombate TELEMETRIA =
            new TelemetriaDeCombate(NenConfig::telemetriaDeInimigosAtiva);

    /** Um combate em andamento, por inimigo. */
    private static final Map<UUID, Combate> EM_CURSO = new LinkedHashMap<>();

    private TelemetriaDeInimigoHooks() { }

    public static TelemetriaDeCombate telemetria() { return TELEMETRIA; }

    /** Estado de UM combate. Runtime puro: ele morre com o processo, e deve. */
    private static final class Combate {
        private final int tickInicial;
        private final Set<UUID> jogadores = new LinkedHashSet<>();
        private float danoCausado;
        private float danoRecebido;

        private Combate(int tickInicial) { this.tickInicial = tickInicial; }
    }

    /**
     * Cada golpe conta, dos dois lados.
     *
     * <p>{@code LivingDamageEvent.Post} e o dano DEPOIS de armadura e
     * resistencia -- que e o numero que descreve o combate. O dano pedido
     * descreveria a arma, e a arma ja e conhecida.</p>
     */
    @SubscribeEvent
    public static void aoLevarDano(LivingDamageEvent.Post evento) {
        if (!TELEMETRIA.ligada()) return;
        LivingEntity ferido = evento.getEntity();
        if (ferido.level().isClientSide) return;

        if (ferido instanceof HxHEnemy) {
            if (!(evento.getSource().getEntity() instanceof Player jogador)) return;
            Combate combate = EM_CURSO.computeIfAbsent(ferido.getUUID(),
                    id -> new Combate(ferido.tickCount));
            combate.jogadores.add(jogador.getUUID());
            combate.danoCausado += evento.getNewDamage();
            return;
        }

        // O outro lado: o jogador levando dano DE um inimigo do mod.
        if (ferido instanceof Player
                && evento.getSource().getEntity() instanceof LivingEntity atacante
                && atacante instanceof HxHEnemy) {
            Combate combate = EM_CURSO.get(atacante.getUUID());
            if (combate != null) combate.danoRecebido += evento.getNewDamage();
        }
    }

    @SubscribeEvent
    public static void aoMorrer(LivingDeathEvent evento) {
        LivingEntity morto = evento.getEntity();
        if (morto.level().isClientSide) return;

        Combate combate = EM_CURSO.remove(morto.getUUID());
        if (combate == null || !(morto instanceof HxHEnemy inimigo)) return;
        if (!TELEMETRIA.ligada()) return;

        int ticks = Math.max(1, morto.tickCount - combate.tickInicial);
        TELEMETRIA.registrar(new AmostraDeCombate(inimigo.enemyMetadata().id().getPath(),
                Math.max(1, combate.jogadores.size()), ticks,
                combate.danoCausado, combate.danoRecebido, true));
    }

    /**
     * Grava o que houver ao parar o servidor, e ESQUECE o resto.
     *
     * <p>Gravar so no fim e deliberado: escrever a cada morte abriria o arquivo
     * dezenas de vezes por sessao, e o custo apareceria como engasgo justamente
     * quando alguem acabou de matar alguma coisa. O preco esta declarado -- um
     * crash perde a sessao inteira de telemetria, e telemetria perdida nao e um
     * problema de jogo.</p>
     *
     * <p>Os combates EM CURSO sao descartados: eles nao terminaram, e um combate
     * interrompido pelo desligamento mede o desligamento.</p>
     */
    @SubscribeEvent
    public static void aoPararOServidor(ServerStoppingEvent evento) {
        EM_CURSO.clear();
        if (!TELEMETRIA.ligada() || TELEMETRIA.emMemoria() == 0) return;

        ServerLevel overworld = evento.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld == null) return;
        Path destino = evento.getServer().getWorldPath(
                net.minecraft.world.level.storage.LevelResource.ROOT).resolve(ARQUIVO);
        if (TELEMETRIA.bateuNoTeto()) {
            LOG.warn("A telemetria bateu no teto de {} amostras nesta sessao: as ultimas foram"
                    + " RECUSADAS, e nao descartadas em silencio.", TelemetriaDeCombate.TETO_EM_MEMORIA);
        }
        int escritas = TELEMETRIA.gravar(destino);
        LOG.info("Telemetria local de inimigos: {} amostras em {}. Nada saiu desta maquina.",
                escritas, destino);
    }
}
