# A HUD do jogador

Fonte de verdade do **painel de Nen** — o que ele mostra, onde cada coisa mora,
e o que ainda ninguém olhou.

> **Redesenhado em 2026-09-25.** A versão anterior era `250x63`, com duas barras
> permanentes (Aura e Output), nenhuma barra de vida, uma moldura em PNG de
> `512x128` e um badge que desenhava `念` sem dizer nada sobre o estado.

---

## 1. O diagnóstico, e o que cada problema virou

| O problema | A causa | O que mudou |
| --- | --- | --- |
| larga demais para a informação | duas barras + rótulos em `250px` | `200x44`, com a coluna de conteúdo ancorada nas duas pontas |
| peso visual no contorno | moldura em PNG com borda grossa | contorno de **1px** desenhado em código, com acento só nas pontas cortadas |
| hierarquia fraca | cada renderer posicionava o próprio texto | três colunas fixas no layout; nenhum renderer escolhe onde escrever |
| consumo horizontal em GUI Scale 2 | largura fixa de 250 | 200, e o `Fluxo` sai do permanente |
| identidade genérica | moldura retangular, badge decorativo | pontas cortadas em diagonal, aro de canto no retrato, chip de estado |

---

## 2. O que está na tela

### Permanente

| Elemento | Origem do dado |
| --- | --- |
| **Retrato** | skin do cliente, com aro de quatro cantos |
| **Nome** | `GameProfile`, cortado por largura |
| **Vida** | `mc.player`, **nunca o delta de Nen** — ver §4 |
| **Aura** | `DeltaDeRuntimeS2C`, interpolada |
| **Chip de estado** | técnica dominante das ativas |

### Contextual

| Elemento | Quando aparece | Por quê |
| --- | --- | --- |
| **Fluxo** (microbarra) | ≥ 1 técnica ligada | sem técnica o output está em repouso, e a barra só repetiria a ausência do chip |
| **Fila de indicadores** | ≥ **2** técnicas ligadas | com uma só, o chip já disse — desenhar as duas é a mesma informação a dois centímetros de distância |

> A regra do Fluxo é por **presença**, e não por magnitude. Zetsu zera o output;
> uma regra por magnitude esconderia a barra exatamente quando ela explica o
> zero.

---

## 3. A geometria, em pixels de GUI

```
margem 8
┌────────────────────────────────────────┐  200 x 44   (51 com Fluxo)
│◤  ┌────┐  Aira                  [ TEN ]│
│   │face│  VIDA ████████░░░░    100/100 │
│   └────┘  AURA ██████░░░░░░      120/240│
│           ░░░░░░░░░░░░░░░░             │  ← Fluxo, contextual
└───────────────────────────────────────◢┘
   ● ● ●                                     ← fila, ≥2 técnicas
```

| Medida | Valor | Faixa do plano |
| --- | --- | --- |
| largura | **200** | 180–240 ✅ |
| altura compacta | **44** | 34–48 ✅ |
| altura com Fluxo | **51** | 46–58 ✅ |
| retrato | **26** | 22–28 ✅ |
| margem externa | **8** | 6–8 ✅ |
| retrato → barras | **6** | 6 ✅ |
| entre barras | **4** | 3–4 ✅ |
| altura da barra | **7** | 6–8 ✅ |
| altura do Fluxo | **3** | 3–4 ✅ |

**As três faixas são portão**, e mordem dos dois lados: `NenHudLayoutTest`
reprova tanto a HUD crescendo além do acordado quanto encolhendo a ponto de a
barra colapsar.

> **A altura com Fluxo foi corrigida pelo portão.** A primeira tentativa punha a
> microbarra em `y=41`, terminando exatamente na altura compacta — ela cabia na
> moldura baixa, então os pixels extras eram padding puro, e a HUD crescia sem
> precisar.

---

## 4. Vida não vem do delta de Nen

Ela é do Minecraft, já chega sincronizada, e pô-la no payload criaria a segunda
fonte da mesma verdade — com a agravante de a nossa chegar sempre um tick atrás.

`ProjecaoDeVida` existe mesmo assim, porque o **saneamento tem casos** e caso sem
teste é onde mora a divisão por zero:

| Caso | O que faz | Por quê |
| --- | --- | --- |
| máximo zero | `"--"`, fração 0 | não é "vida vazia", é *sem leitura* |
| absorção (28/20) | fração presa em 1.0 | acima de 1 desenharia por cima do valor |
| meio coração | arredonda para **cima** | `"0"` para quem está vivo é a pior leitura possível |
| `NaN` | fração 0, indisponível | nenhum dos três levanta exceção; todos desenham errado |

---

## 5. O comportamento por estado de Nen

