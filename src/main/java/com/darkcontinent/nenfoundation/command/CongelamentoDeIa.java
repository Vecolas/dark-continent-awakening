package com.darkcontinent.nenfoundation.command;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * O interruptor UNICO do congelamento de IA dos inimigos do mod.
 *
 * <p><b>ONDE O ESTADO MORA.</b> Em dois lugares, e isso e uma escolha, nao um
 * descuido:
 *
 * <ol>
 *   <li>o campo {@link #ligado} desta classe -- memoria do PROCESSO do servidor,
 *       que nasce {@code false} e morre com o servidor;</li>
 *   <li>a flag {@code NoAI} de cada mob alcancado -- que o {@code Mob} do
 *       vanilla GRAVA no NBT da entidade ({@code Mob#addAdditionalSaveData}).</li>
 * </ol>
 *
 * <p><b>O QUE ACONTECE SE O SERVIDOR REINICIAR CONGELADO.</b> O interruptor
 * volta em {@code off} -- e os bichos voltam congelados, porque o {@code NoAI}
 * deles foi salvo junto com o resto da entidade. Isso NAO da erro nenhum: nao ha
 * excecao, nao ha linha de log, nao ha nada na tela. O que ha e uma arena de
 * teste inteira parada, e um relato de bug que diz "os inimigos nao reagem mais"
 * — o pior relato possivel, porque manda a proxima pessoa procurar defeito na
 * percepcao.
 *
 * <p>A saida e uma so, e ela e deliberada: <b>{@code freeze off} SEMPRE varre</b>,
 * mesmo quando o interruptor ja diz {@code off}. Um "ja esta desligado, nao vou
 * fazer nada" seria a otimizacao que tranca a porta por fora.
 *
 * <p><b>QUEM LIGA, DESLIGA -- e por isso e UM metodo, nao dois.</b>
 * {@link #varrer(MinecraftServer, boolean)} recebe o SENTIDO como argumento.
 * Com um {@code ligar()} e um {@code desligar()} separados, nada impede alguem
 * de acrescentar um chamador so do primeiro; com um metodo so, o sentido e
 * sempre um parametro que alguem teve de escrever.
 *
 * <p><b>PONTOS CEGOS DECLARADOS.</b>
 *
 * <ul>
 *   <li>Inimigo que CARREGA depois do {@code freeze on} (chunk que entra, mundo
 *       que troca) nao passa por aqui e nao vem congelado. So o que
 *       {@link EnemyDebugCommands} spawna consulta {@link #aplicarA(Mob)}.
 *       Congelar tudo que nasce exigiria um listener de tick ou de entity-join,
 *       que e um ciclo de vida a mais para uma ferramenta de teste.</li>
 *   <li>{@code freeze off} limpa o {@code NoAI} de TODO inimigo do mod no
 *       alcance da varredura, inclusive de um que tivesse {@code NoAI} posto por
 *       outra pessoa (um {@code /data merge}, um mob de cenario). O
 *       congelamento nao guarda o valor anterior de cada bicho -- guardar
 *       exigiria um mapa por entidade que teria de sobreviver a reinicio, e ai
 *       seriam TRES fontes para a mesma verdade.</li>
 * </ul>
 */
public final class CongelamentoDeIa {

    /**
     * Memoria do processo, nao do mundo.
     *
     * <p>{@code volatile} porque a leitura acontece na thread do servidor e o
     * teste unitario escreve na thread dele. Nao ha aqui operacao composta que
     * precise de trava: o comando e serializado pela fila de comandos.
     */
    private static volatile boolean ligado;

    private CongelamentoDeIa() {
    }

    /** O interruptor, lido na hora. Nunca copie este valor para um campo. */
    public static boolean ligado() { return ligado; }

    /**
     * Muda o interruptor -- os DOIS sentidos num metodo so.
     *
     * <p>Separado da varredura de proposito: a varredura precisa de servidor, e
     * o interruptor precisa ser exercitavel sem um. Portao que so roda com o
     * jogo de pe nao roda.
     *
     * @param congelar o sentido; {@code true} congela, {@code false} descongela
     * @return o estado resultante, para quem quiser relatar sem reler
     */
    public static boolean definir(boolean congelar) {
        ligado = congelar;
        return ligado;
    }

    /**
     * Aplica o estado CORRENTE a um mob recem-criado.
     *
     * <p>Existe para o {@code spawn}: sem isto, spawnar durante um congelamento
     * entregaria um bicho andando no meio de uma arena parada -- e quem esta
     * medindo alcance de golpe mediria contra um alvo que se mexe, sem nenhum
     * aviso de que o congelamento nao valeu para ele.
     */
    public static void aplicarA(Mob mob) {
        mob.setNoAi(ligado);
    }

    /**
     * Muda o interruptor E alcanca todos os inimigos do mod ja carregados.
     *
     * <p>Varre TODOS os niveis do servidor, e nao so o de quem digitou: uma
     * arena de teste frequentemente vive numa dimensao propria, e um
     * congelamento que para na dimensao do operador pareceria simplesmente nao
     * ter funcionado.
     *
     * @param servidor o servidor cuja lista de niveis sera varrida
     * @param congelar o sentido; {@code false} varre do mesmo jeito, sempre
     * @return quantos mobs foram alcancados, para o relato dizer um numero
     */
    public static int varrer(MinecraftServer servidor, boolean congelar) {
        definir(congelar);

        // A fila e lida UMA vez por varredura, e nao uma vez por entidade: a
        // varredura passa por todo mob carregado do servidor, e reconstruir o
        // conjunto a cada um transformaria uma ferramenta de debug numa
        // travada. Lida na hora, porem, e nao guardada num campo estatico --
        // registro consultado no boot e o jeito classico de ficar com uma lista
        // velha depois de um reload.
        Set<ResourceLocation> fila = EnemyDebugCommands.idsDaFila();

        int alcancados = 0;
        for (ServerLevel nivel : servidor.getAllLevels()) {
            for (Entity entidade : nivel.getAllEntities()) {
                if (entidade instanceof Mob mob && EnemyDebugCommands.daFila(mob, fila)) {
                    mob.setNoAi(congelar);
                    alcancados++;
                }
            }
        }
        return alcancados;
    }
}
