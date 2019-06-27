#!/usr/bin/env bash

OUTPUT_DIR=./out
INPUT_DIR=./input
APKS_DIR=apks
DONE_DIR=./done
NR_SEEDS=1
ACTION_LIMIT=10

mkdir ${INPUT_DIR}

echo "Cleaning output folder ${OUTPUT_DIR}"
rm -rf ${OUTPUT_DIR}
mkdir ${OUTPUT_DIR}

echo "Running initial exploration and storing data into ${OUTPUT_DIR}"
./gradlew run --args="run --Exploration-apksDir=${APKS_DIR} --Exploration-launchActivityDelay=5000 --Exploration-widgetActionDelay=800 --Selectors-actionLimit=${ACTION_LIMIT} --Selectors-resetEvery=50 --Selectors-randomSeed=1 --Deploy-installApk=true --Deploy-uninstallApk=true --Selectors-pressBackProbability=0.00 --StatementCoverage-enableCoverage=true --Output-outputDir=${OUTPUT_DIR}/droidMate"

echo "Cleaning input folder ${INPUT_DIR}/${APKS_DIR}/"
rm -rf ${INPUT_DIR}/${APKS_DIR}/droidMate

echo "Removing previous grammar and input values from ${INPUT_DIR}/${APKS_DIR}/"
rm -rf ${INPUT_DIR}/${APKS_DIR}/*.txt

mkdir ${INPUT_DIR}/${APKS_DIR}

echo "Moving exploration output from ${OUTPUT_DIR}/droidMate into ${INPUT_DIR}/${APKS_DIR}/"
mv ${OUTPUT_DIR}/droidMate ${INPUT_DIR}/${APKS_DIR}/

echo "Extracting grammar from ${INPUT_DIR}/${APKS_DIR}/"
./gradlew run --args="extract ${INPUT_DIR}/${APKS_DIR}/droidMate/model/ ${INPUT_DIR}/${APKS_DIR}/"

echo "Generating input values from grammar into ${INPUT_DIR}/${APKS_DIR}"
python3 grammar_terminal_inputs.py ${INPUT_DIR} ${APKS_DIR} ${NR_SEEDS}

echo "Running grammar inputs from ${INPUT_DIR}/${APKS_DIR}"
./gradlew run --args="-i ${INPUT_DIR}/${APKS_DIR}/ --Exploration-apksDir=${APKS_DIR} --Output-outputDir=${OUTPUT_DIR} --Exploration-launchActivityDelay=3000 --Exploration-widgetActionDelay=800 --Selectors-randomSeed=1 --Deploy-installApk=true --Deploy-uninstallApk=true --Selectors-pressBackProbability=0.00 --StatementCoverage-enableCoverage=true"

echo "Moving grammar results from ${OUTPUT_DIR} to ${DONE_DIR}"
rm -rf ${DONE_DIR}/${APKS_DIR}
mkdir ${DONE_DIR}/
mv ${OUTPUT_DIR} ${DONE_DIR}/
mv ${DONE_DIR}/${OUTPUT_DIR} ${DONE_DIR}/${APKS_DIR}

echo "Summary"
cat ${DONE_DIR}/${APKS_DIR}/summary.txt

echo "Done"