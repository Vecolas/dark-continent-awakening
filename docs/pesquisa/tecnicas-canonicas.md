# As técnicas de Nen, pelo cânone

**Origem:** documento escrito por @Vecolas em 2026-09-12, em resposta às
decisões de balanceamento do M4.
**Estatuto:** documento-fonte. Quando o código e este texto discordarem, é
motivo de conversa — não de correção silenciosa de nenhum dos dois lados.

Ele existe porque as decisões de balanceamento estavam sendo tomadas uma de
cada vez, e cada uma isolada parecia razoável. Vistas juntas, elas se
contradiziam — e a contradição de Zetsu (ver
[ADR-013](../adr/ADR-013-saldo-so-para-quem-libera-aura.md)) só apareceu quando
alguém olhou o conjunto.

---

## 1. Ten — 纏 — "Envolver"

A aura escapa continuamente pelos pontos de aura do corpo. Ten é manter essa
aura circulando **através e ao redor** do corpo em vez de deixá-la escapar. O
resultado é uma **camada fina e contínua** envolvendo a pessoa.

> Não deveria parecer fumaça. É uma segunda pele energética.

**O que faz:** defesa básica de Nen; relevante contra pressão e hostilidade
transmitidas por Nen, **insuficiente** contra ataques de Nen realmente
poderosos. Impede a energia vital de escapar — a obra associa isso à
preservação da vitalidade.

**Em combate:** é o estado de base de um usuário treinado.

| | |
| --- | --- |
| Proteção Nen | básica |
| Aura externa | pequena |
| Consumo | **extremamente baixo** |
| Controle | estável |

> No mod, Ten deve ser quase o estado operacional normal depois de certa
> proficiência.

## 2. Zetsu — 絶 — "Suprimir"

O oposto de Ten: o usuário **fecha** os pontos de aura e interrompe a emissão
externa. A presença energética praticamente desaparece.

| | |
| --- | --- |
| Output externo | **0** |
| Recuperação | **alta** |
| Detecção por Nen | muito baixa |
| Defesa Nen | **praticamente 0** |

Serve para perseguição, infiltração, emboscada, esconder presença e
**descansar/recuperar-se** — a obra relaciona Zetsu à redução da fadiga, porque
o corpo deixa de manter a camada externa.

**A desvantagem é enorme.** Quem está em Zetsu e é atingido por um ataque
reforçado com Nen sofre dano muito maior que alguém em Ten ou Ken.

> Zetsu não é "modo furtivo gratuito". É **"desliguei minha armadura para
> desaparecer do radar"**.

## 3. Ren — 練 — "Amplificar"

Produzir e exteriorizar **muito mais** aura que em Ten. Se Ten é controlar o
que se tem, Ren é abrir a torneira. A camada continua originada no corpo, mas
fica muito mais espessa e intensa.

Aumenta força, resistência e a **quantidade de aura disponível para técnicas**.
É base energética de várias técnicas avançadas.

**Custo:** consome aura muito mais intensamente que Ten — e por isso existe
**treino específico de quanto tempo se consegue manter Ren**. Biscuit usa
duração de Ren/Ken como parte central do treinamento.

**Ren hostil:** com intenção assassina, a aura exerce pressão psicológica —
medo, paralisia, até consequências físicas. No mod isso seria
`attackerOutput` contra `targetNenDefense`, **não** um debuff automático: a
diferença de output e domínio deve importar.

## 4. Hatsu — 発 — "Expressar"

**Não é "o poder especial do personagem".** É o princípio pelo qual o usuário
expressa a aura conforme a própria natureza e categoria. As Nen Abilities
nascem dessa expressão.

| Categoria | Conceito central |
| --- | --- |
| Enhancement | reforçar propriedades existentes |
| Transmutation | alterar propriedades da aura |
| Emission | manter aura separada do corpo |
| Conjuration | materializar construções de aura |
| Manipulation | controlar pessoas, objetos, alvos |
| Specialization | o que não se enquadra nas demais |

> Hatsu **não** deve ser um botão como Ten ou Zetsu. É uma camada do sistema de
> habilidades.

## 5. Gyo — 凝 — "Concentrar"

Aplicação avançada de Ren: concentrar parte maior da aura numa região do corpo.
A região fica mais poderosa e **o resto recebe proporcionalmente menos**.

```
normal                 Gyo no braço direito
cabeça   15%           cabeça    8%
tronco   30%           tronco   20%
braços   25%           braço E  10%   BRAÇO D 47%
pernas   30%           pernas   15%
```

**Gyo nos olhos** permite ver aura sutil, rastros, objetos de Nen e **aura
escondida por In**.

> No mod, Gyo tem **dois** usos reais — percepção (olhos) e concentração
> (membro). Não apenas "visão especial".

