package com.darkcontinent.nenfoundation.enemy.telemetry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Junta amostras de combate e grava num arquivo LOCAL -- quando ligada.
 *
 * <p><b>O interruptor e consultado a cada amostra, e nao guardado.</b> A
 * configuracao e recarregavel: uma copia lida no boot ignoraria o
 * {@code /reload} e continuaria gravando depois de alguem desligar. Ninguem
 * reclamaria -- o arquivo so continuaria crescendo.</p>
 *
 * <p><b>Nada aqui sai da maquina.</b> Nao ha cliente HTTP, socket, resolucao de
 * host nem fila de envio. Se um dia alguem quiser enviar, isso e OUTRA decisao,
 * com outro consentimento -- e o lugar de discuti-la nao e dentro de uma classe
 * que ja tem os dados na mao.</p>
 *
 * <p><b>O arquivo cresce, e por isso ele tem TETO.</b> Uma sessao longa de
 * playtest produz milhares de linhas, e um arquivo sem limite acaba grande o
 * bastante para atrapalhar o proprio save. Ao bater o teto, a telemetria PARA de
 * acumular em memoria e avisa -- em vez de descartar as antigas em silencio, que
 * enviesaria a amostra justamente para o fim da sessao.</p>
 */
public final class TelemetriaDeCombate {

    /**
     * Quantas amostras cabem em memoria antes de a gravacao ser obrigatoria.
     *
     * <p>NAO e botao de balanceamento: e o ponto em que segurar amostra deixa de
     * ser barato. Duzentas linhas de CSV sao alguns kilobytes; duzentas mil sao
     * um problema que aparece como lentidao sem causa.</p>
     */
    public static final int TETO_EM_MEMORIA = 2_000;

    private final BooleanSupplier ligada;
    private final List<AmostraDeCombate> amostras = new ArrayList<>();
    private boolean avisouDoTeto;

    /**
     * @param ligada consultado A CADA amostra; normalmente
     *        {@code NenConfig::telemetriaDeInimigosAtiva}
     */
    public TelemetriaDeCombate(BooleanSupplier ligada) {
        this.ligada = Objects.requireNonNull(ligada, "interruptor de telemetria ausente");
    }

    public boolean ligada() { return ligada.getAsBoolean(); }

    public int emMemoria() { return amostras.size(); }

    public boolean cheia() { return amostras.size() >= TETO_EM_MEMORIA; }

    /**
     * Registra uma amostra, se a telemetria estiver ligada.
     *
     * @return true quando a amostra foi guardada
     */
    public boolean registrar(AmostraDeCombate amostra) {
        Objects.requireNonNull(amostra, "amostra ausente");
        if (!ligada()) return false;
        if (cheia()) {
            avisouDoTeto = true;
            return false;
        }
        amostras.add(amostra);
        return true;
    }

    /** Alguem bateu no teto desde a ultima gravacao? O relatorio precisa dizer. */
    public boolean bateuNoTeto() { return avisouDoTeto; }

    public List<AmostraDeCombate> amostras() { return List.copyOf(amostras); }

    /**
     * Grava o que ha em memoria, ACRESCENTANDO ao arquivo.
     *
     * <p>Acrescenta, e nao substitui: cada sessao soma a anterior, que e o que
     * torna a media util. Substituir faria a ultima sessao apagar todas as
     * outras, e ninguem repararia ate a media mudar sem motivo.</p>
     *
     * @param destino caminho LOCAL; quem chama garante que ele esta dentro do
     *        diretorio do mundo
     * @return quantas linhas foram escritas
     */
    public int gravar(Path destino) {
        Objects.requireNonNull(destino, "destino ausente");
        if (amostras.isEmpty()) return 0;
        try {
            if (destino.getParent() != null) Files.createDirectories(destino.getParent());
            boolean novo = !Files.exists(destino);
            StringBuilder texto = new StringBuilder();
            if (novo) texto.append(AmostraDeCombate.cabecalho()).append('\n');
            for (AmostraDeCombate amostra : amostras) {
                texto.append(amostra.linha()).append('\n');
            }
            Files.writeString(destino, texto.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
        int escritas = amostras.size();
        amostras.clear();
        avisouDoTeto = false;
        return escritas;
    }

    /**
     * Media por criatura das amostras EM MEMORIA.
     *
     * <p>Separado por numero de jogadores, e nao junto: juntar solo e grupo faz
     * todo mob parecer facil demais em solo e dificil demais em grupo, ao mesmo
     * tempo, e a media resultante nao descreve nenhum dos dois.</p>
     */
    public Map<String, Map<Integer, Double>> segundosMedios() {
        Map<String, Map<Integer, List<Double>>> coletado = new LinkedHashMap<>();
        for (AmostraDeCombate amostra : amostras) {
            coletado.computeIfAbsent(amostra.mobId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(amostra.jogadores(), k -> new ArrayList<>())
                    .add(amostra.segundos());
        }
        Map<String, Map<Integer, Double>> medias = new LinkedHashMap<>();
        coletado.forEach((mob, porJogadores) -> {
            Map<Integer, Double> linha = new LinkedHashMap<>();
            porJogadores.forEach((quantos, valores) -> linha.put(quantos,
                    valores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D)));
            medias.put(mob, Map.copyOf(linha));
        });
        return Map.copyOf(medias);
    }
}
