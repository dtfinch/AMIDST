#!/bin/bash

nice java -jar amidst.jar \
    -tryseeds 1000000 \
    -history seeds.txt \
    -sleeppct 25 \
    -mcpath "$HOME/.minecraft" \
    -mcjar "$HOME/.minecraft/versions/1.7.10/1.7.10.jar"
