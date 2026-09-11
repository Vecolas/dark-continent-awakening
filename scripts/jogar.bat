@echo off
setlocal

rem ===========================================================================
rem  jogar.bat -- sobe o servidor da instancia e, quando ele estiver DE PE,
rem  abre o cliente que entra nele.
rem
rem  POR QUE ISTO EXISTE: o cmd.exe nao executa .ps1. Clicar duas vezes no
rem  instancia.ps1, ou chama-lo de um prompt do cmd, abre o bloco de notas em
rem  vez de rodar o script. Este arquivo e a ponte, e nada alem disso: toda a
rem  logica continua morando no instancia.ps1.
rem
rem  "EM SEQUENCIA" AQUI NAO E UM DEPOIS DO OUTRO. O servidor precisa continuar
rem  rodando enquanto o cliente joga. Entao ele vai para uma janela propria, e
rem  este script fica esperando a PORTA ABRIR antes de soltar o cliente.
rem
rem  E a espera e por porta, e nao por um sleep de N segundos, de proposito. O
rem  tempo fixo erra dos dois lados e as duas falhas sao ruins: curto demais e
rem  o cliente tenta entrar antes da hora e leva "Connection refused" -- que
rem  parece bug de rede e nao e; longo demais e voce olha para uma tela preta
rem  sem saber se travou. A porta responde a pergunta certa: o servidor aceita
rem  conexao, sim ou nao.
rem
rem  Uso:  scripts\jogar.bat          (jogador "Dev")
rem        scripts\jogar.bat Gon      (jogador "Gon")
rem ===========================================================================

pushd "%~dp0.." || (echo Nao consegui entrar na raiz do repositorio. & exit /b 1)
set "RAIZ=%CD%"

set "JOGADOR=%~1"
if "%JOGADOR%"=="" set "JOGADOR=Dev"

rem O PowerShell PELO CAMINHO ABSOLUTO, e nao `powershell` solto.
rem
rem Mesmo motivo pelo qual o instancia.ps1 resolve o Java 21 a mao em vez de
rem confiar no `java` do PATH: nesta maquina o PATH ja tem surpresa. Na
rem primeira versao deste arquivo eu usei `timeout /t 5` e peguei o `timeout`
rem do coreutils (Git Bash) em vez do do Windows -- que nao entende `/t` e
rem falhou na cara. Nome solto no PATH e uma aposta; caminho absoluto nao e.
set "PWSH=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"

rem 600s = 10 minutos, que cobre o primeiro build frio do Gradle com folga.
set "ESPERA_MAX=600"

echo.
echo == Nen Foundation -- servidor + cliente
echo    repositorio : %RAIZ%
echo    jogador     : %JOGADOR%

if not exist "%PWSH%" (
    echo.
    echo    NAO ACHEI O POWERSHELL em:
    echo        %PWSH%
    echo    Sem ele este script nao tem como chamar o instancia.ps1.
    goto :fim_erro
)

if not exist "%RAIZ%\instancia\servidor\run.bat" (
    echo.
    echo    A INSTANCIA NAO ESTA INSTALADA.
    echo    Falta o arquivo instancia\servidor\run.bat.
    echo.
    echo    Rode isto uma vez, num PowerShell, na raiz do repositorio:
    echo.
    echo        .\scripts\instancia.ps1 instalar
    echo.
    goto :fim_erro
)

rem A PORTA SAI DO server.properties, e nao de uma constante aqui.
rem
rem Ela ja mora la: o `instalar` escreve `server-port=` com o valor do -Porta.
rem Cravar 25565 tambem aqui seria a mesma verdade em duas fontes -- e a
rem divergencia seria SILENCIOSA do pior jeito: quem instalasse com outra
rem porta veria este script esperar dez minutos por uma porta que ninguem vai
rem abrir, e a mensagem de desistencia apontaria para o lugar errado.
rem
rem A LEITURA PASSA POR UM ARQUIVO, e nao por `for /f ... in (\`comando\`)`.
rem
rem Foi a primeira tentativa e ela FALHOU CALADA: com o caminho do .exe entre
rem aspas, o cmd reparte a linha errado, o comando nao roda -- e a variavel
rem simplesmente fica com o valor anterior. No meu teste isso leu 25565 de um
rem arquivo que dizia 25570, sem uma linha de erro. Exatamente a divergencia
rem que este bloco existe para impedir.
rem
rem O caminho vai por %PROPS% na ENV do processo filho ($env:PROPS), e nao
rem interpolado no texto do comando: assim espaco, acento e apostrofo no
rem caminho do repositorio nao viram um bug de aspas.
set "PORTA="
set "PROPS=%RAIZ%\instancia\servidor\server.properties"
rem O arquivo temporario vai para dentro de instancia\, que e gitignored:
rem nao suja o repositorio e nao depende de %TEMP%.
set "ARQ_PORTA=%RAIZ%\instancia\porta-lida-%RANDOM%.tmp"

