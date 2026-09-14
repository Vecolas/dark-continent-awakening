package com.darkcontinent.nenfoundation.enemy.encounter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O relogio dos encontros: arma, ativa, conclui, falha e recarrega.
 *
 * <p><b>O caso que este arquivo inteiro existe para resolver e o RESTART.</b> Um
 * encontro ativo quando o servidor cai volta com o estado salvo dizendo ACTIVE, e
 * a pergunta que decide tudo e: <em>as entidades daquele episodio sobreviveram?</em>
 * Elas costumam sobreviver -- entidades persistem no chunk. Quem nao perguntar
 * spawna outra leva por cima, e o resultado nao e um erro: sao dois chefes, e a
 * recompensa sai duas vezes. Quem perguntar errado -- assumindo que nunca
 * sobrevivem -- produz o oposto: um encontro que nunca mais acontece.</p>
 *
 * <p><b>A reconciliacao acontece UMA vez, ao iniciar</b>, e nao a cada tick. A
 * cada tick ela custaria uma consulta por entidade por encontro; uma vez, ela
 * custa nada e responde a unica pergunta que importa.</p>
 *
 * <p><b>Episodio interrompido pelo restart NAO vira sucesso.</b> Se nenhuma
 * entidade sobreviveu, o episodio FALHOU -- e nao "foi concluido". A diferenca
 * decide se a recompensa e paga, e assumir sucesso seria pagar por um combate que
 * ninguem terminou.</p>
 */
public final class EncounterController {

    private static final Logger LOG = LoggerFactory.getLogger(EncounterController.class);

    private final EncounterSavedData dados;
    private final EncounterRules regras;

    public EncounterController(EncounterSavedData dados, EncounterRules regras) {
        this.dados = Objects.requireNonNull(dados, "save de encontros ausente");
        this.regras = Objects.requireNonNull(regras, "regras de encontro ausentes");
    }

    public EncounterSavedData dados() { return dados; }
    public EncounterRules regras() { return regras; }

    // ------------------------------------------------------- reconciliacao

    /**
     * Ao iniciar o servidor: confere quem ainda existe e conserta o que sobrou.
     *
     * <p>Chame UMA vez, depois de os niveis estarem carregados. As entidades de um
     * chunk descarregado nao sao encontraveis -- e isso e tratado como "existe",
     * e nao como "sumiu": marcar sumido um bicho que so esta dormindo no disco
     * faria o encontro spawnar de novo assim que alguem se aproximasse, e o
     * primeiro apareceria junto.</p>
     *
     * @return quantos episodios foram corrigidos
     */
    public int reconciliarAoIniciar(MinecraftServer servidor) {
        Objects.requireNonNull(servidor, "servidor ausente");
        int corrigidos = 0;
        for (EncounterInstance instancia : dados.instancias().values()) {
            if (!instancia.estado().temEntidadesVivas()) continue;

            ServerLevel nivel = servidor.getLevel(instancia.dimensao());
            if (nivel == null) {
                // A dimensao sumiu (datapack removido). O episodio nao tem onde
                // acontecer, e insistir nele deixaria um encontro eternamente ativo
                // num lugar que nao existe.
                LOG.warn("Encontro {} aponta para a dimensao ausente {}; marcando como falho.",
                        instancia.id(), instancia.dimensao().location());
                instancia.estado(EncounterState.FAILED);
                corrigidos++;
                continue;
            }

            Set<UUID> sobreviventes = new LinkedHashSet<>();
            boolean algumChunkDormindo = false;
            for (UUID id : instancia.entidades()) {
                Entity entidade = nivel.getEntity(id);
                if (entidade != null && entidade.isAlive()) {
                    sobreviventes.add(id);
                } else if (entidade == null && !nivel.isLoaded(instancia.ancora())) {
                    // Nao achar a entidade com o chunk do ancoradouro descarregado
                    // nao prova nada: ela pode estar no disco. Tratar como sumida
                    // aqui e o caminho direto para dois chefes.
                    sobreviventes.add(id);
                    algumChunkDormindo = true;
                }
            }

            if (sobreviventes.isEmpty()) {
                LOG.info("Encontro {} voltou sem nenhuma entidade viva; o episodio FALHOU"
                        + " (nao foi concluido -- ninguem terminou o combate).", instancia.id());
                instancia.estado(EncounterState.FAILED);
                corrigidos++;
                continue;
            }

            if (sobreviventes.size() != instancia.entidades().size()) {
                for (UUID id : Set.copyOf(instancia.entidades())) {
                    if (!sobreviventes.contains(id)) instancia.esquecerEntidade(id);
                }
                corrigidos++;
            }
            if (algumChunkDormindo) {
                LOG.debug("Encontro {}: parte das entidades esta em chunk descarregado e foi"
                        + " mantida no registro de proposito.", instancia.id());
            }
        }
        if (corrigidos > 0) dados.sujar();
        return corrigidos;
    }