## 6. In — 隱 — "Ocultar"

Forma avançada de Zetsu, e a diferença é essencial:

| | |
| --- | --- |
| **Zetsu** | a aura deixa de ser emitida |
| **In** | a aura **continua existindo**, mas é ocultada |

Esconde aura de outros usuários de Nen em condições normais, e também
construções e efeitos. **Counter: Gyo nos olhos.**

## 7. En — 円 — "Círculo"

Ren + Ten: expandir a aura muito além do corpo e usá-la organizada numa área.
Tudo dentro dela é percebido — presença, posição, movimento, forma. É um radar
de aura.

**Muito caro.** O critério básico citado é mais de dois metros por mais de um
minuto; usuários excepcionais chegam a dezenas ou centenas.

> O raio **não** é valor fixo: depende de output, controle, proficiência e
> tempo de manutenção.

## 8. Shu — 周 — "Envolver um objeto"

Extensão de Ten a um objeto, que passa a funcionar como extensão energética do
corpo: mais resistente, mais poderoso, interage melhor com Nen. Não se limita a
armas — cartas, pedras, ferramentas, projéteis, fios.

> No Minecraft: segurar a arma + ativar Shu = a aura envolve o `ItemStack`.
> Pode haver treino por objeto.

## 9. Ko — 硬 — concentração extrema

Combina Ten + Zetsu + Ren + Hatsu + Gyo: praticamente **toda** a aura numa
única região.

```
punho  ~100%
resto    ~0%
```

O resto do corpo perde a defesa de Nen. Errar o golpe e ser atingido no tronco
é catastrófico. Altíssimo risco.

## 10. Ken — 堅 — "Fortificar"

Ten + Ren sustentados juntos: muito mais aura envolvendo **o corpo inteiro** —
a obra cita cerca de dez vezes a de Ten, sem que isso seja fórmula universal.

| | |
| --- | --- |
| **Ko** | uma região extrema, resto vulnerável |
| **Ken** | corpo inteiro com defesa alta |

Principal defesa geral contra usuários de Nen, e muito mais cansativo que Ten.

> No mod: **Ten** é o estado ocioso/padrão de combate; **Ken** é o estado
> defensivo, com dreno contínuo.

## 11. Ryu — 流 — "Fluxo"

**Gyo dinâmico enquanto mantém Ken** — redistribuir a aura em tempo real:

```
antes de socar     braço atacante 70%   resto 30%
ao ver o chute     perna defensora 80%  resto 20%
```

Difícil porque exige, ao mesmo tempo: controlar a própria aura, prever o ataque
e **ler a distribuição do adversário**. E redistribuir devagar demais entrega o
próximo movimento.

> É por isso que combate de Nen avançado não é "quem tem mais aura".

---

## A tabela inteira

| Técnica | Função | Aura | Risco |
| --- | --- | --- | --- |
| Ten | manter aura em volta do corpo | baixo consumo | baixo |
| Zetsu | interromper aura externa | recuperação | defesa ~zero |
| Ren | aumentar produção/output | alto | fadiga |
| Hatsu | expressão individual | variável | depende |
| Gyo | concentrar numa região | médio | outras regiões enfraquecem |
| In | ocultar aura | variável | Gyo detecta |
| En | expandir como sensor | muito alto | fadiga/exposição |
| Shu | envolver objeto | médio | consumo |
| Ko | quase tudo num ponto | altíssimo local | resto desprotegido |
| Ken | muito em todo o corpo | alto contínuo | fadiga |
| Ryu | redistribuir dinamicamente | alto/variável | exige controle enorme |

## A progressão

```
INICIANTE        Ten · Zetsu · Ren · Hatsu
INTERMEDIÁRIO    Gyo · In · Shu
AVANÇADO         Ken · Ko · En
ALTO NÍVEL       Ryu + Hatsu individual + leitura do adversário
```

Coerente com a obra: as avançadas vêm depois do domínio dos quatro princípios,
e Biscuit treina Gon e Killua em Ken, Ko e depois Ryu.

## A direção que isto dá ao mod

Controláveis diretamente: **Ten, Zetsu, Ren, Gyo, Ken** e eventualmente **En**.
**Hatsu** é o framework das habilidades. **Ryu** evolui para o sistema central
de combate avançado, controlando quanto da aura fica em cada região.

> A progressão deixa de ser *"tenho 20% mais aura"* e passa a ser *"sei usar
> melhor a aura que tenho"* — que é muito mais fiel a Hunter × Hunter.

**Consequência de arquitetura:** a distribuição de aura por região do corpo não
é detalhe de Gyo. Ela é a espinha de Gyo, Ko, Ken e Ryu — quatro das sete
técnicas que faltam. Ela precisa nascer no servidor, autoritativa, e não como
enfeite de renderização.
