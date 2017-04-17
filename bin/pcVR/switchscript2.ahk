switchscript2.ahk
sleep 2000
WinActivate, Galaxy S6
sleep 1000
ControlFocus, Galaxy S6
WinGetActiveStats, Title, Width, Height, X, Y
sleep 1000
MouseMove, 6*(Width / 12), 31*(Height / 32), 0
sleep 1000
send {LButton}
sleep 1000

MouseMove, 5*(Width / 12), 13*(Height / 16), 0
sleep 2000
send {LButton}
sleep 2000
send {F12}

