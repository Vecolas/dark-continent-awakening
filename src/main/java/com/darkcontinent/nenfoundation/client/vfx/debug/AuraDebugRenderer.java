package com.darkcontinent.nenfoundation.client.vfx.debug;

import com.darkcontinent.nenfoundation.client.particle.AuraSparkParticle;
import com.darkcontinent.nenfoundation.client.vfx.AuraBodyRegion;
import com.darkcontinent.nenfoundation.client.vfx.AuraDistribution;
import com.darkcontinent.nenfoundation.client.vfx.AuraRenderLod;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualMode;
import com.darkcontinent.nenfoundation.client.vfx.MedidorDeVfx;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualState;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualSystem;
import com.darkcontinent.nenfoundation.client.vfx.SobreposicaoDeVfx;
import com.darkcontinent.nenfoundation.client.vfx.SobreposicaoDeVfx.AjusteDePerfil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * O overlay de tuning da aura: o que esta desenhando, quanto custou, e o que
 * foi forcado na mao.
 *
 * <p>ELE E SEPARADO DO OVERLAY DE REDE, e de proposito. O outro responde "o que
 * o cliente recebeu, e quando"; este responde "o que foi para a tela, e a que
 * preco". Juntar os dois faria a sessao de arte rolar por cima de dez linhas de
 * diagnostico de protocolo, e a de rede por cima de dez linhas de alpha.
 *
 * <p><b>CAMPO SEM CONSUMIDOR APARECE COMO TRACO</b>, e nunca como zero. O
 * tamanho do alvo de bloom e a visibilidade calculada por observador nao
 * existem hoje -- eles nascem no AV5 e no AV6. Escrever {@code 0} nos dois
 * faria o overlay afirmar que o passe rodou e nao custou nada, que e uma
 * mentira dificil de desfazer depois.
 *
 * <p><b>O AVISO DE SOBREPOSICAO E A LINHA MAIS IMPORTANTE DAQUI.</b> Sem ele,
 * alguem tira uma captura de aprovacao com um slider fora do perfil, aprova o
 * gate, e o jogo que os jogadores veem nunca foi aquele.
 *
 * <p>A MONTAGEM DO TEXTO E SEPARADA DO DESENHO, como no overlay de rede: a
 * parte que decide o que dizer da para provar sem tela, e e ela que erra.
 *
 * <p>CLIENT-ONLY.
 */
public final class AuraDebugRenderer {

    private static final int MARGEM = 4;
    private static final int ALTURA_DA_LINHA = 10;
    private static final int COR = 0xFFE0E0E0;
    private static final int COR_DE_AVISO = 0xFFFFAA00;

    /** O que se escreve onde ainda nao existe consumidor. */
    public static final String SEM_CONSUMIDOR = "--";

    /**
     * Os rotulos curtos das seis regioes, na ordem do enum.
     *
     * <p>CURTOS PORQUE A LINHA E UMA SO. Seis nomes por extenso passariam da
     * largura util da tela em resolucao baixa, e uma linha que corta e pior que
     * uma abreviada: ela esconde justamente as ultimas regioes, que sao as
     * pernas -- onde Ko e Ryu tiram aura.
     *
     * <p>A ORDEM ACOMPANHA {@link AuraBodyRegion#values()}, e o teste reprova se
     * as duas se separarem. Uma regiao nova no enum sem rotulo aqui mostraria
     * cinco numeros de seis, sem erro nenhum.
     */
    static final String[] ROTULOS = {"cab", "tor", "bE", "bD", "pE", "pD"};

    /**
     * Abaixo disto, duas regioes se leem como iguais.
     *
     * <p>Existe para a linha dizer "uniforme" em vez de listar seis vezes
     * {@code 1.00} -- e para nao dizer "uniforme" quando um Gyo sutil estiver
     * ligado.
     */
    private static final float TOLERANCIA = 0.005F;

    private boolean visivel;

    /** Chamado pela tecla. */
    public void alternar() {
        this.visivel = !this.visivel;
    }

    public boolean visivel() {
        return this.visivel;
    }

    /** Quem liga, desliga: sair do mundo fecha o overlay junto. */
    public void limpar() {
        this.visivel = false;
    }

