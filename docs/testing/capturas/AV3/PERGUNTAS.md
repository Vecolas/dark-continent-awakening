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

## Resultado da sessão de 2026-09-22

| # | Resultado |
| --- | --- |
| **P1 · P2 · P3** — primeira pessoa | ✅ PASSA — aparece, acompanha, e **só o braço** entra em cena |
| **A** — sete ambientes | ✅ PASSA |
| **D1 · D2** — distância | ✅ PASSA — sem degrau, e a 40 b ainda comunica |
| **M1 · M3** — movimento, slim/default | ✅ PASSA — **carregadas** do AV2 |
| **E2** — três regressões | ✅ PASSA — **carregadas** do AV2 |
| **Z1** — `particulas 0` | ✅ PASSA — **carregada** (terceira vez que seria perguntada) |
| **Z2** — `bloom off` | ❌ **REPROVA** — e o defeito é do **AV5**, não do AV3 |
| **M2** — armadura completa | ⬜ nunca perguntada |
| **C1–C4** — os contadores | ⬜ **dívida herdada do AV2** |

### O que "carregadas" significa, e por que não é atalho

O pedido foi explícito: *"é perda de tempo responder a mesma coisa sem que nada
tenha mudado"*. Está certo — `particulas 0` seria a **terceira** vez.

Mas carregar um veredito de boca é como se fabrica falso verde. Então virou
regra: [`../../RESPOSTAS.md`](../../RESPOSTAS.md) registra cada resposta **com os
arquivos que a invalidam**, e `RespostasAindaValidasTest` reprova o build quando
um deles muda. **A resposta não expira por tempo; expira por causa.**

Entre o AV2 (`280696b`) e hoje mudaram quatro arquivos no caminho de VFX, e os
quatro são a instrumentação de `/nenvfx bloom`. Nenhum invalidador das respostas
carregadas está entre eles. **Não é confiança: é a verificação que coube.**

### O Z2, que é o achado desta sessão

Com `particulas 0` **e** `bloom off` ao mesmo tempo, o relato foi: sobram
filamentos e um pouco da shell, *"bem transparentes, quase não dá para ver"*.

O [ADR-016](../../../adr/ADR-016-pos-processamento-proprio-da-aura.md) promete
por escrito o contrário:

> `OFF` — nenhum passe extra. **A aura continua legível pela borda Fresnel e
> pelas ribbons.** [...] **o sistema principal funciona inteiro com ele
> desligado.**

**A causa é conhecida e não é mistério.** `FAST` compensa a falta do composite
por geometria — `REFORCO_DE_HALO_EM_FAST = 1.9` e dois degraus extras. **`OFF`
não compensa com nada:** herda `alpha_borda: 0.20` do perfil, que foi calibrado
com o bloom LIGADO, porque AV0, AV1 e AV2 rodaram todos com ele ligado. Ninguém
nunca olhou o `OFF`.

**Por que isso não reprova o AV3.** O critério do
[ADR-015](../../../adr/ADR-015-aura-e-geometria-e-shader.md) é sobre
**partícula**, não sobre bloom — e é o Z1, que passa. O Z2 pergunta sobre um
nível de bloom, e bloom é o AV5. **Ter posto o Z2 nesta folha foi alcance meu
além do marco**, e a resposta certa é entregar o achado ao gate dono dele, não
consertar o AV5 por dentro do AV3.

Fica registrado em [`marcos.md`](../../../processo/marcos.md) como dívida
nomeada do AV5.

---

## O quadro da sessão — tudo que precisa de resposta

**Doze julgamentos** (PASSA/REPROVA) e **quatro números**. O E1 é meu.

| # | Pergunta | Como verificar |
| --- | --- | --- |
| **P1** | Em primeira pessoa, **Ten aparece**? | F5 para 1ª pessoa, Ten ligado |
| **P2** | A borda **acompanha o braço**? | mover e atacar em 1ª pessoa |
| **P3** | **Nenhuma coluna** entra em cena? | olhar para baixo em 1ª pessoa |
| **A** | A aura se lê nos **sete ambientes**? | dia, noite, caverna, Nether, neve, água, chuva |
| **D1** | Há **degrau de LOD** entre 2b→40b? | 2ª cliente parado em Ten; afastar-se |
| **D2** | A **40 blocos** ainda comunica Ten? | idem, na distância máxima |
| **M1** | Correndo/agachado/nadando, **shell e filamentos acompanham**? | mover-se |
| **M2** | Com **armadura completa**, aura aparece **e** armadura reconhecível? | vestir set completo |
| **M3** | **Slim** e **overlay de skin** ok? | trocar de skin |
| **Z1** | Com **partícula 0**, Ten se lê? | `/nenvfx particulas 0` |
| **Z2** | Com **bloom off**, Ten se lê? | `/nenvfx bloom off` |
| **E2** | As **três regressões** seguem consertadas? | grama · acoplamento · ripple |

### Os quatro números (bloco C) — não são passa/reprova

| # | O que anotar | Onde |
| --- | --- | --- |
| **C1** | filamentos vivos | `F6` |
| **C2** | partículas vivas | `F6` |
| **C3** | chamadas de desenho | `F6` |
| **C4** | LOD efetivo | `F6` |

São a **linha de base do AV8**, e os dois primeiros pagam a dívida que o AV2
deixou em branco.

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
