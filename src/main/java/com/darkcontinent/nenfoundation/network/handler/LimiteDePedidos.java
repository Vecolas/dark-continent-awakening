package com.darkcontinent.nenfoundation.network.handler;

/**
 * Orcamento por conexao, compartilhado entre todos os pedidos C2S.
 * Usa tempo monotono e limita tambem trabalho pendente: TPS baixo nao cria
 * uma fila ilimitada. Morte/dimensao invalidam trabalho antigo sem renovar cota.
 */
public final class LimiteDePedidos {
    private static final long SEGUNDO = 1_000_000_000L;
    private static final int MAX_PENDENTES = 8;
    private long inicio;
    private int usados;
    private int pendentes;
    private long geracao;
    private boolean encerrado;
    private long aceitos;
    private long recusados;

    public LimiteDePedidos(long agora) {
        this.inicio = agora;
    }

    /** -1 e recusa; geracoes validas comecam em zero. */
    public synchronized long admitir(long agora, int maxPorSegundo) {
        if (agora - this.inicio >= SEGUNDO) {
            this.inicio = agora;
            this.usados = 0;
        }
        if (this.encerrado || this.usados >= maxPorSegundo || this.pendentes >= MAX_PENDENTES) {
            this.recusados++;
            return -1;
        }
        this.usados++;
        this.pendentes++;
        this.aceitos++;
        return this.geracao;
    }

    public synchronized boolean atual(long versao) {
        return !this.encerrado && versao == this.geracao;
    }

    public synchronized void concluir() {
        if (this.pendentes <= 0) {
            throw new IllegalStateException("pedido concluido sem admissao");
        }
        this.pendentes--;
    }

    public synchronized void invalidar() {
        this.geracao++;
    }

    public synchronized void encerrar() {
        this.encerrado = true;
        invalidar();
    }

    /** Contadores para medir a defesa, sem log por pacote nem UUID. */
    public synchronized long aceitos() { return this.aceitos; }
    public synchronized long recusados() { return this.recusados; }
}