    public void aoRenderizar(RenderGuiEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        if (!this.visivel || mc.options.hideGui || mc.player == null) {
            return;
        }
        GuiGraphics g = evento.getGuiGraphics();
        int y = MARGEM;
        List<String> linhas = linhas(coletar());
        for (String linha : linhas) {
            boolean aviso = linha.startsWith("!");
            g.drawString(mc.font, aviso ? linha.substring(1) : linha,
                    MARGEM, y, aviso ? COR_DE_AVISO : COR, true);
            y += ALTURA_DA_LINHA;
        }
    }

    /** Le o estado vivo e o congela num record, para o texto sair de dado puro. */
    private static Dados coletar() {
        AuraCaptureMode captura = AuraCaptureMode.instancia();
        // A DISTRIBUICAO E A DO JOGADOR LOCAL, pelo mesmo ponto de entrada que o
        // renderer usa -- e nao por uma segunda leitura da sessao. Se a regua
        // lesse de outro lugar, ela mediria uma coisa e a tela desenharia outra,
        // que e o modo de falha exato que uma regua existe para nao ter.
        AuraVisualState estado = AuraVisualSystem.estadoDe(Minecraft.getInstance().player);
        AuraVisualMode modo = SobreposicaoDeVfx.modoForcado();
        AuraRenderLod lod = SobreposicaoDeVfx.lodForcado();
        return new Dados(
                !SobreposicaoDeVfx.desligado(),
                SobreposicaoDeVfx.congelado(),
                modo == null ? null : modo.name().toLowerCase(Locale.ROOT),
                SobreposicaoDeVfx.outputForcado(),
                SobreposicaoDeVfx.densidadeForcada(),
                lod == null ? null : lod.name().toLowerCase(Locale.ROOT),
                SobreposicaoDeVfx.ribbonsForcadas(),
                MedidorDeVfx.chamadasDeDesenho(),
                MedidorDeVfx.filamentos(),
                AuraSparkParticle.totalAtivas(),
                MedidorDeVfx.jogadoresComAura(),
                captura.ligado(),
                captura.emLote() ? captura.progressoDoLote() : null,
                InfoDeBuild.commit(),
                ajustesDePerfil(),
                // AURA DESLIGADA NAO TEM DISTRIBUICAO, e nao tem uma
                // distribuicao de zeros. Zetsu zera de proposito e "nao ha
                // estado" nao zera nada -- escrever `0.00` nos dois casos
                // apagaria a diferenca, que e a regra desta tela inteira.
                estado.enabled() ? estado.distribution() : null);
    }

    private static String ajustesDePerfil() {
        StringBuilder texto = new StringBuilder();
        for (AjusteDePerfil ajuste : AjusteDePerfil.values()) {
            float valor = SobreposicaoDeVfx.valorDe(ajuste);
            if (Float.isNaN(valor)) {
                continue;
            }
            if (texto.length() > 0) {
                texto.append(' ');
            }
            texto.append(ajuste.nome()).append('=')
                    .append(String.format(Locale.ROOT, "%.3f", valor));
        }
        return texto.length() == 0 ? null : texto.toString();
    }

    /**
     * O texto do overlay.
     *
     * <p>Linha que comeca com {@code !} sai em cor de aviso; o marcador nao
     * aparece na tela. E feio, e e proposital: assim a decisao de "o que grita"
     * fica no mesmo lugar que decide o que dizer, e nao espalhada no desenho.
     */
    public static List<String> linhas(Dados d) {
        List<String> l = new ArrayList<>();
        l.add("[Nen VFX] commit " + d.commit()
                + " | desenho: " + (d.desenhoLigado() ? "on" : "OFF"));

        l.add("custo do ultimo quadro: " + d.chamadasDeDesenho() + " chamadas | "
                + d.filamentos() + " filamentos | " + d.particulas() + " faiscas ativas | "
                + d.jogadoresComAura() + " com aura");

        l.add("regioes: " + regioes(d.distribuicao()));

        // O QUE AINDA NAO EXISTE, DITO COMO NAO EXISTINDO.
        l.add("alvo de bloom: " + SEM_CONSUMIDOR + " (AV5)   visibilidade: "
                + SEM_CONSUMIDOR + " (AV6)");

        if (d.capturaLigada()) {
            l.add("modo de captura: LIGADO"
                    + (d.progressoDoLote() == null ? "" : "  lote " + d.progressoDoLote()));
        }

        List<String> forcados = new ArrayList<>();
        if (!d.desenhoLigado()) {
            forcados.add("desenho=off");
        }
        if (d.congelado()) {
            forcados.add("congelado");
        }
        if (d.modoForcado() != null) {
            forcados.add("estado=" + d.modoForcado());
        }
        if (!Float.isNaN(d.outputForcado())) {
            forcados.add(String.format(Locale.ROOT, "output=%.2f", d.outputForcado()));
        }
        if (!Float.isNaN(d.densidadeForcada())) {
            forcados.add(String.format(Locale.ROOT, "particulas=%.2f", d.densidadeForcada()));
        }
        if (d.lodForcado() != null) {
            forcados.add("lod=" + d.lodForcado());
        }
        if (d.ribbonsForcadas() >= 0) {
            forcados.add("ribbons=" + d.ribbonsForcadas());
        }
        if (d.ajustesDePerfil() != null) {
            forcados.add(d.ajustesDePerfil());
        }

        if (forcados.isEmpty()) {
            l.add("sem sobreposicao: o que esta na tela e o que o perfil manda");
        } else {
            // O AVISO QUE IMPEDE UMA CAPTURA MENTIROSA DE VIRAR APROVACAO.
            l.add("!OVERRIDE ATIVO -- esta captura NAO vale como aprovacao");
            l.add("!  " + String.join("  ", forcados));
        }
        return l;
    }

