# AV2 — as perguntas da sessão (#181)

Folha para ser **respondida durante a sessão**, com o jogo aberto. O
[`LEIA-ME.md`](LEIA-ME.md) diz o que cada item é; aqui está o que cada um
**pergunta**, com espaço para o veredicto ao lado.

> **Toda pergunta é escrita para que PASSA signifique aprovado.** Registre
> `PASSA` ou `REPROVA`, nunca sim/não — a folha é lida meses depois por quem não
> estava lá.

**Montagem:** servidor dedicado (nunca singleplayer), **um cliente** basta.
Overlay `F6` **sem** o aviso `OVERRIDE ATIVO`.

> **Esta folha é a evidência inteira do AV2.** A trilha dispensou captura em
> 2026-09-22 (AV0–AV8). Os nomes de arquivo continuam sendo o **roteiro do que
> olhar**; eles só não viram PNG.

---

## O que o AV2 acrescenta, e contra o que ele é julgado

O AV1 aprovou a **shell**. O AV2 põe **filamentos** sobre ela — e é a primeira
vez que a aura tem algo que *se move por conta própria*. A referência B tem
filamentos; **a comparação com ela deixa de ser parcial aqui**, e é este o gate
em que ela finalmente vale inteira.

O perfil do Ten declara oito deles:

```json
"filamentos": { "quantidade": 8, "comprimento_min": 0.15,
                "comprimento_max": 0.60, "largura": 0.009,
                "ciclo_segundos": 1.1 }
```

Tudo isso é dado de resource pack e **F3+T recarrega** — dá para iterar na
própria sessão.

---

## Bloco N — os filamentos NASCEM na superfície? (o nome do gate)

### N1 · `av2_ten_2b` — a dois blocos
**Os filamentos saem da superfície da shell, ou flutuam soltos ao redor?**
REPROVA se parecem partículas orbitando em vez de fios presos ao corpo.

- [ ] PASSA  [ ] REPROVA — `____________________`

### N2 · `av2_ten_5b` · `av2_ten_10b`
**A cinco e dez blocos eles ainda LEEM como filamento, ou viram ruído?**
A largura declarada é 0,009 — fino o bastante para sumir, e é essa a pergunta.

- [ ] PASSA  [ ] REPROVA — `____________________`

---

## Bloco M — eles ACOMPANHAM os membros? (a outra metade do nome)

> É aqui que o gate se decide. Parado, um filamento mal preso passa; em
> movimento, ele fica para trás ou atravessa o corpo.

### M1 · `av2_ten_correndo`
**Correndo, os filamentos seguem os membros — ou ficam para trás do corpo?**
- [ ] PASSA  [ ] REPROVA — `____________________`

### M2 · `av2_ten_agachado`
**Agachar não deixa filamento atravessando o próprio corpo?**
- [ ] PASSA  [ ] REPROVA — `____________________`

### M3 · `av2_ten_nadando`
**Na horizontal eles continuam presos, sem apontar para cima?**
- [ ] PASSA  [ ] REPROVA — `____________________`

### M4 · `av2_ten_atacando`
**No golpe eles acompanham o braço, ou chegam atrasados?**
O ciclo declarado é 1,1 s; um golpe é mais rápido que isso.
- [ ] PASSA  [ ] REPROVA — `____________________`

---

## Bloco C — os dois corpos

### C1 · `av2_ten_slim` · `av2_ten_default`
**Nos dois modelos os filamentos nascem no lugar certo?**
No slim o braço é mais fino: âncora calculada para o default apareceria
flutuando ao lado do braço.

- [ ] PASSA  [ ] REPROVA — `____________________`

---

## Bloco D — as duas que não são sobre estética

### D1 · `av2_frame_congelado_1` vs `_2` — **determinismo**

```
/nenvfx freeze
```

Olhe a aura, espere alguns segundos, olhe de novo. **Tem de estar idêntica** —
nada se movendo, nem fluxo, nem filamento.

- [ ] PASSA  [ ] REPROVA — `____________________`

> **Esta verificação teria reprovado sem haver defeito na curva.** Antes de
> 2026-09-22 o `/nenvfx freeze` parava a interpolação de estado mas **não o
> relógio de animação** — o fluxo do shader e o ciclo dos filamentos continuavam
> correndo. Corrigido, com `RelogioCongeladoTest` travando a conta.
>
> **Se ainda divergir, aí sim o achado é real**, e é o que o `LEIA-ME` descreve:
> *"a curva do filamento está lendo algo que muda entre quadros, e o defeito
> aparece em jogo como cintilação que ninguém consegue reproduzir"*. Nesse caso
> a régua nova não cobre a causa — ela prova que o **número** ficou parado, não
> que a **tela** ficou.

### D2 · `av2_overlay_contadores` — o número que o AV8 vai comparar

No `F6`, anote **quantos filamentos** estão vivos com um jogador em Ten:

- filamentos vivos: `____________`  (o perfil declara `quantidade: 8`)
- chamadas de desenho: `____________`

> Não é pergunta de passa/reprova: é a **linha de base**. O AV8 mede orçamento,
> e sem este número ele não tem contra o que comparar.

---

## Bloco Z — o critério do AV1, repetido com mais geometria

### Z1 · `av2_sem_particulas_ten`

```
/nenvfx particulas 0
```

**Com partícula em zero, a aura *com filamentos* ainda se lê?**

- [ ] PASSA  [ ] REPROVA — `____________________`

> O AV1 já passou nisto **sem** os filamentos. A pergunta aqui é outra: os
> filamentos **ajudam** a leitura, ou competem com a shell? Se a resposta for
> "ficou mais confuso", o ADR-015 continua valendo — a resposta é voltar para a
> geometria, não subir partícula nem brilho.
>
> **Ao terminar:** `/nenvfx particulas auto` e `/nenvfx freeze` para descongelar.

---

## Bloco E — o que não é imagem

### E1 · log do servidor DEDICADO
**Há `NoClassDefFoundError` ou `ClassNotFoundException` de `net.minecraft.client.*`?**

> **Use a INSTÂNCIA, não o `runServer`** — no workspace de dev as classes de
> cliente estão no classpath e o log sai limpo pelo motivo errado.

- [ ] log limpo  [ ] achou ocorrência — `____________________`

### E2 · regressões dos gates anteriores
Três consertos recentes tocaram o mesmo caminho de desenho. Uma olhada rápida:

- [ ] a grama ainda oclui o halo? `____________`
- [ ] ligar Ren ainda **não** clareia a aura dos outros? `____________`
- [ ] o ripple ainda acende ao levar pancada? `____________`

---

## Ao fechar

- [ ] esta folha preenchida, **na mesma sessão**;
- [ ] `o-que-nao-provamos.md` com o que a sessão **não** provou;
- [ ] `../../../processo/marcos.md` atualizado;
- [ ] `compatibility.md`, se a sessão tocar renderer ou shader pack.

> **O AV2 é a linha de base do AV3**, que aprova o Ten por inteiro. E como a
> trilha não arquiva imagem, o que sobrevive desta sessão é o que estiver
> escrito aqui — inclusive os dois números do D2.
