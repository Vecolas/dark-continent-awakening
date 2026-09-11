<#
.SYNOPSIS
    Instancia de teste manual: Minecraft + Nen Foundation, sempre na versao
    mais recente do repositorio.

.DESCRIPTION
    A instancia vive em `instancia/`, que e IGNORADA pelo git. Este script e
    versionado; ela nao. Assim quem clonar o repositorio recria tudo com um
    comando, e nem o Minecraft instalado nem os mundos entram no historico.

    Subcomandos:
      instalar    baixa e instala o servidor NeoForge dedicado
      atualizar   compila o mod e troca o JAR da instancia
      servidor    recompila, troca o JAR e sobe o servidor (sem Gradle)
      cliente     sobe um cliente que entra nele sozinho
      status      mostra o que existe e qual versao esta instalada

.EXAMPLE
    .\scripts\instancia.ps1 instalar
    .\scripts\instancia.ps1 servidor      # num terminal
    .\scripts\instancia.ps1 cliente       # noutro
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('instalar', 'atualizar', 'servidor', 'cliente', 'status')]
    [string]$Comando = 'status',

    [string]$Jogador = 'Dev',
    [int]$Porta = 25565,
    [switch]$SemEntrar,

    # Sobe o JAR que JA esta instalado, sem recompilar.
    #
    # Existe para um caso so: o build esta vermelho e voce quer entrar no jogo
    # assim mesmo -- para investigar, ou para comparar com a versao anterior.
    # Fora disso, use o padrao: o proposito desta pasta e estar sempre na
    # versao mais recente.
    [switch]$SemAtualizar
)

$ErrorActionPreference = 'Stop'

$Raiz      = Split-Path -Parent $PSScriptRoot
$Instancia = Join-Path $Raiz 'instancia'
$Servidor  = Join-Path $Instancia 'servidor'
$Cliente   = Join-Path $Instancia 'cliente'
$Gradlew   = Join-Path $Raiz 'gradlew.bat'

function Escrever($texto) { Write-Host $texto }
function Titulo($texto)   { Write-Host ""; Write-Host "== $texto" -ForegroundColor Cyan }
function Aviso($texto)    { Write-Host "   $texto" -ForegroundColor Yellow }
function Erro($texto)     { Write-Host "   $texto" -ForegroundColor Red }

# Escreve texto SEM BOM.
#
# POR QUE ISTO EXISTE, e nao um `Set-Content -Encoding utf8`:
#
# No Windows PowerShell 5.1, `-Encoding utf8` escreve UTF-8 COM BOM. O
# Minecraft le eula.txt e server.properties como properties, e o BOM gruda na
# primeira chave: `eula` vira `﻿eula`, e `online-mode` vira
# `﻿online-mode`.
#
# Aconteceu de verdade na primeira execucao, e as duas falhas sao de tipos
# diferentes:
#   - a EULA recusada e BARULHENTA: o servidor diz que falta aceitar;
#   - o online-mode volta ao padrao TRUE em SILENCIO. O servidor sobe, o
#     cliente de dev nao consegue entrar, e nada em lugar nenhum menciona
#     encoding.
#
# A segunda e a razao de isto ser uma funcao e nao um detalhe.
function Escrever-Arquivo($caminho, [string[]]$linhas) {
    $semBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($caminho, (($linhas -join "`r`n") + "`r`n"), $semBom)
}

