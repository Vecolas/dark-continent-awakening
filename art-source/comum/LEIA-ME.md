# `art-source/comum/` — a biblioteca dos geradores de arte

Codigo em portugues **sem acento**, como o resto de `art-source/`. Este
documento segue a mesma regra de proposito: ele fica ao lado do codigo, nao em
`docs/`.

---

## Por que ela existe

Os sete primeiros mobs foram escritos com o pincel, o ruido, o layout de caixa e
os portoes **copiados** de arquivo em arquivo: 21 geradores, 18.814 linhas. Isso
foi deliberado enquanto eram poucos — quatro usos com paletas diferentes nao
pagavam a indirecao, e o comentario que dizia isso estava escrito no proprio
`spider_eagle_textura.py`, com o gatilho de troca declarado:

> *"Se um quinto mob precisar do mesmo pincel, ele vira modulo."*

Faltam **dezessete** mobs. Mas o que decide a troca nao e o tamanho: e que a
duplicacao **ja comecou a divergir**, e cada divergencia e silenciosa.

| Regua | Quem tem hoje | O que o resto nao sabe que tem |
| --- | --- | --- |
| `valida_sem_buraco` | 3 de 7 | face pintada 5 de 6 — buraco no bicho, visto de um angulo so |
| `valida_pivots` | 1 de 7 | membro girando em torno de um ponto que nao existe |
| `valida_faces_coplanares` | 1 de 7 | z-fighting, que o jogador le como bug de driver |
| geo obrigatorio na lane de animacao | 6 de 7 | geo ausente virando verde |
| `faixa_no_topo` vs `faixa_no_topo_da_face` | 5 contra 2 | nada — e so o sintoma |

**Nenhuma dessas divergencias da erro.** Cada uma e uma regua que um mob tem e o
vizinho nao, e o mob sem a regua sai verde exatamente porque ninguem o mede.

> **Aviso honesto:** quando um mob antigo for migrado para ca, as reguas que ele
> nao tinha passam a rodar, e e **esperado** que alguma folha reprove na primeira
> execucao. Isso e a regua encontrando o que ja estava la — nao e regressao.

---

## O que sobe para ca e o que fica no mob

**Sobe o que e do FORMATO.** Layout de caixa do Bedrock, serializacao, conta do
atlas, hierarquia de ossos, ruido deterministico, esqueleto dos portoes.

**Fica no arquivo do mob tudo que e do BICHO.** As tabelas `OSSOS` e `CAIXAS`
(com o comentario que justifica cada peca), a paleta, o script de pintura, os
limiares nomeados e as validacoes semanticas que os cobram:

```python
TESTA_ALTURA_MINIMA = 0.62   # HunterExamProfiles: WeakPointResolver("forehead", ..., 0.62D, 0.5D)
```

Essas validacoes — `valida_testa`, `valida_boca`, `valida_envergadura` — **sao o
valor do projeto**. A biblioteca existe para elas caberem no arquivo do mob em
vez de disputarem espaco com seiscentas linhas de encanamento.

**A biblioteca nao pode ter numero de design.** `PX_POR_BLOCO` e
`TICKS_POR_SEGUNDO` sao do Minecraft; largura de hitbox, tick de windup e fracao
minima de boca sao do bicho, e ficam onde o comentario que os explica esta.

**A mensagem de erro pertence a biblioteca; o motivo, ao mob.** Por isso
`valida_hitbox` aceita um `rotulo`: a biblioteca sabe o que mediu e qual era o
limite, mas so o mob sabe que aquele limite se chama *a coleira do ninho*.

---

## Como importar

Nao ha pacote instalavel e nao se cria `__init__.py` em `art-source/`. O gerador
de um mob, que mora em `art-source/enemies/<mob>/<mob>_geo.py`, poe `art-source`
no path e importa `comum`:

```python
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402
from comum import animacao as anim      # noqa: E402
from comum import textura as tex        # noqa: E402
```

Todos os geradores **rodam a partir da raiz do repositorio** — os caminhos de
saida sao montados com `raiz="."`:

```bash
python art-source/enemies/<mob>/<mob>_geo.py
python art-source/enemies/<mob>/<mob>_animacoes.py   # depois do geo: ele LE o geo
python art-source/enemies/<mob>/<mob>_textura.py
```

