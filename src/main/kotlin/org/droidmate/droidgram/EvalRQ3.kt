package org.droidmate.droidgram

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList
import kotlin.system.exitProcess

class EvalRQ3 {
    companion object {
        @JvmStatic
        private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

        private val appList = """be.digitalia.fosdem_1600162
cc.echonet.coolmicapp_10
ch.bailu.aat_18
ch.fixme.status_21
cl.coders.faketraveler_6
com.adam.aslfms_48
com.alaskalinuxuser.justcraigslist_10
com.ames.books_7
com.aptasystems.dicewarepasswordgenerator_9
com.bmco.cratesiounofficial_6
com.gabm.fancyplaces_9
com.iamtrk.androidexplorer_1
com.ilm.sandwich_35
com.kgurgul.cpuinfo_40200
com.totsp.bookworm_19
com.vlille.checker_723
com.workingagenda.democracydroid_43
cz.jiriskorpil.amixerwebui_8
de.baumann.sieben_35
de.christinecoenen.code.zapp_31
de.koelle.christian.trickytripper_23
de.thecode.android.tazreader_3090203
de.vibora.viborafeed_28
edu.cmu.cs.speech.tts.flite_4
eu.siacs.conversations.legacy_258
eu.uwot.fabio.altcoinprices_78
fr.mobdev.goblim_11
fr.ybo.transportsrennes_413
github.vatsal.easyweatherdemo_11
info.metadude.android.bitsundbaeume.schedule_51
it.mn.salvi.linuxDayOSM_6
me.blog.korn123.easydiary_136
mobi.boilr.boilr_9
net.justdave.nwsweatheralertswidget_10
net.justdave.nwsweatheralertswidget_10
net.sf.times_37
net.usikkert.kouchat.android_16
org.asdtm.goodweather_13
org.fdroid.fdroid_1007002
org.fossasia.openevent_101
org.jamienicol.episodes_12
org.quantumbadger.redreader_87
org.schabi.newpipe_730
org.thosp.yourlocalweather_123""".split("\n")
            .map { it.trim() }
            .drop(1)

        private fun getTargets(appDir: Path): List<String> {
            val targetSymbols = appDir
                .resolve("input")
                // .resolve("apks")
                .resolve("targetSymbols.txt")

            return if (Files.exists(targetSymbols)) {
                Files.readAllLines(targetSymbols)
            } else {
                emptyList()
            }
        }

        private fun getInput(appDir: Path, seedNr: Int, inputNr: Int): String {
            val seedInputs = appDir
                .resolve("input")

            if (!Files.exists(seedInputs)) {
                return ""
            }

            val inputFile = Files.list(seedInputs).use { p ->
                p.toList()
                    .first { it.fileName.toString() == "symbolInputs${seedNr.toString().padStart(2, '0')}.txt" }
            }

            return Files.readAllLines(inputFile)[inputNr]
        }

        private fun getSeedDirs(outputDir: Path): List<Path> {
            return Files.list(outputDir).use { p ->
                p.filter { Files.isDirectory(it) }
                    .filter { it.fileName.toString().startsWith("seed") }
                    .sorted()
                    .toList()
            }
        }

        private fun getCodeCoverageData(seedDir: Path, inputIdx: Int): String {
            val coverageFile = seedDir
                .resolve("input$inputIdx")
                .resolve("codeCoverage.txt")
            return "<start>" + if (Files.exists(coverageFile)) {
                Files.readAllLines(coverageFile)
                    .filter { it.startsWith("Reached:") }
                    .flatMap { it.replace("Reached: ", "'").split(" ") }
            } else {
                log.warn("Missing code coverage file for input $inputIdx on $seedDir")
                emptyList()
            }.joinToString(" ")
        }

        private fun getGrammarCoverageData(seedDir: Path, inputIdx: Int): String {
            val modelDir = seedDir
                .resolve("input$inputIdx")
                .resolve("model")

            if (!Files.exists(modelDir)) {
                return ""
            }

            val grammarFile = Files.list(modelDir).use { p ->
                p.toList()
                    .first()
                    .resolve("reachedTerminals.txt")
            }

            return "<start>" + if (Files.exists(grammarFile)) {
                Files.readAllLines(grammarFile)
            } else {
                log.warn("Missing code coverage file for input $inputIdx on $seedDir")
                emptyList()
            }.joinToString(" ")
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val baseDir = Paths.get("/Users/nataniel/Downloads/exp/rq_2_3/renamed/")

            val result = StringBuilder()
            result.append("App")
                .append("\t")
                .append("Avg Input Size")
                .append("\t")
                .append("Inputs")
                .append("\t")
                .append("Success LOC")
                .append("\t")
                .append("Success Input")
                .append("\n")

            val appDirs = Files.list(baseDir).use { p ->
                p.filter { Files.isDirectory(it) }
                    .filter { it.fileName.toString() in appList }
                    .sorted()
                    .toList()
            }

            for (appDir in appDirs) {
                val appName = appDir.fileName.toString()
                val targetLOCs = getTargets(appDir)
                val numInputs = targetLOCs.size

                val outputDir = appDir
                    .resolve("rq3")

                var totalInputSize = 0
                val numSuccessLOC = mutableListOf<Int>() // 0
                val numSuccessInput = mutableListOf<Int>() // 0

                for ((inputIdx, targetLOC) in targetLOCs.withIndex()) {
                    numSuccessLOC.add(0)
                    numSuccessInput.add(0)

                    val seedDirs = getSeedDirs(outputDir)

                    for (seedDir in seedDirs) {
                        val seedName = seedDir.fileName.toString()
                        val seedNr = seedName.split("_").last().replace("seed", "").toInt()
                        val fuzzedInput = getInput(appDir, seedNr, inputIdx)

                        totalInputSize += fuzzedInput.split("<").size

                        val targetInput = fuzzedInput.split("<").last().removeSuffix(">")
                        val coveredCode = getCodeCoverageData(seedDir, inputIdx)
                        val coveredTerminals = getGrammarCoverageData(seedDir, inputIdx)

                        if (targetLOC in coveredCode) {
                            numSuccessLOC[numSuccessLOC.size - 1]++
                        }

                        if (targetInput in coveredTerminals) {
                            numSuccessInput[numSuccessInput.size - 1]++
                        }

                        /*
                        if (numSuccessInput.last() > 0 || numSuccessLOC.last() > 0) {
                            break
                        }
                        */
                    }
                }

                result.append(appName.replace(".", ","))
                    .append("\t")
                    .append((totalInputSize.toFloat() / numInputs).toString().replace(".", ","))
                    .append("\t")
                    // .append(numInputs) //  * 10)
                    .append(numInputs * 10)
                    .append("\t")
                    .append(numSuccessLOC.map { if (it > 10) 10 else it }.sum())
                    .append("\t")
                    .append(numSuccessInput.map { if (it > 10) 10 else it }.sum())
                    .append("\n")
            }

            result.forEach { line -> print(line) }

            getInputSizeRQ3(baseDir)
        }

        private fun getInputSizeRQ3(baseDir: Path) {
            val files = Files.walk(baseDir)
                .filter {
                    appList.any { p -> it.toString().contains(p) }
                    !it.fileName.toString().contains("11") &&
                    it.fileName.toString().startsWith("symbolInputs") &&
                            it.fileName.toString().endsWith(".txt")
                }
                .toList()

            val inputs = files.map {inputFile ->
                val data = Files.readAllLines(inputFile)

                Pair(data.size,
                    data.map { line ->
                        line.split("<").count()
                    }
                )
            }

            val numInputFiles = files.size
            val numInputs = inputs.map { it.first }.sum()
            val sizeInputs = inputs.map { it.second.sum() }.sum()

            println("Num input files = $numInputFiles")
            println("Num inputs (action sets) = $numInputs")
            println("Total input length (actions) = $sizeInputs")
            println("Avg input length = ${sizeInputs / numInputs.toFloat()}")
        }
    }
}