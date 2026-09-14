package com.darkcontinent.nenfoundation.enemy.encounter;

import com.darkcontinent.nenfoundation.NenFoundation;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O ciclo de vida dos encontros, ligado ao servidor.
 *
 * <p><b>Ele se registra sozinho pela anotacao</b>, como {@code EnemyGameEvents}:
 * o ponto de entrada do mod continua apenas registrando subsistemas, e nao ganha
 * mais uma linha -- ele e o arquivo hostil a merge deste repositorio.</p>
 *
 * <p><b>A reconciliacao acontece UMA vez, no start</b>, e nao a cada tick. E ela
 * e a peca que impede dois chefes: um encontro ativo quando o servidor caiu volta
 * dizendo ACTIVE, e a pergunta que decide tudo e se as entidades daquele episodio
 * sobreviveram. Elas costumam sobreviver.</p>
 *
 * <p><b>O controlador morre com o servidor.</b> Guardado num campo estatico sem
 * limpeza, ele sobreviveria a troca de mundo num cliente integrado: o segundo
 * mundo herdaria os encontros do primeiro, e o sintoma seria um chefe de outro
 * save aparecendo num mundo novo -- sem erro nenhum, porque a entidade e
 * legitima.</p>
 */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class EncounterServerHooks {

    private static final Logger LOG = LoggerFactory.getLogger(EncounterServerHooks.class);

    /** Uma instancia por SERVIDOR, e nula fora dele. Ver o javadoc da classe. */
    private static EncounterController controlador;
    private static com.darkcontinent.nenfoundation.server.NenTickScheduler.Registro
            registroNoScheduler;
    private static final EncounterSpawner SPAWNER = new EncounterSpawnerPadrao();

    private EncounterServerHooks() { }

    /** O controlador vivo, se houver servidor. Vazio no cliente e entre mundos. */
    public static Optional<EncounterController> controlador() {
        return Optional.ofNullable(controlador);
    }

    @SubscribeEvent
    public static void aoIniciar(ServerStartedEvent evento) {
        MinecraftServer servidor = evento.getServer();
        controlador = new EncounterController(EncounterSavedData.de(servidor),
                EncounterRules.campo());
        registroNoScheduler = com.darkcontinent.nenfoundation.server.NenTickScheduler
                .registrarDoMundo(EncounterServerHooks::tickDosEncontros);
        int corrigidos = controlador.reconciliarAoIniciar(servidor);
        if (corrigidos > 0) {
            LOG.info("Encontros reconciliados no start: {} episodio(s) corrigido(s).", corrigidos);
        }
    }

    @SubscribeEvent
    public static void aoParar(ServerStoppedEvent evento) {
        // Quem liga, desliga -- e aqui o par e literal: o registro no scheduler
        // e fechado antes de o controlador cair. Deixar o registro aberto faria
        // o laco central chamar um controlador nulo a cada tick do servidor
        // seguinte, e o log encheria de erro por um mundo que nem existe mais.
        if (registroNoScheduler != null) {
            registroNoScheduler.close();
            registroNoScheduler = null;
        }
        controlador = null;
    }

    /**
     * Um tick de todos os encontros, chamado pelo LACO UNICO.
     *
     * <p>Ele NAO assina o evento de tick do servidor por conta propria, e o
     * portao {@code NenRuntimeBoundaryTest} cobra isso: dois lacos globais nao
     * dao erro, so deixam a ordem entre eles indefinida -- e a ordem so passa a
     * importar no dia em que um depender do outro, quando ja e tarde para
     * descobrir qual roda primeiro. A primeira versao deste arquivo tinha o
     * proprio laco, e o portao reprovou na hora.</p>
     *
     * <p>O portao le a FONTE, entao o nome do evento nem aparece escrito aqui:
     * citar o que ele procura faria a explicacao reprovar junto com o defeito
     * que ela explica.</p>
     */
    private static void tickDosEncontros(MinecraftServer servidor) {
        if (controlador == null) return;
        controlador.tick(servidor, SPAWNER);
    }
}