# ---------------------------------------------------------------- Java 21
#
# POR QUE ISTO NAO E OPCIONAL: nesta maquina `java -version` responde 16,
# porque um JDK antigo esta primeiro no PATH. O build funciona porque o Gradle
# usa JAVA_HOME -- mas um servidor iniciado com `java` pegaria o 16 e morreria
# com uma mensagem sobre versao de classe, que nao diz nada sobre PATH.
function Resolver-Java {
    $home21 = $env:JAVA_HOME
    if (-not $home21) { $home21 = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine') }
    if (-not $home21) {
        throw "JAVA_HOME nao esta definida. O NeoForge 1.21.1 exige Java 21; instale o Temurin 21."
    }
    $exe = Join-Path $home21 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $exe)) {
        throw "JAVA_HOME aponta para '$home21', mas nao ha bin\java.exe la."
    }
    # A versao sai do arquivo `release` do JDK, e nao de `java -version`.
    #
    # POR QUE: `java -version` escreve em STDERR. Com ErrorActionPreference
    # = 'Stop', o PowerShell transforma stderr de executavel nativo em erro
    # terminante -- e o script morria dizendo "NativeCommandError" logo depois
    # de imprimir a versao CERTA. Ler um arquivo nao tem esse problema.
    $release = Join-Path $home21 'release'
    if (Test-Path -LiteralPath $release) {
        $linha = Select-String -LiteralPath $release -Pattern '^JAVA_VERSION="?([^"]+)"?'
        if ($linha) {
            $versao = $linha.Matches[0].Groups[1].Value
            if ($versao -notmatch '^21\.') {
                throw "JAVA_HOME aponta para Java $versao, e o NeoForge 1.21.1 exige 21."
            }
        }
    } else {
        Write-Host "   (nao consegui conferir a versao: $release nao existe)" -ForegroundColor Yellow
    }
    return $exe
}

function Versao-Do-Mod {
    $linha = Select-String -Path (Join-Path $Raiz 'gradle.properties') -Pattern '^mod_version=(.+)$'
    if (-not $linha) { throw "mod_version nao encontrado em gradle.properties." }
    return $linha.Matches[0].Groups[1].Value.Trim()
}

function Versao-Do-Neo {
    $linha = Select-String -Path (Join-Path $Raiz 'gradle.properties') -Pattern '^neo_version=(.+)$'
    if (-not $linha) { throw "neo_version nao encontrado em gradle.properties." }
    return $linha.Matches[0].Groups[1].Value.Trim()
}

