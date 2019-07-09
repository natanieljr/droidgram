import re
import json
import sys

from fuzzingbook.Grammars import START_SYMBOL, RE_NONTERMINAL, nonterminals
from fuzzingbook.GrammarFuzzer import all_terminals
from fuzzingbook.GrammarCoverageFuzzer import GrammarCoverageFuzzer


class TerminalCoverageGrammar(GrammarCoverageFuzzer):
    def __init__(self, *args, **kwargs):
        # invoke superclass __init__(), passing all arguments
        super().__init__(*args, **kwargs)
        self.non_terminal_inputs = False

    def use_non_terminals_input(self, value):
        self.non_terminal_inputs = value

    def expansion_to_children(self, expansion):
        children = super(GrammarCoverageFuzzer, self).expansion_to_children(expansion)

        correct_children = list(filter(lambda x: " " not in x[0], children))
        incorrect_children = list(filter(lambda x: " " in x[0], children))
        flattened_children = [item for sublist in map(lambda x: x[0].strip().split(" "), incorrect_children) for item in sublist]
        mapped_children = [("%s" % x, []) for x in flattened_children]

        return correct_children + mapped_children

    def add_coverage(self, symbol, new_children):
        """Coverage computation: number of times each symbol has been seen"""
        if self.log and symbol not in self.covered_expansions.keys():
            print("Now covered:", symbol)

        if symbol not in self.covered_expansions.keys():
            self.covered_expansions[symbol] = 0

        curr_val = self.covered_expansions[symbol]
        self.covered_expansions[symbol] = curr_val + 1

    def reset_coverage(self):
        self.covered_expansions = dict()

    def expansion_coverage(self):
        return self.covered_expansions.keys()

    def all_symbols_grammar(self):
        symbols = {START_SYMBOL}

        for symbol in self.grammar.keys():
            for expansion in self.grammar[symbol]:
                for expansion_segment in list(filter(lambda x: x != "", re.split(RE_NONTERMINAL, expansion))):
                    if self.non_terminal_inputs:
                        locs = expansion_segment.split(" ")
                        for loc in locs:
                            symbols.add(loc)
                    else:
                        symbols.add(expansion_segment)

        return set(filter(lambda x: "<" not in x, symbols))

    def _uncovered_children(self, possible_children):
        # Prefer uncovered expansions
        uncovered_children = []
        index_map = []
        for idx, child_list in enumerate(possible_children):
            for child_tuple in child_list:
                (child, grand_child) = child_tuple
                if child not in self.expansion_coverage() and child_list not in uncovered_children:
                    uncovered_children.append(child_list)
                    index_map.append(idx)

        return index_map, uncovered_children

    def _minimally_expanded(self, possible_children):
        symbol_count = dict()
        for idx, child_list in enumerate(possible_children):
            for child_tuple in child_list:
                (child, grand_child) = child_tuple

                assert (child in self.covered_expansions.keys())
                symbol_count[child] = self.covered_expansions[child]

        min_count = min(symbol_count.values())

        # Prefer least covered expansions
        uncovered_children = []
        index_map = []
        for idx, child_list in enumerate(possible_children):
            for child_tuple in child_list:
                (child, grand_child) = child_tuple
                if symbol_count[child] == min_count and child_list not in uncovered_children:
                    uncovered_children.append(child_list)
                    index_map.append(idx)

        return index_map, uncovered_children

    def process_chosen_children(self, chosen_children, expansion):
        """Add terminals to coverage."""
        for expansion_segment in list(filter(lambda x: x != "", re.split(RE_NONTERMINAL, expansion))):
            if self.non_terminal_inputs:
                locs = expansion_segment.split(" ")
                for loc in locs:
                    if loc not in nonterminals(loc):
                        self.add_coverage(loc, chosen_children)
            else:
                if expansion_segment not in nonterminals(expansion):
                    self.add_coverage(expansion_segment, chosen_children)
        return chosen_children

    def choose_node_expansion(self, node, possible_children):
        index_map, uncovered_children = self._uncovered_children(possible_children)

        if len(uncovered_children) == 0:
            # Search for least explored
            index_map, min_covered_children = self._minimally_expanded(possible_children)
            # All expansions covered the same amount of times - use superclass method
            index = self.choose_covered_node_expansion(node, min_covered_children)

            return index_map[index]

        # Select from uncovered nodes
        index = self.choose_uncovered_node_expansion(node, uncovered_children)

        return index_map[index]

    def fuzz(self):
        self.derivation_tree = self.fuzz_tree()
        if self.non_terminal_inputs:
            return all_non_terminals(self.derivation_tree).replace("<empty>", "")
        else:
            return all_terminals(self.derivation_tree)


def generate_inputs(grammar, use_non_terminals):
    fuzzer = TerminalCoverageGrammar(grammar, min_nonterminals=1, log=False)
    fuzzer.use_non_terminals_input(use_non_terminals)
    max_exp = fuzzer.all_symbols_grammar()
    reached = set()
    while len(max_exp - fuzzer.expansion_coverage()) > 0:
        # print("Missing inputs: %d of %d" % (len(max_exp - fuzzer.expansion_coverage()), len(max_exp)))
        inp = fuzzer.fuzz()
        # print("Reached %s" % inp)
        reached.add(inp)

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
    # filename = '%s/%s/grammarWithCoverage.txt' % (input_dir, package_name)
    if use_non_terminals:
        filename = '%s/%s/grammarWithCoverage.txt' % (input_dir, package_name)
    else:
        filename = '%s/%s/grammar.txt' % (input_dir, package_name)
    with open(filename) as f:
        return json.load(f)


def generate_experiments_inputs(input_dir, package_name, use_non_terminals):
    for i in range(11):
        grammar = load_grammar(input_dir, package_name, use_non_terminals)
        inputs = generate_inputs(grammar, use_non_terminals)
        save_inputs_to_file(input_dir, inputs, package_name, i, use_non_terminals)


if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise Exception("Script requires 2 arguments: (1) input directory and (2) package name [3] use non terminals (0/1)")

    input_d = sys.argv[1]
    package = sys.argv[2]

    if len(sys.argv) > 3:
        use_non_terminals = sys.argv[3] == '1' or sys.argv[3].lower() == 'true'
    else:
        use_non_terminals = False

    generate_experiments_inputs(input_d, package, use_non_terminals)
