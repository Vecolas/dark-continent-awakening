# AV1 — as perguntas da sessão (#176)

Folha para ser **respondida durante a sessão**, com o jogo aberto. Ela não
substitui o [`LEIA-ME.md`](LEIA-ME.md), que diz o que cada captura é; aqui está
o que cada uma **pergunta**, com espaço para o veredicto ao lado.

---

## Resultado da sessão de 2026-09-22 (`b8b9295`)

## O #176 ESTÁ FECHADO.
**As quinze perguntas do gate foram respondidas, e todas passaram.**

| Bloco | Resultado |
| --- | --- |
| **Z1 — o gate inteiro** | ✅ com `particulas 0`, **evidente** em Ten *e nas outras técnicas* |
| A1–A7 — os sete ambientes | ✅ dia, noite, caverna, neve, Nether, chuva, água |
| B1 · B2 — corpo | ✅ modelo slim correto; overlay **não engole** |
| D1 — ruído | ✅ **veios**, com `escala_de_ruido` em 4.0 sem precisar girar |
| D2 — giro 360° | ✅ **sem z-fighting** |
| E1 — log do dedicado | ✅ **limpo**, dois jogadores, varrido em produção |
| C1 · C2 — série de distância | ✅ sem degrau de LOD; a 40 b ainda comunica |
| E2 — ripple (#103) | ⚠️ **não acendia** — três defeitos corrigidos, **reteste pendente** |

### O que o Z1 prova, e é mais do que o gate pedia

O critério era *"ainda se lê 'essa pessoa está em Ten'"*. O veredicto foi além:
a leitura se manteve para as **outras técnicas** também, com partícula em zero.
É o oposto exato do modo de falhar que o [ADR-015](../../../adr/ADR-015-aura-e-geometria-e-shader.md)
teme — uma shell fraca carregada pelo acabamento. Aqui o acabamento saiu e a
shell ficou de pé sozinha.

### O que a sessão encontrou além do gate

**O E2 não é item do #176, e foi o mais produtivo.** O ripple **não acendia
nunca**, e a resposta expôs **três** defeitos que nenhum dos onze testes pegava:
o teto de 1.0 que o tornava impossível em Ten, o gatilho que exigia dois
pacotes no mesmo tick, e uma força de 0,05 para um soco. Corrigidos; **o
reteste é a única coisa que o AV1 ainda deve**.

E dois defeitos do **bloom** saíram da mesma sessão: o halo atravessando grama
(máscara copiada antes do passe de *cutout*) e o acoplamento em que o Ren de um
jogador clareava a aura dos outros.

---

> **Toda pergunta é escrita para que PASSA signifique aprovado.** A folha do AV0
> misturou polaridade — duas perguntas formuladas de modo que "sim" significava
> *reprovou* —, e a primeira sessão devolveu "sim para todos", que não dava para
> interpretar. **Registre PASSA ou REPROVA, nunca sim/não.**

**Montagem:** servidor dedicado (nunca singleplayer), **um cliente** basta —
diferente do AV0, a série de distância do AV1 não exige o segundo. Overlay `F6`
**sem** o aviso `OVERRIDE ATIVO`. Comparação com a referência de arte **na mesma
sessão**.

> **O que NÃO está em julgamento.** A referência B tem **filamentos**, e o AV1
> não os entrega — ribbons são AV2. A comparação com B é **parcial**, e isso
> precisa estar escrito ao lado do veredicto: sem essa nota, alguém lê o gate
> como *"ficou aquém da referência"*. **Bloom é AV5**, e este gate assume a
> ausência dele de propósito.

> **Esta folha é a evidência inteira do AV1.** As capturas da trilha foram
> aposentadas em 2026-09-22 (AV0–AV8) — ver
> [`../../CAMPANHA-EVIDENCIAS.md`](../../CAMPANHA-EVIDENCIAS.md) §6.1. Os nomes
> de arquivo abaixo continuam sendo o **roteiro do que olhar**; eles só não
> viram PNG.

---

## Bloco Z — o gate inteiro

> **É esta pergunta que decide o #176.** As outras dezesseis descrevem; esta
> aprova ou reprova.

```
/nenvfx particulas 0
```

### Z1 · `sem_particulas_ten`
**Com partícula em ZERO, ainda se lê "essa pessoa está em Ten"?**
REPROVA se parece o jogador normal.

- [x] **PASSA** — `e21a6dc`, 2026-09-22: *"com `/nenvfx particulas 0` ainda é
  evidente que o jogador está em Ten ou outras técnicas"*.

> **O gate do #176 está respondido, e com folga.** O veredicto não foi só sobre
> Ten: a leitura se manteve para as **outras técnicas** também, sem partícula
> nenhuma. É o oposto do modo de falhar que o ADR-015 teme — a shell não estava
> sendo carregada pelo acabamento.

> **Se reprovar, a resposta é VOLTAR PARA A SHELL** — não subir a partícula nem
> o brilho. Está no [ADR-015](../../../adr/ADR-015-aura-e-geometria-e-shader.md),
> e existe porque compensar uma shell fraca com mais partícula é a forma mais
> comum de falhar nesta trilha. Uma aura aprovada com partícula alta é uma aura
> que ninguém vai conseguir consertar depois.

**Ao terminar:** `/nenvfx particulas auto` devolve o controle à config.

---

## Bloco A — a shell se lê nos sete ambientes? (1 cliente)

Todos com partícula **no valor normal**, salvo nota.

### A1 · `ten_dia` — luz plena, sem estourar
- [x] **PASSA** — `e21a6dc`, 2026-09-22 (luz plena)

### A2 · `ten_noite` — escuro, sem sumir
- [x] **PASSA** — `e21a6dc`, 2026-09-22 (escuro)

### A3 · `ten_caverna` — luz zero; **não vira borrão?**
- [x] **PASSA** — `e21a6dc`, 2026-09-22 (luz zero)

### A4 · `ten_neve` — **fundo branco**
O caso que mais ataca a borda: branco contra branco.
- [x] **PASSA** — `e21a6dc`, 2026-09-22 (fundo branco)

### A5 · `ten_nether` — **fundo vermelho saturado**
O outro extremo: a aura compete com um ambiente que já é todo cor.
- [x] **PASSA** — `e21a6dc`, 2026-09-22 (fundo vermelho)

### A6 · `ten_chuva` — partícula de chuva cruzando a shell
- [x] **PASSA** — `e21a6dc`, 2026-09-22 (chuva)

### A7 · `ten_agua` — submerso, com a névoa de água por cima
- [x] **PASSA** — `e21a6dc`, 2026-09-22 (submerso)

---

## Bloco B — a shell veste qualquer corpo? (1 cliente)

### B1 · `ten_slim`
**O cliente slim usa o modelo slim, e o braço fino não fica com aura larga?**
- [x] **PASSA** — `b8b9295`, 2026-09-22: *"usa modelo slim"*.

### B2 · `ten_overlay_skin`
**A shell sobrevive a uma skin com segunda camada completa (`hat`/`jacket`/`sleeve`)?**
PASSA se a aura continua visível; REPROVA se as camadas a engolem.
- [x] **PASSA — não engole** — `b8b9295`, 2026-09-22.

> Consistente com o AV0, que respondeu o mesmo em `5a9182b`. **Duas sessões
> independentes, um dia de distância, mesma resposta** — é a única pergunta
> desta trilha com confirmação repetida, e ela cobre o caso mais hostil à
> leitura da shell.

---

## Bloco C — a série de distância (1 cliente)

### C1 · `ten_2b` · `ten_5b` · `ten_10b` · `ten_20b` · `ten_40b`
**A troca de nível de detalhe passa SEM degrau perceptível?**
PASSA se a transição é contínua; REPROVA se há *LOD popping*.
- [x] **PASSA** — `1fdc965`, 2026-09-22: *"a aura vai ficando mais fraca à
  medida que se afasta, sem degrau"*.

> Vale para o build corrigido: a conta de um Ten **sozinho** é 0,04 antes e
> depois do conserto do bloom. A sessão julgou com os dois em Ten, que é a
> condição em que o acoplamento não contamina a leitura.

### C2
**Em 40 blocos a aura ainda comunica "esta pessoa está com Ten ligado"?**
- [x] **PASSA** — `1fdc965`, 2026-09-22: *"comunica que está com Nen"*.

---

## Bloco D — as duas perguntas técnicas

### D1 · `ten_ruido_ampliado`
**O ruído tem VEIOS, ou nuvens?**
Aproxime a câmera até a shell encher a tela. PASSA se lê como fibra/veio;
REPROVA se lê como mancha difusa.

- [x] **PASSA — veios** — `b8b9295`, 2026-09-22: *"o ruído tem veios"*.

> Nenhum número precisou ser girado: `escala_de_ruido` fica em **4.0**, como
> está em `ten.json`. O AV2 herda esse valor como linha de base.

> **Este é o único item do AV1 que se conserta sem recompilar.** O número vive em
> `assets/nenfoundation/nen_vfx/ten.json`, e **F3+T recarrega**:
> `"escala_de_ruido": 4.0` — mais alto aperta o veio, mais baixo alarga a mancha.
> Dá para iterar na própria sessão. Se você girar, **anote o valor final aqui**,
> porque ele precisa entrar no arquivo para virar real:
> `escala_de_ruido final = ____________`

### D2 · `ten_360`
**Girando a câmera 360° ao redor do jogador, há z-fighting?**
Z-fighting = faces piscando/rasgando quando duas superfícies coincidem.
- [x] **PASSA** — `b8b9295`, 2026-09-22: *"girando em 360 não há z-fighting"*.

> Vale registrar o que isso prova além da tela: a **escada de geometria**
> (`AuraGeometryLadder`) empilha passes de shell concêntricos, e faces
> coplanares entre passes seriam exatamente o sintoma. Não houve.

---

## Bloco E — o que não é imagem

### E1 · vazamento de classe client-only
**O log do servidor DEDICADO tem `NoClassDefFoundError` ou `ClassNotFoundException`
de `net.minecraft.client.*`?**

> **Use a INSTÂNCIA, não o `runServer`.** No workspace de desenvolvimento as
> classes de cliente estão no classpath: um vazamento encontra a classe e **não
> lança nada**, e o log sai limpo pelo motivo errado. Foi assim que o E1 do AV0
> quase fechou com meia prova.

- [x] **log limpo** — `b8b9295`, 2026-09-22, varrido por mim na instância, com
  **dois jogadores conectados** (`Gon` 14:52, `Kurapika` 14:54) e o mod carregado
  (`Protocolo de rede v9`):

  | Procurado | `latest.log` | `debug.log` (531 linhas) |
  | --- | --- | --- |
  | `NoClassDefFoundError` / `ClassNotFoundException` | **0** | **0** |
  | `net.minecraft.client` / `net/minecraft/client` | **0** | **0** |
  | `nenfoundation.client` | **0** | **0** |
  | `[ERROR]` / `[FATAL]` / `Exception` / `Caused by` | **0** | **0** |

  > **Lido com o servidor AINDA DE PÉ.** Vale para a sessão até 14:54; o que
  > acontecer depois não está nesta varredura, e o save-ao-parar (`stop`) não
  > foi exercitado — é onde um defeito de persistência apareceria.

### E2 · o ripple de impacto (#103), que entrou nesta versão
**Ao apanhar, a aura acende no tronco e apaga em ~8 ticks?**

Não é item do #176 — é oportunidade: ele acabou de ser mergeado e **nunca foi
visto em tela**. Os onze testes medem `float` e `HashMap`, não pixel, e a ligação
no cliente **não tem régua nenhuma**.

- [ ] acende e decai  [ ] não acende  [ ] fica preso aceso — `____________________`
- levou pancada e a aura **não** reagiu? Anote o tipo de dano: `____________`

---

## Ao fechar

- [ ] esta folha preenchida, **na mesma sessão**;
- [x] ~~decidir se as 17 capturas são arquivadas~~ — **aposentadas em 2026-09-22**,
      junto com as de toda a trilha;
- [ ] `o-que-nao-provamos.md` com o que a sessão **não** provou;
- [ ] `../../../processo/marcos.md` atualizado;
- [ ] `compatibility.md`, se a sessão tocar renderer ou shader pack.

> ### O custo que a aposentadoria deixa aqui
>
> A objeção de "não é revisável por terceiro" **não se aplica**: uma única
> pessoa faz a conferência desta trilha.
>
> O que sobra é específico deste gate: **o AV1 é a linha de base do AV2.** Os
> filamentos entram sobre esta mesma shell, e sem o "antes" a única forma de
> saber o que eles mudaram é a memória de quem viu os dois. As duas perguntas
> mais caras de não ter arquivado são **D1** (`ten_ruido_ampliado` — o AV2 mexe
> no mesmo ruído) e **Z1** (o AV2 terá de passar no mesmo critério, com mais
> geometria por cima).
