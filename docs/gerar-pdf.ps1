# Gera "InovaGAB - DevOps.pdf" a partir de documentacao-tecnica.html usando o Edge (ou Chrome) em modo headless.
# Coloque os prints em docs/prints com os nomes indicados no README e rode:
#   powershell -ExecutionPolicy Bypass -File docs\gerar-pdf.ps1

$pasta = Split-Path -Parent $MyInvocation.MyCommand.Path
$html  = Join-Path $pasta 'documentacao-tecnica.html'
$pdf   = Join-Path $pasta 'InovaGAB - DevOps.pdf'

$navegadores = @(
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe"
)
$exe = $navegadores | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $exe) { throw 'Edge ou Chrome nao encontrado.' }

$url = 'file:///' + ($html -replace '\\', '/')
# perfil temporario: evita que o navegador repasse a tarefa para uma janela ja aberta
$perfil = Join-Path $env:TEMP 'inovagab-pdf-perfil'
if (Test-Path $pdf) { Remove-Item $pdf }
$argumentos = @('--headless', '--disable-gpu', '--no-pdf-header-footer', '--virtual-time-budget=5000',
    "--user-data-dir=`"$perfil`"", "--print-to-pdf=`"$pdf`"", "`"$url`"")
Start-Process -FilePath $exe -ArgumentList $argumentos -Wait -WindowStyle Hidden

if (Test-Path $pdf) { Write-Host "PDF gerado: $pdf" } else { throw 'Falha ao gerar o PDF.' }
