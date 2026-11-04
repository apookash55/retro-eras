param(
  [Parameter(Mandatory)][string]$OutFile,
  # Colour at the dark and bright ends of the swirl, as "#rrggbb".
  [string]$Dark = '#1e6e28',
  [string]$Bright = '#c8f064',
  [int]$Frames = 32
)
# Draws an animated, semi-transparent swirl strip (16 x 16*Frames) for an era portal, plus its .mcmeta.
# Translucent pixels make the game render the block in the translucent layer, like the nether portal.
Add-Type -AssemblyName System.Drawing
# .NET resolves relative paths against the process directory, not PowerShell's current location.
$OutFile = [IO.Path]::GetFullPath($OutFile, (Get-Location).Path)
function Rgb($hex) { @([Convert]::ToInt32($hex.Substring(1, 2), 16), [Convert]::ToInt32($hex.Substring(3, 2), 16), [Convert]::ToInt32($hex.Substring(5, 2), 16)) }
$lo = Rgb $Dark; $hi = Rgb $Bright
$bmp = New-Object Drawing.Bitmap 16, (16 * $Frames)
for ($f = 0; $f -lt $Frames; $f++) {
  $t = 2 * [Math]::PI * $f / $Frames
  for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
    $dx = $x - 7.5; $dy = $y - 7.5
    $r = [Math]::Sqrt($dx * $dx + $dy * $dy); $a = [Math]::Atan2($dy, $dx)
    $v = [Math]::Sin($a * 3 + $r * 0.9 - $t) * 0.5 + [Math]::Sin($x * 0.8 + $t * 2) * 0.25 + [Math]::Cos($y * 0.7 - $t) * 0.25
    $v = ($v + 1) / 2
    $c = 0..2 | ForEach-Object { [int]($lo[$_] + ($hi[$_] - $lo[$_]) * $v * $v) }
    $bmp.SetPixel($x, $y + 16 * $f, [Drawing.Color]::FromArgb([int](150 + 70 * $v), $c[0], $c[1], $c[2]))
  } }
}
New-Item -ItemType Directory -Force (Split-Path $OutFile) | Out-Null
$bmp.Save($OutFile, [Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
Set-Content -Encoding utf8NoBOM "$OutFile.mcmeta" "{`n  `"animation`": {`n    `"frametime`": 2`n  }`n}"
