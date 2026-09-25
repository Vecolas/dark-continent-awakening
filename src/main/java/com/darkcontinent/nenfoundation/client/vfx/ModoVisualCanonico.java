package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.nen.technique.Gyo;
import com.darkcontinent.nenfoundation.nen.technique.Ken;
import com.darkcontinent.nenfoundation.nen.technique.Ko;
import com.darkcontinent.nenfoundation.nen.technique.PrecedenciaDeTecnicas;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Shu;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * A UNICA tabela de tecnica -> modo de shell.
 *
 * <p><b>O DEFEITO QUE ELA FECHA.</b> Existiam dois caminhos independentes
 * decidindo que aura desenhar, e eles discordavam:
 *
 * <pre>
 *   ModoVisualDeTecnica    List.of(Zetsu, Ren, Ten)    -> Ken caia em OFF
 *   EstadoVisualDeTerceiro switch exaustivo             -> Ken desenhava como REN
 * </pre>
 *
 * <p>Como {@code Ken.excluidas()} devolve {@code Set.of(Ten, Ren, Zetsu)},
 * ligar Ken DESLIGAVA a tecnica que estava acesa e caia em OFF: a aura do
 * jogador se apagava na tela dele, enquanto todo mundo em volta continuava
 * vendo-o brilhar. O mesmo valia para Gyo, Shu e Ko ligados sozinhos.
 *
 * <p>Agora ha uma tabela so, e os dois caminhos sao ADAPTERS dela:
 * {@link ModoVisualDeTecnica} traduz o conjunto de tecnicas que o jogador local
 * conhece; {@link EstadoVisualDeTerceiro} traduz o {@code SinalDeAura} pobre que
 * chega dos outros. Eles diferem no que SABEM, e nunca mais no que DECIDEM.
 *
 * <p><b>A COMPLETUDE E COBRADA</b> por {@code ModoVisualCanonicoTest}, contra
 * {@link PrecedenciaDeTecnicas}. Uma tecnica sem modo aqui reprova o build
 * nomeando-se -- que e exatamente o que faltava quando o Ken entrou.
 */
public final class ModoVisualCanonico {

    /**
     * Que shell cada tecnica desenha.
     *
     * <p>SAO TRES MODOS PARA SETE TECNICAS, e isso e deliberado. O modo escolhe
     * o PERFIL DE ARTE ({@code ten.json}, {@code ren.json}); a identidade de
     * cada tecnica vem da COR, de {@code AparenciaDeTecnica}, e da ALOCACAO por
     * regiao, que viaja em separado. Sete presets de arte para sete tecnicas
     * seria sete arquivos para manter em sincronia sem que nenhum jogador
     * conseguisse dizer a diferenca entre quatro deles.
     */
    private static final Map<ResourceLocation, AuraVisualMode> MODOS = Map.of(
            // A ausencia de aura tem shell propria: ela precisa parecer
            // SUPRIMIDA para quem esta em Zetsu, e nao apagada -- apagada e
            // indistinguivel de nao ter Nen.
            Zetsu.ID, AuraVisualMode.ZETSU,

            // Ten e o estado retido: pelicula fina e estavel.
            Ten.ID, AuraVisualMode.TEN,

            // GYO E SHU DESENHAM COMO TEN. As duas nao mudam o envelope -- elas
            // redistribuem o que ja esta la. O que conta a concentracao e a
            // ALOCACAO, que o renderer ja le por regiao e que normaliza pela
            // regiao mais concentrada: o lugar escolhido fica cheio e o resto
            // do corpo escurece. Dar a elas um modo proprio duplicaria arte
            // para representar uma multiplicacao.
            Gyo.ID, AuraVisualMode.TEN,
            Shu.ID, AuraVisualMode.TEN,

            // Ren e a aura liberada em volume.
            Ren.ID, AuraVisualMode.REN,

            // KEN DESENHA COMO REN, e {@code EstadoVisualDeTerceiro} ja fazia
            // isso desde que KEN entrou em SinalDeAura. Esta linha e o outro
            // caminho finalmente concordando com ele.
            Ken.ID, AuraVisualMode.REN,

            // KO TAMBEM: e aura liberada, so que quase toda num lugar. O
            // envelope e grande; o que o separa de Ren na tela e a alocacao
            // quase zerada no resto do corpo, mais a cor de alerta.
            Ko.ID, AuraVisualMode.REN);

    private ModoVisualCanonico() {
    }

    /** As tecnicas que esta tabela sabe desenhar. Usada pelo portao. */
    public static Set<ResourceLocation> conhecidas() {
        return MODOS.keySet();
    }

    /**
     * O modo de uma tecnica, ou vazio quando ela nao e conhecida.
     *
     * <p>VAZIO, e nao {@code OFF}. A diferenca e a que custou o defeito do Ken:
     * {@code OFF} afirma "esta pessoa nao tem aura", e a resposta certa para um
     * id que este arquivo nao conhece e "nao sei". Quem chama decide o que fazer
     * com o desconhecimento -- e o que os adapters fazem e ignorar a tecnica na
     * disputa, e nao apagar a aura.
     */
    public static Optional<AuraVisualMode> modoDe(ResourceLocation id) {
        return Optional.ofNullable(id).map(MODOS::get);
    }

    /**
     * O modo para um conjunto de tecnicas ligadas, pela precedencia do dominio.
     *
     * <p>Conjunto vazio, nulo, ou so com tecnicas desconhecidas devolve
     * {@code OFF} -- aqui sim, porque "nenhuma tecnica que eu saiba desenhar
     * esta ligada" e de fato a aura desligada.
     */
    public static AuraVisualMode deConjunto(Set<ResourceLocation> ativas) {
        return dominante(ativas).flatMap(ModoVisualCanonico::modoDe)
                .orElse(AuraVisualMode.OFF);
    }

    /**
     * A tecnica dominante do conjunto, ENTRE AS QUE SABEM DESENHAR.
     *
     * <p>O filtro importa: a precedencia do dominio conhece as sete, e se um dia
     * uma delas nao tiver modo, deixa-la dominar apagaria a aura de quem tem
     * outra tecnica desenhavel ligada junto. Hoje o filtro nao tira nada -- o
     * portao de completude garante isso --, e ele existe para o dia em que
     * alguem registre a oitava.
     */
    public static Optional<ResourceLocation> dominante(Set<ResourceLocation> ativas) {
        if (ativas == null || ativas.isEmpty()) {
            return Optional.empty();
        }
        for (ResourceLocation id : PrecedenciaDeTecnicas.ordem()) {
            if (ativas.contains(id) && MODOS.containsKey(id)) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }
}