A ordem nao e sugestao. O gerador de animacao **le o `.geo.json` do disco** e
recusa rodar sem ele.

---

## `comum.geometria` — a geometria

```python
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("body", "root", (0, 9, 0)),
    geo.Osso("head", "body", (0, 10, -4)),
)

# Caixa(nome, osso, u, v, x, y, z, w, h, d)
CAIXAS = (
    geo.Caixa("corpo",  "body", 0,  0, -3, 6, -4, 6, 6, 8),
    geo.Caixa("cabeca", "head", 44, 0, -2, 9, -7, 4, 4, 3),
)

def valida_testa(m):
    """A validacao SEMANTICA vive aqui, no mob, com o numero que a explica."""
    testa = m.caixa("testa")
    if testa.y < TESTA_ALTURA_MINIMA * m.hitbox_altura_px:
        raise geo.ErroDeArte("...o que foi medido, o limite, e o que acontece em jogo")

modelo = geo.Modelo("great_stamp", OSSOS, CAIXAS,
                    uv=(128, 64),
                    hitbox_blocos=(1.9, 1.55))   # o literal de .sized(1.9F, 1.55F)

if __name__ == "__main__":
    modelo.emitir(extras=(valida_testa,))
```

`emitir()` **valida, escreve e relata, nesta ordem**. Validar antes de produzir
texto e o que impede meio arquivo no disco: um `.geo.json` truncado carrega
parcialmente e o sintoma e um mob sem pernas.

O `identifier` do Bedrock e **derivado** do nome do mob (`"geometry." + mob`),
porque e exatamente isso que o portao Java `identifierBateComOId` exige. Declarar
os dois criaria duas fontes para o mesmo nome, e a divergencia faz o modelo nao
ser achado — o mob some da tela sem uma linha de log.

### Portoes que rodam sempre

| Portao | O defeito que ele pega, e que nao da erro |
| --- | --- |
| `valida_ossos` | pai inexistente, duas raizes, ciclo na hierarquia, caixa pendurada no nada, osso sem volume |
| `valida_pivots` | pivot de filho fora do volume do pai |
| `valida_uv` | sobreposicao, estouro do atlas, nome repetido, tamanho fracionario |
| `valida_faces_coplanares` | duas caixas com a mesma face no mesmo plano (z-fighting) |
| `valida_hitbox` | piso fora de `y=0`; modelo mais alto, mais largo ou mais comprido que a caixa de colisao |

Fora da bateria, a pedido: `valida_encadeamento(ossos, pares, motivo)` — para
quando *qual* e o pai importa (a ponta da asa tem de pendurar no braco, e nao no
corpo; pendurada no corpo o pai existe, o portao Java passa, e a asa abre pela
metade).

**Tamanho fracionario e recusado; origem fracionaria, nao.** O portao Java
arredonda o `size` para medir o atlas; um `size` fracionario faria as duas contas
medirem retangulos diferentes, e elas so discordariam no dia em que uma delas
achasse sobreposicao.

---

## `comum.animacao` — os clipes

```python
geometria = anim.carregar_geo("great_stamp")     # obrigatorio: le do disco

LOOPS = {          # esta e a UNICA lista de clipes, e o loop mora AQUI
    "idle":   True,
    "windup": "hold_on_last_frame",
    "charge": False,
}

# Ticks COPIADOS do servidor, com o metodo de origem ao lado.
ATAQUES = {"charge": anim.Ataque(windup=18,     # greatStampCharge().windupTicks
                                 active=20,     # greatStampCharge().activeTicks
                                 recovery=12)}  # greatStampCharge().recoveryTicks

clipes = anim.Animacoes("great_stamp", geometria, LOOPS)

bones = {}
anim.curva(bones, "body", "rotation",
           [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(4.0, 4.0, 2.0)])
anim.derivar(bones, "leg_front_left", "hoof_front_left", "rotation", -0.4)
clipes.clipe("idle", 4.0, bones)
...
clipes.emitir(ataques=ATAQUES)
```

### O `loop` mora no arquivo, nunca no codigo

