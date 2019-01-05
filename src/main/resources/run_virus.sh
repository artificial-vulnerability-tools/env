#!/usr/bin/env bash
set -e

VIRUS_CLASS=$1
ENV_PORT=$2
NAME_OF_JAR_WITH_VIRUS="virus.jar"

mkdir logs
echo $(whoami) > logs/whoami.log.txt
echo ${VIRUS_CLASS} > logs/virus_class_name.log.txt
echo ${ENV_PORT} > logs/env_port.log.txt

cat >AVTVirus.java <<EOL
import $VIRUS_CLASS;

public class AVTVirus {
  public static void main(String[] args) {
    new $VIRUS_CLASS().start($ENV_PORT);
  }
}
EOL

javac -cp ${NAME_OF_JAR_WITH_VIRUS} AVTVirus.java &> logs/javac_cp_log.txt
jar cf jar_with_main.jar AVTVirus.class &> logs/jarcf_log.txt
java -cp jar_with_main.jar:${NAME_OF_JAR_WITH_VIRUS} AVTVirus &> logs/log.txt &
echo $! | tee virus_process.pid
