/**
 * A superficie que addons e conteudo do modpack podem usar.
 *
 * <p>REGRA: o que entra aqui vira compromisso. Um metodo publicado nesta arvore e
 * usado por um addon de terceiro nao pode mudar de assinatura sem quebrar aquele
 * addon em silencio -- ele compila contra a versao antiga e estoura em runtime.
 * Enquanto a API for 0.x isso e aceitavel e esta dito no README; a partir da 1.0,
 * mudanca aqui e mudanca de contrato.
 *
 * <p>O nucleo pode depender desta arvore. Esta arvore nao pode depender de
 * {@code client} nem de {@code integration}.
 */
package com.darkcontinent.nenfoundation.api;
