param(
    [string]$Device = "4d894da0",
    [string]$Action = "dump",
    [string]$Text = "",
    [string]$Id = ""
)

$adb = "C:\Android\Sdk\platform-tools\adb.exe"

function Get-UiXml {
    & $adb -s $Device shell uiautomator dump /sdcard/ui.xml 2>&1 | Out-Null
    (& $adb -s $Device exec-out cat /sdcard/ui.xml) -join ""
}

function Find-Bounds($xml, $needle) {
    $pattern = 'text="' + [regex]::Escape($needle) + '"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    $m = [regex]::Match($xml, $pattern)
    if ($m.Success) {
        $x1 = [int]$m.Groups[1].Value
        $y1 = [int]$m.Groups[2].Value
        $x2 = [int]$m.Groups[3].Value
        $y2 = [int]$m.Groups[4].Value
        return @([int](($x1 + $x2) / 2), [int](($y1 + $y2) / 2))
    }
    return $null
}

function Find-BoundsById($xml, $needle) {
    $pattern = 'resource-id="' + [regex]::Escape($needle) + '"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    $m = [regex]::Match($xml, $pattern)
    if ($m.Success) {
        $x1 = [int]$m.Groups[1].Value
        $y1 = [int]$m.Groups[2].Value
        $x2 = [int]$m.Groups[3].Value
        $y2 = [int]$m.Groups[4].Value
        return @([int](($x1 + $x2) / 2), [int](($y1 + $y2) / 2))
    }
    return $null
}

$xml = Get-UiXml

switch ($Action) {
    "dump" { $xml }
    "tap" {
        $point = Find-Bounds $xml $Text
        if (-not $point) { "NOT FOUND: $Text"; exit 1 }
        & $adb -s $Device shell input tap $point[0] $point[1]
        "tapped '$Text' at $($point[0]),$($point[1])"
    }
    "tapId" {
        $point = Find-BoundsById $xml $Id
        if (-not $point) { "NOT FOUND: $Id"; exit 1 }
        & $adb -s $Device shell input tap $point[0] $point[1]
        "tapped id '$Id' at $($point[0]),$($point[1])"
    }
    "type" {
        & $adb -s $Device shell input text $Text
        "typed '$Text'"
    }
    default { "unknown action" }
}
