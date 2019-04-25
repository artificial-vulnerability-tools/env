#!/usr/bin/env bash
gradle clean test --tests '*Two*' --info
find ./env-core -name log.txt -type f | { read fl; read fl2; echo "file=$fl"; cat $fl; echo; echo "file=$fl2"; cat $fl2; echo; }
