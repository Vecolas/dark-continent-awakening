package com.darkcontinent.nenfoundation.enemy.perception;

/**
 * O mob DECLARA que tem ouvido, e so entao um som chega a ele.
 *
 * <p><b>Por que isto e uma interface e nao um metodo em {@code BaseHxHMob}.</b>
 * Ate a issue #142 nao havia um unico produtor de {@link HearingEvent} no
 * repositorio: o evento existia, estava testado, e ninguem o emitia. O Radio Rat
 * e o primeiro, e emitir exige achar VIZINHOS -- ou seja, falar com entidades
 * que nao sao do mesmo tipo. {@code PerceptionController} e privado da entidade
 * de proposito (ver {@code EnemyDebugView}: um getter publico do runtime daria a
 * qualquer chamador o poder de {@code limpar()} no meio de uma perseguicao), e
 * {@code protected} nao atravessa de uma subclasse para outra em Java.</p>
 *
 * <p>Entao o ouvido e declarado, peca por peca. A consequencia esta escrita aqui
 * para nao virar surpresa: <b>um mob que nao implementa esta interface nao ouve
 * nada, e isso nao da erro nenhum</b> -- o relatorio do rato simplesmente passa
 * por ele. Quem estiver migrando um inimigo e quiser que ele receba avisos
 * precisa declarar a interface e delegar para o proprio
 * {@code PerceptionController}, em duas linhas. Nao ha portao que cobre isso,
 * porque "este bicho devia ouvir" e uma decisao de design, nao um fato mecanico:
 * uma planta carnivora surda e uma escolha legitima.</p>
 *
 * <p>O que NAO se faz aqui: mandar o alvo pronto. Quem recebe o evento continua
 * decidindo com as proprias regras -- alcance de audicao, faccao, memoria --, e e
 * por isso que o aviso nao vira teletransporte de informacao. Um
 * {@code setTarget} direto do rato no vizinho seria uma segunda autoridade sobre
 * a escolha de alvo, e as duas discordariam em silencio.</p>
 */
public interface OuvidoDeInimigo {

    /**
     * Entrega um som ja identificado pelo servidor.
     *
     * <p>Sempre server-side. Chamar isto no cliente escreveria memoria de ameaca
     * numa copia que o proximo pacote sobrescreve -- nao da erro, so produz um
     * mob que "as vezes" reage.</p>
     */
    void ouvir(HearingEvent evento);
}
