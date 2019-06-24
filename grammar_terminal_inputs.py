import json
import re
import sys

from fuzzingbook.Grammars import RE_NONTERMINAL, START_SYMBOL, nonterminals
from fuzzingbook.GrammarFuzzer import all_terminals
from fuzzingbook.GrammarCoverageFuzzer import GrammarCoverageFuzzer

recursion_limit = sys.getrecursionlimit()


def get_stack_size():
    """Get stack size for caller's frame.

    %timeit len(inspect.stack())
    8.86 ms ± 42.5 µs per loop (mean ± std. dev. of 7 runs, 100 loops each)
    %timeit get_stack_size()
    4.17 µs ± 11.5 ns per loop (mean ± std. dev. of 7 runs, 100000 loops each)
    """
    size = 2  # current frame and caller's frame always exist
    while True:
        try:
            sys._getframe(size)
            size += 1
        except ValueError:
            return size - 1  # subtract current frame


def expansion_key(symbol, expansion):
    """Convert (symbol, children) into a key. `children` can be an expansion string or a derivation tree."""
    if isinstance(expansion, tuple):
        expansion = expansion[0]
    if not isinstance(expansion, str):
        children = expansion
        expansion = all_terminals((symbol, children))

    terminals = list(filter(
        lambda x: "<" not in x,
        filter(
            lambda x: x != "",
            re.split(RE_NONTERMINAL, expansion)
        )
    ))

    if len(terminals) > 1:
        raise Exception("Found terminal " + str(terminals) + " in " + expansion +
                        " from symbol " + symbol)
    elif len(terminals) > 0:
        exp = terminals[0]
    else:
        exp = ''

    return exp


class TerminalCoverageGrammar(GrammarCoverageFuzzer):
    def __init__(self, *args, **kwargs):
        # invoke superclass __init__(), passing all arguments
        super().__init__(*args, **kwargs)
        self.last_symbol = ""
        self.last_symbol_count = 0

    def any_possible_expansions(self, node):
        # Take care with recursion level
        if get_stack_size() >= recursion_limit:
            return False

        (symbol, children) = node
        if children is None:
            return True

        return any(self.any_possible_expansions(c) for c in children)

    def _max_expansion_coverage(self, symbol, max_depth):
        # Take care with recursion level
        if (max_depth <= 0) or (get_stack_size() >= recursion_limit):
            return set()

        self._symbols_seen.add(symbol)

        expansions = set()
        for expansion in self.grammar[symbol]:
            expansions.add(expansion_key(symbol, expansion))
            for nonterminal in nonterminals(expansion):
                if nonterminal not in self._symbols_seen:
                    expansions |= self._max_expansion_coverage(
                        nonterminal, max_depth - 1)

        return expansions

    def add_coverage(self, symbol, new_children):
        key = expansion_key(symbol, new_children)

        if self.log and key not in self.covered_expansions:
            print("Now covered:", key)
        self.covered_expansions.add(key)

    def _choose_node_expansion(self, node, possible_children):
        # Prefer uncovered expansions
        (symbol, children) = node
        uncovered_children = [c for (i, c) in enumerate(possible_children)
                              if expansion_key(symbol, c) not in self.covered_expansions]
        index_map = [i for (i, c) in enumerate(possible_children)
                     if c in uncovered_children]

        if len(uncovered_children) == 0:
            # All expansions covered - use superclass method
            return self.choose_covered_node_expansion(node, possible_children)

        # Select from uncovered nodes
        index = self.choose_uncovered_node_expansion(node, uncovered_children)

        return index_map[index]

    def new_child_coverage(self, symbol, children, max_depth=float('inf')):
        """Return new coverage that would be obtained by expanding (symbol, children)"""
        new_cov = self._new_child_coverage(children, max_depth)
        new_cov.add(expansion_key(symbol, children))
        new_cov -= self.expansion_coverage()   # -= is set subtraction
        return new_cov

    def get_empty(self, possible_children):
        return [idx for idx, child in enumerate(possible_children) if child[0][0] == "<empty>"][0]

    def choose_node_expansion(self, node, possible_children):
        (symbol, children) = node
        new_coverages = self.new_coverages(node, possible_children)

        if new_coverages is None:
            # In a loop, look for empty
            if ((hasattr(self, 'derivation_tree') and len(all_terminals(self.derivation_tree)) > len(self.grammar)) or
                    (len(self.covered_expansions) >= len(self.grammar))) and "<empty>" in str(possible_children):
                return self.get_empty(possible_children)
            else:
                # All expansions covered - use superclass method
                return self.choose_covered_node_expansion(node, possible_children)

        if node == self.last_symbol:
            if self.last_symbol_count < 3:
                self.last_symbol_count += 1
            elif "<empty>" in str(possible_children):
                return self.get_empty(possible_children)
        else:
            self.last_symbol = node
            self.last_symbol_count = 0

        max_new_coverage = max(len(cov) for cov in new_coverages)

        children_with_max_new_coverage = [c for (i, c) in enumerate(possible_children)
                                          if len(new_coverages[i]) == max_new_coverage]
        index_map = [i for (i, c) in enumerate(possible_children)
                     if len(new_coverages[i]) == max_new_coverage]

        # Select a random expansion
        new_children_index = self.choose_uncovered_node_expansion(
            node, children_with_max_new_coverage)
        new_children = children_with_max_new_coverage[new_children_index]

        # Save the expansion as covered
        key = expansion_key(symbol, new_children)

        if self.log:
            print("Now covered:", key)
        self.covered_expansions.add(key)

        return index_map[new_children_index]


