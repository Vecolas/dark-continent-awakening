package com.darkcontinent.nenfoundation.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do protocolo: a tabela em Java e a tabela em
 * {@code docs/multiplayer/protocol.md} tem de dizer a mesma coisa.
 *
 * <p>POR QUE ESTE PORTAO EXISTE: a mesma informacao esta em dois lugares -- o
 * codigo, que executa, e o documento, que as pessoas leem antes de escrever
 * codigo. Quando eles divergem, quem ganha e o codigo e quem e lido e o
 * documento. Alguem implementa um handler contra a direcao errada, o cliente
 * passa a poder enviar o que so o servidor deveria enviar, e nada nisso produz
 * erro: com um cliente honesto o jogo funciona perfeitamente.
 *
 * <p>O portao MORDE DOS DOIS LADOS: payload no codigo e ausente do documento
 * reprova, e payload no documento e ausente do codigo tambem.
 */
class ProtocoloCongeladoTest {

    private static final String DOC = "docs/multiplayer/protocol.md";

    @Test
    @DisplayName("a tabela do documento e a tabela do codigo tem os mesmos payloads")
    void tabelasCasam() {
        Map<String, String> noDoc = tabelaDoDocumento();
        Map<String, String> noCodigo = tabelaDoCodigo();

        assertFalse(noCodigo.isEmpty(),
                "NenProtocol.TABELA esta vazia. Portao com zero verificacoes reprova.");
        assertFalse(noDoc.isEmpty(),
                "Nenhuma linha de payload encontrada em " + DOC + ". Ou o documento"
                        + " perdeu a tabela, ou o formato mudou e este portao passou a"
                        + " medir o vazio -- que e pior do que nao existir.");

        Set<String> soNoCodigo = new LinkedHashSet<>(noCodigo.keySet());
        soNoCodigo.removeAll(noDoc.keySet());
        assertTrue(soNoCodigo.isEmpty(),
                "Payload no codigo e ausente de " + DOC + ": " + soNoCodigo);

        Set<String> soNoDoc = new LinkedHashSet<>(noDoc.keySet());
        soNoDoc.removeAll(noCodigo.keySet());
        assertTrue(soNoDoc.isEmpty(),
                "Payload documentado em " + DOC + " que nao existe no codigo: " + soNoDoc
                        + ". Quem ler o documento vai implementar contra um id que"
                        + " ninguem registra.");

        for (Map.Entry<String, String> e : noCodigo.entrySet()) {
            assertEquals(e.getValue(), noDoc.get(e.getKey()),
                    "Direcao divergente para " + e.getKey() + ". Codigo diz "
                            + e.getValue() + ", documento diz " + noDoc.get(e.getKey())
                            + ". Uma das duas versoes vai ser implementada.");
        }
    }

    @Test
    @DisplayName("a versao do protocolo no documento e a mesma do codigo")
    void versaoCasa() {
        Matcher m = Pattern.compile("(?m)^-\\s*\\*\\*Versao do protocolo:\\*\\*\\s*(\\d+)")
                .matcher(Repo.texto(DOC));
        assertTrue(m.find(),
                "Nao achei a linha de versao do protocolo em " + DOC + ".");
        assertEquals(NenProtocol.VERSION, Integer.parseInt(m.group(1)),
                "Versao de protocolo divergente entre codigo e documento.");
    }

    @Test
    @DisplayName("nenhum payload C2S carrega estado que so o servidor decide")
    void c2sNaoCarregaEstado() {
        int conferidos = 0;
        for (NenProtocol.Registro r : NenProtocol.TABELA) {
            if (r.direcao() != Direcao.C2S) {
                continue;
            }
            conferidos++;
            assertFalse(r.campos().isEmpty(),
                    "O payload C2S " + r.id() + " nao declara nenhum campo. Lista vazia"
                            + " passa por este portao sem ele ter olhado nada.");
            for (String campo : r.campos()) {
                String normalizado = campo.toLowerCase(java.util.Locale.ROOT);
                for (String proibido : NenProtocol.PROIBIDOS_EM_C2S) {
                    assertFalse(normalizado.contains(proibido),
                            "O payload C2S " + r.id() + " carrega o campo '" + campo
                                    + "', e '" + proibido + "' e valor que o SERVIDOR"
                                    + " decide. Cliente manda intencao. Ver ADR-001.");
                }
            }
        }
        assertTrue(conferidos > 0,
                "Nenhum payload C2S na tabela. Um laco que nao entra em nenhuma"
                        + " iteracao imprime aprovacao com zero verificacoes.");
    }

    @Test
    @DisplayName("o portao de campo C2S reprova de verdade quando alimentado com um caso ruim")
    void oPortaoMorde() {
        NenProtocol.Registro ruim = new NenProtocol.Registro(
                net.minecraft.resources.ResourceLocation
                        .fromNamespaceAndPath("nenfoundation", "payload_de_controle"),
                Direcao.C2S,
                java.util.List.of("tecnicaId", "auraGasta"),
                "caso de controle: um payload C2S que afirma o proprio gasto de aura");

        boolean pegou = ruim.campos().stream()
                .map(c -> c.toLowerCase(java.util.Locale.ROOT))
                .anyMatch(c -> NenProtocol.PROIBIDOS_EM_C2S.stream().anyMatch(c::contains));

        assertTrue(pegou,
                "O criterio de PROIBIDOS_EM_C2S nao pegou um caso que deveria reprovar."
                        + " Regua que nunca reprova e carimbo — este teste existe para"
                        + " provar que a de cima morde.");
    }

    /** Le a tabela em prosa: linhas {@code | `id` | C2S | ... |}. */
    private static Map<String, String> tabelaDoDocumento() {
        Pattern linha = Pattern.compile(
                "(?m)^\\|\\s*`([a-z0-9_]+)`\\s*\\|\\s*(C2S|S2C)\\s*\\|");
        Matcher m = linha.matcher(Repo.texto(DOC));
        Map<String, String> achados = new LinkedHashMap<>();
        while (m.find()) {
            achados.put(m.group(1), m.group(2));
        }
        return achados;
    }

    private static Map<String, String> tabelaDoCodigo() {
        Map<String, String> achados = new LinkedHashMap<>();
        for (NenProtocol.Registro r : NenProtocol.TABELA) {
            achados.put(r.id().getPath(), r.direcao().name());
        }
        return achados;
    }
}
