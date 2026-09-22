# AV3 — as perguntas da sessão (#187)

Folha para ser **respondida durante a sessão**, com o jogo aberto. Registre
`PASSA` ou `REPROVA`, nunca sim/não.

**Montagem:** servidor dedicado, **dois clientes** (o bloco de distância pede o
segundo jogador). Overlay `F6` **sem** `OVERRIDE ATIVO`.

> **É aqui que Ten fecha.** O AV0 provou aderência, o AV1 provou a shell sozinha,
> o AV2 provou os filamentos. O AV3 junta os três — e a partir dele **a nuvem de
> partícula antiga sai de cena**. Um REPROVA aqui não é um detalhe a ajustar: é
> Ten não estar pronto.

> **Esta folha é a evidência inteira do AV3.** A trilha dispensou captura em
> 2026-09-22.

---

## Bloco P — **primeira pessoa** (o que só este gate vê)

> A primeira pessoa foi **consertada em 2026-09-21** (#301): a shell desenhava
> no centro do corpo em vez de no braço, e **Ten ficava invisível** por causa
> disso. Agora ela espelha a pose que a vanilla monta, e três filamentos por
> braço nasceram junto.
>
> É a única parte da aura que **nenhum gate anterior olhou**, e a que tinha um
> defeito que a tornava inexistente.

### P1 · `primeira_pessoa_ten` — Ten **aparece**?
Em primeira pessoa, com Ten ligado, você vê a aura no próprio braço?
REPROVA se não há nada — era exatamente esse o defeito de #301.

- [ ] PASSA  [ ] REPROVA — `____________________`

### P2 · a borda **acompanha o braço**?
Mova e ataque. A borda segue o braço, ou fica para trás / flutua no centro?

- [ ] PASSA  [ ] REPROVA — `____________________`

### P3 · **nenhuma coluna** entra em cena?
Colunas são pressão de chão, do Ren. Em Ten elas são **zero por dado**
(`pressao.colunas: 0`), e em primeira pessoa uma coluna apareceria dentro da
câmera.

- [ ] PASSA  [ ] REPROVA — `____________________`

---

## Bloco A — os sete ambientes

Dia, noite, caverna, Nether, neve, água, chuva. O AV1 já passou nisto **sem
filamentos**; a pergunta aqui é se eles sobrevivem aos mesmos fundos.

- [ ] PASSA  [ ] REPROVA — `____________________`

---

## Bloco D — a tabela de distâncias (**dois clientes**)

`2b` · `5b` · `10b` · `20b` · `40b`, com o outro jogador em Ten.

### D1 · há degrau de LOD em alguma troca?
- [ ] PASSA  [ ] REPROVA — `____________________`

### D2 · a 40 blocos ainda comunica "está em Ten"?
- [ ] PASSA  [ ] REPROVA — `____________________`

---

## Bloco M — movimento e corpo

`correndo` · `agachado` · `nadando` — e `slim` · `armadura` · `overlay_skin`.

### M1 · em movimento, shell e filamentos acompanham?
- [ ] PASSA  [ ] REPROVA — `____________________`

### M2 · com **armadura completa**, a aura aparece E a armadura continua reconhecível?
O perfil declara `borda_com_armadura: 0.065` contra `0.052` normal — só o
bastante para vazar por fora da armadura **padrão**.

- [ ] PASSA  [ ] REPROVA — `____________________`

### M3 · slim e overlay de skin?
- [ ] PASSA  [ ] REPROVA — `____________________`

---

## Bloco Z — os dois critérios que decidem

### Z1 · `sem_particulas_ten` — **o critério do ADR-015**

```
/nenvfx particulas 0
```

**Com partícula em zero, Ten ainda se lê?** Terceira vez que esta pergunta é
feita, e a primeira com shell + filamentos + tuning juntos.

- [ ] PASSA  [ ] REPROVA — `____________________`

### Z2 · `sem_bloom_ten` — **novo, e agora possível**

```
/nenvfx bloom off
```

**Sem brilho nenhum, Ten ainda se lê?** O bloom é AV5; aqui ele sai de cena para
provar que a leitura **não depende dele**.

- [ ] PASSA  [ ] REPROVA — `____________________`

> **Este comando não existia até hoje.** O nível de bloom só vinha da config, e
> não há tela de config neste mod — mudar exigia sair do jogo, editar o `.toml`
> e reiniciar, o que quebra a regra de comparar **na mesma sessão**. Criado ao
> preparar este gate.
>
> **Ao terminar:** `/nenvfx bloom auto` e `/nenvfx particulas auto`.

---

## Bloco C — os contadores (**inclui a dívida do AV2**)

No `F6`, com um jogador em Ten, anote:

- [ ] filamentos vivos: `____________`
- [ ] partículas vivas: `____________`
- [ ] chamadas de desenho: `____________`
- [ ] LOD efetivo: `____________`

> **Não é passa/reprova — é a linha de base do AV8**, que mede orçamento. E os
> dois primeiros **pagam a dívida que o AV2 deixou**: lá eles não foram
> anotados, e número não escrito não existe depois.

---

## Bloco E — o que não é imagem

### E1 · log do servidor **dedicado** (a instância, não o `runServer`)
- [ ] log limpo  [ ] achou ocorrência — `____________________`

### E2 · regressões
- [ ] a grama ainda oclui o halo? `____________`
- [ ] ligar Ren ainda **não** clareia a aura dos outros? `____________`
- [ ] o ripple ainda acende ao levar pancada? `____________`

---

## Ao fechar

- [ ] esta folha preenchida, **na mesma sessão**;
- [ ] `o-que-nao-provamos.md` com o que a sessão **não** provou;
- [ ] `../../../processo/marcos.md` atualizado;
- [ ] `compatibility.md`, se a sessão tocar renderer ou shader pack.

> **Fechando o AV3, Ten está aprovado por inteiro** e a nuvem de partícula antiga
> sai de cena. O AV4 é Ren — outra técnica, outro perfil, e a primeira em que a
> aura toca o chão.
