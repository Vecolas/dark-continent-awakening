package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

/**
 * Le um perfil visual do disco, pelo mesmo codec que o jogo usa.
 *
 * <p>POR QUE NAO UM PERFIL DE MENTIRA. Os testes de emissao passaram a depender
 * dos numeros de arte, que sairam do codigo e viraram
 * {@code assets/nenfoundation/nen_vfx/*.json}. Um perfil inventado aqui dentro
 * provaria que a aritmetica do emissor funciona com NUMEROS QUE NINGUEM
 * CARREGA -- e o dia em que alguem zerasse {@code taxa_de_faiscas} no
 * JSON, o verde continuaria igual e Ten sumiria em jogo.
 *
 * <p>{@code AuraPerfis} nao serve aqui: ele estende
 * {@code SimpleJsonResourceReloadListener} e so existe com o gerenciador de
 * recursos do Minecraft de pe. Este atalho le o MESMO arquivo pelo MESMO codec,
 * que e o que importa.
 */
final class PerfilDoDisco {

    private static final String DIRETORIO = "src/main/resources/assets/nenfoundation/nen_vfx";

    private PerfilDoDisco() {
    }

    /** O perfil do modo, lido de {@code <modo>.json}. */
    static AuraPerfilVisual de(AuraVisualMode modo) {
        if (modo == AuraVisualMode.ZETSU || modo == AuraVisualMode.OFF) {
            // A MESMA REGRA DE `AuraPerfis.de`: modo sem brilho nao tem arquivo,
            // e o apagado e a resposta. Repeti-la aqui e o custo de o carregador
            // de verdade nao rodar sem o Minecraft; se as duas divergirem, o
            // teste `zetsuNaoEmiteNemComOPerfilDeTen` e quem acusa.
            return AuraPerfilVisual.SEGURO.apagado();
        }
        String nome = modo.name().toLowerCase(java.util.Locale.ROOT) + ".json";
        JsonElement json = JsonParser.parseString(Repo.texto(DIRETORIO + "/" + nome));
        return AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(erro -> new AssertionError(nome + " nao le: " + erro));
    }
}
