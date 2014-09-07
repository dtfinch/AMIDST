
rem entirely untested (no Windows here)
rem make sure java is in your path, and the -mcjar version is correct

java -jar amidst.jar ^
    -tryseeds 1000000 ^
	-pruneseeds ^
    -history seeds.txt ^
    -sleeppct 25 ^
    -mcpath "%APPDATA%/.minecraft" ^
    -mcjar "%APPDATA%/.minecraft/versions/1.7.10/1.7.10.jar"

pause
