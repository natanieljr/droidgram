package org.droidmate.droidgram

import org.droidmate.droidgram.grammar.Production
import org.droidmate.droidgram.grammar.SingleValueProduction

val grammarExpr = mapOf(
    SingleValueProduction("<start>") to setOf(
        Production("<expr>")
    ),
    SingleValueProduction("<expr>") to setOf(
        Production("<term>", "+", "<expr>"),
        Production("<term>", "-", "<expr>"),
        Production("<term>")
    ),
    SingleValueProduction("<term>") to setOf(
        Production("<factor>", "*", "<term>"),
        Production("<factor>", "/", "<term>"),
        Production("<factor>")
    ),
    SingleValueProduction("<factor>") to setOf(
        Production("+", "<factor>"),
        Production("-", "<factor>"),
        Production("(", "<expr>", ")"),
        Production("<integer>", ".", "<integer>", "<integer>")
    ),
    SingleValueProduction("<integer>") to setOf(
        Production("<digit>", "<integer>"),
        Production("<digit>")
    ),
    SingleValueProduction("<digit>") to setOf(
        Production("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
    )
)

val grammarCGI = mapOf(
    SingleValueProduction("<start>") to setOf(
        Production("<string>")
    ),
    SingleValueProduction("<string>") to setOf(
        Production("<letter>"),
        Production("<letter>", "<string>")
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
        Production("%", "<hexdigit>", "<hexdigit>")
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
        Production("<scheme>", "://", "<authority>", "<path>", "<query>")
    ),
    SingleValueProduction("<scheme>") to setOf(
        Production("http"),
        Production("https"),
        Production("ftp"),
        Production("ftps")
    ),
    SingleValueProduction("<authority>") to setOf(
        Production("<host>"),
        Production("<host>", ":", "<port>"),
        Production("<userinfo>", "@", "<host>"),
        Production("<userinfo>", "@", "<host>", ":", "<port>")
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
        Production("<digit>", "<digit>")
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
        Production("x", "<digit>", "<digit>")
    ),
    SingleValueProduction("<query>") to setOf(
        Production(""),
        Production("?", "<params>")
    ),
    SingleValueProduction("<params>") to setOf(
        Production("<param>"),
        Production("<param>", "&", "<params>")
    ),
    // Just a few
    SingleValueProduction("<param>") to setOf(
        Production("<id>", "=", "<id>"),
        Production("<id>", "=", "<nat>")
    )
)

val grammarTitle = mapOf(
    SingleValueProduction("<start>") to setOf(
        Production("<title>")
    ),
    SingleValueProduction("<title>") to setOf(
        Production("<topic>", ":", "<subtopic>")
    ),
    SingleValueProduction("<topic>") to setOf(
        Production("Generating Software Tests"),
        Production("<fuzzing-prefix>", "Fuzzing"),
        Production("The Fuzzing Book")
    ),
    SingleValueProduction("<fuzzing-prefix>") to setOf(
        Production(""),
        Production("The Art of "),
        Production("The Joy of ")
    ),
    SingleValueProduction("<subtopic>") to setOf(
        Production("<subtopic-main>"),
        Production("<subtopic-prefix>", "<subtopic-main>"),
        Production("<subtopic-main>", "<subtopic-suffix>")
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
        Production(" for", "<reader-property>", "and", "<reader-property>"),
        Production(" for", "<software-property>", "and", "<software-property>")
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