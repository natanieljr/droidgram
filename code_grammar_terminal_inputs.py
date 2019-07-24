import json
import re
import sys

from fuzzingbook.Grammars import RE_NONTERMINAL, nonterminals
from fuzzingbook.GrammarCoverageFuzzer import GrammarCoverageFuzzer


def iterative_all_terminals(tree):
    if tree is None or len(tree) < 2:
        return ''

    (symbol, children) = tree

    terminals = []

    if children is None or len(children) == 0:
        # This is a nonterminal symbol not expanded yet
        # Or This is a terminal symbol
        terminals.append(symbol)
    else:
        # This is an expanded symbol:
        # Concatenate all terminal symbols from all children
        q = [c for c in children]

        while q:
            new_tree = q.pop(0)
            (new_symbol, new_children) = new_tree

            if new_children is None or len(new_children) == 0:
                terminals.append(new_symbol)
            else:
                [q.append(c) for c in new_children]

    value = ''.join(terminals)

    return value


class TerminalCoverageGrammar(GrammarCoverageFuzzer):
    def __init__(self, *args, **kwargs):
        # invoke superclass __init__(), passing all arguments
        self.derivation_tree = []
        super().__init__(*args, **kwargs)
        self.last_symbol = ""
        self.last_symbol_count = 0
        self.non_terminal_inputs = False
        self._cache = {}
        self._symbols_seen = set()

    def expansion_key(self, symbol, expansion):
        """Convert (symbol, children) into a key. `children` can be an expansion string or a derivation tree."""
        if isinstance(expansion, tuple):
            expansion = expansion[0]
        if not isinstance(expansion, str):
            children = [((" " + x[0]).replace(" <", "<"), x[1]) for x in expansion]
            self._symbols_seen = set()
            expansion = iterative_all_terminals((symbol, children))

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

        key = (symbol, max_depth)
        if key in self._cache:
            if self.log:
                print("Using cached data for (%s, %d)" % key)
            return set(filter(lambda x: x not in self.covered_expansions, self._cache[key]))

        expansions = set()
        self._symbols_seen.add(symbol)
        queue = [symbol]

        cur_depth = 0
        elements_to_depth_increase = 1
        next_elements_to_depth_increase = 0

        while queue:
            current = queue.pop(0)
            sum = 0

            # process
            for expansion in self.grammar[current]:
                expansion_key = self.expansion_key(current, expansion)
                covered_exp = self.covered_expansions
                new_exp = filter(lambda x: x not in covered_exp, expansion_key)
                for exp in new_exp:
                    expansions.add(exp)
                for nonterminal in nonterminals(expansion):
                    if (nonterminal, cur_depth) in self._cache:
                        new_exp = filter(lambda x: x not in self.covered_expansions,
                                         self._cache[(nonterminal, cur_depth)])
                        for exp in new_exp:
                            expansions.add(exp)
                    elif nonterminal not in self._symbols_seen:
                        sum += 1
                        queue.append(nonterminal)
                        self._symbols_seen.add(nonterminal)  # moved below
            # end process

            # bookkeeping for iterative BFS with depth limiting
            next_elements_to_depth_increase += sum
            elements_to_depth_increase -= 1
            if elements_to_depth_increase == 0:
                if cur_depth > 0:
                    self._cache[(symbol, cur_depth-1)] = expansions
                cur_depth += 1
                if cur_depth >= max_depth:
                    break

                elements_to_depth_increase = next_elements_to_depth_increase
                next_elements_to_depth_increase = 0

        self._cache[key] = expansions
        return expansions

    def use_non_terminals_input(self, value):
        self.non_terminal_inputs = value

    def expansion_to_children(self, expansion):
        children = super().expansion_to_children(expansion)

        if self.non_terminal_inputs:
            correct_children = list(filter(lambda x: " " not in x[0], children))
            incorrect_children = list(filter(lambda x: " " in x[0], children))
            flattened_children = [item for sublist in map(lambda x: x[0].strip().split(" "), incorrect_children)
                                  for item in sublist]
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

    def new_coverages(self, node, possible_children):
        """Return coverage to be obtained for each child at minimum depth"""
        (symbol, children) = node
        for max_depth in range(len(self.grammar)):
            if self.log:
                print("Looking for best element in depth %d of %d" % (max_depth, len(self.grammar)))
            new_coverages = [
                self.new_child_coverage(symbol, c, max_depth)
                for c in possible_children]
            max_new_coverage = max(len(new_coverage)
                                   for new_coverage in new_coverages)
            if max_new_coverage > 0:
                # Uncovered node found
                return new_coverages

        # All covered
        return None

    def new_child_coverage(self, symbol, children, max_depth=float('inf')):
        """Return new coverage that would be obtained by expanding (symbol, children)"""
        new_cov = self._new_child_coverage(children, max_depth)
        new_exp = filter(lambda x: x not in self.covered_expansions, self.expansion_key(symbol, children))
        for exp in new_exp:
            new_cov.add(exp)
        new_cov -= self.expansion_coverage()   # -= is set subtraction
        return new_cov

    @staticmethod
    def get_empty(possible_children):
        return [idx for idx, child in enumerate(possible_children) if child[0][0] == "<empty>"][0]

    def choose_node_expansion(self, node, possible_children):
        (symbol, children) = node
        new_coverages = self.new_coverages(node, possible_children)

        if new_coverages is None:
            self._symbols_seen = set()
            # In a loop, look for empty
            if ((hasattr(self, 'derivation_tree') and
                 len(iterative_all_terminals(self.derivation_tree)) > len(self.grammar)) or
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

        for key in keys:
            if self.log:
                print("Now covered:", key)

            self.covered_expansions.add(key)

        return index_map[new_children_index]

    def fuzz(self):
        self.derivation_tree = self.fuzz_tree()
        if self.non_terminal_inputs:
            return all_non_terminals(self.derivation_tree).replace("<empty>", "")
        else:
            self._symbols_seen = set()
            return iterative_all_terminals(self.derivation_tree)

    def expand_node_randomly(self, node):
        (symbol, children) = node
        assert children is None

        if self.log:
            print("Expanding", iterative_all_terminals(node), "randomly")

        # Fetch the possible expansions from grammar...
        expansions = self.grammar[symbol]
        possible_children = [self.expansion_to_children(
            expansion) for expansion in expansions]

        # ... and select a random expansion
        index = self.choose_node_expansion(node, possible_children)
        chosen_children = possible_children[index]

        # Process children (for subclasses)
        chosen_children = self.process_chosen_children(chosen_children,
                                                       expansions[index])

        # Return with new children
        return symbol, chosen_children

    def _possible_expansions(self, node):
        (symbol, children) = node
        if children is None:
            return 1

        return sum(self._possible_expansions(c) for c in children)

    def possible_expansions(self, node):
        total = 0
        (symbol, children) = node
        if children is None:
            total += 1

        if children is not None:
            q = [c for c in children]
            while q:
                new_node = q.pop(0)
                (new_symbol, new_children) = new_node
                if new_children is None:
                    total += 1
                elif len(new_children) > 0:
                    [q.append(c) for c in new_children]

        return total

    def _any_possible_expansions(self, node):
        (symbol, children) = node
        if children is None:
            return True

        return any(self._any_possible_expansions(c) for c in children)

    def any_possible_expansions(self, node):
        (symbol, children) = node
        if children is None:
            return True

        result = False
        if children is not None:
            q = [c for c in children]
            while q and not result:
                new_node = q.pop(0)
                (new_symbol, new_children) = new_node
                if new_children is None:
                    result = True
                elif len(new_children) > 0:
                    [q.append(c) for c in new_children]

        return result


def generate_inputs(grammar, use_non_terminals):
    fuzz = TerminalCoverageGrammar(grammar, min_nonterminals=1, log=False)
    fuzz.use_non_terminals_input(use_non_terminals)
    max_exp = fuzz.max_expansion_coverage(max_depth=len(grammar))
    reached = set()

    count = 0
    while len(max_exp - fuzz.expansion_coverage()) > 0:
        original_set_diff = max_exp - fuzz.expansion_coverage()
        inp = fuzz.fuzz()
        new_set_diff = max_exp - fuzz.expansion_coverage()

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
        inputs = int(sys.argv[3])
    else:
        inputs = 10

    if len(sys.argv) >= 5:
        non_terminals = sys.argv[4] == '1' or sys.argv[4].lower() == 'true'
    else:
        non_terminals = False

    generate_experiments_inputs(input_d, package, inputs, non_terminals)
