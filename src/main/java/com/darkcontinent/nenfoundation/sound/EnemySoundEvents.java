package com.darkcontinent.nenfoundation.sound;

import com.darkcontinent.nenfoundation.NenFoundation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A voz dos inimigos: cinco sons por bicho, registrados na FILA JA EXISTENTE.
 *
 * <p><b>Ela reusa {@link NenSoundEvents#SOUND_EVENTS} de proposito.</b> Um
 * {@code DeferredRegister} proprio para sons de mob funcionaria -- e seria a
 * segunda fila que a issue #266 pagou caro para eliminar do lado das entidades.
 * Duas filas nao dao erro: dao um conjunto que fica de fora de tudo que o outro
 * ganhar depois, e o sintoma e um mob mudo que ninguem consegue explicar.</p>
 *
 * <p><b>Por que cinco, e nao um.</b> Ambiente diz onde ele esta antes de voce
 * ver; alerta diz que ele percebeu voce, e e a unica chance de recuar; ataque
 * casa com o WINDUP, e nao com o dano; dor confirma o acerto que a barra de vida
 * nao confirma; morte fecha. Faltando alerta, o mob ataca do nada. Com o som de
 * ataque tocando junto com o dano, o aviso chega quando ja nao da tempo --
 * nenhum dos dois da erro, e os dois produzem um jogo que parece injusto.</p>
 *
 * <p><b>A lista de ids e derivada, e nao escrita duas vezes.</b> Ela sai do mesmo
 * conjunto que o gerador de audio usa; um mob que entrar la e nao aqui ficaria
 * com arquivo no disco e sem evento registrado -- e o portao
 * {@code VozDeInimigoTest} reprova exatamente isso, dos dois lados.</p>
 */
public final class EnemySoundEvents {

    /**
     * Reexportado de {@link EnemyVoiceCatalog} -- a lista NAO mora aqui.
     *
     * <p>Ela ficou neste arquivo por uma versao, e o portao de voz nao conseguia
     * ler: carregar esta classe puxa {@code DeferredRegister}, que exige o
     * bootstrap do Minecraft, e um teste puro morria em
     * {@code ExceptionInInitializerError} antes de conferir qualquer coisa. Uma
     * lista que o portao nao alcanca e uma lista sem portao.</p>
     */
    public static final List<String> MOMENTOS = EnemyVoiceCatalog.MOMENTOS;

    /** Reexportado de {@link EnemyVoiceCatalog}; ver a nota acima. */
    public static final List<String> COM_VOZ = EnemyVoiceCatalog.COM_VOZ;

    /**
     * Alcance dos sons de mob, em blocos.
     *
     * <p>NAO e botao de balanceamento: e a distancia em que o aviso ainda serve
     * de aviso. Dezesseis blocos sao mais do que o alcance de percepcao da
     * maioria dos mobs, de proposito -- o jogador precisa poder ouvir o bicho
     * ANTES de entrar no campo de visao dele, senao o som deixa de ser
     * informacao e vira trilha sonora.</p>
     */
    static final float ALCANCE_DE_VOZ = 16.0F;

    private static final Map<String, EnemyVoice> VOZES = new LinkedHashMap<>();

    static {
        for (String mob : COM_VOZ) {
            VOZES.put(mob, new EnemyVoice(
                    registrar(mob, "ambient"), registrar(mob, "alert"),
                    registrar(mob, "attack"), registrar(mob, "hurt"),
                    registrar(mob, "death")));
        }
    }

    private EnemySoundEvents() { }

    private static DeferredHolder<SoundEvent, SoundEvent> registrar(String mob, String momento) {
        String id = "entity." + mob + "." + momento;
        return NenSoundEvents.SOUND_EVENTS.register(id,
                () -> SoundEvent.createFixedRangeEvent(NenFoundation.id(id), ALCANCE_DE_VOZ));
    }

    /**
     * A voz de um mob, se ele tiver uma.
     *
     * <p>Vazio NAO e erro: mob sem voz e um mob que ainda nao ganhou identidade
     * sonora, e tratar isso como falha derrubaria o carregamento por causa de
     * uma ausencia que o jogo tolera. O que NAO se pode e fingir que existe: quem
     * chama precisa distinguir "sem voz" de "voz silenciosa".</p>
     */
    public static Optional<EnemyVoice> voz(String mobId) {
        return Optional.ofNullable(VOZES.get(mobId));
    }

    /** Todas as vozes, para portao e para diagnostico. */
    public static Map<String, EnemyVoice> todas() { return Map.copyOf(VOZES); }
}