No GeckoLib 4.8.3 o `RawAnimation` carrega o `LoopType` e **vence** o JSON.
`.thenLoop(...)` no Java transforma o dicionario `LOOPS` em documentacao — e
documentacao que discorda do comportamento. O portao Java
`oLoopMoraNoArquivoDeAnimacao` reprova os atalhos do lado de la; aqui, `LOOPS` e
a unica declaracao **e tambem** a lista de clipes, para que ninguem acrescente um
clipe numa lista e esqueca da outra.

### Portoes

| Portao | O defeito |
| --- | --- |
| `valida_clipes` | chave fora do contrato, loop invalido, canal invalido, quadro fora de `[0, duracao]`, escala `<= 0` (que **some** com o osso) |
| `valida_ossos` | osso citado que a geometria nao tem; raiz animada |
| `valida_ossos_com_volume` | osso animado cuja subarvore inteira nao tem cubo |
| `valida_loop_fecha` | clipe que repete e nao termina onde comecou |
| `valida_duracao_de_ataque` | clipe mais curto que o orcamento de ticks do servidor |

**Sobre a duracao de ataque.** O servidor gasta `windup + active + recovery`
ticks no golpe, e durante todos eles o jogador esta decidindo se recua. Se o
clipe acabar antes, o bicho **relaxa no meio do golpe que ainda vai acertar** —
dano certo, cooldown certo, log limpo, e a unica coisa que o jogador tem para ler
quebrada. Clipe mais **longo** que o orcamento e permitido: o servidor manda no
fim e o Java corta o clipe.

Um ataque repartido em varios clipes encadeados se declara com uma tupla, e o
orcamento e conferido contra a **soma**:

```python
ATAQUES = {("dive_windup", "dive", "recover"): anim.Ataque(14, 6, 18)}
```

### O conjunto de ossos sai do geo, e so de la

`Animacoes` recebe o dicionario que `carregar_geo` leu. Uma segunda lista de
nomes seria uma segunda fonte para a mesma verdade, e a divergencia apareceria
como um membro que nao se mexe — sem erro, sem log. Pela mesma razao o arquivo e
**obrigatorio**: um dos sete geradores antigos lia o geo `if os.path.exists(...)`,
e um geo ausente virava verde.

Para ler medidas do proprio geo em vez de chutar: `ossos_do_geo`, `cubos_de`,
`pivot_de`, `centro_de`, `faixa_de`, `filhos_de`, `subarvore_tem_volume`.

---

## `comum.textura` — a folha

```python
from great_stamp_geo import CAIXAS, UV_LARGURA, UV_ALTURA   # fonte unica

PALETA = tex.Paleta(
    COURO=(86, 64, 50),     # o flanco
    DORSO=(52, 39, 32),     # o que se ve de cima
    VENTRE=(128, 104, 84),  # o que se ve de baixo
)

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)

p = folha["corpo"]
p.tudo(PALETA.COURO)
p.face("topo", PALETA.DORSO)
p.faixa_no_pe("frente", 3, PALETA.VENTRE)
p.salpicar("topo", PALETA.DORSO, 70, semente=3)
p.ponto("direita", 2, 1, PALETA.VENTRE)

folha.emitir("great_stamp")
```

### A tabela de caixas e unica, e agora por construcao

`Folha` **recebe** a mesma tupla `CAIXAS` que o modulo de geometria validou e
emitiu. Duas tabelas divergem na primeira correcao de modelo, e a divergencia nao
da erro — da face pintada no lugar errado, visivel so na tela e so de um angulo.

### Duas coordenadas, e confundi-las e o erro que nao da erro

- `retangulo(x, y, w, h, ...)` fala em coordenada da **folha** e so garante o
  retangulo da caixa;
- `na_face(face, dx, dy, w, h, ...)` fala em **(coluna, linha) dentro da face**.

Um olho pintado 2 px fora cai na face vizinha, e o portao de sobreposicao **nem
pisca**, porque continua dentro da mesma caixa. Tudo que desenha detalhe usa
`na_face` e os derivados dele: `face`, `tudo`, `linha`, `linha_central`,
`coluna`, `faixa_no_topo`, `faixa_no_pe`, `ponto`, `salpicar`.

### Nomes de face, em portugues

