package org.droidmate.droidgram

import org.droidmate.droidgram.exploration.GrammarReplayMF
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.asSequence
import kotlin.streams.toList

object ResultBuilder {
    @JvmStatic
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * Pattern: inputs00.txt, inputs01.txt, ..., inputs10.txt
     */
    private fun getFilesInputs(inputDir: Path): List<Path> {
        return Files.walk(inputDir)
            .filter { it.fileName.toString().startsWith("inputs") }
            .filter { it.fileName.toString().endsWith(".txt") }
            .filter { it.fileName.toString().length == 12 }
            .toList()
    }

    /**
     * Pattern: reachedTerminals.txt
     */
    private fun getFilesReachedWidgets(explorationResultDir: Path): List<Path> {
        return Files.walk(explorationResultDir)
            .filter { it.fileName.toString() == GrammarReplayMF.reachedTerminals }
            .toList()
    }

    /**
     * Pattern *\coverage\*-statements-*
     */
    private fun getFilesReachedStatements(explorationResultDir: Path): List<Path> {
        return Files.walk(explorationResultDir)
            .filter { it.toAbsolutePath().toString().contains("/coverage/") }
            .filter { it.fileName.toString().contains("-statements-") }
            .toList()
    }

    private fun getReachedTerminals(baseDir: Path): Set<String> {
        return getFilesReachedWidgets(baseDir)
            .flatMap { Files.readAllLines(it) }
            .filter { it.isNotEmpty() }
            .map { stmt ->
                val res = stmt.substringBefore(");").trim() + ")"

                check(res.contains('(')) { "Invalid reached statement $stmt" }
                check(res.endsWith(')')) { "Invalid reached statement $stmt" }

                res
            }.toSet()
    }

    private fun getReachedStatements(baseDir: Path): Set<Long> {
        return getFilesReachedStatements(baseDir)
            .flatMap { Files.readAllLines(it) }
            .filter { it.isNotEmpty() }
            .map { stmt -> stmt.takeWhile { it != ';' }.toLong() }
            .toSet()
    }

    private fun getCodeCoverage(allStatements: Set<Long>, dir: Path): Result<Long> {
        return calculateCodeCoverage(allStatements, dir)
    }

    fun generateCodeCoverage(allStatements: Set<Long>, dir: Path) {
        val coverage = getCodeCoverage(allStatements, dir)
        val coverageFile = dir.resolve("codeCoverage.txt")

        coverage.save(coverageFile)
    }

    private fun calculateCodeCoverage(allStatements: Set<Long>, dir: Path): Result<Long> {
        val reached = getReachedStatements(dir)
        val missing = allStatements - reached

        val res = Result(allStatements.toList(), reached, missing)

        check(res.coverage in 0.0..1.0) { "Expected code coverage between 0 and 1. Found ${res.coverage}" }

        return res
    }

    fun generateGrammarCoverage(input: String, dir: Path) {
        ResultBuilder.generateGrammarCoverage(listOf(input), dir)
    }

    private fun getGrammarCoverage(allTerminals: List<String>, dir: Path): Result<String> {
        return calculateGrammarCoverage(allTerminals, dir)
    }

    fun generateGrammarCoverage(allTerminals: List<String>, dir: Path) {
        val coverage = getGrammarCoverage(allTerminals, dir)
        val coverageFile = dir.resolve("grammarCoverage.txt")

        coverage.save(coverageFile)
    }

    private fun getInputSize(allTerminals: List<String>): List<Int> {
        return allTerminals.map { input ->
            val inputs = input
                .split(" ")
                .filter { it.isNotEmpty() }

            inputs.size
        }
    }

    fun generateInputSize(allTerminals: List<String>, dir: Path) {
        val inputSize = getInputSize(allTerminals)
        val totalSize = inputSize.sum()

        val sb = StringBuilder()
        sb.appendln("Total input size: $totalSize")

        inputSize.forEachIndexed { idx, size ->
            sb.appendln("$idx\t$size")
        }

        val inputSizeFile = dir.resolve("inputSize.txt")
        Files.write(inputSizeFile, sb.toString().toByteArray())
    }

    fun generateSummary(inputs: List<List<String>>, allStatements: Set<Long>, dir: Path) {
        val sb = StringBuilder()
        sb.appendln("Seed\tInput Size\tGrammarReached\tGrammarMissed\tGrammarCov\tCodeReached\tCodeMissed\tCodeCov")

        val allTerminals = inputs.flatten()

        Files.list(dir)
            .filter { Files.isDirectory(it) }
            .filter { it.fileName.toString().startsWith("seed") }
            .sorted()
            .forEach { seedDir ->
                log.info("Writing seed ${seedDir.fileName} into summary")

                val grammarCoverage = getGrammarCoverage(allTerminals, seedDir)
                val codeCoverage = getCodeCoverage(allStatements, seedDir)

                val index = seedDir.fileName.toString().removePrefix("seed").toInt()
                val inputSize = getInputSize(inputs[index])

                sb.append(index)
                    .append("\t")
                    .append(inputSize.average())
                    .append("\t")
                    .append(grammarCoverage.reached.size)
                    .append("\t")
                    .append(grammarCoverage.missed.size)
                    .append("\t")
                    .append(grammarCoverage.coverage)
                    .append("\t")
                    .append(codeCoverage.reached.size)
                    .append("\t")
                    .append(codeCoverage.missed.size)
                    .append("\t")
                    .append(codeCoverage.coverage)
                    .appendln()
            }

        val summaryFile = dir.resolve("summary.txt")
        Files.write(summaryFile, sb.toString().toByteArray())
    }

