package com.darkcontinent.nenfoundation.sound;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registro unico dos sons autorais do Nen Foundation. */
public final class NenSoundEvents {

    /** Alcance de projeto: a doze blocos a ativacao ja deve estar inaudivel. */
    static final float ALCANCE_DA_ATIVACAO_DE_TEN = 10.0F;

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, NenFoundation.MOD_ID);

    /**
     * Alcance do estouro de Ren.
     *
     * <p>DEZESSEIS BLOCOS, e nao mais. Ren e mais alto que Ten, e a tentacao e
     * dar-lhe alcance a altura -- mas um Ren a quarenta blocos dominando a
     * trilha sonora de quem esta perto e exatamente o que a issue #192 reprova.
     * O que cresce com o poder e a DENSIDADE do som, e nao a distancia em que
     * ele se impoe.
     */
    static final float ALCANCE_DO_ESTOURO_DE_REN = 16.0F;

    /** Alcance do zumbido continuo. Mais curto que o estouro: presenca, nao anuncio. */
    static final float ALCANCE_DO_LOOP_DE_REN = 12.0F;

    public static final DeferredHolder<SoundEvent, SoundEvent> TEN_ACTIVATE =
            SOUND_EVENTS.register("vfx.nen.ten_activate", () -> SoundEvent.createFixedRangeEvent(
                    NenFoundation.id("vfx.nen.ten_activate"), ALCANCE_DA_ATIVACAO_DE_TEN));

    /** O estouro de pressao da ativacao de Ren. Abstrato e grave -- nunca eletrico. */
    public static final DeferredHolder<SoundEvent, SoundEvent> REN_BURST =
            SOUND_EVENTS.register("vfx.nen.ren_burst", () -> SoundEvent.createFixedRangeEvent(
                    NenFoundation.id("vfx.nen.ren_burst"), ALCANCE_DO_ESTOURO_DE_REN));

    /** O zumbido enquanto Ren estiver ligado. UMA instancia por jogador. */
    public static final DeferredHolder<SoundEvent, SoundEvent> REN_LOOP =
            SOUND_EVENTS.register("vfx.nen.ren_loop", () -> SoundEvent.createFixedRangeEvent(
                    NenFoundation.id("vfx.nen.ren_loop"), ALCANCE_DO_LOOP_DE_REN));

    /** A succao curta que fecha ao sair de Ren. */
    public static final DeferredHolder<SoundEvent, SoundEvent> REN_RELEASE =
            SOUND_EVENTS.register("vfx.nen.ren_release", () -> SoundEvent.createFixedRangeEvent(
                    NenFoundation.id("vfx.nen.ren_release"), ALCANCE_DO_LOOP_DE_REN));

    private NenSoundEvents() {}

    /**
     * Mantem a chamada de registro num lugar so.
     *
     * <p><b>A PRIMEIRA LINHA E UMA CORRECAO DE DEFEITO, e nao cerimonia.</b>
     * {@link EnemySoundEvents} registra 120 eventos NESTA fila, e o faz no bloco
     * estatico dele. Mas nada carregava aquela classe durante o registro: o
     * unico caminho ate ela era {@code BaseHxHMob.getVoice()}, chamado quando um
     * mob tenta falar -- ou seja, DEPOIS de {@code RegisterEvent} ter fechado o
     * {@code DeferredRegister}.
     *
     * <p>O resultado nao era um som faltando: era
     * {@code ExceptionInInitializerError} no primeiro bicho que abrisse a boca,
     * derrubando o servidor. O {@code runGameTestServer} morria no primeiro lote
     * dos 143 testes, e o {@code runServer} sozinho nunca chegava la porque
     * nenhum mob nasce num mundo vazio -- que e por que isto sobreviveu a um
     * marco inteiro sem ninguem ver.
     *
     * <p>Forcar a inicializacao AQUI e o lugar certo: quem abre a fila e quem
     * garante que todos os contribuintes dela ja chegaram. A alternativa --
     * chamar de {@code NenFoundation.java} -- tocaria o arquivo mais hostil do
     * repositorio para dizer a mesma coisa.
     *
     * <p>O portao disto e o proprio {@code runGameTestServer}: com o defeito, ele
     * crasha no primeiro lote; sem ele, os 143 testes rodam.
     */
    public static void register(IEventBus modEventBus) {
        EnemySoundEvents.forcarRegistro();
        SOUND_EVENTS.register(modEventBus);
    }
}
