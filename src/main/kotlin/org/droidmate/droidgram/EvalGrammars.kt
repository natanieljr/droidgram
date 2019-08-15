package org.droidmate.droidgram

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

object EvalGrammars {

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

    @JvmStatic
    fun main(args: Array<String>) {

        val path = Paths.get("/Users/nataniel/Downloads/exp/e/experiments/")

        val translationTableName = "translationTable.txt".toLowerCase()

        val done = mutableListOf<String>()

        val mappings = Files.walk(path)
            .filter {
                val app = appList.firstOrNull { p -> it.toString().contains(p) }

                val res = app != null &&
                        app !in done &&
                        it.fileName.toString().toLowerCase() == translationTableName

                if (res) {
                    done.add(app!!)
                }

                res
            }.toList()

        val result = mutableMapOf<Path, Pair<Int, Int>>()

        mappings.forEach { m ->
            val grammar = Files.readAllLines(m.resolveSibling("grammar.txt")).joinToString(" ")

            val data = Files.readAllLines(m)
            val numStates = data
                .filter { it.startsWith("s") }
                .filter { grammar.contains(it.split(";").first()) }
                .count()
            val numWidgets = data
                .filter { it.startsWith("w") }
                .filter { grammar.contains(it.split(";").first()) }
                .count()

            result[m] = Pair(numStates, numWidgets)
        }

        result.entries.forEach {
            println("${it.key}\t${it.value.first}\t${it.value.second}")
        }

        val totalStates = result.values.map { it.first }.sum()
        val totalWidgets = result.values.map { it.second }.sum()

        println("Total\t$totalStates\t$totalWidgets")
    }
}