    // --------------------------------------------------------------- ciclo

    /**
     * Um tick de TODOS os encontros. Barato: nao consulta entidade fora de ACTIVE.
     *
     * @param spawner quem cria as entidades quando um encontro ativa
     */
    public void tick(MinecraftServer servidor, EncounterSpawner spawner) {
        Objects.requireNonNull(servidor, "servidor ausente");
        Objects.requireNonNull(spawner, "spawner ausente");
        boolean mudou = false;
        for (EncounterInstance instancia : dados.instancias().values()) {
            instancia.tick();
            ServerLevel nivel = servidor.getLevel(instancia.dimensao());
            if (nivel == null) continue;
            mudou |= switch (instancia.estado()) {
                case ARMED -> tickArmado(nivel, instancia, spawner);
                case ACTIVE -> tickAtivo(nivel, instancia);
                case COOLDOWN -> tickRecarga(instancia);
                default -> false;
            };
        }
        if (mudou) dados.sujar();
    }

    /** Armado: espera alguem entrar no raio de ativacao. */
    private boolean tickArmado(ServerLevel nivel, EncounterInstance instancia, EncounterSpawner spawner) {
        Optional<ServerPlayer> chegou = jogadorNoRaio(nivel, instancia, regras.raioDeAtivacao());
        if (chegou.isEmpty()) return false;

        instancia.estado(EncounterState.ACTIVE);
        instancia.entrar(chegou.get().getUUID());

        List<UUID> criados = spawner.spawnar(nivel, instancia);
        if (criados.isEmpty()) {
            // Spawn falhou (sem espaco, chunk fechando). O episodio NAO comecou --
            // deixa-lo ACTIVE sem entidade produziria um encontro que nunca conclui
            // e nunca falha, e o ancoradouro ficaria morto para sempre.
            LOG.warn("Encontro {} ativou e nao conseguiu spawnar nada; falhando o episodio.",
                    instancia.id());
            instancia.estado(EncounterState.FAILED);
            return true;
        }
        criados.forEach(instancia::registrarEntidade);
        return true;
    }

    /** Ativo: conclui quando tudo morre, falha quando todos vao embora. */
    private boolean tickAtivo(ServerLevel nivel, EncounterInstance instancia) {
        boolean mudou = false;
        for (UUID id : instancia.entidades()) {
            Entity entidade = nivel.getEntity(id);
            if (entidade == null || !entidade.isAlive()) {
                if (nivel.isLoaded(instancia.ancora()) && instancia.esquecerEntidade(id)) mudou = true;
            }
        }

        atualizarParticipantes(nivel, instancia);

        if (instancia.entidades().isEmpty()) {
            instancia.estado(EncounterState.COMPLETED);
            return true;
        }
        if (instancia.participantes().isEmpty()
                && instancia.ticksNoEstado() >= regras.ticksSemParticipante()) {
            instancia.estado(EncounterState.FAILED);
            return true;
        }
        return mudou;
    }

    /** Recarga: so um relogio. Zero de cooldown significa "nao volta". */
    private boolean tickRecarga(EncounterInstance instancia) {
        if (!regras.repetivel()) return false;
        if (instancia.ticksNoEstado() < regras.ticksDeCooldown()) return false;
        instancia.estado(EncounterState.ARMED);
        return true;
    }

