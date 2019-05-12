#!/usr/bin/env bash
set -e
cd ..
gradle clean build
java -jar dist/build/libs/env-dist-0.0.1.jar -p 4000 &
java -jar dist/build/libs/env-dist-0.0.1.jar -p 4001 &
echo 'sleeping for 10 secs'
sleep 10
echo 'wake up'
TOPOLOGY_PORT=$(curl -s --data-binary '@/home/sammers/virus.jar' 'http://localhost:4000/infect' | jq -r '.topology_service_port')
echo "obtained port: '$TOPOLOGY_PORT'"
curl -s -d '["127.0.0.1:4001:0"]' -H "Content-Type: application/json" -X POST "http://localhost:$TOPOLOGY_PORT/gossip"\
echo "gossip has launched"
sleep 10
tail -f .avtenv/**/*.txt
