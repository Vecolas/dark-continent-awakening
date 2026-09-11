package com.darkcontinent.nenfoundation.client.screen;

/**
 * Limites da pagina atual. Recalculados na consulta: lock/reset e resize nao
 * podem deixar a tela presa numa pagina vazia ou esconder ids fora do painel.
 */
public record PaginacaoDaFicha(int total, int porPagina, int pagina) {
    public PaginacaoDaFicha {
        if (total < 0 || porPagina < 1) throw new IllegalArgumentException("paginacao invalida");
        int paginas = Math.max(1, (int) ((total + (long) porPagina - 1) / porPagina));
        pagina = Math.max(0, Math.min(pagina, paginas - 1));
    }

    public int paginas() { return Math.max(1, (int) ((this.total + (long) this.porPagina - 1) / this.porPagina)); }
    public int inicio() { return this.pagina * this.porPagina; }
    public int fim() { return (int) Math.min(this.total, inicio() + (long) this.porPagina); }
}
