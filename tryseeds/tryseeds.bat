
rem mostly untested (no Windows here)
rem make sure java is in your path, and the -mcjar version is correct

rem run this from the folder with the amidst.jar
rem ensure that dependency jars (args4j, etc.) are in the lib subfolder of the parent folder

java -jar amidst.jar ^
    -tryseeds 1000000 ^
    -history seeds.txt ^
    -sleeppct 25 ^
    -mcpath "%APPDATA%/.minecraft" ^
    -mcjar "%APPDATA%/.minecraft/versions/1.7.10/1.7.10.jar"

pause
