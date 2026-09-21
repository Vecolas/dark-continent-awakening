package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.sound.NenSoundEvents;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * O zumbido grave que acompanha Ren enquanto ele durar.
 *
 * <p><b>UMA INSTANCIA POR JOGADOR, e ela morre com o dono.</b> O criterio de
 * aceite e literal: o loop morre no desligamento, na morte, no logout e na troca
 * de dimensao. Tres dos quatro caminhos sao cobertos por
 * {@link #canPlaySound()} e pela checagem de dono aqui dentro -- ou seja, pelo
 * CICLO DE VIDA do proprio som, e nao por lembretes espalhados pelos pontos de
 * saida. O quarto, o logout, passa por {@code AudioDeAura.limpar}, que e o
 * mesmo ponto que ja limpa tudo o mais.
 *
 * <p>Isso e deliberado e vale a pena dizer em voz alta: o erro numero 3 do
 * {@code CLAUDE.md} e limpeza espalhada pelos pontos de saida, com um deles
 * faltando -- e aqui o sintoma de faltar um seria um zumbido grave tocando para
 * sempre, sem nada na tela para associar a ele.
 *
 * <p>O VOLUME E O PITCH SOBEM COM O OUTPUT EFETIVO, e nao com a reserva. Reserva
 * grande nao e aura grande: aura e o que esta sendo liberado. Ligar o som a
 * reserva faria o zumbido CAIR justamente enquanto o jogador gasta.
 *
 * <p>O output chega por {@link IntSupplier} em milesimos, e nao como float
 * guardado: a fonte da verdade e o delta do servidor, e congelar o valor na
 * construcao seria o erro numero 1 da lista -- multiplicador congelado na
 * ativacao, cego a todo ajuste posterior.
 */
public final class ZumbidoDeRen extends AbstractTickableSoundInstance {

    /**
     * O volume com output zero, e o quanto ele cresce ate o output cheio.
     *
     * <p>O PISO NAO E ZERO de proposito: Ren com output baixo continua sendo
     * Ren, e um zumbido inaudivel faria a tecnica parecer desligada. O teto
     * modesto e o que impede um Ren a doze blocos de dominar a trilha de quem
     * esta perto -- o alcance ja e curto, e o volume acompanha.
     */
    private static final float VOLUME_MINIMO = 0.16F;
    private static final float VOLUME_MAXIMO = 0.46F;

    /** O pitch sobe pouco: muita variacao vira sirene, e sirene nao e pressao. */
    private static final float PITCH_MINIMO = 0.86F;
    private static final float PITCH_MAXIMO = 1.06F;

    /** Quanto o volume caminha por tick rumo ao alvo. */
    private static final float SUAVIZACAO = 0.12F;

    /**
     * Quem ja foi criado, para a regua saber quantos existem.
     *
     * <p>REGUA, e nao controle. O criterio de aceite da issue #192 e "UMA
     * instancia por jogador", e a unica forma de verificar isso sem confiar na
     * leitura do codigo e ter o numero na tela -- o mesmo motivo pelo qual a
     * contagem de faisca existe desde o AV0.
     *
     * <p><b>UMA LISTA, E NAO UM CONTADOR, e a razao e do motor.</b>
     * {@code AbstractTickableSoundInstance.stop()} e FINAL: nao ha onde
     * decrementar quando o gerenciador de som descarta a instancia por conta
     * propria. Um contador incrementado no construtor e decrementado a mao pelo
     * dono acabaria mentindo no unico caminho que ninguem lembra -- o descarte
     * em lote. A lista responde perguntando a cada instancia se ela ja parou,
     * que e a unica fonte que nao pode divergir.
     *
     * <p>ELA E PODADA NA LEITURA E NA CRIACAO, entao nao cresce sozinha com o
     * overlay desligado.
     */
    private static final java.util.List<ZumbidoDeRen> REGISTRO = new java.util.ArrayList<>();

    private final int donoId;
    private final IntSupplier outputEmMilesimos;
    private float volumeAtual = VOLUME_MINIMO;

    /**
     * @param outputEmMilesimos de 0 a 1000; perguntado A CADA TICK, nunca congelado
     */
    public ZumbidoDeRen(Entity dono, IntSupplier outputEmMilesimos) {
        super(NenSoundEvents.REN_LOOP.get(), SoundSource.PLAYERS,
                net.minecraft.client.resources.sounds.SoundInstance.createUnseededRandom());
        this.donoId = dono.getId();
        this.outputEmMilesimos = outputEmMilesimos;
        this.looping = true;
        // SEM ATRASO: o zumbido comeca junto do estouro. Um delay aqui abriria
        // um vao de silencio entre a liberacao e a presenca.
        this.delay = 0;
        this.volume = VOLUME_MINIMO;
        this.pitch = PITCH_MINIMO;
        this.x = dono.getX();
        this.y = dono.getY();
        this.z = dono.getZ();
        podar();
        REGISTRO.add(this);
    }

    /** Quantos zumbidos estao vivos agora. Regua do overlay de dev. */
    public static int vivos() {
        podar();
        return REGISTRO.size();
    }

    /** Quem liga, desliga: a contagem do mundo anterior nao sobrevive ao logout. */
    public static void limparContagem() {
        REGISTRO.clear();
    }

    private static void podar() {
        REGISTRO.removeIf(ZumbidoDeRen::isStopped);
    }

    /** O dono deste zumbido. Usado para garantir UMA instancia por jogador. */
    public int donoId() {
        return this.donoId;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        Entity dono = mc.level == null ? null : mc.level.getEntity(this.donoId);
        if (dono == null || dono.isRemoved() || !dono.isAlive()) {
            // MORTE E TROCA DE DIMENSAO PASSAM POR AQUI. Os dois removem a
            // entidade do nivel do cliente, e sem esta guarda o zumbido
            // continuaria tocando na ultima posicao conhecida -- para sempre.
            stop();
            return;
        }
        // O SOM SEGUE O DONO. Sem isto ele ficaria preso onde Ren comecou, e
        // andar em Ren deixaria a propria presenca para tras.
        this.x = dono.getX();
        this.y = dono.getY();
        this.z = dono.getZ();

        float alvo = volumePara(this.outputEmMilesimos.getAsInt());
        // A SUAVIZACAO EXISTE PORQUE O OUTPUT CHEGA EM DEGRAUS. O delta do
        // servidor so e reenviado quando muda acima de um limiar, e saltar o
        // volume junto produziria um degrau audivel a cada pacote.
        this.volumeAtual += (alvo - this.volumeAtual) * SUAVIZACAO;
        this.volume = this.volumeAtual;
        this.pitch = pitchPara(this.outputEmMilesimos.getAsInt());
    }

    @Override
    public boolean canPlaySound() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getEntity(this.donoId) != null;
    }

    /** O volume para um output em milesimos. Pura, e por isso provavel sem o jogo. */
    public static float volumePara(int outputEmMilesimos) {
        float t = Math.clamp(outputEmMilesimos / 1000.0F, 0.0F, 1.0F);
        return VOLUME_MINIMO + (VOLUME_MAXIMO - VOLUME_MINIMO) * t;
    }

    /** O pitch para um output em milesimos. */
    public static float pitchPara(int outputEmMilesimos) {
        float t = Math.clamp(outputEmMilesimos / 1000.0F, 0.0F, 1.0F);
        return PITCH_MINIMO + (PITCH_MAXIMO - PITCH_MINIMO) * t;
    }
}
