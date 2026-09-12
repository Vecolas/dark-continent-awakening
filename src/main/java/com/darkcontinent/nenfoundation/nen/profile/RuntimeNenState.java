package com.darkcontinent.nenfoundation.nen.profile;

import com.darkcontinent.nenfoundation.api.ability.ActiveAbility;
import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.AuraPool;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Estado de combate temporario de um jogador, mantido somente em memoria.
 *
 * <p>Este objeto e mutavel porque muda durante o tick. Ele nunca e attachment,
 * nunca e serializado e nunca carrega progresso. O ciclo de vida que o cria,
 * reinicia e remove mora no servidor; ver ADR-002.
 *
 * <p>As colecoes expostas sao visoes somente-leitura. Toda mudanca passa por
 * esta classe para que futuros motores possam acrescentar invariantes sem
 * procurar mutacoes diretas pelo projeto inteiro.
 */
public final class RuntimeNenState {

    private final AuraPool aura;
    private boolean auraSuja = true;
    private long revisao;
    private final Set<ResourceLocation> tecnicasAtivas = new HashSet<>();
    private final Map<ResourceLocation, Integer> cooldowns = new HashMap<>();
    private ActiveAbility canalizacao;

    /**
     * O multiplicador de regeneracao das tecnicas ativas (ADR-010).
     *
     * <p>E DERIVADO do conjunto de tecnicas ativas, com recalculo explicito a
     * cada ativacao e desativacao -- e nao escrito por cada tecnica. Duas
     * tecnicas escrevendo no mesmo campo e a forma mais barata de uma esquecer
     * de limpar a sua parte, e o sintoma seria regeneracao acelerada para
     * sempre depois que a tecnica ja parou.
     *
     * <p>Ha portao exigindo que ele seja SEMPRE igual ao produto das ativas.
     */
    private double multiplicadorDeRegeneracao = 1.0D;

    /**
     * Onde a aura esta, pelo corpo.
     *
     * <p>DERIVADO, como o teto e o multiplicador. Nenhuma tecnica escreve aqui
     * de fora: o servico recalcula a partir do conjunto de ativas, e por isso
     * nao ha ponto de saida que possa esquecer de limpar. Ver o ADR-014.
     *
     * <p>Comeca UNIFORME, e isso e um estado definido e nao ausencia de estado:
     * quem nao esta concentrando nada tem a aura espalhada por igual.
     */
    private AlocacaoDeAura alocacao = AlocacaoDeAura.uniforme();

    public RuntimeNenState() {
        this.aura = new AuraPool();
    }

    public RuntimeNenState(double auraMaxima) {
        this.aura = new AuraPool(auraMaxima);
    }

    /** Aura disponivel neste instante, validada pela fronteira do pool. */
    public double auraAtual() {
        return this.aura.atual();
    }

    /** Atualiza a medida autoritativa; somente codigo server-side possui este objeto. */
    public void definirAuraAtual(double auraAtual) {
        double antes = this.aura.atual();
        this.aura.definirAtual(auraAtual);
        if (antes != this.aura.atual()) marcarAlterado();
    }

    /** Nao expoe o pool mutavel: toda mutacao precisa marcar a revisao. */
    public double auraMaxima() {
        return this.aura.maxima();
    }

    /** O que o jogador escolheu liberar. */
    /** O multiplicador de regeneracao em vigor. Ver ADR-010. */
    /** Onde a aura esta distribuida agora. Nunca nula. */
    public AlocacaoDeAura alocacao() {
        return this.alocacao;
    }

    /**
     * Troca a alocacao.
     *
     * <p>RECUSA UMA ALOCACAO QUE NAO FECHA. A soma ser 1.0 e a invariante que
     * da risco as tecnicas -- sem ela, concentrar num lugar deixaria de tirar
     * de outro, e Gyo viraria bonus em vez de escolha. Aceitar aqui e descobrir
     * o problema tres camadas adiante, como um numero estranho na tela.
     */
    public void definirAlocacao(AlocacaoDeAura nova) {
        if (nova == null || !nova.soma()) {
            throw new IllegalArgumentException(
                    "alocacao invalida (a soma precisa fechar em 1.0): " + nova);
        }
        this.alocacao = nova;
    }

    public double multiplicadorDeRegeneracao() {
        return this.multiplicadorDeRegeneracao;
    }

    /**
     * Recalcula o multiplicador. <b>So o servico de tecnicas chama isto.</b>
     *
     * <p>Nao marca o runtime como alterado: o multiplicador nao viaja no delta.
     * O que o cliente ve e a aura RESULTANTE, e ela ja e marcada quando muda.
     * Marcar aqui produziria um pacote por ativacao sem nenhum campo novo.
     */
    public void definirMultiplicadorDeRegeneracao(double multiplicador) {
        if (!Double.isFinite(multiplicador) || multiplicador < 0.0D) {
            throw new IllegalArgumentException(
                    "multiplicador de regeneracao invalido: " + multiplicador);
        }
        this.multiplicadorDeRegeneracao = multiplicador;
    }

