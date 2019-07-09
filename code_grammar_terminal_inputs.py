import json
import re
import sys

from fuzzingbook.Grammars import RE_NONTERMINAL, START_SYMBOL, nonterminals
from fuzzingbook.GrammarFuzzer import all_terminals
from fuzzingbook.GrammarCoverageFuzzer import GrammarCoverageFuzzer


class TerminalCoverageGrammar(GrammarCoverageFuzzer):
    def __init__(self, *args, **kwargs):
        # invoke superclass __init__(), passing all arguments
        super().__init__(*args, **kwargs)
        self.last_symbol = ""
        self.last_symbol_count = 0
        self.non_terminal_inputs = False

    def expansion_key(self, symbol, expansion):
        """Convert (symbol, children) into a key. `children` can be an expansion string or a derivation tree."""
        if isinstance(expansion, tuple):
            expansion = expansion[0]
        if not isinstance(expansion, str):
            children = [((" " + x[0]).replace(" <", "<"), x[1]) for x in expansion]
            expansion = all_terminals((symbol, children))

        terminals = list(filter(
            lambda x: "<" not in x,
            filter(
                lambda x: x != "",
                re.split(RE_NONTERMINAL, expansion)
            )
        ))

        if self.non_terminal_inputs:
            exp = [x.strip() for y in terminals for x in y.split(" ") if x.strip() != ""]
        else:
            if len(terminals) > 1:
                raise Exception("Found terminal " + str(terminals) + " in " + expansion +
                                " from symbol " + symbol)
            elif len(terminals) > 0:
                exp = [terminals[0]]
            else:
                exp = ['']

        return exp

    def _max_expansion_coverage(self, symbol, max_depth):
        if max_depth <= 0:
            return set()

        expansions = set()
        self._symbols_seen.add(symbol)
        Q = [symbol]

        curDepth = 0
        elementsToDepthIncrease = 1
        nextElementsToDepthIncrease = 0

        while Q != []:
            current = Q.pop(0)
            sum = 0

            #process
            seen_non_terminals = []
            for expansion in self.grammar[current]:
                new_exp = self.expansion_key(current, expansion)
                for exp in new_exp:
                    expansions.add(exp)
                for nonterminal in nonterminals(expansion):
                    if nonterminal not in self._symbols_seen:
                        sum += 1
                        Q.append(nonterminal)
                        seen_non_terminals.append(nonterminal)
                        #self._symbols_seen.add(nonterminal) #moved below
            #end process

            #bookkeeping for iterative BFS with depth limiting
            nextElementsToDepthIncrease += sum
            elementsToDepthIncrease -= 1
            if (elementsToDepthIncrease == 0):
                curDepth += 1
                if curDepth >= max_depth:
                    return expansions
                elementsToDepthIncrease = nextElementsToDepthIncrease
                nextElementsToDepthIncrease = 0

            #It's safer to do it here, not in loop above, in order to avoid pruning tuples that have the same nonterminals but different terminals on same depth
            for nt in seen_non_terminals:
                self._symbols_seen.add(nt)

        return expansions

    def use_non_terminals_input(self, value):
        self.non_terminal_inputs = value

    def expansion_to_children(self, expansion):
        children = super(GrammarCoverageFuzzer, self).expansion_to_children(expansion)

        if self.non_terminal_inputs:
            correct_children = list(filter(lambda x: " " not in x[0], children))
            incorrect_children = list(filter(lambda x: " " in x[0], children))
            flattened_children = [item for sublist in map(lambda x: x[0].strip().split(" "), incorrect_children) for item in sublist]
            mapped_children = [("%s" % x, []) for x in flattened_children]

            return correct_children + mapped_children
        else:
            return children

    def add_coverage(self, symbol, new_children):
        keys = self.expansion_key(symbol, new_children)

        for key in keys:
            if self.log and key not in self.covered_expansions:
                print("Now covered:", key)
            self.covered_expansions.add(key)

    def _choose_node_expansion(self, node, possible_children):
        # Prefer uncovered expansions
        (symbol, children) = node
        uncovered_children = [c for (i, c) in enumerate(possible_children)
                              if any(map(lambda x: x not in self.covered_expansions, self.expansion_key(symbol, c)))]
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
        new_exp = self.expansion_key(symbol, children)
        for exp in new_exp:
            new_cov.add(exp)
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
        keys = self.expansion_key(symbol, new_children)

        if self.log:
            print("Now covered:", keys)

        for key in keys:
            self.covered_expansions.add(key)

        return index_map[new_children_index]

    def fuzz(self):
        self.derivation_tree = self.fuzz_tree()
        if self.non_terminal_inputs:
            return all_non_terminals(self.derivation_tree).replace("<empty>", "")
        else:
            return all_terminals(self.derivation_tree)


def generate_inputs(grammar, use_non_terminals):
    fuzzer = TerminalCoverageGrammar(grammar, min_nonterminals=1, log=True)
    fuzzer.use_non_terminals_input(use_non_terminals)
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


def all_non_terminals(tree):
    (symbol, children) = tree
    if children is None:
        # This is a nonterminal symbol not expanded yet
        return ""

    if len(children) == 0:
        # This is a terminal symbol
        return ""

    # This is an expanded symbol:
    # Concatenate all terminal symbols from all children
    return "%s %s" % (symbol, ''.join([all_non_terminals(c) for c in children]))


def save_inputs_to_file(input_dir, inputs, package_name, seed_id, use_non_terminals):
    if use_non_terminals:
        filename = '%s/%s/coverageInputs%02d.txt' % (input_dir, package_name, seed_id)
    else:
        filename = '%s/%s/inputs%02d.txt' % (input_dir, package_name, seed_id)
    print(filename)
    f = open(filename, 'w')

    for x in inputs:
        f.write(x + "\n")

    f.close()


def load_grammar(input_dir, package_name, use_non_terminals):
    if use_non_terminals:
        filename = '%s/%s/grammarWithCoverage.txt' % (input_dir, package_name)
    else:
        filename = '%s/%s/grammar.txt' % (input_dir, package_name)
    with open(filename) as f:
        return json.load(f)


def generate_experiments_inputs(input_dir, package_name, num_inputs, use_non_terminals):
    for i in range(num_inputs):
        grammar = load_grammar(input_dir, package_name, use_non_terminals)
        inputs = generate_inputs(grammar, use_non_terminals)
        save_inputs_to_file(input_dir, inputs, package_name, i, use_non_terminals)


if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise Exception("Script requires 2 arguments: "
                        "(1) input directory and "
                        "(2) package name "
                        "[3] number of input sets [optional, default=10] "
                        "[4] code grammar [optional, default=False]")

    input_d = sys.argv[1]
    package = sys.argv[2]

    if len(sys.argv) >= 4:
        num_inputs = int(sys.argv[3])
    else:
        num_inputs = 10

    if len(sys.argv) >= 5:
        use_non_terminals = sys.argv[4] == '1' or sys.argv[4].lower() == 'true'
    else:
        use_non_terminals = False

    generate_experiments_inputs(input_d, package, num_inputs, use_non_terminals)


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
