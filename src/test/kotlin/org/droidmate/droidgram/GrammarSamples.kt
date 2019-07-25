package org.droidmate.droidgram

import org.droidmate.droidgram.grammar.Production

val grammarExpr = mapOf(
    Production("<start>") to setOf(
        Production("<expr>")
    ),
    Production("<expr>") to setOf(
        Production(arrayOf("<term>", "+", "<expr>")),
        Production(arrayOf("<term>", "-", "<expr>")),
        Production("<term>")
    ),
    Production("<term>") to setOf(
        Production(arrayOf("<factor>", "*", "<term>")),
        Production(arrayOf("<factor>", "/", "<term>")),
        Production("<factor>")
    ),
    Production("<factor>") to setOf(
        Production(arrayOf("+", "<factor>")),
        Production(arrayOf("-", "<factor>")),
        Production(arrayOf("(", "<expr>", ")")),
        Production(arrayOf("<integer>", ".", "<integer>", "<integer>"))
    ),
    Production("<integer>") to setOf(
        Production(arrayOf("<digit>", "<integer>")),
        Production("<digit>")
    ),
    Production("<digit>") to setOf(
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
    Production("<start>") to setOf(
        Production("<string>")
    ),
    Production("<string>") to setOf(
        Production("<letter>"),
        Production(arrayOf("<letter>", "<string>"))
    ),
    Production("<letter>") to setOf(
        Production("<plus>"),
        Production("<percent>"),
        Production("<other>")
    ),
    Production("<plus>") to setOf(
        Production("+")
    ),
    Production("<percent>") to setOf(
        Production(arrayOf("%", "<hexdigit>", "<hexdigit>"))
    ),
    Production("<hexdigit>") to setOf(
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
    Production("<other>") to setOf(
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
    Production("<start>") to setOf(
        Production("<url>")
    ),
    Production("<url>") to setOf(
        Production(arrayOf("<scheme>", "://", "<authority>", "<path>", "<query>"))
    ),
    Production("<scheme>") to setOf(
        Production("http"),
        Production("https"),
        Production("ftp"),
        Production("ftps")
    ),
    Production("<authority>") to setOf(
        Production("<host>"),
        Production(arrayOf("<host>", ":", "<port>")),
        Production(arrayOf("<userinfo>", "@", "<host>")),
        Production(arrayOf("<userinfo>", "@", "<host>", ":", "<port>"))
    ),
    // Just a few
    Production("<host>") to setOf(
        Production("cispa.saarland"),
        Production("www.google.com"),
        Production("fuzzingbook.com")
    ),
    Production("<port>") to setOf(
        Production("80"),
        Production("8080"),
        Production("<nat>")
    ),
    Production("<nat>") to setOf(
        Production("<digit>"),
        Production(arrayOf("<digit>", "<digit>"))
    ),
    Production("<digit>") to setOf(
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
    Production("<userinfo>") to setOf(
        Production("user:password")
    ),
    // Just a few
    Production("<path>") to setOf(
        Production(""),
        Production("/"),
        Production("/<id>")
    ),
    // Just a few
    Production("<id>") to setOf(
        Production("abc"),
        Production("def"),
        Production(arrayOf("x", "<digit>", "<digit>"))
    ),
    Production("<query>") to setOf(
        Production(""),
        Production(arrayOf("?", "<params>"))
    ),
    Production("<params>") to setOf(
        Production("<param>"),
        Production(arrayOf("<param>", "&", "<params>"))
    ),
    // Just a few
    Production("<param>") to setOf(
        Production(arrayOf("<id>", "=", "<id>")),
        Production(arrayOf("<id>", "=", "<nat>"))
    )
)

val grammarTitle = mapOf(
    Production("<start>") to setOf(
        Production("<title>")
    ),
    Production("<title>") to setOf(
        Production(arrayOf("<topic>", ":", "<subtopic>"))
    ),
    Production("<topic>") to setOf(
        Production("Generating Software Tests"),
        Production(arrayOf("<fuzzing-prefix>", "Fuzzing")),
        Production("The Fuzzing Book")
    ),
    Production("<fuzzing-prefix>") to setOf(
        Production(""),
        Production("The Art of "),
        Production("The Joy of ")
    ),
    Production("<subtopic>") to setOf(
        Production("<subtopic-main>"),
        Production(arrayOf("<subtopic-prefix>", "<subtopic-main>")),
        Production(arrayOf("<subtopic-main>", "<subtopic-suffix>"))
    ),
    Production("<subtopic-main>") to setOf(
        Production("Breaking Software"),
        Production("Generating Software Tests"),
        Production("Principles, Techniques and Tools")
    ),
    Production("<subtopic-prefix>") to setOf(
        Production(""),
        Production("Tools and Techniques for ")
    ),
    Production("<subtopic-suffix>") to setOf(
        Production(arrayOf(" for", "<reader-property>", "and", "<reader-property>")),
        Production(arrayOf(" for", "<software-property>", "and", "<software-property>"))
    ),
    Production("<reader-property>") to setOf(
        Production("Fun"),
        Production("Profit")
    ),
    Production("<software-property>") to setOf(
        Production("Robustness"),
        Production("Reliability"),
        Production("Security")
    )
)