    public float outputSelecionado() {
        return this.aura.outputSelecionado();
    }

    /** O teto atual. Nada o abaixa ainda; ver AuraPool. */
    public float outputMaximo() {
        return this.aura.outputMaximo();
    }

    /**
     * O valor que o jogo consome: {@code min(selecionado, maximo)}.
     *
     * <p>E ESTE que vai para o delta S2C e para qualquer formula. Mandar o
     * selecionado faria a interface mostrar um numero que o servidor nao usa.
     */
    public float outputEfetivo() {
        return this.aura.outputEfetivo();
    }

    public void definirOutputSelecionado(float novoPercent) {
        if (this.aura.definirOutputSelecionado(novoPercent)) {
            marcarAlterado();
        }
    }

    /** Troca o teto. Ainda sem produtor; existe como costura. */
    public void definirOutputMaximo(float novoMaximo) {
        if (this.aura.definirOutputMaximo(novoMaximo)) {
            marcarAlterado();
        }
    }

    /** Um passo para cima, do tamanho definido pelo dominio. */
    public void aumentarOutput() {
        if (this.aura.aumentarOutput()) {
            marcarAlterado();
        }
    }

    /** Um passo para baixo, do tamanho definido pelo dominio. */
    public void diminuirOutput() {
        if (this.aura.diminuirOutput()) {
            marcarAlterado();
        }
    }

    public boolean exausto() {
        return this.aura.maxima() > 0.0D && this.aura.exausto();
    }

    /** Troca a capacidade e marca o runtime para o proximo delta. */
    public void definirAuraMaxima(double auraMaxima) {
        double antes = this.aura.maxima();
        this.aura.definirMaxima(auraMaxima);
        if (antes != this.aura.maxima()) marcarAlterado();
    }

    /** Debita por inteiro ou deixa o pool intacto, marcando somente mudanca real. */
    public boolean gastarAura(double quantidade) {
        boolean gastou = this.aura.gastar(quantidade);
        if (gastou) marcarAlterado();
        return gastou;
    }

    /** Recupera e informa se o valor mudou. */
    public double recuperarAura(double quantidade) {
        double recuperada = this.aura.recuperar(quantidade);
        if (recuperada != 0.0D) marcarAlterado();
        return recuperada;
    }

    public boolean auraSuja() {
        return this.auraSuja;
    }

    /** Consome a marca apenas depois que o transporte aceitou o delta. */
    public void marcarAuraSincronizada() {
        this.auraSuja = false;
    }

    public long revisao() {
        return this.revisao;
    }

    /** Uma mutacao durante o envio permanece pendente para o proximo delta. */
    public void confirmarSincronizacao(long revisaoEnviada) {
        if (this.revisao == revisaoEnviada) this.auraSuja = false;
    }

    private void marcarAlterado() {
        this.auraSuja = true;
        this.revisao++;
    }

    public Set<ResourceLocation> tecnicasAtivas() {
        return Collections.unmodifiableSet(this.tecnicasAtivas);
    }

    public boolean ativarTecnica(ResourceLocation tecnica) {
        boolean mudou = this.tecnicasAtivas.add(Objects.requireNonNull(tecnica, "tecnica"));
        if (mudou) marcarAlterado();
        return mudou;
    }

    public boolean desativarTecnica(ResourceLocation tecnica) {
        boolean mudou = this.tecnicasAtivas.remove(Objects.requireNonNull(tecnica, "tecnica"));
        if (mudou) marcarAlterado();
        return mudou;
    }

    /** Ticks restantes por habilidade. A contagem e a politica chegam no M5. */
    public Map<ResourceLocation, Integer> cooldowns() {
        return Collections.unmodifiableMap(this.cooldowns);
    }

    public void definirCooldown(ResourceLocation habilidade, int ticksRestantes) {
        Objects.requireNonNull(habilidade, "habilidade");
        if (ticksRestantes < 0) {
            throw new IllegalArgumentException("ticksRestantes nao pode ser negativo");
        }
        if (ticksRestantes == 0) {
            removerCooldown(habilidade);
            return;
        }
        Integer antes = this.cooldowns.put(habilidade, ticksRestantes);
        if (!Objects.equals(antes, ticksRestantes)) marcarAlterado();
    }

    public void removerCooldown(ResourceLocation habilidade) {
        if (this.cooldowns.remove(Objects.requireNonNull(habilidade, "habilidade")) != null) {
            marcarAlterado();
        }
    }

    /** Instancia viva em canalizacao, quando houver. */
    public Optional<ActiveAbility> canalizacao() {
        return Optional.ofNullable(this.canalizacao);
    }

    public void iniciarCanalizacao(ActiveAbility instancia) {
        this.canalizacao = Objects.requireNonNull(instancia, "instancia");
    }

    public void encerrarCanalizacao() {
        this.canalizacao = null;
    }
}
