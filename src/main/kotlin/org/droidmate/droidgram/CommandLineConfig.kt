package org.droidmate.droidgram

import com.natpryce.konfig.booleanType
import com.natpryce.konfig.uriType
import com.natpryce.konfig.getValue
import com.natpryce.konfig.intType

object CommandLineConfig {
    val inputDir by uriType
    val seedNr by intType
    val useCoverageGrammar by booleanType
}