"%PWSH%" -NoProfile -ExecutionPolicy Bypass -Command "$m = Select-String -LiteralPath $env:PROPS -Pattern '^server-port=([0-9]+)'; if ($m) { $m.Matches[0].Groups[1].Value }" > "%ARQ_PORTA%" 2>nul
set /p PORTA=<"%ARQ_PORTA%"
del "%ARQ_PORTA%" >nul 2>&1

rem E confere o que leu. Sem esta linha, todo o cuidado acima ainda poderia
rem terminar num PORTA vazio seguindo em frente.
echo(%PORTA%| "%SystemRoot%\System32\findstr.exe" /r /c:"^[0-9][0-9]*$" >nul
if errorlevel 1 (
    echo.
    echo    NAO CONSEGUI LER A PORTA de:
    echo        %PROPS%
    echo    Esperava uma linha 'server-port=^<numero^>'.
    echo.
    echo    Nao vou chutar 25565: se a instancia estiver noutra porta, isso
    echo    daria dez minutos de espera silenciosa por uma porta que ninguem
    echo    vai abrir, e a mensagem de desistencia apontaria para o lado errado.
    goto :fim_erro
)
echo    porta       : %PORTA%

rem Se ja tem alguem escutando, subir um segundo servidor so produziria um
rem "Address already in use" numa janela que ninguem esta olhando.
call :esperar_porta 0
if not errorlevel 1 (
    echo.
    echo    Ja tem algo escutando em 127.0.0.1:%PORTA%.
    echo    Nao vou subir um segundo servidor; vou direto para o cliente.
    goto :cliente
)

echo.
echo == Subindo o servidor numa janela propria
echo    ele recompila o mod e troca o JAR antes de subir.
echo    para para-lo depois: digite 'stop' naquela janela.

rem Sem -Porta aqui de proposito: o `servidor` roda o run.bat do instalador,
rem que le a porta do server.properties. Passar -Porta daria a impressao de
rem trocar a porta do servidor, e nao troca.
start "Nen Foundation -- servidor" "%PWSH%" -NoProfile -ExecutionPolicy Bypass -File "%RAIZ%\scripts\instancia.ps1" servidor

echo.
echo == Esperando a porta %PORTA% abrir
echo    (build frio do Gradle + boot do servidor; pode levar minutos)

call :esperar_porta %ESPERA_MAX%
if errorlevel 1 goto :desistiu

echo.
echo    servidor de pe.
goto :cliente

:desistiu
echo.
echo    DESISTI de esperar: a porta %PORTA% nao abriu em %ESPERA_MAX% segundos.
echo    O cliente NAO foi aberto -- ele so levaria "Connection refused".
echo.
echo    Olhe a janela "Nen Foundation -- servidor": o motivo esta la.
echo    Os dois mais comuns:
echo      - o build ficou vermelho (o `servidor` recompila antes de subir);
echo      - ha mais de um nenfoundation-*.jar em instancia\servidor\mods.
echo.
goto :fim_erro

:cliente
echo.
echo == Subindo o cliente (gradlew runClient)
echo    esta janela fica presa no cliente ate voce fechar o jogo.
echo.

"%PWSH%" -NoProfile -ExecutionPolicy Bypass -File "%RAIZ%\scripts\instancia.ps1" cliente -Jogador "%JOGADOR%" -Porta %PORTA%
set "SAIDA=%ERRORLEVEL%"

echo.
echo == Cliente encerrado (codigo %SAIDA%)
echo    O SERVIDOR CONTINUA DE PE na outra janela, de proposito: assim voce
echo    reabre o cliente sem esperar outro boot. Para derruba-lo, digite
echo    'stop' na janela dele.
echo.
popd
pause
exit /b %SAIDA%

rem --------------------------------------------------------------- sub-rotinas

rem :esperar_porta ^<segundos^>
rem
rem Pergunta a unica coisa que importa: da para conectar em 127.0.0.1:PORTA?
rem Com 0 segundos, e uma tentativa unica -- que e o teste de "ja tem servidor".
rem Devolve 0 se a porta abriu, 1 se o prazo acabou antes.
rem
rem A espera mora DENTRO do PowerShell, e nao num laco de batch, porque o laco
rem precisaria de um comando de sleep -- e foi exatamente ai que o `timeout` do
rem PATH errado apareceu.
:esperar_porta
"%PWSH%" -NoProfile -ExecutionPolicy Bypass -Command "$fim=(Get-Date).AddSeconds(%~1); do { $c=New-Object Net.Sockets.TcpClient; try { $c.Connect('127.0.0.1',%PORTA%); Write-Host ''; exit 0 } catch { } finally { $c.Dispose() }; if ((Get-Date) -lt $fim) { Write-Host '.' -NoNewline; Start-Sleep -Seconds 3 } } while ((Get-Date) -lt $fim); Write-Host ''; exit 1"
exit /b %ERRORLEVEL%

:fim_erro
popd
pause
exit /b 1
