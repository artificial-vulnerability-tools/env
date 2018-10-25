#!/usr/bin/env bash

VIRUS_CLASS=$1

echo ${VIRUS_CLASS} > virus_class_name.txt

cat >Main.java <<EOL
import $VIRUS_CLASS;

public class Main {
  public static void main(String[] args) {
    new $VIRUS_CLASS().launch();
  }
}
EOL

javac -cp uploaded.jar Main.java
jar cf jar_with_main.jar Main.class
rm Main.java
rm Main.class
java -cp jar_with_main.jar:uploaded.jar Main &> log.txt
