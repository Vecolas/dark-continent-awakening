/**
 * Os records de payload e seus StreamCodecs.
 *
 * <p>Os ids e as direcoes estao CONGELADOS em
 * {@code com.darkcontinent.nenfoundation.network.NenProtocol}, junto da lista
 * de campos que cada payload pode carregar. Tres portoes cruzam as fontes:
 *
 * <ul>
 *   <li>{@code ProtocoloCongeladoTest} — a tabela em Java e
 *       {@code docs/multiplayer/protocol.md} dizem a mesma coisa, e nenhum
 *       payload C2S declara campo que so o servidor decide;
 *   <li>{@code RecordsDePayloadTest} — todo payload da tabela tem record, todo
 *       record esta na tabela, e os COMPONENTES de cada record sao exatamente
 *       os campos declarados, na mesma ordem;
 *   <li>o mesmo teste faz ida e volta de cada payload pelo proprio codec, e
 *       confere que nao sobram bytes no buffer.
 * </ul>
 *
 * <p>O segundo portao e o que impede o primeiro de virar decoracao: sem ele,
 * alguem acrescenta um campo ao record, a tabela fica para tras, e a proibicao
 * de campo de servidor em payload C2S deixa de enxergar aquele payload.
 *
 * <p>DECISAO: nada de serializacao generica de objeto Java. Cada payload e um
 * record de campos minimos com StreamCodec escrito a mao em
 * {@code CodecsDePayload}. [NF-4]
 *
 * <p>O QUE AINDA NAO EXISTE AQUI: o registro no {@code PayloadRegistrar}.
 * Registrar exige handler; handler C2S precisa do servico de perfil (issue #2)
 * e handler S2C precisa do cache de cliente (issue #6). Registrar agora com
 * handler vazio seria codigo que nao roda hoje e faz a coisa errada em
 * silencio no dia em que rodar.
 *
 * <p>Owner: Dev A (envio S2C e validacao C2S), Dev B (leitura no cliente).
 */
package com.darkcontinent.nenfoundation.network.payload;