| Estado | Tratamento da barra de Aura | Chip |
| --- | --- | --- |
| nenhum | neutro | não desenhado |
| Ten, Gyo, Shu, Ko | neutro | cor e nome da técnica |
| **Ren, Ken** | clareia até 18%, pulso de 34 ticks | idem |
| **Zetsu** | **dessatura** até 80%, rampa de 10 ticks | idem |

**Zetsu dessatura, e não escurece.** Escurecer leria como reserva baixa — a
informação errada, e bem no estado em que o jogador mais precisa saber que a
reserva continua cheia. Há portão comparando a luminância das duas operações.

### A precedência do chip

```java
Zetsu · Ko · Ken · Ren · Shu · Gyo · Ten
```

Do maior compromisso para o menor. **Zetsu primeiro** porque um chip dizendo
outra coisa enquanto o jogador acha que está escondido é a pior informação que
esta HUD poderia dar. **Ko antes de Ken** porque Ko deixa o resto do corpo nu
com relógio correndo.

> ### Por que esta lista tem portão de completude
>
> `EstadoDeNenNaHudTest` exige que ela cubra **exatamente** as técnicas que
> `AparenciaDeTecnica` conhece. Isso não é zelo: `ModoVisualDeTecnica` tem uma
> lista de três com a regra "técnica desconhecida não acende nada" — correta
> para datapack de terceiro —, e ela **engoliu o Ken em silêncio**. Alimentado
> com o Ken removido, este portão reprova nomeando a técnica que ficou de fora.

---

## 6. As animações

Todas com **relógio injetado**, em `AnimacoesDaHud`. A curva inteira é provada
sem esperar um tick.

| Transição | Duração | Regra |
| --- | --- | --- |
| flash de dano | 6 ticks | só a **queda** acende; curar não |
| fade do chip | 4 ticks | nasce apagado e **acende**; o inverso leria como estado terminando |
| rampa de Zetsu | 10 ticks | congela no valor atual ao inverter, para a cor não saltar |

O tempo andando para trás reinicia tudo — sem isso, o flash de uma sessão ficaria
"em andamento" por horas na seguinte.

**O flash clareia, e não pisca de vermelho.** Vermelho já é o alerta de aura
zerada; usá-lo nos dois faria "levei pancada" e "estou sem aura" lerem igual.

---

## 7. Nada de textura

A HUD não blita arquivo nenhum. Os três PNGs anteriores — `frame.png`,
`aura_pool_fill.png`, `aura_output_fill.png` — **foram removidos**.

**Por quê:** a moldura precisa de altura variável, e um blit estica, ele não
cresce só de um lado. E os gradientes travavam a cor no arquivo, enquanto este
redesenho precisa tingir a Aura por estado.

`HudAssetsTest` **inverteu de sentido**: antes cobrava presença e dimensão; agora
reprova textura órfã voltando para `nen_hud/`, e reprova código apontando para as
três que saíram.

---

## 8. Ocultar o vanilla

Duas chaves de cliente, **as duas desligadas por padrão**:

| Chave | O que faz |
| --- | --- |
| `hud.ocultarVidaVanilla` | tira os corações; a HUD de Nen já desenha vida |
| `hud.ocultarFomeVanilla` | tira a fome — **isto não remove duplicação, remove informação** |

**A regra que importa: só oculta quando há substituto na tela.** Não basta a
config estar ligada — a HUD de Nen tem de estar sendo desenhada naquele quadro.
Com F1, em espectador, ou antes do primeiro delta, os corações voltam sozinhos.
Sem isso, o relato seria *"minha vida sumiu"*, sem erro para procurar.

---

## 9. Os dois modos

O plano pede um modo compacto e um expandido. **O expandido já existia**, e não
foi duplicado:

| Modo | Onde |
| --- | --- |
| compacto | este painel |
| expandido | `TelaDoJogador` (tecla da ficha) e `OverlayDeDebug` |

---

## 10. O que nenhum portão prova

Os portões desta HUD são de **geometria e de aritmética de cor**. Nenhum deles
olha para a tela.

- **Ninguém viu a HUD redesenhada.** Se a escada da diagonal serrilha feio em
  GUI Scale 1, se o nome de dezesseis caracteres cabe, se as cores funcionam
  sobre neve e dentro de caverna — tudo isso é do olho.
- **As durações de animação não têm régua.** Seis ticks de flash pode ser rápido
  demais para o olho registrar; dez de rampa pode ser lento demais para parecer
  resposta.
- **As duas chaves de ocultar o vanilla nunca foram ligadas em jogo.** Se o id da
  camada mudar de nome numa versão futura do NeoForge, a regra passa a nunca
  casar e a opção vira botão morto, sem erro nenhum.

As três estão em [`o-que-nao-provamos.md`](o-que-nao-provamos.md), que continua
sendo a fonte.
