#!/bin/bash
java -jar ~/personal/perseida/jifra-cli/jifra.jar jar
jifra jar
# java -jar jifc.jar Main.class
# java -jar jifc.jar ./target/dev/levia/jifc/ParseClassFile.class
# java -jar jifc.jar Main.class

# Mainer
compiling=maner/Mainer.class

# ParseClassFile
# compiling=./target/dev/levia/jifc/ParseClassFile.class

# Main
# compiling=Main.class




java -jar jifc.jar $compiling # maner/Mainer.class
