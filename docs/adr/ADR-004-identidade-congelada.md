# ADR-004 — mod_id, package e ids de categoria congelados no M0

- **Status:** aceita
- **Data:** 2026-09-10
- **Marco:** M0

## Contexto

O plano tecnico, secao 22, lista o que precisa estar congelado antes de o
trabalho paralelo comecar. A razao e dupla.

A primeira e de colaboracao: duas pessoas trabalhando contra um contrato que
ainda muda inventam nomes diferentes para a mesma coisa, e a integracao vira
renomeacao.

A segunda e mais cara. Estes identificadores vao para NBT de save, ids de
datapack, nomes de asset, chaves de traducao e condicoes de quest. Renomear
qualquer um deles depois do primeiro mundo criado **nao produz crash** — produz
um jogador que perde a categoria, ou uma quest que nunca completa.

## Decisao

Congelados a partir deste commit:

| O que | Valor |
| --- | --- |
| `mod_id` | `nenfoundation` |
| package base | `com.darkcontinent.nenfoundation` |
| namespace de recurso | `nenfoundation:` |
| categorias | `undetermined`, `enhancement`, `transmutation`, `emission`, `conjuration`, `manipulation`, `specialization` |
| schema do perfil | `PersistentNenData` v1 |
| protocolo | ids e direcoes de `NenProtocol`, versao 1 |
| interfaces | `NenTechnique`, `NenAbility` |

Descongelar exige: um ADR novo apontando este, um degrau em
`NenProfileMigrator`, e um plano para os mundos existentes.

Dois portoes cobram: `NenCategoryTest` (nomes e ordem) e
`ProtocoloCongeladoTest` (ids, direcoes e versao).

## Custo assumido

- **Nomes escolhidos cedo, com pouca informacao.** `undetermined` para o neutro
  e `nenfoundation` para um mod que talvez cresca alem de Nen sao escolhas
  feitas antes de o projeto existir. Aceito: um nome imperfeito custa menos que
  um save invalido.
- **O nome do pack e o do mod divergem.** O projeto se chama Dark Continent
  Awakening; o mod, Nen Foundation. E confuso na primeira leitura, e esta
  escrito no README.
- **Refatoracao de pacote fica cara.** Mover uma classe entre pacotes internos
  continua livre; mudar o package BASE nao.

## O que NAO muda

- Nomes EXIBIDOS continuam livres. "Reforco" ou "Enhancement" na tela e decisao
  de traducao, e a chave e que esta congelada.
- Categorias novas podem ser acrescentadas depois — no FIM da lista, com id
  novo. O congelamento proibe renomear e reordenar, nao crescer.
- Ids internos que nunca chegam a save nem a datapack nao estao cobertos.

---

## Emenda 1 — o campo `contextoDeInput` saiu antes da primeira implementacao

**Data:** 2026-09-10. **Marco:** M1.

A tabela de payloads congelada por este ADR listava, em
`activate_technique_request`, um campo `contextoDeInput` — herdado da frase
"pequeno contexto de input" do plano tecnico. Ao implementar os records, nenhum
consumidor foi encontrado:

- Ten e Ren sao alternaveis e ja tem um payload proprio de desligamento;
- tecla segurada e resolvida por **idempotencia no servidor**, e nao por um
  sinalizador na rede — isso ja estava escrito no proprio `protocol.md`.

Campo que ninguem le e um botao morto. Pior que inerte: ele convida a proxima
pessoa a preenche-lo com algo errado, e um campo a mais num payload **C2S** e
justamente onde estado do servidor entra sem ninguem notar.

**O campo foi removido da tabela, do documento e do record.**

### Custo assumido

- **Uma emenda a um ADR com dois dias de vida.** Congelar cedo e util, e
  congelar um placeholder nao e. A licao esta registrada: a tabela devia ter
  nascido com os campos que a implementacao confirmasse, e nao com os que o
  documento-fonte sugeria.
- **Se o M4 revelar que segurar-versus-tocar importa**, o campo volta — mas ai
  custa uma versao de protocolo e um handshake que recusa cliente antigo. Hoje
  custou zero.

### O que NAO muda

- **O congelamento continua valendo**, e para tudo o mais: `mod_id`, package,
  namespace, os sete ids de categoria, `PersistentNenData` v1, os ids e as
  direcoes dos sete payloads, `NenTechnique` e `NenAbility`.
- **`NenProtocol.VERSION` continua 1.** A versao existe para fazer cliente e
  servidor divergentes falharem alto. Nada nunca esteve na rede: nao ha
  nenhum par de versoes para divergir, e subir o numero agora seria ruido.
- **A regra de desfazer continua a mesma:** dai em diante, mudar a tabela exige
  bump de versao e handshake. Esta emenda so foi barata porque o protocolo
  ainda nao existia em lugar nenhum.