    private fun calculateGrammarCoverage(inputs: List<String>, dir: Path): Result<String> {
        val allTerminals = inputs.flatMap {
            it.split(" ")
                .toList()
        }.toSet()

        val reached = getReachedTerminals(dir)
        val missing = allTerminals - reached

        val res = Result(allTerminals.toList(), reached, missing)

        check(res.coverage in 0.0..1.0) { "Expected terminal coverage between 0 and 1. Found ${res.coverage}" }

        return res
    }

    private fun Path.containsDir(dirName: String): Boolean {
        return Files.list(this)
            .toList()
            .any { Files.isDirectory(it) && it.fileName.toString() == dirName }
    }

    private fun Path.getOutputDir(rootOutputDir: Path): Path {
        val fileName = if (this.fileName.toString().startsWith("CHECK")) {
            this.fileName.toString().removePrefix("CHECK").trim()
        } else {
            this.fileName.toString()
        }

        val outputDir = rootOutputDir.resolve(fileName)

        check(Files.exists(outputDir)) { "Output dir $outputDir not found" }

        return outputDir
    }

    private fun Path.getInputs(): List<List<String>> {
        val files = getFilesInputs(this)

        check(files.isNotEmpty()) { "Input directory $this doesn't contain any input file (inputs*.txt)" }

        val data = files.map { inputFile ->
            Files.readAllLines(inputFile)
                .filter { it.isNotEmpty() }
        }

        check(data.size == 11) { "Expecting 11 seeds per app. Found ${data.size}" }

        return data
    }

    private fun Path.getSeedDirs(): List<Path> {
        val seedDirs = Files.list(this)
            .sorted()
            .filter { Files.isDirectory(it) }
            .filter { it.fileName.toString().startsWith("seed") }
            .toList()

        check(seedDirs.size == 11) { "Expecting 11 seed results. Found ${seedDirs.size}" }

        return seedDirs
    }

    private fun Path.findApkJSON(apk: String): Path {
        val jsonFile = Files.walk(this)
            .toList()
            .firstOrNull { it.fileName.toString().startsWith(apk) &&
                    it.fileName.toString().endsWith(".apk.json")
            }

        check(jsonFile != null && Files.exists(jsonFile)) { "Instrumentation file $jsonFile not found" }

        return jsonFile
    }

    private fun getTotalLOC(jsonFile: Path): Set<Long> {
        val jsonData = String(Files.readAllBytes(jsonFile))
        val jObj = JSONObject(jsonData)

        val jMap = jObj.getJSONObject("allMethods")

        return jMap.keys()
            .asSequence()
            .map { it.toLong() }
            .toSet()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val rootDir = Paths.get("/Volumes/Experiments/20-icse-regression-with-grammars")
        val inputDir = rootDir.resolve("input")
        val resultDir = rootDir.resolve("done")
        val apksDir = rootDir.resolve("apks")

        Files.list(inputDir)
            .sorted()
            .asSequence()
            .filter { Files.isDirectory(it) }
            .filterNot { it.fileName.toString().startsWith("_") }
            .filterNot { it.fileName.toString().startsWith("CHECK") }
            .filterNot { it.fileName.toString().startsWith("apks") }
            .filter { it.fileName.toString().contains("org.asdtm.fas") }
            .forEach { appInputDir ->
                try {
                    check(appInputDir.containsDir("droidmate") || appInputDir.containsDir("droidMate")) {
                        "Droidmate dir not found in $appInputDir"
                    }
                    log.debug("Processing input dir: $appInputDir")

                    val appOutputDir = appInputDir.getOutputDir(resultDir)
                    log.debug("Processing output dir: $appOutputDir")

                    val jsonFile = apksDir.findApkJSON(appInputDir.fileName.toString())
                    log.debug("Processing instrumentation file: $jsonFile")
                    val loc = getTotalLOC(jsonFile)

                    val translationTableFile = appInputDir.resolve("translationTable.txt")
                    check(Files.exists(translationTableFile)) { "Input directory $appInputDir missing translation table file" }
                    log.debug("Processing translation table file: $translationTableFile")

                    val translationTable = Files.readAllLines(translationTableFile)
                        .filter { it.isNotEmpty() }

                    val inputs = appInputDir.getInputs()
                    val originalStatements = getReachedStatements(appInputDir)

                    val seedDirs = appOutputDir.getSeedDirs()

                    val result = AppData(appInputDir.fileName.toString(), translationTable, loc, originalStatements)

                    seedDirs.forEachIndexed { idx, seedDir ->
                        val input = inputs[idx]
                        val code = calculateCodeCoverage(originalStatements, seedDir)
                        val grammar = calculateGrammarCoverage(input, seedDir)

                        result.addRun(input, grammar, code)
                    }

                    // log.info(result.toString())
                    println(result.toString())
                } catch (e: IllegalStateException) {
                    log.error("${appInputDir.fileName} - ${e.message}")
                }
            }
    }
}