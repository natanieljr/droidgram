package org.droidmate.droidgram

import org.droidmate.droidgram.grammar.Production
import org.droidmate.droidgram.grammar.SingleValueProduction

val grammarExpr = mapOf(
    SingleValueProduction("<start>") to setOf(
        Production("<expr>")
    ),
    SingleValueProduction("<expr>") to setOf(
        Production(arrayOf("<term>", "+", "<expr>")),
        Production(arrayOf("<term>", "-", "<expr>")),
        Production("<term>")
    ),
    SingleValueProduction("<term>") to setOf(
        Production(arrayOf("<factor>", "*", "<term>")),
        Production(arrayOf("<factor>", "/", "<term>")),
        Production("<factor>")
    ),
    SingleValueProduction("<factor>") to setOf(
        Production(arrayOf("+", "<factor>")),
        Production(arrayOf("-", "<factor>")),
        Production(arrayOf("(", "<expr>", ")")),
        Production(arrayOf("<integer>", ".", "<integer>", "<integer>"))
    ),
    SingleValueProduction("<integer>") to setOf(
        Production(arrayOf("<digit>", "<integer>")),
        Production("<digit>")
    ),
    SingleValueProduction("<digit>") to setOf(
        Production("0"),
        Production("1"),
        Production("2"),
        Production("3"),
        Production("4"),
        Production("5"),
        Production("6"),
        Production("7"),
        Production("8"),
        Production("9")
    )
)

val grammarCGI = mapOf(
    SingleValueProduction("<start>") to setOf(
        Production("<string>")
    ),
    SingleValueProduction("<string>") to setOf(
        Production("<letter>"),
        Production(arrayOf("<letter>", "<string>"))
    ),
    SingleValueProduction("<letter>") to setOf(
        Production("<plus>"),
        Production("<percent>"),
        Production("<other>")
    ),
    SingleValueProduction("<plus>") to setOf(
        Production("+")
    ),
    SingleValueProduction("<percent>") to setOf(
        Production(arrayOf("%", "<hexdigit>", "<hexdigit>"))
    ),
    SingleValueProduction("<hexdigit>") to setOf(
        Production("0"),
        Production("1"),
        Production("2"),
        Production("3"),
        Production("4"),
        Production("5"),
        Production("6"),
        Production("7"),
        Production("8"),
        Production("9"),
        Production("a"),
        Production("b"),
        Production("c"),
        Production("d"),
        Production("e"),
        Production("f")
    ),
    // Actually, could be _all_ letters
    SingleValueProduction("<other>") to setOf(
        Production("0"),
        Production("1"),
        Production("2"),
        Production("3"),
        Production("4"),
        Production("5"),
        Production("a"),
        Production("b"),
        Production("c"),
        Production("d"),
        Production("e"),
        Production("-"),
        Production("_")
    )
)

val grammarURL = mapOf(
    SingleValueProduction("<start>") to setOf(
        Production("<url>")
    ),
    SingleValueProduction("<url>") to setOf(
        Production(arrayOf("<scheme>", "://", "<authority>", "<path>", "<query>"))
    ),
    SingleValueProduction("<scheme>") to setOf(
        Production("http"),
        Production("https"),
        Production("ftp"),
        Production("ftps")
    ),
    SingleValueProduction("<authority>") to setOf(
        Production("<host>"),
        Production(arrayOf("<host>", ":", "<port>")),
        Production(arrayOf("<userinfo>", "@", "<host>")),
        Production(arrayOf("<userinfo>", "@", "<host>", ":", "<port>"))
    ),
    // Just a few
    SingleValueProduction("<host>") to setOf(
        Production("cispa.saarland"),
        Production("www.google.com"),
        Production("fuzzingbook.com")
    ),
    SingleValueProduction("<port>") to setOf(
        Production("80"),
        Production("8080"),
        Production("<nat>")
    ),
    SingleValueProduction("<nat>") to setOf(
        Production("<digit>"),
        Production(arrayOf("<digit>", "<digit>"))
    ),
    SingleValueProduction("<digit>") to setOf(
        Production("0"),
        Production("1"),
        Production("2"),
        Production("3"),
        Production("4"),
        Production("5"),
        Production("6"),
        Production("7"),
        Production("8"),
        Production("9")
    ),
    // Just one
    SingleValueProduction("<userinfo>") to setOf(
        Production("user:password")
    ),
    // Just a few
    SingleValueProduction("<path>") to setOf(
        Production(""),
        Production("/"),
        Production("/<id>")
    ),
    // Just a few
    SingleValueProduction("<id>") to setOf(
        Production("abc"),
        Production("def"),
        Production(arrayOf("x", "<digit>", "<digit>"))
    ),
    SingleValueProduction("<query>") to setOf(
        Production(""),
        Production(arrayOf("?", "<params>"))
    ),
    SingleValueProduction("<params>") to setOf(
        Production("<param>"),
        Production(arrayOf("<param>", "&", "<params>"))
    ),
    // Just a few
    SingleValueProduction("<param>") to setOf(
        Production(arrayOf("<id>", "=", "<id>")),
        Production(arrayOf("<id>", "=", "<nat>"))
    )
)

val grammarTitle = mapOf(
    SingleValueProduction("<start>") to setOf(
        Production("<title>")
    ),
    SingleValueProduction("<title>") to setOf(
        Production(arrayOf("<topic>", ":", "<subtopic>"))
    ),
    SingleValueProduction("<topic>") to setOf(
        Production("Generating Software Tests"),
        Production(arrayOf("<fuzzing-prefix>", "Fuzzing")),
        Production("The Fuzzing Book")
    ),
    SingleValueProduction("<fuzzing-prefix>") to setOf(
        Production(""),
        Production("The Art of "),
        Production("The Joy of ")
    ),
    SingleValueProduction("<subtopic>") to setOf(
        Production("<subtopic-main>"),
        Production(arrayOf("<subtopic-prefix>", "<subtopic-main>")),
        Production(arrayOf("<subtopic-main>", "<subtopic-suffix>"))
    ),
    SingleValueProduction("<subtopic-main>") to setOf(
        Production("Breaking Software"),
        Production("Generating Software Tests"),
        Production("Principles, Techniques and Tools")
    ),
    SingleValueProduction("<subtopic-prefix>") to setOf(
        Production(""),
        Production("Tools and Techniques for ")
    ),
    SingleValueProduction("<subtopic-suffix>") to setOf(
        Production(arrayOf(" for", "<reader-property>", "and", "<reader-property>")),
        Production(arrayOf(" for", "<software-property>", "and", "<software-property>"))
    ),
    SingleValueProduction("<reader-property>") to setOf(
        Production("Fun"),
        Production("Profit")
    ),
    SingleValueProduction("<software-property>") to setOf(
        Production("Robustness"),
        Production("Reliability"),
        Production("Security")
    )
)