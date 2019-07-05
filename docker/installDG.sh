#!/usr/bin/env bash
cd /test

git clone https://x-token-auth:DNS9DFvsAVSyjCL44sZi@projects.cispa.saarland/nataniel.borges/droidgram.git

cd droidgram

./gradlew clean build

cd /test
