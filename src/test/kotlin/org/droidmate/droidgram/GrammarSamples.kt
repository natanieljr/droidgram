package org.droidmate.droidgram

val grammarExpr = mapOf(
    "<start>" to setOf("<expr>"),
    "<expr>" to setOf("<term> + <expr>", "<term> - <expr>", "<term>"),
    "<term>" to setOf("<factor> * <term>", "<factor> / <term>", "<factor>"),
    "<factor>" to setOf("+<factor>", "-<factor>", "(<expr>)", "<integer>.<integer>", "<integer>"),
    "<integer>" to setOf("<digit><integer>", "<digit>"),
    "<digit>" to setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
)

val grammarCGI = mapOf(
    "<start>" to setOf("<string>"),
    "<string>" to setOf("<letter>", "<letter><string>"),
    "<letter>" to setOf("<plus>", "<percent>", "<other>"),
    "<plus>" to setOf("+"),
    "<percent>" to setOf("%<hexdigit><hexdigit>"),
    "<hexdigit>" to setOf("0", "1", "2", "3", "4", "5", "6", "7",
        "8", "9", "a", "b", "c", "d", "e", "f"),
    // Actually, could be _all_ letters
    "<other>" to setOf("0", "1", "2", "3", "4", "5", "a", "b", "c", "d", "e", "-", "_")
)

val grammarURL = mapOf(
    "<start>" to setOf("<url>"),
    "<url>" to setOf("<scheme>://<authority><path><query>"),
    "<scheme>" to setOf("http", "https", "ftp", "ftps"),
    "<authority>" to setOf("<host>", "<host>:<port>", "<userinfo>@<host>", "<userinfo>@<host>:<port>"),
    // Just a few
    "<host>" to setOf("cispa.saarland", "www.google.com", "fuzzingbook.com"),
    "<port>" to setOf("80", "8080", "<nat>"),
    "<nat>" to setOf("<digit>", "<digit><digit>"),
    "<digit>" to setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
    // Just one
    "<userinfo>" to setOf("user:password"),
    // Just a few
    "<path>" to setOf("", "/", "/<id>"),
    // Just a few
    "<id>" to setOf("abc", "def", "x<digit><digit>"),
    "<query>" to setOf("", "?<params>"),
    "<params>" to setOf("<param>", "<param>&<params>"),
    // Just a few
    "<param>" to setOf("<id>=<id>", "<id>=<nat>")
)

val grammarTitle = mapOf(
    "<start>" to setOf("<title>"),
    "<title>" to setOf("<topic>: <subtopic>"),
    "<topic>" to setOf("Generating Software Tests", "<fuzzing-prefix>Fuzzing", "The Fuzzing Book"),
    "<fuzzing-prefix>" to setOf("", "The Art of ", "The Joy of "),
    "<subtopic>" to setOf("<subtopic-main>",
        "<subtopic-prefix><subtopic-main>",
        "<subtopic-main><subtopic-suffix>"),
    "<subtopic-main>" to setOf("Breaking Software",
        "Generating Software Tests",
        "Principles, Techniques and Tools"),
    "<subtopic-prefix>" to setOf("", "Tools and Techniques for "),
    "<subtopic-suffix>" to setOf(" for <reader-property> and <reader-property>",
        " for <software-property> and <software-property>"),
    "<reader-property>" to setOf("Fun", "Profit"),
    "<software-property>" to setOf("Robustness", "Reliability", "Security")
)