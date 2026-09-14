package com.darkcontinent.nenfoundation.enemy.ai;

import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRegistry;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;

/**
 * Um {@link SquadRegistry} por nivel de servidor -- e a resposta a "onde isso e limpo".
 *
 * <p><b>Por que por NIVEL e nao um so.</b> Um registro global faria um bando do
 * Nether guardar o UUID de um alvo do Overworld, e {@code ServerLevel.getEntity}
 * do nivel errado devolve {@code null} -- o bando ficaria perseguindo um fantasma
 * para sempre, sem erro nenhum. E o registro por nivel tambem e o que torna a
 * faxina barata: ela varre quatro bandos, e nao todos os do servidor.</p>
 *
 * <p><b>Por que {@link WeakHashMap} e nao um mapa por {@code ResourceKey}.</b>
 * Chaveado pelo id da dimensao, o registro do "minecraft:overworld" de um mundo
 * sobreviveria ao fechamento dele e seria entregue ao PROXIMO mundo carregado no
 * mesmo processo -- singleplayer faz isso o tempo todo. O bando novo nasceria com
 * o alvo de um combate que acabou em outro save. E exatamente o bando fantasma
 * que {@code SquadRegistry} documenta, um nivel acima. Chaveado pelo OBJETO do
 * nivel (e {@code Level} nao redefine {@code equals}, entao a chave e a
 * identidade), o registro morre quando o nivel morre, sem ninguem precisar
 * lembrar de apagar.</p>
 *
 * <p><b>Onde cada coisa e limpa -- os tres pontos, escritos juntos de proposito:</b></p>
 *
 * <ol>
 *   <li><b>o membro</b> sai em {@code WolfPackHunterEntity.remove}, que e o UNICO
 *       ponto de saida: morte, unload de chunk, troca de dimensao e comando
 *       passam todos por ele. Espalhar a saida por {@code die}, por um evento de
 *       unload e por outro de dimensao e como um deles fica esquecido -- e o
 *       esquecido deixa o bando contando um lobo que nao existe mais, o que
 *       segura o teto de reforco ocupado para sempre;</li>
 *   <li><b>o bando vazio</b> sai em {@link SquadRegistry#removerDissolvidos()},
 *       chamado no MESMO ritmo da coordenacao (10 ticks) e nao "quando der".
 *       Bando sem ninguem nao consome tick e nao aparece em lugar nenhum -- ele
 *       so ocupa memoria e, pior, pode receber um membro novo mais tarde e
 *       ressuscitar com o alvo de uma hora atras;</li>
 *   <li><b>o nivel inteiro</b> sai quando o {@code ServerLevel} e coletado, pela
 *       fraqueza da chave.</li>
 * </ol>
 *
 * <p><b>Thread.</b> Tudo aqui e chamado de {@code customServerAiStep} e de
 * {@code remove}, os dois no thread do servidor. O mapa e sincronizado assim
 * mesmo porque o custo e uma trava sem disputa a cada dez ticks, e o preco de
 * estar errado sobre isso e corrupcao silenciosa de um {@code WeakHashMap} --
 * que se manifesta como laco infinito, nao como excecao.</p>
 */
public final class RegistroDeMatilhas {

    private static final Map<ServerLevel, SquadRegistry> POR_NIVEL = new WeakHashMap<>();

    private RegistroDeMatilhas() { }

    /** O registro deste nivel, criado na primeira vez que alguem precisa dele. */
    public static SquadRegistry doNivel(ServerLevel nivel) {
        Objects.requireNonNull(nivel, "nivel ausente");
        synchronized (POR_NIVEL) {
            return POR_NIVEL.computeIfAbsent(nivel, ignorado -> new SquadRegistry());
        }
    }

    /**
     * Apaga o registro de um nivel, se houver.
     *
     * <p>Existe para o caso administrativo -- recarregar o mundo sem reiniciar o
     * processo. O caminho normal nao precisa dela: a chave fraca ja faz isso.</p>
     */
    public static void esquecer(ServerLevel nivel) {
        Objects.requireNonNull(nivel, "nivel ausente");
        synchronized (POR_NIVEL) {
            SquadRegistry registro = POR_NIVEL.remove(nivel);
            if (registro != null) registro.limpar();
        }
    }
}
