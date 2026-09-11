package com.darkcontinent.nenfoundation.client;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.network.handler.RecebedorDeNen;
import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import com.darkcontinent.nenfoundation.network.payload.FeedbackDeErroS2C;
import com.darkcontinent.nenfoundation.network.payload.FxDeHabilidadeS2C;
import com.darkcontinent.nenfoundation.network.payload.SnapshotDePerfilS2C;
import com.darkcontinent.nenfoundation.client.hud.AuraInterpolation;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O que o cliente sabe sobre o proprio Nen.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. ELE SO GUARDA. Nenhum metodo aqui calcula regra, decide custo ou
 * infere estado. O HUD desenha o que o servidor mandou. Um cache que recalcula
 * acerta em quase todos os casos e mente exatamente no caso em que o jogador
 * precisava saber.
 *
 * <p>2. NAO EXISTE VALOR PADRAO UTIL. Enquanto nada chegou, {@link #snapshot()}
 * e {@link #delta()} sao vazios, e {@link #auraOuZero()} devolve zero COM
 * {@link #recebeuAlgumDelta()} em {@code false}. A diferenca entre "aura zero"
 * e "ainda nao sei" precisa ser visivel: um HUD que desenha barra vazia por
 * "ainda nao sei" mente para o jogador logo no login.
 *
 * <p>3. {@link #limpar()} no logout, e o par mora no ciclo de vida do cliente.
 * Sem isso, trocar de servidor mostra o perfil do servidor anterior — dado de
 * outro mundo, sem nada acusar.
 *
 * <p>4. Os contadores nao sao enfeite. Eles existem porque o plano exige
 * medir a frequencia de sync, e numero que ninguem consegue medir nunca sera
 * girado com confianca. Sao lidos pelo overlay de debug.
 *
 * <p>CLIENT-ONLY. Nada em {@code nen/}, {@code network/} ou {@code server/}
 * pode importar esta classe; o portao {@code PacotesDeclaradosTest} reprova.
 */
public final class NenClientCache implements RecebedorDeNen {

    /**
     * Diagnostico de recepcao, so em modo de desenvolvimento.
     *
     * <p>POR QUE ELE EXISTE: sem uma linha no log, "o payload chegou ao
     * cliente" so pode ser observado olhando o overlay na tela — o que nao
     * serve para verificacao automatizada nem para um relato de bug. A
     * ausencia de erro no servidor prova que ele ENVIOU; ela nao prova que
     * alguem recebeu.
     *
     * <p>Fica atras de um interruptor porque uma linha por delta seria uma
     * linha por tick.
     *
     * <p>E o interruptor e INJETADO, nao lido de {@code NenConfig} aqui
     * dentro. A primeira versao deste log chamava a config direto e quebrou
     * cinco testes com {@code Cannot get config value before config is
     * loaded} — o mesmo defeito de ordem de inicializacao que o M0 ja tinha
     * corrigido no {@code NenFoundation}. Cometido de novo, e pego pelo
     * portao.
     *
     * <p>Injetado, o cache continua testavel sem o jogo carregado, e quem
     * compoe le a config NA HORA, que e onde essa leitura pertence.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NenClientCache.class);

    private Optional<SnapshotDePerfilS2C> snapshot = Optional.empty();
    private Optional<DeltaDeRuntimeS2C> delta = Optional.empty();

    private int snapshotsRecebidos;
    private int deltasRecebidos;
    private int fxRecebidos;
    private int errosRecebidos;

    /** Tick do CLIENTE em que o ultimo delta chegou. -1 = nunca. */
    private long tickDoUltimoDelta = -1L;
    private final AuraInterpolation auraInterpolation;

    private Optional<String> ultimoErro = Optional.empty();

    /**
     * Fonte do tick do cliente.
     *
     * <p>Injetada para que o cache seja testavel sem o jogo carregado. Ela nao
     * e um numero guardado: o cache pergunta na hora em que o delta chega.
     */
    private final java.util.function.LongSupplier tickDoCliente;

    /** Se o diagnostico de recepcao deve ser registrado. Perguntado na hora. */
    private final java.util.function.BooleanSupplier diagnosticoLigado;

    public NenClientCache(java.util.function.LongSupplier tickDoCliente,
            java.util.function.BooleanSupplier diagnosticoLigado) {
        this(tickDoCliente, diagnosticoLigado, () -> 0);
    }

    public NenClientCache(java.util.function.LongSupplier tickDoCliente,
            java.util.function.BooleanSupplier diagnosticoLigado,
            java.util.function.IntSupplier duracaoDaInterpolacao) {
        this.tickDoCliente = tickDoCliente;
        this.diagnosticoLigado = diagnosticoLigado;
        this.auraInterpolation = new AuraInterpolation(duracaoDaInterpolacao);
    }

    // ------------------------------------------------------------ recepcao

    @Override
    public void aoReceberSnapshot(SnapshotDePerfilS2C payload) {
        this.snapshot = Optional.of(payload);
        this.snapshotsRecebidos++;
        if (this.diagnosticoLigado.getAsBoolean()) {
            LOG.info("snapshot recebido #{}: categoria={} tecnicas={} habilidades={} marcos={}",
                    this.snapshotsRecebidos,
                    payload.categoriaVisivel().getSerializedName(),
                    payload.tecnicasDesbloqueadas().size(),
                    payload.habilidadesDesbloqueadas().size(),
                    payload.marcos().size());
        }
    }

    @Override
    public void aoReceberDelta(DeltaDeRuntimeS2C payload) {
        long tick = this.tickDoCliente.getAsLong();
        // Valida antes de publicar no cache ou incrementar contadores.
        this.auraInterpolation.receber(payload, tick);
        this.delta = Optional.of(payload);
        this.deltasRecebidos++;
        this.tickDoUltimoDelta = tick;
        if (this.diagnosticoLigado.getAsBoolean()) {
            LOG.info("delta recebido #{}: aura={}/{} tecnicasAtivas={} cooldowns={}",
                    this.deltasRecebidos, payload.aura(), payload.auraMaxima(),
                    payload.tecnicasAtivas().size(), payload.cooldowns().size());
        }
    }

    @Override
    public void aoReceberFx(FxDeHabilidadeS2C payload) {
        // O cache nao toca em estado por causa de FX, de proposito. Ele so
        // conta, para o overlay poder mostrar que os FX estao chegando.
        this.fxRecebidos++;
    }

    @Override
    public void aoReceberErro(FeedbackDeErroS2C payload) {
        this.ultimoErro = Optional.of(payload.chaveDeTraducao());
        this.errosRecebidos++;
    }

    // -------------------------------------------------------------- leitura

    public Optional<SnapshotDePerfilS2C> snapshot() {
        return this.snapshot;
    }

    public Optional<DeltaDeRuntimeS2C> delta() {
        return this.delta;
    }

    /**
     * A categoria que a interface pode mostrar.
     *
     * <p>Ja vem filtrada do servidor: o snapshot carrega
     * {@code categoriaVisivel}, e nao a categoria real. O cliente nao teria
     * como esconder o que nunca recebeu — e essa e a ideia.
     */
    public NenCategory categoriaVisivel() {
        return this.snapshot
                .map(SnapshotDePerfilS2C::categoriaVisivel)
                .orElse(NenCategory.UNDETERMINED);
    }

    public boolean recebeuAlgumSnapshot() {
        return this.snapshot.isPresent();
    }

    public boolean recebeuAlgumDelta() {
        return this.delta.isPresent();
    }

    /**
     * Aura atual, ou zero se nada chegou.
     *
     * <p>Sempre confira {@link #recebeuAlgumDelta()} antes de desenhar: zero
     * aqui pode significar "sem aura" ou "ainda nao sei", e as duas coisas
     * desenhadas iguais mentem para o jogador no login.
     */
    public float auraOuZero() {
        return this.delta.map(DeltaDeRuntimeS2C::aura).orElse(0.0F);
    }

    public float auraMaximaOuZero() {
        return this.delta.map(DeltaDeRuntimeS2C::auraMaxima).orElse(0.0F);
    }

    /** Valor visual interpolado; o delta bruto continua disponível em {@link #delta()}. */
    public float auraInterpolada() {
        return auraInterpolada(0);
    }

    public float auraInterpolada(float parcial) {
        return this.auraInterpolation.valorAtual(this.tickDoCliente.getAsLong() + parcial);
    }

    public Optional<String> ultimoErro() {
        return this.ultimoErro;
    }

    // ---------------------------------------------------------- diagnostico

    public int snapshotsRecebidos() {
        return this.snapshotsRecebidos;
    }

    public int deltasRecebidos() {
        return this.deltasRecebidos;
    }

    public int fxRecebidos() {
        return this.fxRecebidos;
    }

    public int errosRecebidos() {
        return this.errosRecebidos;
    }

    /** Ha quantos ticks o ultimo delta chegou. -1 = nenhum delta ainda. */
    public long ticksDesdeOUltimoDelta() {
        return this.tickDoUltimoDelta < 0 ? -1L : this.tickDoCliente.getAsLong() - this.tickDoUltimoDelta;
    }

    // ------------------------------------------------------------- limpeza

    /**
     * Esquece tudo. Chamado ao sair de um servidor.
     *
     * <p>Os contadores tambem zeram: eles sao "desta sessao", nao "desde que o
     * jogo abriu". Contador que atravessa sessoes faz o overlay dizer que os
     * pacotes estao chegando quando nao esta chegando nenhum.
     */
    public void limpar() {
        this.snapshot = Optional.empty();
        this.delta = Optional.empty();
        this.ultimoErro = Optional.empty();
        this.snapshotsRecebidos = 0;
        this.deltasRecebidos = 0;
        this.fxRecebidos = 0;
        this.errosRecebidos = 0;
        this.tickDoUltimoDelta = -1L;
        this.auraInterpolation.limpar();
    }
}
