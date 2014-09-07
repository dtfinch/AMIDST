#!/bin/bash

nice java -jar amidst.jar \
    -tryseeds 1000000 \
	-pruneseeds \
    -history seeds.txt \
    -sleeppct 25 \
    -mcpath "$HOME/Library/Application Support/minecraft" \
    -mcjar "$HOME/Library/Application Support/minecraft/versions/1.7.10/1.7.10.jar"
