cd /test

./startEmu.sh &

./installDM.sh

./installDG.sh

#echo "Sleeping 20 seconds for the emulator to start"
#sleep 20

./runExperiment.sh
