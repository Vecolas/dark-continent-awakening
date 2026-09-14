package com.darkcontinent.nenfoundation.sound;

import java.util.List;

/**
 * QUEM tem voz e QUAIS momentos ela cobre -- dados puros, sem registro nenhum.
 *
 * <p><b>Separado de {@link EnemySoundEvents} de proposito, e o motivo e de
 * PORTAO.</b> A classe de registro toca {@code DeferredRegister}, que exige o
 * bootstrap do Minecraft; um teste puro que a carregasse morreria em
 * {@code ExceptionInInitializerError} antes de conferir qualquer coisa -- e o
 * portao mais importante deste dominio (o que confere se o arquivo e mesmo Ogg
 * Vorbis) nao roda com servidor de pe. Aqui as listas ficam alcancaveis sem
 * carregar registro nenhum.</p>
 *
 * <p><b>Uma lista, e nao duas.</b> O gerador de audio em
 * {@code art-source/sons/inimigos.py} e esta lista precisam concordar; quando
 * divergirem, o mob tem arquivo no disco e nenhum evento (o som existe e nunca
 * toca) ou evento sem arquivo (o jogo tenta tocar, nao acha, e nao reclama). Os
 * dois casos sao cobrados por {@code VozDeInimigoTest}, nos dois sentidos.</p>
 */
public final class EnemyVoiceCatalog {

    /**
     * Os cinco momentos, na ordem do ENCONTRO e nao alfabetica.
     *
     * <p>Ela aparece assim aqui e no gerador de audio pelo mesmo motivo: quem ler
     * os dois lados tem de ver a mesma sequencia, senao a correspondencia entre
     * arquivo e evento passa a depender de conferir nome por nome.</p>
     */
    public static final List<String> MOMENTOS =
            List.of("ambient", "alert", "attack", "hurt", "death");

    /**
     * Os vinte e quatro bichos com voz.
     *
     * <p>ACRESCENTE O MOB AQUI NO MESMO PR QUE GERA OS .ogg DELE.</p>
     */
    public static final List<String> COM_VOZ = List.of(
            // exame hunter
            "great_stamp", "foxbear", "frog_in_waiting", "man_faced_ape",
            "spider_eagle", "master_of_the_swamp", "kiriko",
            // ferramenta
            "dummy_enemy",
            // greed island
            "cyclops", "hyper_puffball", "melanin_lizard", "radio_rat",
            "bubble_horse", "king_white_stag_beetle", "wolf_pack_hunter",
            // chimera
            "crab_heavy", "bat_scout", "wolf_runner", "spider_webber",
            "mosquito_officer", "multiarm_centipede", "cheetah_leader",
            "avian_commander", "scorpion_leader");

    private EnemyVoiceCatalog() { }
}