    /**
     * Os seis fatores do [ADR-014](docs/adr/ADR-014-alocacao-de-aura-por-regiao.md),
     * em uma linha.
     *
     * <p><b>POR QUE ESTA LINHA EXISTE.</b> O renderer multiplica intensidade e
     * alpha por regiao desde o AV1 -- no passe de shell e nos filamentos. Ate
     * aqui, ninguem conseguia OBSERVAR esse multiplicador: com tudo em 1.0 ele e
     * invisivel por construcao, e no dia em que Gyo mandar 1.8 num braco a
     * unica forma de conferir seria olhar a tela e achar que esta mais forte.
     * A issue #175 diz a frase: a regua entra junto do numero, senao o
     * multiplicador vira folclore.
     *
     * <p>QUANDO OS SEIS SAO IGUAIS, ela diz "uniforme" e o valor -- e nao seis
     * copias do mesmo numero. Uma linha que repete o mesmo texto seis vezes
     * deixa de ser lida depois do terceiro dia, e a leitura e o unico proposito
     * dela.
     *
     * <p>A MAIOR REGIAO SAI MARCADA com {@code *} quando ha desequilibrio. E o
     * que torna "Gyo no braco direito" legivel de relance, em vez de exigir
     * comparar seis numeros com tres casas.
     */
    static String regioes(AuraDistribution d) {
        if (d == null) {
            return SEM_CONSUMIDOR + " (sem aura)";
        }
        float menor = Float.MAX_VALUE;
        float maior = -Float.MAX_VALUE;
        AuraBodyRegion pico = null;
        for (AuraBodyRegion regiao : AuraBodyRegion.values()) {
            float valor = d.intensidade(regiao);
            menor = Math.min(menor, valor);
            if (valor > maior) {
                maior = valor;
                pico = regiao;
            }
        }
        if (maior - menor <= TOLERANCIA) {
            return String.format(Locale.ROOT, "uniforme %.2f", maior);
        }
        StringBuilder texto = new StringBuilder();
        AuraBodyRegion[] regioes = AuraBodyRegion.values();
        for (int i = 0; i < regioes.length; i++) {
            if (i > 0) {
                texto.append("  ");
            }
            texto.append(regioes[i] == pico ? "*" : "")
                    .append(ROTULOS[i]).append(' ')
                    .append(String.format(Locale.ROOT, "%.2f", d.intensidade(regioes[i])));
        }
        return texto.toString();
    }

    /**
     * O estado vivo, congelado num instante.
     *
     * <p>Numero ausente e {@code NaN} e texto ausente e {@code null}: os dois
     * sao distinguiveis de "zero" e de "vazio", que e a distincao que este
     * overlay inteiro existe para preservar.
     */
    public record Dados(
            boolean desenhoLigado,
            boolean congelado,
            String modoForcado,
            float outputForcado,
            float densidadeForcada,
            String lodForcado,
            int ribbonsForcadas,
            int chamadasDeDesenho,
            int filamentos,
            int particulas,
            int jogadoresComAura,
            boolean capturaLigada,
            String progressoDoLote,
            String commit,
            String ajustesDePerfil,
            AuraDistribution distribuicao) {
    }
}
