package com.darkcontinent.nenfoundation.client.hud;

import com.darkcontinent.nenfoundation.nen.technique.Gyo;
import com.darkcontinent.nenfoundation.nen.technique.Ken;
import com.darkcontinent.nenfoundation.nen.technique.Ko;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Shu;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Que cor e que forma cada tecnica tem na tela.
 *
 * <p>FONTE UNICA, e e por isso que ela existe. A roda ja pintava a tecnica
 * ativa de um jeito; o indicador da HUD pintaria de outro, e ninguem notaria
 * ate alguem olhar as duas coisas ao mesmo tempo e ver Ren de duas cores. Cor
 * de tecnica em dois lugares e a mesma verdade escrita duas vezes.
 *
 * <p>FORMA, E NAO SO COR. O criterio da issue #89 e "distinguiveis olhando, sem
 * ler texto", e cor sozinha nao cumpre isso: cerca de um em doze homens nao
 * separa vermelho de verde, e a HUD encolhe junto com a GUI. A forma carrega o
 * significado, e a cor reforca.
 *
 * <p>NADA AQUI E TEXTURA. As formas sao desenhadas em codigo com os mesmos
 * primitivos da roda. Icone de arquivo exige arte autoral (ADR-007), e arte
 * fraca entrando no repositorio e pior que arte que ainda nao existe.
 *
 * <p>TECNICA DESCONHECIDA NAO QUEBRA A TELA. Um datapack pode registrar
 * tecnica que este arquivo nunca viu; ela ganha cor derivada do proprio id e a
 * forma neutra. Cair aqui por causa de um id novo derrubaria a HUD inteira por
 * um detalhe cosmetico.
 */
public final class AparenciaDeTecnica {

    /** A forma desenhada no indicador. */
    public enum Forma {
        /** Anel fechado: aura retida em volta do corpo. */
        ANEL,
        /** Anel grosso com auréola: aura liberada em volume. */
        AUREOLA,
        /** Contorno fino e vazado: os nos fechados, nada sai. */
        CONTORNO,
        /** Anel grosso e fechado: muita aura, o corpo inteiro coberto. */
        MURALHA,
        /**
         * Um quarto do anel aceso e o resto apagado: a aura foi para um lado.
         *
         * <p>Gyo concentra numa regiao para PERCEBER, e o desenho conta as duas
         * metades do trato: o pedaco aceso e onde a aura esta, e o apagado e o
         * que ela deixou de cobrir.
         */
        SETOR,
        /**
         * Um miolo pequeno e uma ponta saindo ate a borda.
         *
         * <p>Shu e a unica que manda aura para FORA do corpo, sobre um objeto na
         * mao. Nenhuma outra forma sai do circulo, e e assim que ela se separa
         * das outras duas concentradoras de relance.
         */
        LANCA,
        /**
         * Um ponto so, e mais nada.
         *
         * <p>Ko poe quase tudo num lugar, e O VAZIO EM VOLTA E O DESENHO: com a
         * aura toda num ponto, o resto do corpo esta nu. Uma forma que
         * preenchesse o indicador mentiria sobre o risco -- e o risco e o que
         * separa Ko de "Gyo com um numero maior".
         */
        PONTO,
        /** Para tecnica que este arquivo nao conhece. */
        NEUTRA
    }

    /** Cor e forma de uma tecnica. */
    public record Aparencia(int cor, Forma forma) {
    }

    // TODAS AS REGISTRADAS, e o portao cobra isso lendo o pacote de tecnicas --
    // nao uma lista escrita aqui nem la. A versao anterior deste mapa tinha
    // quatro entradas e o teste conferia TRES: Gyo, Shu e Ko cairam na forma
    // neutra durante duas entregas, e Ken entrou no mapa sem prova nenhuma.
    // Lista paralela nao fica errada, fica velha -- e velha e verde ao mesmo
    // tempo.
    private static final Map<ResourceLocation, Aparencia> CONHECIDAS = Map.of(
            Ten.ID, new Aparencia(0xFF_4A_C8_F0, Forma.ANEL),
            Ren.ID, new Aparencia(0xFF_F0_8A_30, Forma.AUREOLA),
            Zetsu.ID, new Aparencia(0xFF_88_78_C8, Forma.CONTORNO),
            Ken.ID, new Aparencia(0xFF_E8_D0_60, Forma.MURALHA),
            Gyo.ID, new Aparencia(0xFF_60_E0_A8, Forma.SETOR),
            Shu.ID, new Aparencia(0xFF_E8_78_B8, Forma.LANCA),
            // VERMELHO DE PROPOSITO, e Ko e a unica. Ela e a tecnica de maior
            // risco do material -- durante a janela o corpo esta quase nu -- e a
            // cor de alerta esta reservada para ela.
            Ko.ID, new Aparencia(0xFF_F0_48_48, Forma.PONTO));

    private AparenciaDeTecnica() {
    }

    /**
     * As tecnicas que este arquivo conhece por nome.
     *
     * <p>EXISTE PARA SER COBRADA. {@code EstadoDeNenNaHudTest} exige que o chip
     * de estado saiba nomear exatamente estas -- duas tabelas de tecnica no
     * mesmo pacote que discordem produzem uma HUD que desenha a forma de uma
     * tecnica e deixa o chip vazio, sem erro nenhum. O projeto ja tem uma
     * ocorrencia desse defeito viva, entre {@code ModoVisualDeTecnica} e
     * {@code EstadoVisualDeTerceiro}; esta e a que nao vai repetir.
     */
    public static java.util.Set<ResourceLocation> conhecidas() {
        return java.util.Collections.unmodifiableSet(CONHECIDAS.keySet());
    }

    /** A aparencia de uma tecnica; nunca nula, nunca lanca. */
    public static Aparencia de(ResourceLocation id) {
        Aparencia conhecida = CONHECIDAS.get(id);
        return conhecida != null ? conhecida
                : new Aparencia(corDerivadaDe(id), Forma.NEUTRA);
    }

    /**
     * Uma cor estavel tirada do proprio id.
     *
     * <p>ESTAVEL E O PONTO: a mesma tecnica tem de sair da mesma cor em toda
     * sessao e em toda maquina. Uma cor sorteada mudaria a cada entrada no
     * mundo, e o jogador nunca aprenderia a associar nada.
     *
     * <p>O brilho e travado alto porque o indicador aparece sobre o mundo, e
     * uma cor escura demais some contra qualquer caverna.
     */
    static int corDerivadaDe(ResourceLocation id) {
        int hash = id.toString().hashCode();
        int r = 0x60 + (((hash >>> 16) & 0xFF) * 0x9F / 0xFF);
        int g = 0x60 + (((hash >>> 8) & 0xFF) * 0x9F / 0xFF);
        int b = 0x60 + ((hash & 0xFF) * 0x9F / 0xFF);
        return 0xFF_00_00_00 | (r << 16) | (g << 8) | b;
    }
}
