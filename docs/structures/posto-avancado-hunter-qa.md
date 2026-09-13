# QA manual do Posto Avançado Hunter

Este roteiro fecha a parte do gate ST1 que não é coberta por JUnit: leitura
visual, worldgen em chunk novo e persistência após reload.

## Mundo dedicado novo

1. Iniciar `./gradlew runServer` com Java 21.
2. Entrar no Overworld e viajar por chunks novos até uma área da tag
   `nenfoundation:hunter_outpost_biomes`.
3. Confirmar o posto sem usar comando: pátio e portão legíveis, anexos baixos,
   torre aberta e cercamento leve com base de smooth stone e iron bars acima;
   fachadas em concreto branco, iluminação em shroomlight, nenhum bloco de
   ferro maciço e nenhuma muralha contínua ou torre fechada. Confirmar que não
   há iron bars nos toldos ou módulos internos, nem luzes flutuando perto da
   torre, e que cada janela de vidro se conecta aos dois lados da fachada.
   Confirmar também uma faixa livre entre prédios e perímetro (o footprint é
   41x41).
4. Abrir os baús visíveis e confirmar suprimentos de campo. Remover o piso do
   laboratório para encontrar o cache opcional e confirmar que ele também tem
   suprimentos.
5. Sair e reiniciar o servidor. Voltar ao chunk e confirmar que o posto não foi
   duplicado e que os baús não foram reescritos.

## Comando de diagnóstico

`/hxh structure spawn <x> <z> <rotation>` exige permissão 2 e serve apenas para
reprodução controlada. O gate natural não pode depender dele.

## Evidência exigida

Capturar quatro vistas: portão, diagonal aérea, pátio/laboratório e torre.
Reprovar a revisão se qualquer vista ler como castelo, forte medieval ou vila.
