# Gera e confere, em jogo, cada obra de Greed Island: as 8 cidades, os 34
# landmarks e as 9 estradas. NAO e portao -- e a ferramenta do gate MICRO.
#
# Ela existe porque "o codigo e o mesmo" nao prova que a proxima cidade nasce:
# Soufrabi assenta na costa, Limeiro e quatro vezes maior, e o aterro numa
# encosta e exatamente o caso que pode deixar paredao.
param([string]$Alvos = "$PSScriptRoot\alvos.csv")

function Rcon([string]$cmd) {
  $c = [Net.Sockets.TcpClient]::new(); $c.Connect('127.0.0.1', 25575)
  $s = $c.GetStream(); $s.ReadTimeout = 180000
  $send = { param($id, $t, $b0)
    $b = [Text.Encoding]::ASCII.GetBytes($b0)
    $ms = [IO.MemoryStream]::new(); $w = [IO.BinaryWriter]::new($ms)
    $w.Write([int]($b.Length + 10)); $w.Write([int]$id); $w.Write([int]$t)
    $w.Write($b); $w.Write([byte]0); $w.Write([byte]0); $w.Flush()
    $a = $ms.ToArray(); $s.Write($a, 0, $a.Length); $s.Flush() }
  $recv = { $h = [byte[]]::new(4); $n = 0
    while ($n -lt 4) { $r = $s.Read($h, $n, 4 - $n); if ($r -le 0) { throw 'eof' }; $n += $r }
    $len = [BitConverter]::ToInt32($h, 0); $buf = [byte[]]::new($len); $n = 0
    while ($n -lt $len) { $r = $s.Read($buf, $n, $len - $n); if ($r -le 0) { throw 'eof' }; $n += $r }
    [Text.Encoding]::ASCII.GetString($buf, 8, [Math]::Max(0, $len - 10)) }
  try { & $send 1 3 'dev'; $null = & $recv; & $send 2 2 $cmd; & $recv } finally { $c.Close() }
}

$linhas = Get-Content $Alvos
$ok = 0; $falhas = @()

foreach ($l in $linhas) {
  $p = $l -split ','
  $tipo = $p[0]; $id = $p[1]; $x = [int]$p[2]; $z = [int]$p[3]; $bloco = $p[4]
  $yEsperado = if ($p.Length -gt 5 -and $p[5]) { [int]$p[5] } else { $null }

  # Uma area pequena em volta do alvo. Chunk de 16: 48x48 cobre o alvo e a volta.
  $null = Rcon "execute in nenfoundation:greed_island run forceload add $($x-24) $($z-24) $($x+24) $($z+24)"
  Start-Sleep -Milliseconds 2500

  $achou = $false; $ondeY = $null
  # A FAIXA VAI ATE O TETO. A primeira versao parava em 200 e reprovou
  # passo_4 e as tres cachoeiras -- que existiam, em y=230 a 312, nos topos
  # de serra. Passo de montanha e queda d'agua ficam ALTO por definicao, e uma
  # regua que nao alcanca o alvo acusa ausencia onde ha presenca.
  $faixa = if ($yEsperado) { @($yEsperado) } else { 40..320 }
  foreach ($y in $faixa) {
    if ((Rcon "execute in nenfoundation:greed_island if block $x $y $z $bloco") -match 'passed') {
      $achou = $true; $ondeY = $y; break
    }
  }
  $null = Rcon "execute in nenfoundation:greed_island run forceload remove $($x-24) $($z-24) $($x+24) $($z+24)"

  if ($achou) { $ok++; "OK   $tipo $id  ($x,$ondeY,$z) = $bloco" }
  else { $falhas += "$tipo $id ($x,?,$z) esperava $bloco"; "FALHA $tipo $id ($x,?,$z) esperava $bloco" }
}

""
"================================================"
"conferidos: $($linhas.Count)   OK: $ok   falhas: $($falhas.Count)"
if ($falhas.Count) { ""; "FALHAS:"; $falhas | ForEach-Object { "  $_" } }
