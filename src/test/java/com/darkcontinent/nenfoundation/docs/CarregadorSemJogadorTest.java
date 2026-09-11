package com.darkcontinent.nenfoundation.docs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao: o que carrega datapack NAO alcanca jogador.
 *
 * <p>POR QUE ELE EXISTE, e por que ele e um portao de FONTE e nao de
 * comportamento:
 *
 * <p>O criterio de aceite e "recarregar o datapack com um jogador online nao
 * altera o perfil dele". Provar isso por comportamento exigiria um
 * {@code /reload} de verdade com gente conectada -- caro, lento e, pior,
 * incompleto: um teste assim prova que NAQUELE caminho nada mudou, e nao que
 * nao existe caminho.
 *
 * <p>Este portao prova a coisa mais forte: o codigo de carregamento nao tem por
 * onde tocar em perfil. Ele nao importa {@code ServerPlayer}, nem
 * {@code PersistentNenData}, nem os servicos de jogador. O dia em que alguem
 * acrescentar "so para invalidar o cache do fulano", o build reprova e a pessoa
 * precisa decidir isso de olhos abertos.
 *
 * <p>O QUE ELE NAO PEGA, declarado: acesso por reflexao, e qualquer caminho que
 * passe por uma classe intermediaria que ele nao conhece. Ele le texto de
 * arquivo, e nao o grafo de chamadas.
 */
class CarregadorSemJogadorTest {

    /**
     * O escopo e {@code data/definition/}, e nao {@code data/} inteiro.
     *
     * <p>A primeira versao deste portao varria {@code data/} e reprovava
     * {@code data/attachment/NenAttachments.java} -- que REGISTRA o attachment
     * do jogador e precisa mencionar {@code PersistentNenData}. O portao estava
     * apontado para o lugar errado, e nao havia violacao nenhuma.
     *
     * <p>A regra e sobre quem le DATAPACK. Quem registra attachment e outro
     * assunto, e mexer em perfil e o trabalho dele.
     */
    private static final String PACOTE =
            "src/main/java/com/darkcontinent/nenfoundation/data/definition";

    /**
     * Tipos que so existem para mexer em jogador.
     *
     * <p>A lista e curta de proposito: ela nomeia o que o carregador nao pode
     * alcancar, e nao tenta adivinhar tudo que existe. Portao estreito que
     * morde vale mais que portao largo cheio de excecao.
     */
    private static final List<String> PROIBIDOS = List.of(
            "ServerPlayer",
            "PersistentNenData",
            "NenProfileService",
            "RuntimeNenState",
            "NenAuraService",
            "NenRuntimeService");

    @Test
    @DisplayName("o codigo de datapack nao alcanca jogador nem perfil")
    void carregadorNaoTocaEmJogador() {
        // package-info nao entra: ele e so comentario, e `semComentarios` o
        // esvaziaria -- mas ele ainda contaria na conferencia de varredura
        // vazia abaixo, e faria o portao parecer estar olhando um arquivo a
        // mais do que realmente olha.
        List<Path> fontes = Repo.varrer(PACOTE, ".java").stream()
                .filter(p -> !p.getFileName().toString().equals("package-info.java"))
                .toList();

        assertTrue(!fontes.isEmpty(),
                "O portao varreu ZERO arquivos em " + PACOTE
                        + ". Varredura vazia passa sempre, e verde com zero"
                        + " verificacoes e o falso verde mais barato que existe.");

        List<String> violacoes = new ArrayList<>();
        for (Path fonte : fontes) {
            String codigo = semComentarios(ler(fonte));
            for (String proibido : PROIBIDOS) {
                if (codigo.contains(proibido)) {
                    violacoes.add(Repo.raiz().relativize(fonte) + " menciona " + proibido);
                }
            }
        }

        assertTrue(violacoes.isEmpty(),
                "Codigo de carregamento de datapack alcancando jogador:\n  "
                        + String.join("\n  ", violacoes)
                        + "\n\nRecarregar datapack nao pode alterar o progresso de"
                        + " ninguem. Se esta mudanca for mesmo necessaria, ela"
                        + " precisa de decisao registrada -- e este portao existe"
                        + " para forcar essa conversa, e nao para ser afrouxado.");
    }

    /**
     * Tira comentarios antes de procurar.
     *
     * <p>Sem isto, o proprio javadoc que EXPLICA a regra -- "ele nao toca em
     * {@code ServerPlayer}" -- reprovaria o portao. Um portao que proibe
     * documentar a propria regra e um portao que alguem vai desligar.
     */
    private static String semComentarios(String codigo) {
        return codigo
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ");
    }

    private static String ler(Path caminho) {
        try {
            return Files.readString(caminho, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