def generate_inputs(grammar):
    fuzzer = TerminalCoverageGrammar(grammar, min_nonterminals=1, log=False)
    max_exp = fuzzer.max_expansion_coverage(max_depth=len(grammar))
    reached = set()

    count = 0
    while len(max_exp - fuzzer.expansion_coverage()) > 0:
        original_set_diff = max_exp - fuzzer.expansion_coverage()
        inp = fuzzer.fuzz()
        new_set_diff = max_exp - fuzzer.expansion_coverage()

        if len(original_set_diff - new_set_diff) > 0:
            count = 0
            reached.add(inp)
        elif original_set_diff == new_set_diff:
            count += 1
            if count > 3:
                print("Unable to produce %d elements with the grammar (%.2f)" %
                      (len(new_set_diff), len(new_set_diff) / len(max_exp)))
                break

    return reached


def save_inputs_to_file(input_dir, inputs, package_name, seed_id):
    filename = '%s/%s/inputs%02d.txt' % (input_dir, package_name, seed_id)
    print(filename)
    f = open(filename, 'w')

    for x in inputs:
        f.write(x + "\n")

    f.close()


def load_grammar(input_dir, package_name):
    filename = '%s/%s/grammar.txt' % (input_dir, package_name)
    with open(filename) as f:
        return json.load(f)


def generate_experiments_inputs(input_dir, package_name, num_inputs):
    for i in range(num_inputs):
        grammar = load_grammar(input_dir, package_name)
        inputs = generate_inputs(grammar)
        save_inputs_to_file(input_dir, inputs, package_name, i)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise Exception("Script requires 2 arguments: (1) input directory and (2) package name (3) number of input "
                        "sets [optional, default=10]")

    input_d = sys.argv[1]
    package = sys.argv[2]

    if len(sys.argv) >= 4:
        num_inputs = int(sys.argv[3])
    else:
        num_inputs = 10

    generate_experiments_inputs(input_d, package, num_inputs)


"""
grammar = {
    "<start>":
        ["<expr>"],

    "<expr>":
        ["<term> + <expr>", "<term> - <expr>", "<term>"],

    "<term>":
        ["<factor> * <term>", "<factor> / <term>", "<factor>"],

    "<factor>":
        ["+<factor>",
         "-<factor>",
         "(<expr>)",
         "<integer>.<integer>",
         "<integer>"],

    "<integer>":
        ["<digit><integer>", "<digit>"],

    "<digit>":
        ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"]
}




def load_grammar(input_dir, package_name):
    filename = '%s/%s/grammar.txt' % (input_dir, package_name)
    with open(filename) as f:
        return json.load(f)


grammar = load_grammar(".", "test")

fuzzer = TerminalCoverageGrammar(grammar, min_nonterminals=1, log=True)
reached = set()
while len(fuzzer.max_expansion_coverage() - fuzzer.expansion_coverage()) > 0:
    inp = fuzzer.fuzz()
    reached.add(inp)
    print(inp)


print("\n\n\n\n\n")
print(len(reached))
[print(x) for x in reached]
"""
