# AV2 — as perguntas da sessão (#181)

Folha para ser **respondida durante a sessão**, com o jogo aberto. O
[`LEIA-ME.md`](LEIA-ME.md) diz o que cada item é; aqui está o que cada um
**pergunta**, com espaço para o veredicto ao lado.

---

## Resultado da sessão de 2026-09-22 (`280696b`)

## O #181 ESTÁ FECHADO.
**As doze perguntas de julgamento passaram.**

| Bloco | Resultado |
| --- | --- |
| **N** — nascem na superfície (2b · 5b · 10b) | ✅ PASSA |
| **M** — acompanham os membros (correr, agachar, nadar, atacar) | ✅ PASSA — **é aqui que o gate se decide** |
| **C** — slim e default | ✅ PASSA |
| **D1** — dois quadros congelados idênticos | ✅ PASSA |
| **Z1** — com `particulas 0`, a aura com filamentos se lê | ✅ PASSA |
| **E1** — log do dedicado | ✅ limpo, varrido por mim |
| **E2** — regressões dos gates anteriores | ✅ grama oclui · Ren não acopla · ripple acende |
| **D2** — os dois contadores | ⬜ **não anotados** |

### O que esta sessão NÃO registra

**Os dois números do D2.** Eles não são passa/reprova — são a linha de base que
o AV8 vai comparar, e um número não escrito não existe depois. É a única dívida
do AV2, e custa `F6` num jogo aberto.

**A janela.** O log mostra `Done` às 17:53:25 e `stop` às 17:55:56 — **2 min
31 s**, com um jogador. Fica escrito porque esta folha é a evidência inteira do
gate: quem a ler em dezembro merece saber o tamanho da janela tanto quanto o
veredicto.

### O que a preparação encontrou, antes de qualquer olhar

O **D1 teria reprovado sem haver defeito na curva**. `/nenvfx freeze` parava a
interpolação de estado mas não o relógio de animação — fluxo e ciclo dos
filamentos continuavam correndo. Achado lendo o gate, corrigido, e travado por
`RelogioCongeladoTest`.

---

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

- [x] **PASSA** — `280696b`, 2026-09-22

### N2 · `av2_ten_5b` · `av2_ten_10b`
**A cinco e dez blocos eles ainda LEEM como filamento, ou viram ruído?**
A largura declarada é 0,009 — fino o bastante para sumir, e é essa a pergunta.

- [x] **PASSA** — `280696b`, 2026-09-22

---

## Bloco M — eles ACOMPANHAM os membros? (a outra metade do nome)

> É aqui que o gate se decide. Parado, um filamento mal preso passa; em
> movimento, ele fica para trás ou atravessa o corpo.

### M1 · `av2_ten_correndo`
**Correndo, os filamentos seguem os membros — ou ficam para trás do corpo?**
- [x] **PASSA** — `280696b`, 2026-09-22

### M2 · `av2_ten_agachado`
**Agachar não deixa filamento atravessando o próprio corpo?**
- [x] **PASSA** — `280696b`, 2026-09-22

### M3 · `av2_ten_nadando`
**Na horizontal eles continuam presos, sem apontar para cima?**
- [x] **PASSA** — `280696b`, 2026-09-22

### M4 · `av2_ten_atacando`
**No golpe eles acompanham o braço, ou chegam atrasados?**
O ciclo declarado é 1,1 s; um golpe é mais rápido que isso.
- [x] **PASSA** — `280696b`, 2026-09-22

---

## Bloco C — os dois corpos

### C1 · `av2_ten_slim` · `av2_ten_default`
**Nos dois modelos os filamentos nascem no lugar certo?**
No slim o braço é mais fino: âncora calculada para o default apareceria
flutuando ao lado do braço.

- [x] **PASSA** — `280696b`, 2026-09-22

---

## Bloco D — as duas que não são sobre estética

### D1 · `av2_frame_congelado_1` vs `_2` — **determinismo**

```
/nenvfx freeze
```

Olhe a aura, espere alguns segundos, olhe de novo. **Tem de estar idêntica** —
nada se movendo, nem fluxo, nem filamento.

- [x] **PASSA** — `280696b`, 2026-09-22

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

- [ ] filamentos vivos: **não anotado**  (o perfil declara `quantidade: 8`)
- [ ] chamadas de desenho: **não anotado**

> **Estes dois números ficaram sem registro, e é a única coisa que o AV2 deve.**
> Eles não são passa/reprova — são a linha de base que o AV8 vai comparar. Como
> a trilha não arquiva imagem, um número não escrito simplesmente não existe
> depois. **O custo é baixo de pagar:** abrir o jogo, ligar Ten, `F6`, ler duas
> linhas. Não exige sessão nem montagem de dois clientes.

---

## Bloco Z — o critério do AV1, repetido com mais geometria

### Z1 · `av2_sem_particulas_ten`

```
/nenvfx particulas 0
```

**Com partícula em zero, a aura *com filamentos* ainda se lê?**

- [x] **PASSA** — `280696b`, 2026-09-22

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

- [x] **log limpo** — `280696b`, 2026-09-22, varrido por mim na instância: zero
  `NoClassDefFoundError`, zero `ClassNotFoundException`, zero menção a `client`,
  zero `[ERROR]`/`Exception` nos dois logs. `stop` limpo, todas as dimensões
  salvas.

### E2 · regressões dos gates anteriores
Três consertos recentes tocaram o mesmo caminho de desenho. Uma olhada rápida:

- [x] a grama ainda oclui o halo — **sim**
- [x] ligar Ren **não** clareia a aura dos outros — **confirmado**
- [x] o ripple ainda acende ao levar pancada — **sim**

---

## Ao fechar

- [x] esta folha preenchida, **na mesma sessão**;
- [x] `o-que-nao-provamos.md` com o que a sessão **não** provou;
- [x] `../../../processo/marcos.md` atualizado;
- [x] `compatibility.md` — **não se aplica**: a sessão não tocou renderer nem
      shader pack.

> **O AV2 é a linha de base do AV3**, que aprova o Ten por inteiro. E como a
> trilha não arquiva imagem, o que sobrevive desta sessão é o que estiver
> escrito aqui — inclusive os dois números do D2.
