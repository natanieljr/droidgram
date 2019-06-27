import sys
import os
import subprocess
import shutil
import time
from os import listdir
from os.path import isfile, join
from joblib import Parallel, delayed
import multiprocessing

base_emulator_nr = 5554


def get_next_enumator_nr():
    global base_emulator_nr
    ret = base_emulator_nr
    base_emulator_nr += 2
    return ret


def get_apk_files_from_dir(apk_dir):
    return [join(apk_dir, f) for f in listdir(apk_dir) if f.endswith(".apk") and isfile(join(apk_dir, f))]


def get_json_from_apk(apk_file):
    return apk_file.replace("-instrumented", "").replace(".apk", ".apk.json")


def init_all_experiments(apk_list):
    data = []

    for apk in apk_list:
        data.append(Data(apk))

    return data


class Data():
    def __init__(self, apk):
        self.root_logs_dir = "./logs"
        self.root_grammar_input_dir = "./input"
        self.root_apks_dir = "./apks"
        self.root_output_dir = "./output"
        self.action_limit = 10
        self.nr_seeds = 1

        self.apk = apk
        self.json = get_json_from_apk(apk)
        emulator_nr = get_next_enumator_nr()
        self.emulator_name = "emulator-%d" % emulator_nr
        self.avd_name = "emulator%d" % emulator_nr

        self.grammar_input_dir = join(self.root_grammar_input_dir, self.avd_name)
        self.output_dir = join(self.root_output_dir, self.avd_name)
        self.apk_dir = join(self.root_apks_dir, self.avd_name)
        self.logs_dir = join(self.root_logs_dir, self.avd_name)

        self.debug()
        self._clean_logs_dir()

    def __str__(self):
        return "Emulator: %s\tAPK %s" % (self.emulator_name, self.apk)

    def debug(self):
        print("APK %s" % self.apk)
        print("JSON %s" % self.json)
        print("Emulator: %s" % self.emulator_name)
        print("AVD Name: %s\n" % self.avd_name)
        print("Grammar input dir: %s" % self.grammar_input_dir)
        print("Output dir: %s" % self.output_dir)
        print("APKs dir: %s" % self.apk_dir)
        print("Logs dir: %s" % self.logs_dir)

    def _clean_logs_dir(self):
        try:
            shutil.rmtree(self.logs_dir)
        except:
            pass
        try:
            os.mkdir(self.root_logs_dir)
        except:
            pass
        try:
            os.mkdir(self.logs_dir)
        except:
            pass

    def _run_command(self, command, file_name):
        print("Running command %s" % str(command))
        try:
            # process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)
            # process.wait()
            os.system(*command)
            # output = process.stdout.readlines()

            # if file_name is not None:
            #    with open(join(self.logs_dir, file_name), "w") as f:
            #        f.write("Command %s\n" % str(command))
            #        f.write("Output:\n")
            #        for line in output:
            #            f.write(str(line))
            #        f.close()
        except Exception as e:
            print(e)
            print(e.args)
            raise e

    def _create_avd(self):
        self._run_command(["avdmanager "
                           "create "
                           "avd "
                           "-n "
                           " %s "
                           "-k "
                           "\"system-images;android-28;google_apis;x86\" "
                           "-d "
                           "pixel "
                           "--force " % self.avd_name
                           ],
                          "create_avd.log"
                          )

    def _delete_avd(self):
        self._run_command(["avdmanager "
                           "delete "
                           "avd "
                           "-n "
                           " %s " % self.avd_name
                           ],
                          "delete_avd.log"
                          )

    def _start_emulator(self):
        android_home = os.environ["ANDROID_HOME"]
        command = ["%s/emulator/emulator-headless "
                   "-avd "
                   " %s "
                   "-no-audio "
                   "-no-window "
                   "-no-snapshot " % (android_home, self.avd_name)]
        print("Running command %s" % str(command))
        self.emulator_process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)

        print("Waiting 60 seconds for the emulator to start")
        time.sleep(10)
        print("Waiting 50 seconds for the emulator to start")
        time.sleep(10)
        print("Waiting 40 seconds for the emulator to start")
        time.sleep(10)
        print("Waiting 30 seconds for the emulator to start")
        time.sleep(10)
        print("Waiting 20 seconds for the emulator to start")
        time.sleep(10)
        print("Waiting 10 seconds for the emulator to start")
        time.sleep(10)
        print("Assuming that the emulator has started proceeding")

    def _copy_original_apk(self):
        try:
            shutil.rmtree(self.apk_dir)
        except:
            pass
        try:
            os.mkdir(self.root_apks_dir)
        except:
            pass
        try:
            os.mkdir(self.apk_dir)
        except:
            pass

        shutil.copy(self.apk, self.apk_dir)
        shutil.copy(self.json, self.apk_dir)

    def _clean_output_dir(self):
        try:
            shutil.rmtree(self.output_dir)
        except:
            pass
        try:
            os.mkdir(self.root_output_dir)
        except:
            pass
        try:
            os.mkdir(self.output_dir)
        except:
            pass

    def _clean_grammar_input_dir(self):
        try:
            shutil.rmtree(self.grammar_input_dir)
        except:
            pass
        try:
            os.mkdir(self.root_grammar_input_dir)
        except:
            pass
        try:
            os.mkdir(self.grammar_input_dir)
        except:
            pass

    def _run_droidgram_explore(self):
        command = ["./01.sh %s %d %s %s " % (self.apk_dir,
                                             self.action_limit,
                                             self.emulator_name,
                                             self.output_dir,
                                             )
                   ]
        self._run_command(command, None)

    def _step1_run_exploration(self):
        self._clean_output_dir()
        self._copy_original_apk()

        self._run_droidgram_explore()

    def _move_exploration_output_to_grammar_input(self):
        shutil.move("%s/droidMate" % self.output_dir, "%s/droidMate" % self.grammar_input_dir)

    def _run_droidgram_extraction(self):
        # log_file = join(self.logs_dir, "02extract.log")
        command = ["./02.sh %s %s/" % (self.grammar_input_dir,
                                       self.grammar_input_dir,
                                       # log_file
                                       )
                   ]
        self._run_command(command, None)

    def _step2_extract_grammar(self):
        self._clean_grammar_input_dir()
        self._run_droidgram_extraction()

    def _step3_fuzz_grammar(self):
        # log_file = join(self.logs_dir, "03fuzz.log")
        command = ["python3 "
                   "grammar_terminal_inputs.py %s %s %d " % (self.root_grammar_input_dir,
                                                             self.avd_name,
                                                             self.nr_seeds,
                                                             # log_file
                                                             )
                   ]
        self._run_command(command, None)

    def _step4_run_grammar_inputs(self):
        log_file = join(self.logs_dir, "04grammar.log")
        command = ["./04.sh %s %s %s %s " % (self.grammar_input_dir,
                                             self.apk_dir,
                                             self.output_dir,
                                             self.emulator_name,
                                             # log_file
                                             )
                   ]
        self._run_command(command, None)

    def start(self):
        self._create_avd()
        self._start_emulator()

        self._step1_run_exploration()
        self._step2_extract_grammar()
        self._step3_fuzz_grammar()
        self._step4_run_grammar_inputs()

    def terminate(self):
        self.emulator_process.terminate()
        self._delete_avd()


def run(item):
    try:
        item.start()
    except Exception as e:
        print("Error `%s` when running the experiment in %s" % (str(e), item))


if __name__ == "__main__":
    apks_dir = sys.argv[1]
    apk_list = get_apk_files_from_dir(apks_dir)

    data = init_all_experiments(apk_list)

    num_cores = multiprocessing.cpu_count()
    results = Parallel(n_jobs=num_cores)(delayed(run)(item) for item in data)

    for item in data:
        try:
            item.terminate()
        except Exception as e:
            print("Error `%s` stopping AVD in %s" % (str(e), item))