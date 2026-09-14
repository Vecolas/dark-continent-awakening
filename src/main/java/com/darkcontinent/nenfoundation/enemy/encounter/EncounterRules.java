package com.darkcontinent.nenfoundation.enemy.encounter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Os quatro relogios de um encontro. Todos sao botao de balanceamento.
 *
 * <p>Eles se leem JUNTOS. Raio de ativacao maior que o de abandono faria o
 * encontro ligar e desligar a cada passo do jogador na borda -- e isso nao da
 * erro: da um chefe que spawna e desaparece em alternancia, o que o jogador le
 * como bug de rede. Por isso a relacao entre os dois e COBRADA aqui.</p>
 *
 * @param raioDeAtivacao a que distancia do ancoradouro o encontro liga
 * @param raioDeAbandono a que distancia ele considera que todos foram embora;
 *        tem de ser MAIOR que o de ativacao, com folga
 * @param ticksSemParticipante quanto tempo vazio antes de FAILED
 * @param ticksDeCooldown quanto tempo o LUGAR espera antes de armar de novo
 */
public record EncounterRules(double raioDeAtivacao, double raioDeAbandono,
        int ticksSemParticipante, int ticksDeCooldown) {

    public static final Codec<EncounterRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("raio_de_ativacao").forGetter(EncounterRules::raioDeAtivacao),
            Codec.DOUBLE.fieldOf("raio_de_abandono").forGetter(EncounterRules::raioDeAbandono),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("ticks_sem_participante")
                    .forGetter(EncounterRules::ticksSemParticipante),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("ticks_de_cooldown")
                    .forGetter(EncounterRules::ticksDeCooldown))
            .apply(instance, EncounterRules::new));

    /** Folga minima entre ligar e desligar, em blocos. E histerese, nao tuning. */
    private static final double FOLGA_MINIMA = 8.0D;

    public EncounterRules {
        if (!Double.isFinite(raioDeAtivacao) || raioDeAtivacao <= 0.0D
                || !Double.isFinite(raioDeAbandono) || raioDeAbandono <= 0.0D
                || ticksSemParticipante < 1 || ticksDeCooldown < 0) {
            throw new IllegalArgumentException("regras de encontro invalidas");
        }
        if (raioDeAbandono < raioDeAtivacao + FOLGA_MINIMA) {
            throw new IllegalArgumentException("raio de abandono (" + raioDeAbandono + ") precisa"
                    + " ficar ao menos " + FOLGA_MINIMA + " blocos acima do de ativacao ("
                    + raioDeAtivacao + "): sem essa folga o encontro liga e desliga a cada passo"
                    + " na borda, e o jogador le isso como bug de rede, nunca como design");
        }
    }

    /** Encontro de campo: liga a 24 blocos, desiste a 48, falha em 30s, volta em 5min. */
    public static EncounterRules campo() {
        return new EncounterRules(24.0D, 48.0D, 600, 6000);
    }

    /** Encontro unico de estrutura: liga perto, nunca volta. */
    public static EncounterRules unico() {
        return new EncounterRules(16.0D, 32.0D, 600, 0);
    }

    /** Este encontro volta depois de concluido? Cooldown zero quer dizer "nunca". */
    public boolean repetivel() { return ticksDeCooldown > 0; }
}
