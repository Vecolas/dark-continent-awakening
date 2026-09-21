package com.darkcontinent.nenfoundation.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import com.darkcontinent.nenfoundation.Repo;

/** Portao estrutural da bancada que sobe dois clientes reais. */
class InstanciaDeTesteTest {

    @Test
    @DisplayName("cada jogador usa gameDir proprio, inclusive com -Codigo")
    void clienteNomeadoNaoDivideDiretorio() {
        String script = Repo.texto("scripts/instancia.ps1");

        assertTrue(linhaAtiva(script,
                "\\$clienteDoJogador = Pasta-Do-Cliente \\$Jogador"),
                "o comando deixou de resolver o diretorio a partir do jogador");
        assertTrue(linhaAtiva(script,
                "\\$argumentos = .*\\\"-PdirCliente=\\$clienteDoJogador\\\""),
                "o Gradle nao recebe o diretorio isolado e absoluto");
        assertFalse(linhaAtiva(script,
                "\\$argumentos = .*\\\"-PdirCliente=\\$Cliente\\\""),
                "todos os jogadores voltaram a disputar instancia/cliente");
        assertTrue(script.contains("if ($nome -ceq 'Dev')"),
                "dev e Dev teriam UUIDs distintos e nao podem compartilhar o perfil legado");
        assertTrue(script.contains("if ($registrado -cne $nome)"),
                "nomes que diferem so por maiusculas colidem no Windows e precisam ser recusados");
    }

    @Test
    @DisplayName("perfil novo recebe diagnostico sem sobrescrever config existente")
    void perfilNomeadoNasceComDiagnostico() {
        String script = Repo.texto("scripts/instancia.ps1");

        assertTrue(script.contains("if (-not (Test-Path -LiteralPath $commonDoJogador))"),
                "a config existente pode ser sobrescrita ou o perfil novo ficar sem diagnostico");
        assertTrue(script.contains("Escrever-Arquivo $commonDoJogador $dev"),
                "dev.enabled nao e gravado no perfil nomeado");
    }

    private static boolean linhaAtiva(String fonte, String trecho) {
        return Pattern.compile("(?m)^\\s*" + trecho).matcher(fonte).find();
    }
}
