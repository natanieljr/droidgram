cd droidgram

OUTPUT_DIR=/test/experiment/output
INPUT_DIR=/test/experiment/input
APKS_DIR=/test/experiment/apks
NR_SEEDS=10
ACTION_LIMIT=1000

mkdir ${INPUT_DIR}
chmod 777 ${INPUT_DIR}

echo "Cleaning output folder ${OUTPUT_DIR}"
rm -rf ${OUTPUT_DIR}
mkdir ${OUTPUT_DIR}
chmod 777 ${OUTPUT_DIR}

echo "Cleaning input folder ${INPUT_DIR}/apks/"
rm -rf ${INPUT_DIR}/apks/droidMate

echo "Removing previous grammar and input values from ${INPUT_DIR}/apks/"
rm -rf ${INPUT_DIR}/apks/*.txt

mkdir ${INPUT_DIR}/apks

echo "Running initial exploration and storing data into ${OUTPUT_DIR}"
echo "Running initial exploration and storing data into ${OUTPUT_DIR}"
./gradlew run --args="run --Exploration-apksDir=${APKS_DIR} --Exploration-launchActivityDelay=5000 --Exploration-widgetActionDelay=800 --Selectors-actionLimit=${ACTION_LIMIT} --Selectors-resetEvery=50 --Selectors-randomSeed=1 --Deploy-installApk=true --Deploy-uninstallApk=true --Selectors-pressBackProbability=0.00 --StatementCoverage-enableCoverage=true --Output-outputDir=${INPUT_DIR}/apks/droidMate"

echo "Extracting grammar from ${INPUT_DIR}/apks/"
./gradlew run --args="extract ${INPUT_DIR}/apks/droidMate/model/ ${INPUT_DIR}/apks/"

echo "Generating input values from grammar into ${INPUT_DIR}/apks"
python3 grammar_terminal_inputs.py ${INPUT_DIR} apks ${NR_SEEDS}

echo "Running grammar inputs from ${INPUT_DIR}/${APKS_DIR}"
./gradlew run --args="-i ${INPUT_DIR}/apks/ --Exploration-apksDir=${APKS_DIR} --Output-outputDir=${OUTPUT_DIR} --Exploration-launchActivityDelay=3000 --Exploration-widgetActionDelay=800 --Selectors-randomSeed=1 --Deploy-installApk=true --Deploy-uninstallApk=true --Selectors-pressBackProbability=0.00 --StatementCoverage-enableCoverage=true"

echo "Summary"
cat ${OUTPUT_DIR}/apks/summary.txt

echo "Done"
