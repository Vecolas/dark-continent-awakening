# ADR-018 — A escada de custo é desenhada em segundos, e a reserva base fica em 100

## Contexto

A issue [#160](../../issues/160) nasceu de um relato de jogo:

> *"as auras já se esgotam muito rápido, não ficar nem 1 minuto ligado estraga a
> experiência de ser um Hunter"*

Com `aura.maximaBase = 100` e `aura.regeneracaoPorSegundo = 1.0`, Ren custava
`10.0`/s e durava **11 segundos** — um golpe, e não uma luta.

A primeira tentativa de consertar isso baixou só o custo de Ren para `4.0`, e
**um portão reprovou**:

```
KenTest > Ken troca pico por duracao  FAILED
  "Ken custa tanto quanto Ren (6.0 vs 4.0), e aí não há o que treinar para durar."
```

Ken existe para ser a versão de Ren que se aguenta. Com Ren a `4.0` e Ken a
`6.0`, a técnica de resistência teria ficado **mais cara que o pico**. A escada
inteira é relativa a Ren, e mexer numa ponta sem olhar as outras quebra o
desenho.

A investigação que se seguiu mostrou que a escada tinha rede **só no meio**:
`ten < ken < ren` e `ren > 2·ten` tinham portão; `shu < gyo`, `ko` maior de
todas, as frações de concentração, as proteções e os reforços existiam **só
como prosa** nos `.comment(...)` — e valiam por coincidência.

---

## Decisão

### 1. `aura.maximaBase` fica em **100**

A reserva é `maximaBase + auraPotential`, **aditiva**. Com base 100:

- *"comecei com 100, hoje tenho 340"* se lê sozinho;
- cada upgrade tem tamanho óbvio — +50 é +50% no primeiro dia;
- 100 é o piso de quem acabou de despertar, e vira a régua de tudo.

Subir a base para 540 (a alternativa que daria 60 s de Ren) entregaria um minuto
de graça a quem nunca treinou, e o upgrade do M6 viraria bônus sobre algo que já
bastava.

**Dois fatos foram verificados no código antes desta decisão**, e os dois
importam:

- `tecnica.ren.custoPorSegundo` alimenta **só o dreno**. A força de Ren mora em
  `reforcoBase` e `tetoDeOutput`, independentes — baixar o custo **não
  enfraquece** Ren.
- A reserva **não muda o visual**. `AuraFormulas` calcula reserva e output como
  somas independentes, e `perfis-visuais.md` §7 fixa
  `visualIntensity = clamp(sqrt(outputEfetivo / outputReferencia), 0, 1)`. Ligar
  brilho à reserva faria a aura *apagar* enquanto o jogador gasta.

### 2. A escada é escolhida em **segundos**, e o custo é consequência

```
duração = maximaBase / (custo − regeneração)
custo   = maximaBase / duração + regeneração
```

Ninguém tem intuição sobre *"4,4 de aura por segundo"*. O relato que abriu o
#160 fala em segundos, os gates de captura pedem *"5 min de Ren contínuo"*, e a
pergunta que um balanceamento responde é sempre *quanto tempo isso dura*.

**Cada `.comment(...)` de custo passa a dizer quantos segundos aquele número
compra.** E o M6 ganha um alvo legível: **+100 de `auraPotential` dobra a escada
inteira** sem tocar em nenhum custo.

### 3. A escada

| Técnica | Custo/s | Saldo/s | Dura (base 100) | Mudou |
| --- | --- | --- | --- | --- |
| Zetsu | 1,20 | **+1,80** | infinita — é o descanso | — |
| Ten | 1,50 | **+0,50** | infinita — retém, não libera | — |
| Shu | **2,40** | −1,40 | **71 s** | 3,0 → 2,4 |
| Gyo | **2,70** | −1,70 | **59 s** | 4,0 → 2,7 |
| Ken | **3,20** | −2,20 | **45 s** | 6,0 → 3,2 |
| Ren | **4,40** | −3,40 | **29 s** | 10,0 → 4,4 |
| Ko | 20,00 | −19,00 | 1 s (20 de aura) | — |

**Ten e Zetsu não mudam.** Pelo [ADR-013](ADR-013-saldo-so-para-quem-libera-aura.md)
eles recuperam de propósito; os preços deles são vulnerabilidade e teto de
Output, não reserva. Mexer neles reabriria uma decisão fechada.

**Ko não muda, e isso é escolha.** `duracaoEmTicks = 20` já o limita a 1 s, então
20,0/s cobra **20 de aura por golpe** — um quinto da reserva.

### 4. A régua de runtime passa a medir os cinco que liberam

`ConferenciaDeBalanceamento` conferia **só Ren**. Ken, Gyo, Shu e Ko liberam
aura pelo mesmo ADR-013 e ficavam de fora: o portão cobria menos do que parecia.

---

## Custo assumido

### O que fica melhor

- **Ren dura 29 s em vez de 11.** É meia luta em vez de um golpe.
- **A prosa virou régua.** `EscadaDeCustoTest` fixa nove afirmações que antes
  valiam por coincidência — e cada uma foi alimentada com um valor que deve
  reprovar, e reprovou.
- **As pilhas legais ficaram jogáveis.** `Ten + Ren` vai de 10,5 s para 25,6 s;
  a pilha de quatro, de 6,1 s para 11,1 s. Ainda punitiva, que é o desenho.

### O que se perde

**Ko ficou relativamente mais caro.** Um Ko valia ~1,8 s de Ren; agora vale
~4,6 s. Isso é desejável — Ko é compromisso, e errar o golpe tem de doer — mas
é uma mudança de peso que ninguém pediu, e está aqui para não passar
despercebida.

**O M5 vai negociar o espaço do Hatsu sem orçamento reservado.** A escada foi
calibrada só para as sete técnicas. Quando as habilidades chegarem, elas
disputam a mesma reserva de 100, e o M5 resolve com os próprios números — custo,
cooldown, ou `auraPotential`. **Decisão explícita, e não esquecimento.**

**`aura.outputBase = 10.0` é um teto por tick, e o M5 vai esbarrar nele.**
`MotorDeAura` recusa com `OUTPUT_EXCEDIDO` qualquer gasto acima disso no mesmo
tick de servidor, somado entre todos os consumidores. Para técnica sustentada é
folga larga — Ren a 4,4/s é 0,22 por tick. Mas **trava qualquer `custoBase`
instantâneo de habilidade em 10**. Está aqui para o M5 descobrir pelo ADR, e não
no depurador.

**`tecnica.ken.protecaoBase` continua um botão parcialmente morto.**
`combate.tetoDeReducaoDeDano = 0.45` corta antes: com alocação uniforme,
qualquer valor de Ken entre 0,45 e 1,0 dá exatamente a mesma redução. O número
só volta a diferenciar com alocação desigual (Ken+Gyo, Ken+Ko). **Este ADR não
muda isso** — mexer em teto de combate é decisão própria — mas
`EscadaDeCustoTest` nomeia o limite, para que ninguém gire o botão esperando
efeito linear.

**`tecnica.zetsu.multiplicadorDeRegeneracao` está exatamente no teto global**
(`aura.multiplicadorMaximoDeRegeneracao = 3.0`). Sem folga: qualquer aumento é
engolido em silêncio. Não morde hoje — Ten e Zetsu se excluem, então o produto
nunca passa de um multiplicador — mas há portão avisando.

## O que NAO muda

### O modelo de aura, e uma coisa quebrada que fica quebrada

**Existem dois "output" e eles não se falam.** `AuraPool.outputEfetivo` é a
percentagem 0..1 que as técnicas levantam e abaixam; `AuraFormulas.output` é
valor absoluto em aura. **`MotorDeAura.gastar` só consulta o segundo** — estar
em Zetsu, com teto efetivo 0,0, **não reduz em nada** quanto se pode gastar por
tick. O `min` só chega ao HUD.

É mudança de modelo de aura, e o [ADR-009](ADR-009-modelo-de-aura-sem-stamina-de-nen.md)
exige decisão conjunta registrada. Fica nomeado, com issue própria.

---

## Como isto é medido

| Régua | O que ela prende |
| --- | --- |
| `EscadaDeCustoTest` | as nove afirmações da escada, lidas do fonte do `NenConfig` |
| `ConferenciaDeBalanceamento` | saldo negativo dos cinco que liberam, no **toml carregado** |
| `TenTest`, `KenTest` | a escada de recuperação e a troca pico-por-duração |
| `ValoresDeBalanceamento` | leitor único, com os limites do regex declarados |

`EscadaDeCustoTest.aEscadaCabeNaReserva` é a única que enxerga o acoplamento com
a reserva: as outras medem **ordem entre custos**, e ordem de custo não vê
reserva nenhuma. A primeira versão dela afirmava a ordem das *durações* — e não
podia reprovar sozinha, porque com multiplicador 1,0 duração é a ordem dos
custos escrita ao contrário. A sabotagem revelou isso, e ela foi trocada por uma
banda absoluta.

**O que nenhuma régua daqui prova:** se Ren dura tempo **bom**. Isso é
cronômetro na mão numa sessão, e é a única pergunta que o #160 realmente fez.

---

## Governança da decisão

O [ADR-009](ADR-009-modelo-de-aura-sem-stamina-de-nen.md) exige aprovação
registrada dos dois desenvolvedores, identificando ambos e indicando a aprovação
de cada um.

| Desenvolvedor | Papel | Aprovação |
| --- | --- | --- |
| **@Vecolas** | Dev A — núcleo | **aprovado**, por instrução direta ao agente nesta sessão |
| **@jonex-01** | Dev B — superfície | **aprovado**, relatado por @Vecolas nesta sessão |

**A procedência de cada aprovação está escrita, e não é a mesma.** A do Dev A
veio direta; a do Dev B chegou relatada pelo Dev A. O agente não assinou por
ninguém — ele registrou o que foi dito e por quem. Se a segunda aprovação não
corresponder ao que @jonex-01 entendeu, **esta linha é o lugar de corrigir**.

**O Dev A acrescentou duas condições à aprovação**, e elas são parte do que foi
aprovado: a base fica em **100** (a alternativa de subir a reserva foi recusada
explicitamente), e o orçamento do Hatsu **não** é reservado agora.
