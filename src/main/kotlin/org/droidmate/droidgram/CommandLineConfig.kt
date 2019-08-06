package org.droidmate.droidgram

import com.natpryce.konfig.uriType
import com.natpryce.konfig.getValue
import com.natpryce.konfig.intType
import com.natpryce.konfig.stringType

object CommandLineConfig {
    val inputDir by uriType
    val seedNr by intType
    val inputFilePrefix by stringType
}