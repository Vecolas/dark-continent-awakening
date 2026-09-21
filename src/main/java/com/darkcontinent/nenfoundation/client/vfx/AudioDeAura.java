package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.sound.NenSoundEvents;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Reproduz o audio posicional que pertence a sessao visual da aura.
 *
 * <p><b>UM DONO SO PARA TODO O AUDIO DA AURA, e nao um controlador por
 * tecnica.</b> A issue #192 propunha um {@code AuraAudioController} novo ao lado
 * deste arquivo; seriam dois objetos guardando {@link SoundInstance} do mesmo
 * jogador, com dois caminhos de limpeza. O erro numero 3 do {@code CLAUDE.md} e
 * exatamente isso -- limpeza espalhada, com um ponto de saida faltando --, e o
 * sintoma aqui seria o pior possivel: um zumbido grave tocando para sempre, sem
 * nada na tela para associar a ele. Ten e Ren dividem um dono.
 *
 * <p>O som usa {@link SoundSource#PLAYERS}: ele acompanha o controle de volume
 * de jogadores, em vez de obrigar alguem a desligar todos os efeitos.
 *
 * <p>NENHUM SOM DE ELETRICIDADE. Os tres assets de Ren sao graves, escuros e sem
 * transiente seco -- o ouvido classifica o efeito antes do olho, e um estalo
 * agudo transformaria a aura em eletricidade mesmo com o render certo. Ver
 * {@code art-source/sons/aura.py}.
 */
public final class AudioDeAura {

    private static final float VOLUME_DA_ATIVACAO = 0.55F;

    /**
     * O estouro de Ren e mais alto que a ativacao de Ten, e nao muito.
     *
     * <p>Ren e um gesto maior, e o som acompanha -- mas o que cresce com o poder
     * e a densidade, e nao o dominio sobre a trilha de quem esta perto. O
     * alcance curto do evento e a outra metade dessa decisao.
     */
    private static final float VOLUME_DO_ESTOURO_DE_REN = 0.70F;

    private static final float VOLUME_DO_FECHAMENTO = 0.45F;

    private final Map<Integer, SoundInstance> ativacoes = new HashMap<>();

    /**
     * Um zumbido por jogador, e no maximo um.
     *
     * <p>A CHAVE E O ID DA ENTIDADE, e a insercao passa sempre por
     * {@link #ligarZumbido}, que para o anterior antes. Sem essa disciplina,
     * duas ativacoes rapidas empilhariam duas instancias do mesmo loop -- e o
     * resultado nao seria um erro, seria um zumbido com o dobro do volume que
     * ninguem consegue explicar.
     */
    private final Map<Integer, ZumbidoDeRen> zumbidos = new HashMap<>();

    // Reutilizado todo tick: audio nao justifica uma colecao descartavel por
    // quadro, multiplicada por cada cliente e por toda a sessao.
    private final Set<Integer> presentes = new HashSet<>();
    private final DetectorDeAtivacaoDeTen detectorDeTerceiros =
            new DetectorDeAtivacaoDeTen();
    private ClientLevel nivelAnterior;

    /**
     * Atualiza o audio do jogador local e dos jogadores observados.
     *
     * <p>A instancia e presa ao dono -- inclusive enquanto ele se move -- e tem
     * alcance fixo, declarado em {@code NenSoundEvents}.
     *
     * @param ativacaoLocal   borda {@code OFF -> TEN} da sessao local
     * @param ativacaoDeRen   borda {@code TEN -> REN} da sessao local
     * @param saidaDeRen      borda que SAI de Ren, por qualquer caminho
     * @param outputLocal     o output efetivo do jogador local, em milesimos
     * @param sinalDe         o sinal percebido de um terceiro, por id de entidade
     */
    public void aoTick(Minecraft mc, boolean ativacaoLocal, boolean ativacaoDeRen,
            boolean saidaDeRen, IntSupplier outputLocal, IntFunction<SinalDeAura> sinalDe) {
        if (mc.level != this.nivelAnterior) {
            // TROCA DE DIMENSAO. Ela recria o nivel do cliente, e sem esta
            // linha os zumbidos do mundo anterior continuariam vivos presos a
            // ids de entidade que ja nao significam nada.
            pararTudo(mc);
            this.detectorDeTerceiros.limpar();
            this.nivelAnterior = mc.level;
        }
        descartarConcluidas(mc);
        if (mc.level == null || mc.player == null || !mc.player.isAlive()) {
            // MORTE E LOGOUT. O mesmo ponto, e nao dois.
            pararTudo(mc);
            return;
        }

        if (ativacaoLocal) {
            tocar(mc, mc.player, NenSoundEvents.TEN_ACTIVATE.get(), VOLUME_DA_ATIVACAO);
        }
        if (ativacaoDeRen) {
            tocar(mc, mc.player, NenSoundEvents.REN_BURST.get(), VOLUME_DO_ESTOURO_DE_REN);
            ligarZumbido(mc, mc.player, outputLocal);
            // SO O JOGADOR LOCAL. O impulso nasce aqui, dentro do ramo que ja e
            // exclusivo dele -- e nao numa checagem de "e o meu jogador?" mais
            // adiante, que alguem poderia mover por engano para o laco dos
            // outros.
            ImpulsoDeCamera.disparar();
        }
        if (saidaDeRen) {
            tocar(mc, mc.player, NenSoundEvents.REN_RELEASE.get(), VOLUME_DO_FECHAMENTO);
            desligarZumbido(mc, mc.player.getId());
        }

        this.presentes.clear();
        this.presentes.add(mc.player.getId());
        for (Player jogador : mc.level.players()) {
            if (jogador == mc.player) {
                continue;
            }
            this.presentes.add(jogador.getId());
            SinalDeAura atual = sinalDe.apply(jogador.getId());
            SinalDeAura anterior = this.detectorDeTerceiros.registrar(jogador.getId(), atual);

            if (anterior == SinalDeAura.NENHUM && atual == SinalDeAura.TEN) {
                tocar(mc, jogador, NenSoundEvents.TEN_ACTIVATE.get(), VOLUME_DA_ATIVACAO);
            }
            if (DetectorDeAtivacaoDeTen.entrouEmLiberacao(anterior, atual)) {
                tocar(mc, jogador, NenSoundEvents.REN_BURST.get(), VOLUME_DO_ESTOURO_DE_REN);
                // O OUTPUT DE UM TERCEIRO NAO CHEGA AO CLIENTE, e nao deve: o
                // sinal e deliberadamente pobre (ver `SinalDeAura`). O zumbido
                // do vizinho toca num volume medio fixo -- e essa limitacao esta
                // declarada aqui em vez de descoberta depois como "o zumbido dos
                // outros nao reage".
                ligarZumbido(mc, jogador, () -> 500);
            }
            if (DetectorDeAtivacaoDeTen.saiuDeLiberacao(anterior, atual)) {
                tocar(mc, jogador, NenSoundEvents.REN_RELEASE.get(), VOLUME_DO_FECHAMENTO);
                desligarZumbido(mc, jogador.getId());
            }
        }
        this.detectorDeTerceiros.reterSomente(this.presentes);
        // QUEM SAIU DO ALCANCE LEVA O ZUMBIDO JUNTO. Sem esta poda, alguem que
        // se afasta em Ren deixaria a instancia viva -- o proprio som se cala
        // pela distancia, mas o objeto continuaria ticando para sempre.
        this.zumbidos.keySet().removeIf(id -> {
            if (this.presentes.contains(id)) {
                return false;
            }
            SoundInstance instancia = this.zumbidos.get(id);
            mc.getSoundManager().stop(instancia);
            return true;
        });
        ImpulsoDeCamera.aoTick();
    }

    /** Quem liga, desliga: logout, morte e dimensao passam por aqui. */
    public void limpar(Minecraft mc) {
        pararTudo(mc);
        this.detectorDeTerceiros.limpar();
        this.presentes.clear();
        this.nivelAnterior = null;
        ImpulsoDeCamera.limpar();
        ZumbidoDeRen.limparContagem();
    }

    /** Quantos zumbidos existem agora. Regua do overlay de dev. */
    public int zumbidosVivos() {
        return this.zumbidos.size();
    }

    private void tocar(Minecraft mc, Player dono, SoundEvent evento, float volume) {
        parar(mc, dono.getId());
        SoundInstance instancia = new EntityBoundSoundInstance(
                evento, SoundSource.PLAYERS, volume, 1.0F, dono,
                SoundInstance.createUnseededRandom().nextLong());
        this.ativacoes.put(dono.getId(), instancia);
        mc.getSoundManager().play(instancia);
    }

    /**
     * Liga o zumbido deste jogador, parando o anterior se houver.
     *
     * <p>PARAR ANTES DE LIGAR e o que garante UMA instancia por jogador. Duas
     * ativacoes rapidas -- Ren, Ten, Ren -- passariam duas vezes por aqui, e sem
     * a parada o segundo loop se somaria ao primeiro.
     */
    private void ligarZumbido(Minecraft mc, Player dono, IntSupplier output) {
        desligarZumbido(mc, dono.getId());
        ZumbidoDeRen zumbido = new ZumbidoDeRen(dono, output);
        this.zumbidos.put(dono.getId(), zumbido);
        mc.getSoundManager().play(zumbido);
    }

    private void desligarZumbido(Minecraft mc, int entidadeId) {
        ZumbidoDeRen anterior = this.zumbidos.remove(entidadeId);
        if (anterior != null) {
            mc.getSoundManager().stop(anterior);
        }
    }

    private void descartarConcluidas(Minecraft mc) {
        this.ativacoes.entrySet().removeIf(
                entrada -> !mc.getSoundManager().isActive(entrada.getValue()));
        // O ZUMBIDO TAMBEM SAI DO MAPA quando ele mesmo se para -- e ele se
        // para sozinho quando o dono morre ou some (ver ZumbidoDeRen.tick).
        // Sem esta linha, o mapa guardaria instancias mortas e
        // `zumbidosVivos()` mentiria para o overlay.
        this.zumbidos.entrySet().removeIf(entrada -> entrada.getValue().isStopped());
    }

    private void parar(Minecraft mc, int entidadeId) {
        SoundInstance anterior = this.ativacoes.remove(entidadeId);
        if (anterior != null) {
            mc.getSoundManager().stop(anterior);
        }
    }

    private void pararTudo(Minecraft mc) {
        for (SoundInstance instancia : this.ativacoes.values()) {
            mc.getSoundManager().stop(instancia);
        }
        this.ativacoes.clear();
        for (ZumbidoDeRen zumbido : this.zumbidos.values()) {
            mc.getSoundManager().stop(zumbido);
        }
        this.zumbidos.clear();
    }
}
