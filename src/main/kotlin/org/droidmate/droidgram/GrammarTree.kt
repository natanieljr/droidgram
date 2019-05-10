/*package org.droidmate.droidgram

data class GrammarTree(private val originalGrammar: Grammar,
                       private val initialSymbol : String = "<start>") {
    private val grammar = mutableSetOf<Node>()

    private fun Set<Node>.get(symbol: String): Node {
        return grammar.firstOrNull { it == Node(symbol) } ?: throw IllegalArgumentException("Symbol $symbol not found")
    }

    fun allTerminals(symbol : String = initialSymbol): List<Node> {
        val node = grammar.get(symbol)

        // This is a non-terminal symbol not expanded yet
        if (node.children == null) {
            return listOf(node)
        }

        // This is a terminal symbol
        if (node.children.isEmpty()) {
            return listOf(node)
        }

        // This is an expanded symbol:
        // Concatenate all terminal symbols from all children
        return (node.children.flatMap { allTerminals(it) })
    }

    fun expand(symbol: String): Node {
        originalGrammar[symbol]
    }
}
*/
    /*


# ## Checking Grammars

if __name__ == "__main__":
    print('\n## Checking Grammars')




import sys

def def_used_nonterminals(grammar, start_symbol=START_SYMBOL):
    defined_nonterminals = set()
    used_nonterminals = {start_symbol}

    for defined_nonterminal in grammar:
        defined_nonterminals.add(defined_nonterminal)
        expansions = grammar[defined_nonterminal]
        if not isinstance(expansions, list):
            print(repr(defined_nonterminal) + ": expansion is not a list",
                  sys.stderr)
            return None, None

        if len(expansions) == 0:
            print(repr(defined_nonterminal) + ": expansion list empty",
                  sys.stderr)
            return None, None

        for expansion in expansions:
            if isinstance(expansion, tuple):
                expansion = expansion[0]
            if not isinstance(expansion, str):
                print(repr(defined_nonterminal) + ": "
                      + repr(expansion) + ": not a string",
                      sys.stderr)
                return None, None

            for used_nonterminal in nonterminals(expansion):
                used_nonterminals.add(used_nonterminal)

    return defined_nonterminals, used_nonterminals

def reachable_nonterminals(grammar, start_symbol=START_SYMBOL):
    reachable = set()

    def _find_reachable_nonterminals(grammar, symbol, reachable):
        #nonlocal reachable
        reachable.add(symbol)
        for expansion in grammar.get(symbol, []):
            for nonterminal in nonterminals(expansion):
                if nonterminal not in reachable:
                    _find_reachable_nonterminals(grammar, nonterminal,
reachable)

    _find_reachable_nonterminals(grammar, start_symbol, reachable)
    return reachable

def unreachable_nonterminals(grammar, start_symbol=START_SYMBOL):
    return set(grammar.keys()) - reachable_nonterminals(grammar,
start_symbol)

def opts_used(grammar):
    used_opts = set()
    for symbol in grammar:
        for expansion in grammar[symbol]:
            used_opts |= set(exp_opts(expansion).keys())
    return used_opts

def is_valid_grammar(grammar, start_symbol=START_SYMBOL, supported_opts=None):
    defined_nonterminals, used_nonterminals = \
        def_used_nonterminals(grammar, start_symbol)
    if defined_nonterminals is None or used_nonterminals is None:
        return False

    # Do not complain about '<start>' being not used,
    # even if start_symbol is different
    if START_SYMBOL in grammar:
        used_nonterminals.add(START_SYMBOL)

    for unused_nonterminal in defined_nonterminals - used_nonterminals:
        print(repr(unused_nonterminal) + ": defined, but not used",
              sys.stderr)
    for undefined_nonterminal in used_nonterminals - defined_nonterminals:
        print(repr(undefined_nonterminal) + ": used, but not defined",
              sys.stderr)

    # Symbols must be reachable either from <start> or given start symbol
    unreachable = unreachable_nonterminals(grammar, start_symbol)
    msg_start_symbol = start_symbol
    if START_SYMBOL in grammar:
        unreachable = unreachable - \
            reachable_nonterminals(grammar, START_SYMBOL)
        if start_symbol != START_SYMBOL:
            msg_start_symbol += " or " + START_SYMBOL
    for unreachable_nonterminal in unreachable:
        print(repr(unreachable_nonterminal) + ": unreachable from " + msg_start_symbol,
              sys.stderr)

    used_but_not_supported_opts = set()
    if supported_opts is not None:
        used_but_not_supported_opts = opts_used(
            grammar).difference(supported_opts)
        for opt in used_but_not_supported_opts:
            print(
                "warning: option " +
                repr(opt) +
                " is not supported",
                sys.stderr)

    return used_nonterminals == defined_nonterminals and len(unreachable) == 0

if __name__ == "__main__":
    assert is_valid_grammar(EXPR_GRAMMAR)
    assert is_valid_grammar(CGI_GRAMMAR)
    assert is_valid_grammar(URL_GRAMMAR)


if __name__ == "__main__":
    assert is_valid_grammar(EXPR_EBNF_GRAMMAR)


if __name__ == "__main__":
    assert not is_valid_grammar({"<start>": ["<x>"], "<y>": ["1"]})


if __name__ == "__main__":
    assert not is_valid_grammar({"<start>": "123"})


if __name__ == "__main__":
    assert not is_valid_grammar({"<start>": []})


if __name__ == "__main__":
    assert not is_valid_grammar({"<start>": [1, 2, 3]})


# ## Lessons Learned

if __name__ == "__main__":
    print('\n## Lessons Learned')




# ## Next Steps

if __name__ == "__main__":
    print('\n## Next Steps')




# ## Background

if __name__ == "__main__":
    print('\n## Background')


"""

# ## Exercises

if __name__ == "__main__":
    print('\n## Exercises')




# ### Exercise 1: A JSON Grammar

if __name__ == "__main__":
    print('\n### Exercise 1: A JSON Grammar')




CHARACTERS_WITHOUT_QUOTE = (string.digits
                            + string.ascii_letters
                            + string.punctuation.replace('"', '').replace('\\', '')
                            + ' ')

JSON_EBNF_GRAMMAR = {
    "<start>": ["<json>"],

    "<json>": ["<element>"],

    "<element>": ["<ws><value><ws>"],

    "<value>": ["<object>", "<array>", "<string>", "<number>", "true", "false", "null"],

    "<object>": ["{<ws>}", "{<members>}"],

    "<members>": ["<member>(,<members>)*"],

    "<member>": ["<ws><string><ws>:<element>"],

    "<array>": ["{<ws>}", "{<elements>}"],

    "<elements>": ["<element>(,<elements>)*"],

    "<element>": ["<ws><value><ws>"],

    "<string>": ['"' + "<characters>" + '"'],

    "<characters>": srange(CHARACTERS_WITHOUT_QUOTE),

    "<number>": ["<int><frac><exp>"],

    "<int>": ["<digit>", "<onenine><digits>", "-<digits>", "-<onenine><digits>"],

    "<digits>": ["<digit>+"],

    "<digit>": ['0', "<onenine>"],

    "<onenine>": crange('1', '9'),

    "<frac>": ["", ".<digits>"],

    "<exp>": ["", "E<sign><digits>", "e<sign><digits>"],

    "<sign>": ["", '+', '-'],

    # "<ws>": srange(string.whitespace)

    "<ws>": [" "]
}

assert is_valid_grammar(JSON_EBNF_GRAMMAR)

JSON_GRAMMAR = convert_ebnf_grammar(JSON_EBNF_GRAMMAR)

if __package__ is None or __package__ == "":
    from ExpectError import ExpectError
else:
    from .ExpectError import ExpectError


if __name__ == "__main__":
    for i in range(10):
        with ExpectError():
            print(simple_grammar_fuzzer(JSON_GRAMMAR, '<object>'))


# ### Exercise 2: Finding Bugs

if __name__ == "__main__":
    print('\n### Exercise 2: Finding Bugs')




if __package__ is None or __package__ == "":
    from ExpectError import ExpectError, ExpectTimeout
else:
    from .ExpectError import ExpectError, ExpectTimeout


if __name__ == "__main__":
    with ExpectError():
        simple_grammar_fuzzer(nonterminal_grammar, log=True)


if __name__ == "__main__":
    with ExpectTimeout(1):
        for i in range(10):
            print(simple_grammar_fuzzer(expr_grammar))


# ### Exercise 3: Grammars with Regular Expressions

if __name__ == "__main__":
    print('\n### Exercise 3: Grammars with Regular Expressions')




# #### Part 1: Convert regular expressions

if __name__ == "__main__":
    print('\n#### Part 1: Convert regular expressions')




# #### Part 2: Identify and expand regular expressions

if __name__ == "__main__":
    print('\n#### Part 2: Identify and expand regular expressions')




# ### Exercise 4: Defining Grammars as Functions (Advanced)

if __name__ == "__main__":
    print('\n### Exercise 4: Defining Grammars as Functions (Advanced)')




def expression_grammar_fn():
    start = "<expr>"
    expr = "<term> + <expr>" | "<term> - <expr>"
    term = "<factor> * <term>" | "<factor> / <term>" | "<factor>"
    factor = "+<factor>" | "-<factor>" | "(<expr>)" | "<integer>.<integer>" | "<integer>"
    integer = "<digit><integer>" | "<digit>"
    digit = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'

if __name__ == "__main__":
    with ExpectError():
        expression_grammar_fn()


import ast
import inspect

if __name__ == "__main__":
    source = inspect.getsource(expression_grammar_fn)
    source


if __name__ == "__main__":
    tree = ast.parse(source)


def get_alternatives(op, to_expr=lambda o: o.s):
    if isinstance(op, ast.BinOp) and isinstance(op.op, ast.BitOr):
        return get_alternatives(op.left, to_expr) + [to_expr(op.right)]
    return [to_expr(op)]

def funct_parser(tree, to_expr=lambda o: o.s):
    return {assign.targets[0].id: get_alternatives(assign.value, to_expr)
            for assign in tree.body[0].body}

if __name__ == "__main__":
    grammar = funct_parser(tree)
    for symbol in grammar:
        print(symbol, "::=", grammar[symbol])


# #### Part 1 (a): One Single Function

if __name__ == "__main__":
    print('\n#### Part 1 (a): One Single Function')




def define_grammar(fn, to_expr=lambda o: o.s):
    source = inspect.getsource(fn)
    tree = ast.parse(source)
    grammar = funct_parser(tree, to_expr)
    return grammar

if __name__ == "__main__":
    define_grammar(expression_grammar_fn)

# #### Part 1 (b): Alternative representations

if __name__ == "__main__":
    print('\n#### Part 1 (b): Alternative representations')




def define_name(o):
    return o.id if isinstance(o, ast.Name) else o.s

def define_expr(op):
    if isinstance(op, ast.BinOp) and isinstance(op.op, ast.Add):
        r = define_expr(op.left)
        return (*r, define_name(op.right))
    return (define_name(op),)

def define_ex_grammar(fn):
    return define_grammar(fn, define_expr)

# #### Part 2: Extended Grammars

if __name__ == "__main__":
    print('\n#### Part 2: Extended Grammars')




def identifier_grammar_fn():
    identifier = idchar * (1,)
"""



     */
