package com.darkcontinent.nenfoundation.enemy.greedisland;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * As medidas da ilha, e a barreira que a fecha.
 *
 * <p><b>ELAS MORAM AQUI E NO JSON, e isso e duplicacao consciente.</b> O
 * {@code noise_settings} precisa do raio para desenhar o relevo, e o servidor
 * precisa dele para saber onde por a barreira -- e os dois formatos nao se
 * leem. O que impede a duplicacao de virar divergencia e
 * {@code GeracaoDaIlhaTest}, que LE o JSON e compara com estes numeros. Sem o
 * portao, alguem afastaria a barreira num dia e a costa no outro, e o sintoma
 * seria nadar quinhentos blocos de mar vazio.
 *
 * <p><b>NAO SAO CONFIG.</b> Sao geometria de mundo: mudar o raio depois de a
 * ilha existir nao regenera os chunks ja gerados, entao o numero novo valeria
 * para uma parte do mapa e nao para a outra. Config que so funciona em mundo
 * novo e pior que constante.
 */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class GreedIslandGeografia {

    /** Onde a terra e garantida, em blocos a partir da origem. */
    public static final double RAIO_DA_TERRA = 620.0D;

    /** A faixa de costa: da praia ao mar aberto. */
    public static final double TRANSICAO = 150.0D;

    /**
     * O raio da barreira do mundo.
     *
     * <p>DEPOIS DA AGUA, e nao na praia: o pedido e que se veja mar antes do
     * limite. A costa acaba por volta de {@code RAIO_DA_TERRA + TRANSICAO} =
     * 770; a barreira em 880 deixa uns cem blocos de mar aberto -- o bastante
     * para a ilha parecer cercada, e pouco o bastante para ninguem remar meia
     * hora achando que ha algo la fora.
     */
    public static final double RAIO_DA_BARREIRA = 880.0D;

    /** Onde a ilha esta centrada. Origem, e o spawn da dimensao segue isso. */
    public static final double CENTRO_X = 0.0D;

    /** Idem. */
    public static final double CENTRO_Z = 0.0D;

    private GreedIslandGeografia() {
    }

    /**
     * Fecha a barreira quando a dimensao carrega.
     *
     * <p><b>PONTO CEGO DECLARADO, e ele e da vanilla.</b> Todo nivel tem um
     * {@code WorldBorder} proprio, mas eles nascem escravos do Overworld: o
     * servidor registra um ouvinte que copia qualquer mudanca de la para ca.
     * Rodar {@code /worldborder set} no Overworld SOBRESCREVE esta barreira, em
     * silencio. Em jogo normal ninguem mexe na barreira do mundo, e por isso o
     * custo foi aceito em vez de desmontar o ouvinte da vanilla -- mas quem
     * mexer precisa saber, e por isso esta escrito aqui e em
     * {@code o-que-nao-provamos.md}.
     *
     * <p>SEM AVISO DE APROXIMACAO: {@code warningBlocks} fica em zero. A tela
     * vermelha da vanilla existe para avisar que o mundo esta ENCOLHENDO; aqui
     * a barreira e fixa, e o vermelho constante na beira da praia leria como
     * dano.
     */
    @SubscribeEvent
    public static void aoCarregarNivel(LevelEvent.Load evento) {
        if (!(evento.getLevel() instanceof ServerLevel nivel)
                || !GreedIslandRegion.dentro(nivel.dimension())) {
            return;
        }
        WorldBorder barreira = nivel.getWorldBorder();
        barreira.setCenter(CENTRO_X, CENTRO_Z);
        // O tamanho da vanilla e o DIAMETRO, e nao o raio. Passar o raio aqui
        // poria a barreira na metade da distancia -- em cima da praia, e o
        // relato seria "a ilha esta cortada".
        barreira.setSize(RAIO_DA_BARREIRA * 2.0D);
        barreira.setWarningBlocks(0);
        barreira.setWarningTime(0);
        barreira.setDamageSafeZone(8.0D);
    }

    /** Onde a costa termina, para o portao conferir contra a barreira. */
    public static double fimDaCosta() {
        return RAIO_DA_TERRA + TRANSICAO;
    }
}
