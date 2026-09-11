# Ficha do jogador — issue #44

Tela de consulta solicitada para conferir o jogador e os efeitos de `/nen`.
Branch: `codex/hud-jogador`, derivada de `codex/m1-c2s-sync` (inclui a #43).

## Como usar

1. Entre no mundo e pressione **V**. O atalho pode ser alterado em
   **Opções → Controles → Teclas → Nen Foundation**.
2. **Status** mostra vida, alimentação, armadura, nível de experiência vanilla,
   categoria visível, aura quando recebida e totais de desbloqueios.
3. **Habilidades de Nen** lista técnicas, habilidades e marcos recebidos, por
   tipo e ID. Um grupo vazio é indicado explicitamente.
4. Use `<` / `>`, **Page Up / Page Down** ou a roda do mouse sobre a lista.
   Passe o mouse sobre um texto abreviado para ler o valor completo.
5. Feche com **V**, **Esc** ou **X**. A tela não pausa o mundo.

Não depende de `dev.enabled`, nem substitui o overlay técnico. Na GUI pequena,
o retrato é omitido para preservar a leitura; os dados continuam paginados.

## Conferir o comando de verdade

Como operador (permissão 2), para o próprio jogador:

```text
/nen technique unlock nenfoundation:ten
```

Abra a aba Nen: `nenfoundation:ten` deve aparecer em **Técnica desbloqueada**.
Depois execute:

```text
/nen technique lock nenfoundation:ten
```

O ID deve desaparecer, sem precisar relogar. Um operador também pode usar
o nome do jogador ao final do comando para testar com a ficha dele aberta.

**O M1 ainda não tem registros/motores de técnicas e habilidades.** O ID
aparecer prova o desbloqueio recebido, não que ele possa ser ativado. Hoje
`/nen` fornece `technique unlock/lock`, não um comando de concessão de
habilidades; a lista de habilidades lê o campo já existente no snapshot.

Categoria indefinida não é prova de jogador não desperto: o cliente não
recebe esse booleano, nem potencial, controle, output ou proficiência. A ficha
não inventa esses valores, não revela categoria oculta e não muda o protocolo.
Aura não recebida aparece como **não informada**, nunca como zero fictício.

## Arquivos e decisões

- `client/screen/TelaDoJogador.java`: duas abas, retrato vanilla e arte
  geométrica original; nenhuma textura extraída da imagem de referência.
- `client/screen/DadosDaFicha.java`: projeção imutável dos IDs recebidos;
  atualiza quando o snapshot muda e descarta dados após limpeza do cache.
- `client/screen/PaginacaoDaFicha.java`: limites recalculados após resize e
  remoção de itens; nenhum ID fica inacessível por falta de espaço.
- `client/keybind/NenKeybinds.java` e `client/NenFoundationClient.java`:
  registro explícito de V e abertura só em jogo, sem roubar chat/inventário.
- `client/screen/package-info.java`, idiomas `pt_br/en_us` e três classes de
  teste da ficha: documentação, traduções e regressões.

Nenhuma alteração em save, payloads, validação server-side, dependências,
configuração de Java ou controles de outros mods.

## Evidências e limites

- `gradlew.bat build`: 105 testes passaram na primeira execução completa.
- Canário: congelar a lista no primeiro snapshot e remover a tradução de
  vida reprovou 3 dos 5 testes filtrados. As quebras foram restauradas.
- Canário de paginação: remover a limitação da página reprovou o teste de
  remoção/resize. A limitação foi restaurada antes do build final.
- `DadosDaFichaTest`: vazio vs ausente, IDs de debug, ordem, unlock/lock/reset,
  lista imutável e limpeza entre sessões.
- `PaginacaoDaFichaTest`: todos os 513 índices alcançáveis uma única vez em
  várias capacidades; bordas, resize/remoção e capacidade inválida.
- `TraducaoDaFichaTest`: varre chaves usadas na ficha e cobra traduções nos
  dois idiomas, inclusive ausência de chaves órfãs.

### Cliente e servidor reais — 11/09/2026

Servidor `runServer` em loopback chegou a `Done (56.319s)`. Um cliente real
entrou como `FichaQA` via `runClient '-PentrarEm=127.0.0.1:25565'
'-Pjogador=FichaQA'`, usando JDK Microsoft 21.0.12.1 e o perfil dev-minimal.

Um arnês temporário chamou os controles dentro do próprio jogo; o snapshot
não foi simulado. O log do cliente registrou `QA FICHA APROVADA` após:

- abertura por key mapping V, troca de abas e fechamento por V/Esc;
- `technique unlock/lock` reais, com o cache e a ficha abertos atualizando
  sem reconexão;
- 11 IDs reais concedidos ao jogador de teste, navegação entre páginas e
  reconstrução da tela ao mudar a escala da GUI;
- proteção do chat e remapeamento para B, seguido de retorno para V;
- remoção de todos os desbloqueios usados no teste.

Capturas reais em `run/client-FichaQA/screenshots/`: `01-status.png`,
`02-nen-vazio.png`, `03-unlock-real.png`, `04-lock-real.png`, `05-lista.png`,
`06-pagina-2.png`, `07-compacto.png`. As capturas de Status e unlock foram
inspecionadas; a primeira inclui notificações de tutorial vanilla. O cliente
usou janela 854×480 e GUI 427×240; alterar a escala para 4 foi limitado pelo
Minecraft, portanto não prova uma resolução menor.

O servidor foi parado pelo comando e salvou os dados. O cliente foi encerrado
pelo processo após a aprovação (por isso a tarefa `runClient` terminou com
código -1, não por crash de renderização). Permissão de operador de FichaQA,
bind de endereço e código temporário foram removidos/restaurados.

**Sem prova ainda:** dois jogadores, clique/tecla físicos, GUI abaixo de
427×240, conflito com outros mods, idiomas além de PT/EN e renderização de
habilidades executáveis futuras. A correção de modificadores no fechamento
foi revisada contra a API pinada e compilada; Ctrl+clique físico não foi
exercitado. O teste de habilidades/marcos preenchidos é unitário, não via
comando real (esses comandos não existem no M1).
