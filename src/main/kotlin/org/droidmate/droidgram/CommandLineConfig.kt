package org.droidmate.droidgram

import com.natpryce.konfig.uriType
import com.natpryce.konfig.getValue

object CommandLineConfig {
    val inputDir by uriType
    val outputDir by uriType
}