#!/usr/bin/env zsh
set -e

VIRUS_CLASS=$1
NAME_OF_JAR_WITH_VIRUS="virus.jar"

mkdir logs

echo $(whoami) > logs/whoami.log.txt

echo ${VIRUS_CLASS} > logs/virus_class_name.txt

cat >Main.java <<EOL
import $VIRUS_CLASS;

public class Main {
  public static void main(String[] args) {
    new $VIRUS_CLASS().start();
  }
}
EOL

javac -cp ${NAME_OF_JAR_WITH_VIRUS} Main.java &> logs/javac_cp_log.txt
jar cf jar_with_main.jar Main.class &> logs/jarcf_log.txt
java -cp jar_with_main.jar:${NAME_OF_JAR_WITH_VIRUS} Main &> logs/log.txt
