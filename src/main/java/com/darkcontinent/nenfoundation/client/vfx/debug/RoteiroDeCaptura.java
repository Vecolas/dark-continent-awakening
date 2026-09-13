package com.darkcontinent.nenfoundation.client.vfx.debug;

import com.darkcontinent.nenfoundation.client.vfx.AuraVisualMode;
import java.util.List;

/**
 * O lote de capturas de um ponto: a mesma pose, a mesma luz, os mesmos numeros,
 * e so o estado da aura mudando.
 *
 * <p>POR QUE UM LOTE. O protocolo A/B de {@code docs/testing/av-aura-visual.md}
 * secao 3 exige mesmo local, mesmo FOV, mesma hora, mesma skin, mesma distancia
 * e a mesma versao de assets entre duas capturas comparadas. Tirar as seis na
 * mao, uma a uma, entre digitar comandos, quebra pelo menos duas dessas
 * condicoes por descuido -- e o descuido nao aparece na imagem.
 *
 * <p><b>O QUE ELE NAO FAZ, E POR QUE ESTA ESCRITO.</b> Ele NAO cobre o eixo de
 * ambiente (dia, noite, caverna, neve, Nether), o de pose (correndo, agachado,
 * nadando), o de equipamento (armadura, capa, elytra) nem a serie de distancia
 * (2b a 40b). Todos esses exigem MOVER o jogador ou o mundo, e um lote que
 * teleportasse para fingi-los produziria imagens que nao correspondem a nenhuma
 * sessao real de jogo. O jeito certo e rodar o lote uma vez por ambiente e por
 * pose, passando a etiqueta na mao -- e por isso o comando pede uma.
 *
 * <p>A serie de DISTANCIA tem um motivo a mais para nao estar aqui: o nivel de
 * detalhe do jogador local e sempre {@code FULL}, porque a distancia dele para
 * a propria camera e zero. Uma serie de distancia tirada em si mesmo nao
 * exercita a tabela de LOD -- ela precisa de um segundo jogador, que e
 * exatamente o que o gate do AV0 ja exige de outra forma.
 *
 * <p>CLASSE PURA: nenhuma referencia ao Minecraft. A maquina de estados do lote
 * e a unica parte dele que da para provar sem tela, e um erro de contagem aqui
 * produziria o pior resultado possivel -- um lote que captura o quadro errado e
 * nomeia certo.
 */
public final class RoteiroDeCaptura {

    /**
     * Quantos quadros esperar depois de mudar o estado, antes de fotografar.
     *
     * <p>NAO E ZERO, e o motivo nao e a transicao -- estado forcado ja nasce com
     * o progresso no fim. E o resto: o perfil trocou, o alpha de cada passe
     * mudou, e o buffer do quadro anterior ainda esta na tela no instante em que
     * o comando roda. Fotografar no mesmo quadro entrega a imagem do estado
     * ANTERIOR com o nome do novo, que e o tipo de erro que ninguem encontra
     * olhando a imagem.
     */
    public static final int QUADROS_DE_ESPERA = 6;

    /**
     * O conjunto que fecha o criterio do ADR-015.
     *
     * <p>A ORDEM NAO E ARBITRARIA. {@code sem_aura} vem por ultimo de proposito:
     * ele e a linha de base contra a qual as outras sao lidas. O criterio do
     * gate nao e "a aura esta bonita", e sim <b>"aprovado se ainda se le que a
     * pessoa esta em Ten; reprovado se parece o jogador normal"</b> -- e essa
     * frase so da para julgar com a foto do jogador normal do lado, tirada na
     * mesma luz e na mesma pose.
     */
    public static final List<Passo> CONJUNTO_MINIMO = List.of(
            new Passo("ten", AuraVisualMode.TEN, -1.0F),
            new Passo("ten_sem_particulas", AuraVisualMode.TEN, 0.0F),
            new Passo("ren", AuraVisualMode.REN, -1.0F),
            new Passo("ren_sem_particulas", AuraVisualMode.REN, 0.0F),
            new Passo("zetsu", AuraVisualMode.ZETSU, -1.0F),
            new Passo("sem_aura", AuraVisualMode.OFF, 0.0F));

    private final List<Passo> passos;
    private int indice;
    private int quadrosNoPasso;
    private boolean ativo;

    public RoteiroDeCaptura() {
        this(CONJUNTO_MINIMO);
    }

    public RoteiroDeCaptura(List<Passo> passos) {
        if (passos == null || passos.isEmpty()) {
            throw new IllegalArgumentException("um roteiro sem passo nenhum capturaria nada"
                    + " e relataria sucesso");
        }
        this.passos = List.copyOf(passos);
    }

    /** Comeca do zero. Chamar com o lote ja rodando reinicia, e isso e deliberado. */
    public void iniciar() {
        this.indice = 0;
        this.quadrosNoPasso = 0;
        this.ativo = true;
    }

    /**
     * Interrompe.
     *
     * <p>Quem liga, desliga: sair do mundo no meio de um lote nao pode deixar a
     * maquina esperando um quadro que nunca chega.
     */
    public void cancelar() {
        this.ativo = false;
        this.indice = 0;
        this.quadrosNoPasso = 0;
    }

    public boolean ativo() {
        return this.ativo;
    }

    /** Quantos passos ja terminaram, para o overlay mostrar progresso. */
    public int concluidos() {
        return this.indice;
    }

    public int total() {
        return this.passos.size();
    }

    /**
     * Um quadro do lote. Chamado uma vez por quadro enquanto {@link #ativo()}.
     *
     * <p>A maquina e: aplicar o passo, esperar {@link #QUADROS_DE_ESPERA}
     * quadros, fotografar, seguir para o proximo. Devolve o passo junto da acao
     * para que quem chama nunca precise adivinhar de qual passo se trata -- um
     * indice lido no momento errado e como um lote inteiro sai com os nomes
     * deslocados em um.
     */
    public Resultado aoQuadro() {
        if (!this.ativo) {
            return new Resultado(Acao.NADA, null);
        }
        if (this.indice >= this.passos.size()) {
            this.ativo = false;
            return new Resultado(Acao.ENCERRAR, null);
        }
        Passo passo = this.passos.get(this.indice);
        int quadro = this.quadrosNoPasso++;
        if (quadro == 0) {
            return new Resultado(Acao.APLICAR, passo);
        }
        if (quadro < QUADROS_DE_ESPERA) {
            return new Resultado(Acao.NADA, passo);
        }
        this.quadrosNoPasso = 0;
        this.indice++;
        return new Resultado(Acao.CAPTURAR, passo);
    }

    /** Um estado a fotografar. Densidade negativa significa "deixa como esta". */
    public record Passo(String nome, AuraVisualMode modo, float densidade) {
        public Passo {
            if (nome == null || nome.isBlank()) {
                throw new IllegalArgumentException("passo sem nome vira arquivo sem nome");
            }
            if (modo == null) {
                throw new NullPointerException("modo obrigatorio");
            }
        }
    }

    /** O que quem chama deve fazer neste quadro. */
    public enum Acao {
        /** Nada: esperando o estado assentar. */
        NADA,
        /** Aplicar o estado do passo. */
        APLICAR,
        /** Fotografar agora, com o nome do passo. */
        CAPTURAR,
        /** Acabou: devolver o controle e limpar a sobreposicao. */
        ENCERRAR
    }

    /** A acao e o passo a que ela se refere. */
    public record Resultado(Acao acao, Passo passo) {
    }
}
