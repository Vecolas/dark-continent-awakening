# Greed Island em bloco: a evidência

Verificação de **2026-09-26**, num servidor dedicado (`runServer`), por RCON.
Mundo de Greed Island **apagado e regenerado** antes da corrida, como a §128
manda.

> **Isto não é um portão.** É a ferramenta do gate MICRO, e ela responde uma
> pergunta só: *cada obra da ilha nasce em bloco?* Ela não diz se está bonito.

A ferramenta é [`scripts/greedisland/verificar-ilha.ps1`](../../scripts/greedisland/verificar-ilha.ps1),
alimentada por `build/alvos.csv` — as coordenadas e o bloco esperado de cada
obra, calculados do próprio layout.

---

## O resultado: 51 de 51

| Grupo | Conferidas | OK |
| --- | --- | --- |
| Cidades | 8 | **8** |
| Landmarks | 38 | **38** |
| Estradas | 9 | **9** |

Tick médio ao fim: **0,2 ms** (P99: 0,6 ms). O alvo é 50 ms.

### As oito cidades

Praça central em `polished_andesite`, na cota que o cálculo previu:

```
shiso_tree  (-18000, 101, 1000)     rubicuta  ( -5000, 104,  8500)
antokiba    (-14500,  77, 5500)     masadora  (  4500,  98, -2000)
aiai        ( 15500,  83, 5500)     dorias    (  8500, 104, 17000)
soufrabi    ( 27000,  69, 22000)    limeiro   ( -1000,  77, 27000)
```

**Soufrabi assenta a y=69**, quase no nível do mar — é a cidade portuária, e o
aterro a acomodou na costa. **Limeiro**, a 1.200×1.500, é quatro vezes a
segunda maior e nasceu igual.

Em Shiso Tree, a coluna inteira foi varrida:

```
y=96..99   stone              terreno natural
y=100      dirt               aterro
y=101      polished_andesite  a praça
y=102+     air
```

Também confirmados ali: avenida em `stone_bricks`, rua em `cobblestone`,
prédio de `oak_planks` num lote, e 26 blocos de tronco da árvore Shiso.

### Os 34 landmarks

Todos nascem. Alguns exemplos, com o bloco e a altura reais:

| Landmark | Posição | Bloco |
| --- | --- | --- |
| árvore Shiso | (-18000, 102, 1000) | `dark_oak_log` |
| farol de Soufrabi | (28600, 70, 23400) | `white_concrete` |
| torre dos Game Masters | (-1994, 104, -12000) | `polished_blackstone` |
| rochedo norte | (-12000, 172, -30000) | `andesite` |
| ninho do Cyclops | (11000, 95, -22000) | `chiseled_stone_bricks` |
| cachoeira 2 | (-29000, **312**, -4000) | `chiseled_stone_bricks` |

### As nove estradas

`dirt_path` no meio de cada rota, fora de cidade:

```
shiso→antokiba   (-15616,  83,  3328)    masadora→dorias   ( 9984, 151,  7936)
antokiba→rubicuta ( -9472,  99,  6912)   dorias→soufrabi   (17664, 162, 19712)
rubicuta→masadora ( -3328, 145,   256)   dorias→limeiro    ( 3328, 102, 22272)
masadora→aiai     ( 10496, 103,  4352)   rubicuta→limeiro  ( 1280, 103, 16128)
aiai→soufrabi     ( 20736, 104, 14592)
```

As alturas contam a história que o A\* prometeu: `rubicuta→masadora` sobe a
145 e `dorias→soufrabi` a 162 — elas **atravessam relevo**, e não o ignoram.

---

## Os dois defeitos que esta corrida achou

### 1. A régua não alcançava o alvo

A primeira passada reprovou `passo_4` e as três cachoeiras. **Elas existiam** —
em y=230, 253, 305 e 312. Minha faixa de busca parava em 200.

Passo de montanha e queda d'água ficam **alto por definição**, e uma régua que
não alcança o alvo acusa ausência onde há presença. A faixa foi para 40–320.

### 2. Uma cachoeira a 312, com o teto em 320

`cachoeira_2` nasceu a **8 blocos do teto do mundo**. O campo macro de elevação
limita picos a 310, mas o ruído local do `noise_settings` soma por cima — e ali
somou até quase encostar.

Não é erro hoje: o bloco coube. Mas a margem é de oito blocos, e qualquer
aumento de amplitude do ruído local passa a cortar montanha no teto. **Está
declarado em [`o-que-nao-provamos.md`](../testing/o-que-nao-provamos.md).**

---

## O que esta corrida NÃO prova

- **Ninguém olhou.** Não há captura de tela. Nada aqui diz se a cidade é
  bonita, se o aterro deixa paredão numa encosta, ou se a estrada lê como
  estrada.
- **Um ponto por obra.** Foi conferido o *centro* de cada cidade e *um* ponto
  de cada estrada. Uma cidade pode estar certa no meio e quebrada na borda.
- **Nada foi jogado.** Ninguém andou de Shiso Tree até Antokiba para ver se a
  estrada é seguível, nem tentou atravessar um passo.

## Como repetir

```powershell
# terminal 1
.\gradlew runServer

# terminal 2, depois de "Done (...)"
.\scripts\greedisland\verificar-ilha.ps1
```

O `alvos.csv` é regerado de dentro do layout — se uma âncora mudar, a
verificação passa a cobrar o lugar novo.