    /**
     * Quem ainda esta perto o bastante para contar como participante.
     *
     * <p>Entrar usa o raio de ATIVACAO e sair usa o de ABANDONO, que e maior. A
     * histerese nao e refinamento: sem ela o jogador na borda entra e sai do
     * episodio a cada passo, e o encontro pisca entre ativo e falho.</p>
     */
    private void atualizarParticipantes(ServerLevel nivel, EncounterInstance instancia) {
        for (UUID id : instancia.participantes()) {
            ServerPlayer jogador = nivel.getServer().getPlayerList().getPlayer(id);
            if (jogador == null || jogador.level() != nivel
                    || jogador.distanceToSqr(centro(instancia)) > quadrado(regras.raioDeAbandono())) {
                instancia.sair(id);
            }
        }
        jogadorNoRaio(nivel, instancia, regras.raioDeAtivacao())
                .ifPresent(jogador -> instancia.entrar(jogador.getUUID()));
    }

    private Optional<ServerPlayer> jogadorNoRaio(ServerLevel nivel, EncounterInstance instancia,
            double raio) {
        net.minecraft.world.phys.Vec3 centro = centro(instancia);
        return nivel.players().stream()
                .filter(jogador -> !jogador.isSpectator())
                .filter(jogador -> jogador.distanceToSqr(centro) <= quadrado(raio))
                .findFirst();
    }

    private static net.minecraft.world.phys.Vec3 centro(EncounterInstance instancia) {
        return net.minecraft.world.phys.Vec3.atCenterOf(instancia.ancora());
    }

    private static double quadrado(double valor) { return valor * valor; }

    // --------------------------------------------------------- recompensa

    /**
     * Paga a recompensa UMA vez por episodio.
     *
     * <p>A trava e consultada e gravada numa operacao so. "Consultar e depois
     * gravar" abre entre as duas linhas exatamente a janela em que o segundo
     * jogador entra -- e o item sai em dobro sem nenhum erro.</p>
     *
     * @return true quando o chamador DEVE pagar agora
     */
    public boolean travarRecompensa(EncounterInstance instancia, String rewardId) {
        Objects.requireNonNull(instancia, "encontro ausente");
        if (instancia.estado() != EncounterState.COMPLETED) {
            throw new IllegalStateException("recompensa pedida para o encontro " + instancia.id()
                    + " em estado " + instancia.estado() + ": so COMPLETED paga. Pagar antes"
                    + " premiaria quem abandonou o combate no meio.");
        }
        boolean travou = dados.ledger().travar(instancia.id(), rewardId);
        if (travou) dados.sujar();
        return travou;
    }

    /**
     * Reset administrativo: devolve o encontro ao inicio e destrava a recompensa.
     *
     * <p>Existe para o operador consertar um encontro quebrado -- e por isso ele
     * e EXPLICITO. Automatizar o destravamento, por tempo ou por qualquer gatilho,
     * transformaria a trava contra duplicata num simples atraso.</p>
     */
    public void resetAdministrativo(EncounterInstance instancia) {
        Objects.requireNonNull(instancia, "encontro ausente");
        dados.ledger().limparEncontro(instancia.id());
        // DORMANT, e nao ARMED: o reset nao deve ligar o encontro na cara de quem
        // estiver por perto no momento em que o operador digitou o comando.
        if (instancia.estado() != EncounterState.DORMANT) {
            if (!EncounterTransitions.permitida(instancia.estado(), EncounterState.DORMANT)) {
                // Caminho legal mais curto ate o inicio, sem inventar transicao nova.
                if (EncounterTransitions.permitida(instancia.estado(), EncounterState.FAILED)) {
                    instancia.estado(EncounterState.FAILED);
                }
                instancia.estado(EncounterState.COOLDOWN);
            }
            instancia.estado(EncounterState.DORMANT);
        }
        dados.sujar();
    }
}
