package com.darkcontinent.nenfoundation.network.handler;

import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import com.darkcontinent.nenfoundation.network.payload.FeedbackDeErroS2C;
import com.darkcontinent.nenfoundation.network.payload.PresencaDeAuraS2C;
import com.darkcontinent.nenfoundation.network.payload.FxDeHabilidadeS2C;
import com.darkcontinent.nenfoundation.network.payload.SnapshotDePerfilS2C;
import com.darkcontinent.nenfoundation.network.payload.BestiarySnapshotS2C;

/**
 * Quem consome os payloads que o servidor manda.
 *
 * <p>POR QUE ESTA INTERFACE EXISTE, e ela nao e abstracao inventada:
 *
 * <p>Um payload S2C precisa ser REGISTRADO nos dois lados — o servidor precisa
 * conhecer o tipo para poder enviar — mas so e TRATADO no cliente. Se o codigo
 * de registro, que roda no servidor dedicado, importasse a classe do cache de
 * cliente, o servidor carregaria uma classe client-only e morreria com
 * {@code NoClassDefFoundError}. E morreria em producao: compila, roda em
 * singleplayer e roda em {@code runClient}.
 *
 * <p>O portao {@code PacotesDeclaradosTest} ja proibe {@code network/} importar
 * {@code client/} exatamente por isso. Esta interface e a resposta a essa
 * proibicao: o registro fala com um PAPEL, e nao com uma classe concreta. E a
 * excecao legitima da regra de comunicacao por contrato — buscar por papel,
 * nunca por posicao na arvore.
 *
 * <p>Quem implementa: {@code client/NenClientCache}, registrado em
 * {@code NenFoundationClient}. No servidor dedicado ninguem implementa, e o
 * no-op de {@link Recebedores} responde.
 */
public interface RecebedorDeNen {

    /** Conhecimento individual do bestiário, enviado somente ao dono. */
    default void aoReceberBestiary(BestiarySnapshotS2C payload) {
    }

    /** O perfil de leitura chegou. Substitui o anterior por inteiro. */
    void aoReceberSnapshot(SnapshotDePerfilS2C payload);

    /** Aura, tecnicas ativas ou cooldowns mudaram. */
    void aoReceberDelta(DeltaDeRuntimeS2C payload);

    /**
     * Som, particula ou animacao.
     *
     * <p>Quem implementa NAO pode alterar estado a partir daqui. Um FX perdido
     * tem de produzir, no maximo, um efeito visual que faltou.
     */
    void aoReceberFx(FxDeHabilidadeS2C payload);

    /** O servidor recusou alguma coisa, e disse por que. */
    void aoReceberErro(FeedbackDeErroS2C payload);

    /**
     * O que se percebe da aura de OUTRA pessoa.
     *
     * <p>Quem implementa nao decide visibilidade: essa decisao ja foi tomada no
     * servidor, e o que chega aqui e so o que o observador tem direito de ver.
     */
    void aoReceberPresenca(PresencaDeAuraS2C payload);

    /**
     * Um golpe acertou alguem, e o servidor disse ONDE.
     *
     * <p>DEFAULT VAZIO, como {@code aoReceberBestiary}: quem nao desenha aura
     * nao precisa aprender o conceito de impacto para compilar.
     */
    default void aoReceberImpacto(
            com.darkcontinent.nenfoundation.network.payload.ImpactoDeAuraS2C payload) {
    }
}
