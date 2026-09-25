package com.darkcontinent.nenfoundation.client.hud;

import com.darkcontinent.nenfoundation.nen.technique.Gyo;
import com.darkcontinent.nenfoundation.nen.technique.Ken;
import com.darkcontinent.nenfoundation.nen.technique.Ko;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Shu;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Qual estado de Nen o chip anuncia, e como ele tinge a HUD.
 *
 * <p>O CHIP MOSTRA UM, e as tecnicas ligadas podem ser varias. Ele responde
 * "em que estado eu estou?", que e uma pergunta de relance -- e relance nao le
 * lista. Quem quiser a lista tem a fila de indicadores, que aparece justamente
 * quando ha mais de uma.
 *
 * <p><b>POR QUE ESTA LISTA NAO E A DE {@code ModoVisualDeTecnica}.</b> Aquela
 * decide que SHELL desenhar no corpo, e tem tres entradas porque so tres
 * tecnicas tem shell. Esta decide que PALAVRA escrever, e toda tecnica tem
 * nome. Sao perguntas diferentes com respostas diferentes, e junta-las faria o
 * chip ficar vazio com Ken ligado.
 *
 * <p><b>E A LISTA E COMPLETA POR PORTAO.</b> {@code EstadoDeNenNaHudTest} exige
 * que ela cubra exatamente as mesmas tecnicas que {@link AparenciaDeTecnica}
 * conhece. Isso existe porque o projeto ja pagou por uma lista incompleta:
 * {@code ModoVisualDeTecnica} usa {@code List.of(Zetsu, Ren, Ten)} com a regra
 * "tecnica desconhecida nao acende nada" -- correta para datapack de terceiro,
 * e ela engoliu o Ken em silencio. Uma lista de primeira parte precisa de
 * alguem cobrando que ela esteja inteira.
 */
public final class EstadoDeNenNaHud {

    /**
     * Quem ganha o chip quando ha mais de uma ligada.
     *
     * <p>A ORDEM E POR COMPROMISSO, do maior para o menor -- quanto mais a
     * tecnica custa e quanto mais ela expoe o jogador, mais cedo ela aparece.
     *
     * <p>ZETSU PRIMEIRO: ele e a ausencia deliberada de aura, e um chip dizendo
     * outra coisa enquanto o jogador acha que esta escondido e a pior
     * informacao que esta HUD poderia dar.
     *
     * <p>KO ANTES DE KEN: Ko poe quase tudo numa regiao e deixa o resto nu, com
     * relogio proprio correndo. E o estado mais perigoso de se estar sem saber.
     *
     * <p>TEN POR ULTIMO: e o estado de repouso. Qualquer outra coisa ligada e
     * mais digna de nota que "estou com a aura presa ao corpo".
     */
    private static final List<ResourceLocation> PRECEDENCIA = List.of(
            Zetsu.ID, Ko.ID, Ken.ID, Ren.ID, Shu.ID, Gyo.ID, Ten.ID);

    private EstadoDeNenNaHud() {
    }

    /** A ordem de precedencia, para o portao conferir. */
    public static List<ResourceLocation> precedencia() {
        return PRECEDENCIA;
    }

