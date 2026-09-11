package com.darkcontinent.nenfoundation.nen.aura;

/** Fonte viva de tuning. O motor pergunta a cada operacao, inclusive apos recarga. */
public interface ParametrosDeAura {
    double maximaBase();
    double regeneracaoPorSegundo();
    double outputBase();

    /**
     * Teto do multiplicador de regeneracao, depois de todas as tecnicas ativas.
     *
     * <p>POR QUE ELE EXISTE (ADR-010, item 5): sem teto, o que impediria um
     * numero absurdo seria a exclusao entre tecnicas -- uma regra de OUTRO
     * lugar, que pode mudar sem ninguem lembrar desta. Teto no modelo nao
     * depende de nenhuma outra regra continuar valendo.
     */
    double multiplicadorMaximoDeRegeneracao();
}