`topo`, `base`, `direita`, `frente`, `esquerda`, `tras` — vocabulario do gerador,
nao do formato. Na face `direita` a **frente** do bicho fica na borda direita; na
`esquerda`, na borda esquerda. Copiar a mesma coluna nos dois lados nao da erro:
da um bicho visivelmente vesgo de um lado so, e ninguem descobre isso sem girar a
camera em volta dele.

### Ruido deterministico

`ruido(x, y, forca)` e `sorteio(x, y, semente)` sao hashes da propria coordenada.
Cor chapada le como plastico a distancia, entao a folha precisa de variacao — mas
a saida e um **binario versionado**, e `random` sem semente faria a PNG mudar a
cada execucao, todo diff virar ruido, e no dia em que a textura mudasse de
verdade ninguem repararia. `forca=0` desliga o ruido: e o que se usa em pupila,
narina e fenda de casco, onde o pixel precisa ser exato.

O hash e embaralhado de proposito. Combinacao linear simples (`x*7 + y*13`)
produz faixas diagonais regulares, que a distancia leem como listra e nao como
pelo, pena ou rocha — e isso so aparece na tela.

### Portoes

- `Folha.valida_sem_buraco()` — nenhum pixel transparente dentro do retangulo de
  uma caixa. Face esquecida nao da erro e nao aparece no atlas aberto num editor;
  aparece como um buraco por onde se ve o interior do bicho. **E a falha mais
  barata de cometer aqui: basta pintar cinco faces de seis.** Roda sempre, dentro
  de `emitir()`.
- `valida_paletas_disjuntas({...})` — a pedido, para mob com duas folhas.

---

## Determinismo

Requisito, nao estilo. Nao ha `random`, nao ha `time`, nao ha iteracao sobre
`set` na hora de escrever. A ordem das faces e fixa; os clipes saem na ordem de
`LOOPS` e nao na ordem em que foram construidos; os keyframes saem ordenados por
tempo. Todo texto e escrito UTF-8 com `\n` (`newline="\n"` e obrigatorio: no
Windows o padrao vira CRLF, e um arquivo que nasce LF numa maquina e CRLF na
outra aparece no diff como o arquivo inteiro reescrito).

**Rodar duas vezes produz bytes identicos** — provado pelo autoteste.

---

## Recusa

Tudo levanta `comum.ErroDeArte`, que **e uma `ValueError`**.

Os geradores antigos divergiam: geo e textura levantavam `ValueError`, animacao
levantava `SystemExit`. A diferenca importa — `SystemExit` nao e pego por
`except Exception`, entao um portao que envolvesse os geradores num laco
engoliria metade das recusas. Um tipo so, herdando de `ValueError` para que quem
ja escreveu `except ValueError` continue funcionando.

Toda mensagem diz **o que foi medido, qual era o limite, e o que acontece em
jogo**. Mensagem que so acusa nao ensina:

```
o modelo tem 24 px de altura e a hitbox tem 16.0 px: a cabeca passa por cima
de quem mira na caixa de colisao, e o tiro que parece acertar erra
```

---

## A prova

```bash
python art-source/comum/autoteste.py
```

Monta um bicho minimo (6 ossos, 6 caixas, 3 clipes, atlas 64x64) num diretorio
temporario e confere geo, animacao, PNG, determinismo e **dez casos que devem
levantar `ValueError`** — inclusive os quatro obrigatorios: osso orfao, caixa
fora do atlas, caixas sobrepostas e modelo maior que a hitbox.

Cada caso confere **o trecho da mensagem**, e nao so que algo foi levantado: um
caso montado para furar a regua do atlas pode acabar reprovando na regua da
hitbox, e ai o autoteste fica verde com a regua do atlas nunca tendo sido medida.
E o proprio verificador e medido — `a_regua_que_mede_as_reguas` o alimenta com um
caso que nao levanta nada e um que levanta pela razao errada, e exige que ele
acuse os dois. Regua que nunca reprova e carimbo.

---

## O que esta biblioteca nao prova

Nada aqui prova que **o bicho parece o bicho**. Silhueta legivel, um unico
contraste alto por folha, pretos contados, leitura a distancia, o degrau entre
asa dobrada e asa aberta — tudo isso segue humano, e e por isso que
`Modelo.resumo()` imprime medidas: elas existem para **serem olhadas**.

Ver `docs/testing/o-que-nao-provamos.md`.