function Jar-Do-Mod {
    $jar = Get-ChildItem -Path (Join-Path $Raiz 'build\libs') -Filter 'nenfoundation-*.jar' `
        -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    return $jar
}

# ------------------------------------------------------------------ acoes

function Comando-Instalar {
    $java = Resolver-Java
    $neo  = Versao-Do-Neo

    Titulo "Instalando o servidor NeoForge $neo"

    if (Test-Path -LiteralPath (Join-Path $Servidor 'libraries')) {
        Aviso "Ja existe uma instalacao em instancia\servidor. Nada a fazer."
        Aviso "Para reinstalar do zero, apague a pasta e rode de novo."
        return
    }

    New-Item -ItemType Directory -Force -Path $Servidor | Out-Null
    $instalador = Join-Path $Servidor "neoforge-$neo-installer.jar"
    $url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neo/neoforge-$neo-installer.jar"

    Escrever "   baixando $url"
    Invoke-WebRequest -Uri $url -OutFile $instalador -UseBasicParsing

    Escrever "   instalando (baixa o Minecraft e as bibliotecas; demora alguns minutos)"
    Push-Location $Servidor
    try {
        & $java -jar $instalador --installServer
        if ($LASTEXITCODE -ne 0) { throw "o instalador do NeoForge saiu com codigo $LASTEXITCODE." }
    } finally { Pop-Location }

    Remove-Item -LiteralPath $instalador -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath "$instalador.log" -ErrorAction SilentlyContinue

    # EULA. Escrita automaticamente porque uma instancia que nao sobe nao
    # serve para teste manual -- mas dito em voz alta, porque e um aceite.
    Escrever-Arquivo (Join-Path $Servidor 'eula.txt') @('eula=true')
    Aviso "eula.txt escrito como 'true': isso ACEITA a EULA da Mojang nesta maquina."

    # Propriedades pensadas para teste manual, nao para servidor de jogo.
    $props = @(
        'online-mode=false',
        'enable-rcon=true',
        'rcon.password=dev',
        'rcon.port=25575',
        'level-type=minecraft\:flat',
        'gamemode=creative',
        'spawn-protection=0',
        'motd=Nen Foundation - instancia de teste',
        "server-port=$Porta"
    )
    Escrever-Arquivo (Join-Path $Servidor 'server.properties') $props
    Aviso "online-mode=false e RCON ligado: e uma instancia LOCAL de teste."
    Aviso "Nao exponha esta porta na internet."

    New-Item -ItemType Directory -Force -Path (Join-Path $Servidor 'mods') | Out-Null

    # Diagnostico LIGADO por padrao nos dois lados. Esta instancia existe para
    # teste manual: sem o log, "o payload chegou ao cliente" so da para ver
    # olhando o overlay na tela, o que nao serve para relato de bug.
    # Num servidor de jogo isto ficaria desligado.
    $configCliente = Join-Path $Cliente 'config'
    $configServidor = Join-Path $Servidor 'config'
    New-Item -ItemType Directory -Force -Path $configCliente | Out-Null
    New-Item -ItemType Directory -Force -Path $configServidor | Out-Null
    $dev = @('[dev]', "`tenabled = true", "`tlogStateTransitions = false")
    Escrever-Arquivo (Join-Path $configCliente 'nenfoundation-common.toml') $dev
    Escrever-Arquivo (Join-Path $configServidor 'nenfoundation-common.toml') $dev
    Aviso "dev.enabled = true nos dois lados: o log mostra o que chega e o que sai."

    Comando-Atualizar
}

function Comando-Atualizar {
    Titulo "Compilando e instalando a versao mais recente do mod"

    $java = Resolver-Java
    $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $java)

    & $Gradlew build --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw ("o build falhou; o JAR nao foi trocado. Para entrar no jogo assim" +
            " mesmo, com a versao ja instalada: .\scripts\instancia.ps1 servidor -SemAtualizar")
    }

    $jar = Jar-Do-Mod
    if (-not $jar) { throw "nenhum nenfoundation-*.jar em build\libs apos o build." }

    $mods = Join-Path $Servidor 'mods'
    New-Item -ItemType Directory -Force -Path $mods | Out-Null

    # APAGAR ANTES DE COPIAR, e nao so sobrescrever. Duas versoes do mesmo
    # mod_id na pasta fazem o NeoForge recusar o boot -- e depois de um bump
    # de versao os nomes sao diferentes, entao sobrescrever nao resolve.
    Get-ChildItem -Path $mods -Filter 'nenfoundation-*.jar' -ErrorAction SilentlyContinue |
        ForEach-Object {
            if ($_.Name -ne $jar.Name) { Escrever "   removendo versao antiga: $($_.Name)" }
            Remove-Item -LiteralPath $_.FullName -Force
        }

    Copy-Item -LiteralPath $jar.FullName -Destination $mods -Force
    Escrever "   instalado: $($jar.Name)"
}

function Comando-Servidor {
    $java = Resolver-Java

    if (-not (Test-Path -LiteralPath $Servidor)) {
        throw "a instancia nao existe. Rode: .\scripts\instancia.ps1 instalar"
    }

    # SUBIR JA ATUALIZA, e este e o ponto da pasta inteira.
    #
    # O servidor daqui roda o JAR de `mods/`, e nao o codigo do repositorio.
    # Se a troca do JAR depender de alguem lembrar de rodar `atualizar`, um dia
    # ela nao acontece -- e o teste manual mede a versao de ontem. A falha e
    # SILENCIOSA: o servidor sobe, o mod carrega, o jogo funciona, e nada em
    # lugar nenhum diz que aquele nao e o codigo que acabou de ser escrito.
    #
    # Pior ainda em par com o `cliente`: o cliente de desenvolvimento compila o
    # repositorio a cada execucao, entao ele SEMPRE esta na versao nova. Um
    # servidor velho contra um cliente novo produz divergencias que parecem bug
    # de sincronizacao.
    #
    # O Gradle e incremental: sem mudanca, isto custa poucos segundos e nao
    # recompila nada.
    if (-not $SemAtualizar) {
        Comando-Atualizar
    } else {
        Aviso "-SemAtualizar: subindo o JAR ja instalado, que pode NAO ser o codigo atual."
    }

    Titulo "Subindo o servidor da instancia"
    Aviso "Este NAO e o gradlew runServer: e o servidor instalado, com o JAR da pasta mods."
    Escrever "   RCON em 127.0.0.1:25575, senha 'dev'"
    Escrever "   para parar: digite 'stop' no console"
    Escrever ""

    # O PATH DO PROCESSO FILHO, e nao so o nosso.
    #
    # O run.bat do instalador chama `java` puro. Nesta maquina isso resolve
    # para o JDK 16 que esta primeiro no PATH, e o servidor morre com
    #   Unsupported major.minor version 65.0
    # -- uma mensagem que nao menciona PATH nem Java 21, e manda quem investiga
    # para o lado errado. Aconteceu de verdade na primeira execucao deste
    # script. Definir JAVA_HOME nao basta: o run.bat nao a consulta.
    $binDoJava = Split-Path -Parent $java
    $env:PATH = "$binDoJava;$env:PATH"
    $env:JAVA_HOME = Split-Path -Parent $binDoJava

    Push-Location $Servidor
    try {
        # O instalador gera os scripts de inicializacao; usamos o dele para
        # nao duplicar a lista de argumentos da JVM, que muda entre versoes.
        if (Test-Path -LiteralPath (Join-Path $Servidor 'run.bat')) {
            & (Join-Path $Servidor 'run.bat') nogui
        } else {
            throw "run.bat nao existe na instancia; a instalacao ficou incompleta."
        }
    } finally { Pop-Location }
}

function Comando-Cliente {
    Titulo "Subindo um cliente de teste"
    Aviso "E o cliente de desenvolvimento (gradlew runClient), nao um instalado por launcher:"
    Aviso "um cliente de launcher exigiria conta Microsoft, e o de dev nao."
    Escrever "   mundos, options.txt e capturas ficam em instancia\cliente\"
    Escrever ""

    $java = Resolver-Java
    $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $java)

    New-Item -ItemType Directory -Force -Path $Cliente | Out-Null

    $argumentos = @('runClient', '--console=plain', "-PdirCliente=instancia/cliente", "-Pjogador=$Jogador")
    if (-not $SemEntrar) { $argumentos += "-PentrarEm=localhost:$Porta" }

    & $Gradlew @argumentos
}

function Comando-Status {
    Titulo "Instancia de teste"

    if (-not (Test-Path -LiteralPath $Instancia)) {
        Escrever "   nao existe ainda."
        Escrever "   crie com: .\scripts\instancia.ps1 instalar"
        return
    }

    $instalado = Test-Path -LiteralPath (Join-Path $Servidor 'libraries')
    Escrever "   servidor instalado : $(if ($instalado) { 'sim' } else { 'NAO' })"

    $naPasta = Get-ChildItem -Path (Join-Path $Servidor 'mods') -Filter 'nenfoundation-*.jar' `
        -ErrorAction SilentlyContinue
    if ($naPasta) {
        foreach ($m in $naPasta) {
            Escrever "   mod instalado      : $($m.Name)  ($($m.LastWriteTime))"
        }
        if ($naPasta.Count -gt 1) {
            Erro "HA MAIS DE UM JAR do mod em mods\. O NeoForge recusa iniciar assim."
            Erro "Rode: .\scripts\instancia.ps1 atualizar"
        }
    } else {
        Escrever "   mod instalado      : nenhum"
    }

    $compilado = Jar-Do-Mod
    if ($compilado) {
        Escrever "   ultimo build       : $($compilado.Name)  ($($compilado.LastWriteTime))"
        if ($naPasta -and $naPasta.Count -eq 1 -and
            $compilado.LastWriteTime -gt $naPasta[0].LastWriteTime) {
            Aviso "O build e mais novo que o JAR instalado. Rode 'atualizar'."
        }
    }

    $mundos = Get-ChildItem -Path $Servidor -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName 'level.dat') }
    Escrever "   mundos no servidor : $(if ($mundos) { ($mundos.Name -join ', ') } else { 'nenhum' })"
    Escrever "   versao do mod      : $(Versao-Do-Mod)   NeoForge: $(Versao-Do-Neo)"
}

switch ($Comando) {
    'instalar'  { Comando-Instalar }
    'atualizar' { Comando-Atualizar }
    'servidor'  { Comando-Servidor }
    'cliente'   { Comando-Cliente }
    'status'    { Comando-Status }
}
