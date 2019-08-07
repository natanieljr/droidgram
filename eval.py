import csv
import math
import os
import json

ABS_COVERAGE = "abs_coverage"

STD_DEV_THRESHOLD = 2
GRAMMAR_COVERAGE_THRESHOLD = 0.2
CODE_COVERAGE_THRESHOLD = 0.4


def has_crashed(value):
    return value["GrammarReached"] == "0" or value["CodeReached"] == "0"


def st_dev(values, mean):
    res = 0

    for value in values:
        res += math.pow(value - mean, 2)

    return math.sqrt(res / max(len(values), 1))


def is_outlier(value, mean, dev, threshold):
    if value < threshold:
        return True

    min = mean - STD_DEV_THRESHOLD * dev
    max = mean + STD_DEV_THRESHOLD * dev
    if value < min or value > max:
        return True

    return False


def csv_dict_list(file, coverage_gt, summary_file):
    dict_list = []
    if not os.path.exists(os.path.join(file, summary_file)):
        return dict_list
    # Open variable-based csv, iterate over the rows and map values to a list of dictionaries containing key/value pairs
    reader = csv.DictReader(open(os.path.join(file, summary_file), 'rt', encoding="utf8"), delimiter='\t')
    # dict_list = []
    for line in reader:
        summary_run = summary_file.split("_")[-1].replace(".txt", "")
        seed = summary_run + "_" + line["Seed"].zfill(2)
        line["Seed"] = seed
        # append seed dir
        line["path"] = str(os.path.join(file, "seed_" + seed))
        # append absolute coverage
        line[ABS_COVERAGE] = int(line["CodeReached"]) / coverage_gt
        dict_list.append(line)
    return dict_list


def compute_metrics(values):
    grammar_cov = 0
    relative_code_cov = 0
    abs_code_cov = 0
    grammar_size = 0
    input_size = 0

    for value in values:
        # ignore crashed for average
        grammar_cov += float(value["GrammarCov"])
        relative_code_cov += float(value["CodeCov"])
        abs_code_cov += float(value[ABS_COVERAGE])
        input_size += float(value["Input Size"])
        if grammar_size == 0:
            grammar_size = int(value["GrammarReached"]) + int(value["GrammarMissed"])

    count = max(len(values), 1)
    avg_grammar_cov = grammar_cov / count
    avg_relative_code_cov = relative_code_cov / count
    avg_abs_code_cov = abs_code_cov / count
    avg_input_size = input_size / count

    filt = lambda f: map(lambda x: float(x[f]), values)

    grammar_cov_values = list(filt("GrammarCov"))
    grammar_std_dev = st_dev(grammar_cov_values,
                             avg_grammar_cov)

    relative_code_coverage_values = list(filt("CodeCov"))
    relative_code_cov_std_dev = st_dev(relative_code_coverage_values,
                                       avg_relative_code_cov)

    abs_code_coverage_values = list(filt(ABS_COVERAGE))
    abs_code_coverage_std_dev = st_dev(abs_code_coverage_values, avg_abs_code_cov)

    input_size_values = list(filt("Input Size"))
    input_size_std_dev = st_dev(input_size_values, avg_input_size)

    return count, 10 - count, grammar_size, avg_input_size, input_size_std_dev, avg_grammar_cov, grammar_std_dev, avg_relative_code_cov, relative_code_cov_std_dev, avg_abs_code_cov, abs_code_coverage_std_dev


def filter_bad_values(values):
    _, _, _, _, _, avg_grammar_cov, grammar_std_dev, avg_relative_code_cov, relative_code_cov_std_dev, _, _ = compute_metrics(
        values)

    filt = lambda v: not is_outlier(float(v["GrammarCov"]), avg_grammar_cov, grammar_std_dev,
                                    GRAMMAR_COVERAGE_THRESHOLD) and not is_outlier(float(v["CodeCov"]),
                                                                                   avg_relative_code_cov,
                                                                                   relative_code_cov_std_dev,
                                                                                   CODE_COVERAGE_THRESHOLD)
    return list(filter(filt, values))[:10]


if __name__ == "__main__":

    rq = "rq1"
    e = os.path.abspath("/Volumes/Experiments/20-icse-regression-with-grammars/experiments")

    with open('./summary_%s.csv' % rq, 'w', newline='') as summary_csv, open('./apps.csv', 'w', newline='') as app_csv:
        summary_writer = csv.writer(summary_csv, delimiter='\t',
                                    quotechar='|', quoting=csv.QUOTE_MINIMAL)
        summary_writer.writerow(
            ("App", "#Working", "#Fail", "Grammar Size", "Avg. Input Size", "St Dev Input Size",
             "Avg. Grammar Coverage", "St Dev Grammar Coverage", "Avg. Relative Code Coverage",
             "St Dev Relative Code Coverage", "Avg. Absolute Code Coverage",
             "St Dev Absolute Code Coverage", "#Stmts"))

        app_writer = csv.writer(app_csv, delimiter='\t',
                                quotechar='|', quoting=csv.QUOTE_MINIMAL)
        app_writer.writerow(("App", "Seed", "Input Size", "Grammar Coverage", "Relative Code Coverage",
                             "Absolute Code Coverage", "Run Path"))

        for app in os.listdir(os.fsencode(e)):
            filename = os.fsdecode(app)
            e_dir = os.path.join(e, filename, rq)

            if not os.path.isdir(e_dir):
                continue

            coverage_gt = 0

            with open(os.path.join(e, filename, "apks", filename + ".apk.json"), 'rt') as coverage_gt_file:
                # data = coverage_gt_file.read().decode("ASCII")
                content = json.load(coverage_gt_file, encoding='utf-8')
                # content = json.loads(data)
                for entry in content["allMethods"]:
                    coverage_gt += 1

            run_data = []
            for summary_file in os.listdir(e_dir):
                if summary_file.startswith("summary") and summary_file.endswith(".txt"):
                    run_data += csv_dict_list(e_dir, coverage_gt, summary_file)

            values = list(
                filter(lambda v: not has_crashed(v),
                       run_data
                       )
            )

            values = filter_bad_values(values)
            summary_writer.writerow((filename, *compute_metrics(values), coverage_gt))

            for value in values:
                app_writer.writerow((
                    filename, value["Seed"], value["Input Size"], value["GrammarCov"], value["CodeCov"], value[
                        ABS_COVERAGE],
                    value["path"]))
