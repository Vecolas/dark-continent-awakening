# AV1 — as perguntas da sessão (#176)

Folha para ser **respondida durante a sessão**, com o jogo aberto. Ela não
substitui o [`LEIA-ME.md`](LEIA-ME.md), que diz o que cada captura é; aqui está
o que cada uma **pergunta**, com espaço para o veredicto ao lado.

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
> precisa estar na legenda de cada captura: sem essa nota, alguém lê o gate como
> *"ficou aquém da referência"*. **Bloom é AV5**, e este gate assume a ausência
> dele de propósito.

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

- [ ] PASSA  [ ] REPROVA — veredicto: `____________________`

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
- [ ] PASSA  [ ] REPROVA — `____________________`

### A2 · `ten_noite` — escuro, sem sumir
- [ ] PASSA  [ ] REPROVA — `____________________`

### A3 · `ten_caverna` — luz zero; **não vira borrão?**
- [ ] PASSA  [ ] REPROVA — `____________________`

### A4 · `ten_neve` — **fundo branco**
O caso que mais ataca a borda: branco contra branco.
- [ ] PASSA  [ ] REPROVA — `____________________`

### A5 · `ten_nether` — **fundo vermelho saturado**
O outro extremo: a aura compete com um ambiente que já é todo cor.
- [ ] PASSA  [ ] REPROVA — `____________________`

### A6 · `ten_chuva` — partícula de chuva cruzando a shell
- [ ] PASSA  [ ] REPROVA — `____________________`

### A7 · `ten_agua` — submerso, com a névoa de água por cima
- [ ] PASSA  [ ] REPROVA — `____________________`

---

## Bloco B — a shell veste qualquer corpo? (1 cliente)

### B1 · `ten_slim`
**O cliente slim usa o modelo slim, e o braço fino não fica com aura larga?**
- [ ] PASSA  [ ] REPROVA — `____________________`

### B2 · `ten_overlay_skin`
**A shell sobrevive a uma skin com segunda camada completa (`hat`/`jacket`/`sleeve`)?**
PASSA se a aura continua visível; REPROVA se as camadas a engolem.
- [ ] PASSA  [ ] REPROVA — `____________________`

---

## Bloco C — a série de distância (1 cliente)

### C1 · `ten_2b` · `ten_5b` · `ten_10b` · `ten_20b` · `ten_40b`
**A troca de nível de detalhe passa SEM degrau perceptível?**
PASSA se a transição é contínua; REPROVA se há *LOD popping*.
- [ ] PASSA  [ ] REPROVA — `____________________`

### C2
**Em 40 blocos a aura ainda comunica "esta pessoa está com Ten ligado"?**
- [ ] PASSA  [ ] REPROVA — `____________________`

---

## Bloco D — as duas perguntas técnicas

### D1 · `ten_ruido_ampliado`
**O ruído tem VEIOS, ou nuvens?**
Aproxime a câmera até a shell encher a tela. PASSA se lê como fibra/veio;
REPROVA se lê como mancha difusa.

- [ ] PASSA (veios)  [ ] REPROVA (nuvens) — `____________________`

> **Este é o único item do AV1 que se conserta sem recompilar.** O número vive em
> `assets/nenfoundation/nen_vfx/ten.json`, e **F3+T recarrega**:
> `"escala_de_ruido": 4.0` — mais alto aperta o veio, mais baixo alarga a mancha.
> Dá para iterar na própria sessão. Se você girar, **anote o valor final aqui**,
> porque ele precisa entrar no arquivo para virar real:
> `escala_de_ruido final = ____________`

### D2 · `ten_360`
**Girando a câmera 360° ao redor do jogador, há z-fighting?**
Z-fighting = faces piscando/rasgando quando duas superfícies coincidem.
- [ ] PASSA  [ ] REPROVA — `____________________`

---

## Bloco E — o que não é imagem

### E1 · vazamento de classe client-only
**O log do servidor DEDICADO tem `NoClassDefFoundError` ou `ClassNotFoundException`
de `net.minecraft.client.*`?**

> **Use a INSTÂNCIA, não o `runServer`.** No workspace de desenvolvimento as
> classes de cliente estão no classpath: um vazamento encontra a classe e **não
> lança nada**, e o log sai limpo pelo motivo errado. Foi assim que o E1 do AV0
> quase fechou com meia prova.

- [ ] log limpo  [ ] achou ocorrência — `____________________`

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
- [ ] decidir se as 17 capturas são arquivadas (ver abaixo);
- [ ] `o-que-nao-provamos.md` com o que a sessão **não** provou;
- [ ] `../../../processo/marcos.md` atualizado;
- [ ] `compatibility.md`, se a sessão tocar renderer ou shader pack.

> ### A decisão sobre arquivar as 17 imagens
>
> O AV0 **aposentou** as suas catorze, e ficou registrado que aquilo **não se
> estende** — cada gate decide por si.
>
> Aqui o peso é maior: **o AV1 é a linha de base do AV2.** Os filamentos entram
> sobre esta mesma shell, e sem o "antes" a única forma de saber o que eles
> mudaram é a memória de quem viu os dois. As mais caras de perder são
> `ten_ruido_ampliado` (o AV2 mexe no mesmo ruído) e `sem_particulas_ten` (o
> AV2 tem de passar no mesmo critério, com mais geometria).
