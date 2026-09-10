# Como escrever uma tecnica

Uma tecnica e Ten, Ren, Zetsu, Gyo — e depois Ken, Ko, Ryu, Shu, En, In.

Todas usam **o mesmo ciclo de vida**: `NenTechnique`. Isso nao e organizacao,
e o que torna as seis proximas baratas. Se voce se pegar querendo um ciclo de
vida diferente, pare e escreva por que na issue: ou o contrato precisa mudar
para todos, ou o que voce quer nao e uma tecnica.

---

## O contrato

```java
public interface NenTechnique {
    ResourceLocation id();
    Set<ResourceLocation> incompativeisCom();
    TechniqueActivationResult canActivate(ServerPlayer j, NenContext ctx);
    void onActivate(ServerPlayer j, NenContext ctx);
    void serverTick(ServerPlayer j, NenContext ctx);
    void onDeactivate(ServerPlayer j, NenContext ctx, StopReason motivo);
}
```

Nao ha `clientTick`. A tecnica roda inteira no servidor. O cliente recebe o
resultado por payload e desenha ([ADR-001](../adr/ADR-001-servidor-autoritativo.md)).

---

## As cinco regras

### 1. Nao guarde valor calculado

```java
// ERRADO — congela o custo no instante da ativacao
private double custo;
public void onActivate(ServerPlayer j, NenContext ctx) {
    this.custo = ctx.perfil().output() * 0.5;   // e ignora todo buff futuro
}

// CERTO — pergunta na hora de gastar
public void serverTick(ServerPlayer j, NenContext ctx) {
    if (!ctx.gastarAura(custoPorTick(ctx))) {
        // aura acabou; o motor encerra com OUT_OF_AURA
    }
}
```

Um multiplicador guardado e uma copia que vai divergir da fonte — e a
divergencia **nao da erro**. Ela aparece como "esse buff nao esta fazendo
nada".

### 2. A implementacao e um singleton; ela nao guarda estado de jogador

A classe da tecnica e registrada uma vez e usada por todos os jogadores. Um
campo de instancia com o estado de alguem e estado global disfarcado: dois
jogadores usando Ren escrevem no mesmo campo.

Estado por jogador mora no `RuntimeNenState`.

### 3. `onActivate` e idempotente

O jogador segura a tecla. O pedido chega varias vezes.

Reentrar numa tecnica ja ativa **nao** pode reiniciar duracao, cobrar custo de
novo nem empilhar modificador.

### 4. Quem liga, desliga — e o par mora em `onDeactivate`

Todo modificador, listener, entidade e timer criado em `onActivate` some em
`onDeactivate`. **Nesse metodo, e em nenhum outro lugar.**

Espalhar a limpeza pelos varios pontos de onde se pode sair — morte, logout,
troca de dimensao, aura zerada — e o desenho que ja perdeu uma chamada. E o
sintoma e silencioso: o buff fica ligado para sempre.

`onDeactivate` precisa aguentar ser chamado para uma tecnica que ja parou.

### 5. Incompatibilidade e DADO, declarada dos dois lados

```java
// Em Zetsu
public Set<ResourceLocation> incompativeisCom() {
    return Set.of(NenFoundation.id("ten"), NenFoundation.id("ren"));
}

// Em Ten — a MESMA relacao, do outro lado
public Set<ResourceLocation> incompativeisCom() {
    return Set.of(NenFoundation.id("zetsu"));
}
```

A maquina de estados **verifica a simetria na inicializacao** e reprova se so
um lado declarar. Uma exclusao declarada pela metade e uma combinacao ilegal
que funciona.

Nunca escreva `if (tecnicaAtiva(ZETSU)) return;` dentro do corpo de Ten. Regra
de exclusao espalhada por condicionais diverge: Ten lembra de Zetsu, Zetsu
esquece de Ren, e ninguem percebe.

---

## Passo a passo

1. **Issue primeiro**, com os cinco campos. Ver
   [board-e-issues.md](../processo/board-e-issues.md).
2. Classe em `nen/technique/<nome>/`.
3. Id no namespace `nenfoundation:`, minusculo, sem espaco. Ele vai para NBT —
   escolha com cuidado, porque renomear depois exige migracao.
4. Declarar `incompativeisCom()` **nos dois lados**.
5. `canActivate` recusando **com motivo** em cada caso.
6. Custo lido de config, nunca escrito no codigo. E a chave de config nasce
   com esta tecnica, nao antes.
7. Chave de traducao para o nome, a descricao e cada motivo de recusa.
8. Teste unitario da conta; gametest do ciclo de vida.
9. **Smoke test em `runServer`.** Nao em singleplayer.
10. Documentar na tabela abaixo.

---

## Recusa sempre tem motivo

```java
return TechniqueActivationResult.negado("nenfoundation.recusa.aura_insuficiente");
```

Recusa silenciosa produz o pior relato de bug que existe: *"aperto a tecla e
nao acontece nada"* — e nao ha log que diga qual das oito validacoes reprovou.

E o motivo **nunca revela estado alheio**. "Alvo protegido por Ten" conta ao
atacante o que ele nao deveria saber.

---

## Tecnicas registradas

| Id | Marco | Owner | Estado |
| --- | --- | --- | --- |
| `nenfoundation:ten` | M4 | Dev A | nao implementada |
| `nenfoundation:ren` | M4 | Dev A | nao implementada |
| `nenfoundation:zetsu` | M4 | Dev B | nao implementada |
| `nenfoundation:gyo` | M4 | Dev B | nao implementada |
| Ken, Ko, Ryu | pos-MVP (F1) | — | — |
| Shu, En, In | pos-MVP (F2) | — | — |

---

## Um cuidado especifico de Gyo

Gyo e percepcao. A tentacao e mandar tudo para o cliente e deixar ele filtrar.

**Nao.** O servidor calcula o que aquele observador pode perceber e manda **so
isso**. Filtrar no cliente entrega a informacao inteira a qualquer cliente
modificado — e a mecanica de In, que chega depois, deixa de existir antes de
nascer.