    /**
     * A tecnica que o chip anuncia, ou vazio quando nao ha nenhuma conhecida.
     *
     * <p>TECNICA DESCONHECIDA NAO GANHA O CHIP. Um datapack pode registrar id
     * que este arquivo nunca viu; escrever o path cru dele num chip de quarenta
     * pixels produziria texto cortado sem significado. Ela continua aparecendo
     * na fila de indicadores, que tem forma neutra para exatamente isso.
     */
    public static Optional<ResourceLocation> dominante(Set<ResourceLocation> ativas) {
        if (ativas == null || ativas.isEmpty()) {
            return Optional.empty();
        }
        for (ResourceLocation id : PRECEDENCIA) {
            if (ativas.contains(id)) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    /**
     * A chave de traducao do nome curto.
     *
     * <p>DERIVADA DO ID, e nao de um mapa: um mapa id -> chave seria a terceira
     * tabela de tecnicas neste pacote, e a que ninguem lembraria de atualizar.
     * A convencao {@code nenfoundation.tecnica.<path>} ja e a que a roda usa, e
     * {@code TraducaoDerivadaTest} ja cobra que toda tecnica registrada tenha a
     * sua nos dois idiomas.
     */
    public static String chaveDeNome(ResourceLocation id) {
        return "nenfoundation.tecnica." + id.getPath();
    }

    /**
     * Como este estado trata a barra de Aura.
     *
     * <p>SO A DE AURA. Vida nao muda com Nen, e tingir as duas faria a HUD
     * inteira parecer com defeito durante o Zetsu em vez de parecer suprimida.
     */
    public static Tratamento tratamentoDe(Optional<ResourceLocation> dominante) {
        if (dominante.isEmpty()) {
            return Tratamento.NEUTRO;
        }
        ResourceLocation id = dominante.orElseThrow();
        if (Zetsu.ID.equals(id)) {
            return Tratamento.SUPRIMIDO;
        }
        // REN E KEN PULSAM, e nenhuma outra: as duas sao aura LIBERADA e
        // sustentada, que e o que o canone descreve como sentido de longe. Ko e
        // um pico curto com relogio proprio -- um pulso nele leria como estado
        // continuo, e ele nao e.
        if (Ren.ID.equals(id) || Ken.ID.equals(id)) {
            return Tratamento.PRESSIONADO;
        }
        return Tratamento.NEUTRO;
    }

    /** O que o estado faz com a cor da Aura. */
    public enum Tratamento {
        /** Nada. A cor sai como esta na paleta. */
        NEUTRO,
        /** Dessaturada: a aura continua la, e deixou de ser emitida. */
        SUPRIMIDO,
        /** Clareada: ha aura saindo, e o olho precisa ver isso sem ler. */
        PRESSIONADO;

        /**
         * A cor da barra de Aura neste estado, dada a INTENSIDADE do tratamento.
         *
         * <p>A intensidade significa coisas diferentes em cada caso, e e o
         * mesmo numero de proposito: para {@code PRESSIONADO} ela e a fase do
         * pulso, e para {@code SUPRIMIDO} e quanto da rampa de Zetsu ja entrou.
         * Dois parametros -- um pulso e uma rampa -- fariam todo chamador
         * passar um zero para o que nao usa.
         *
         * <p>ELA ENTRA COMO ARGUMENTO, e nao e lida de um relogio aqui dentro:
         * uma funcao que consulta o tempo nao da para provar sem esperar, e o
         * teste dela viraria um {@code Thread.sleep}.
         *
         * <p>INTENSIDADE ZERO E A IDENTIDADE nos tres casos, e isso e o que
         * permite a transicao existir: no primeiro quadro de Zetsu a barra
         * ainda esta na cor de repouso, e ela caminha dali.
         */
        public int aplicar(int cor, float intensidade) {
            float i = Math.clamp(intensidade, 0.0F, 1.0F);
            return switch (this) {
                case NEUTRO -> cor;
                case SUPRIMIDO -> PaletaDaHud.dessaturar(cor, 0.8F * i);
                // O TETO E BAIXO de proposito. Um pulso forte numa barra que
                // fica na tela o tempo todo e o tipo de animacao que cansa em
                // cinco minutos, e o plano pede brilho CONTIDO.
                case PRESSIONADO -> PaletaDaHud.clarear(cor, 0.18F * i);
            };
        }
    }

    /** As tecnicas que o chip sabe nomear. Usada pelo portao de completude. */
    public static Set<ResourceLocation> nomeaveis() {
        return new LinkedHashSet<>(PRECEDENCIA);
    }
}
