# Como escrever uma habilidade

## O criterio que esta API precisa cumprir

> **Acrescentar uma habilidade simples nova nao pode exigir editar o
> AuraEngine, o NenProfile, o registro de rede ou o HUD.**

Registra-se um `AbilitySpec` mais uma implementacao de `NenAbility`, e o resto
do pipeline continua igual.

Se voce esta escrevendo a sua habilidade e precisou tocar num desses quatro,
**pare**. Nao acrescente o `if` com o nome da habilidade dentro do engine —
esse e o caminho que transforma um sistema em uma colecao de excecoes. Abra
issue: falta um componente reutilizavel.

O M5 tem uma revisao arquitetural marcada depois da **sexta** habilidade,
exatamente para colher esses casos.

---

## Duas partes: dado e codigo

| Parte | Onde | Por que |
| --- | --- | --- |
| nome, descricao, icone | lang + dado | traducao e arte nao sao codigo |
| categoria e afinidade | dado | balanceamento |
| custo, cooldown, alcance | dado | alguem vai girar esses numeros |
| requisito de tecnica e de marco | dado | o pack decide a progressao |
| efeito simples (dano, status, projetil) | componente reutilizavel | e o que se repete |
| comportamento unico | classe Java | e o que nao se repete |
| **validacao de alvo, distancia, linha de visao** | **Java, sempre** | invariante de multiplayer |
| **votos e condicoes compostas** | pos-MVP (F3) | engine propria, sem scripting livre |

A ultima linha da tabela de Java e a que nao se negocia. Numero sai de dado;
**regra de seguranca fica em Java**. Um datapack que pudesse desligar a
validacao de alcance seria um datapack que desliga a autoridade do servidor.

---

## O contrato

```java
public interface NenAbility {
    ResourceLocation id();
    AbilitySpec spec();
    ActivationResult validate(ServerPlayer caster, AbilityRequest req, NenContext ctx);
    void activate(ServerPlayer caster, AbilityRequest req, NenContext ctx);
    default void tick(ServerPlayer caster, ActiveAbility inst, NenContext ctx) {}
    default void stop(ServerPlayer caster, ActiveAbility inst, StopReason motivo) {}
}
```

### `spec()` le do registro; nao guarda

```java
// ERRADO — o spec recebido no construtor ignora recarga de datapack
private final AbilitySpec spec;

// CERTO
public AbilitySpec spec() {
    return NenRegistries.specs().get(id());
}
```

Uma recarga de datapack precisa mudar o custo **de quem ja tem a habilidade
equipada**.

### `validate()` nao reimplementa a validacao comum

Quando o seu metodo roda, o pipeline **ja** conferiu: unlock, aura, cooldown,
categoria, alcance, dimensao e rate limit. Conferir de novo cria a segunda
fonte da mesma verdade, e a sua copia vai divergir na primeira mudanca de
regra.

`validate()` responde so pelo que e especifico: *"esta habilidade so funciona
sobre alvo vivo"*, *"so debaixo do ceu"*.

### O alvo vem como id, e o servidor o reconstroi

```java
// NUNCA
Entity alvo = req.entidadeQueOClienteMandou();   // nao existe, e de proposito

// SEMPRE
Entity alvo = caster.serverLevel().getEntity(req.alvoId().getAsInt());
// e entao: existe? mesma dimensao? dentro do alcance do spec? linha de visao?
```

Aceitar a entidade que o cliente apontou e aceitar dano a qualquer coisa em
qualquer lugar. **Com um cliente honesto, isso funciona perfeitamente** — e por
isso nenhum playtest encontra o buraco.

### Instancia viva, e nao estado na classe

A implementacao e um singleton. Estado de execucao mora em `ActiveAbility`.

### `stop()` limpa tudo, e aguenta ser chamado duas vezes

Projetil, construct, particula persistente, modificador, listener. Trabalho
iniciado morre com quem o iniciou.

Projetil orfao e a causa classica de vazamento de tick em servidor — e ele nao
aparece como erro, aparece como TPS caindo devagar ao longo de uma semana.

---

## Passo a passo

1. **Issue primeiro**, com os cinco campos.
2. Classe em `nen/ability/<nome>.java`. **Um arquivo por habilidade** — e o que
   permite as duas pessoas trabalharem em paralelo sem conflito.
3. `AbilitySpec` em dado, com custo, cooldown, alcance e requisitos.
4. Chaves de traducao: nome, descricao, e **cada** motivo de recusa.
5. Se precisou de um componente novo (projetil, area, buff temporario), ele
   nasce **reutilizavel**, em `nen/ability/componente/`, e nao dentro da sua
   classe.
6. Testes: conta unitaria; gametest de ativacao, cooldown e limpeza.
7. Teste de abuso: spoof de alvo, spam de pacote, troca de dimensao com a
   habilidade ativa, logout com construct no mundo.
8. **`runServer` com dois clientes.**
9. Registrar na tabela abaixo.

---

## As seis habilidades de prova do MVP

Elas nao representam personagens. Existem para provar que a arquitetura
aguenta seis arquetipos diferentes — e para revelar, na sexta, o que precisa
virar componente.

| Categoria | Habilidade | O que ela testa | Owner |
| --- | --- | --- | --- |
| Enhancement | Impacto Reforcado | buff curto de melee; custo e cooldown | Dev A |
| Emission | Disparo de Aura | projetil server-authoritative e sync de entidade | Dev A |
| Manipulation | Marca de Controle | condicao em mob, com validacao de alvo | Dev A |
| Transmutation | Aura Cortante | propriedade alterada da aura; hitbox curta | Dev B |
| Conjuration | Lamina Conjurada | item/entidade temporaria vinculada ao caster | Dev B |
| Specialization | Analise de Aura | efeito unico; leitura **limitada** de estado | Dev B |

A ultima merece cuidado: Specialization nao e "100% em tudo". Ela e regra
separada, e "Analise de Aura" so pode revelar o que o servidor decidir que
aquele observador percebe.

**Estado: nenhuma implementada.** Marco M5.
