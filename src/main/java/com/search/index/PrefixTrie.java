package com.search.index;

import java.util.*;

public class PrefixTrie {
    private final Node root = new Node();

    public void insert(String term) {
        if (term == null || term.isBlank()) {
            return;
        }
        String normalized = term.toLowerCase();
        Node current = root;
        for (char c : normalized.toCharArray()) {
            current = current.children.computeIfAbsent(c, k -> new Node());
        }
        current.isTerminal = true;
        current.hits++;
    }

    public List<String> suggest(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) {
            return Collections.emptyList();
        }
        String normalized = prefix.toLowerCase();
        Node current = root;
        for (char c : normalized.toCharArray()) {
            current = current.children.get(c);
            if (current == null) {
                return Collections.emptyList();
            }
        }
        List<Suggestion> results = new ArrayList<>();
        dfs(current, new StringBuilder(normalized), results, limit);

        // Sort at the end once instead of during every recursion step
        results.sort((a, b) -> Integer.compare(b.hits, a.hits));
        
        List<String> terms = new ArrayList<>();
        for (Suggestion suggestion : results) {
            terms.add(suggestion.term);
        }
        return terms;
    }

    private void dfs(Node node, StringBuilder builder, List<Suggestion> results, int limit) {
        if (node.isTerminal) {
            results.add(new Suggestion(builder.toString(), node.hits));
        }

        // Use a priority-aware limit if needed, but for now just find all and sort later
        // or stop if we have "enough" candidates to find the best ones.
        // We find up to 100 candidates to ensure we get the best 'limit' ones after sorting.
        if (results.size() >= Math.max(limit * 2, 50)) {
            return;
        }

        for (Map.Entry<Character, Node> entry : node.children.entrySet()) {
            builder.append(entry.getKey());
            dfs(entry.getValue(), builder, results, limit);
            builder.setLength(builder.length() - 1);
            if (results.size() >= Math.max(limit * 2, 50)) {
                break;
            }
        }
    }

    private static class Node {
        private final Map<Character, Node> children = new HashMap<>();
        private boolean isTerminal;
        private int hits = 0;
    }

    private record Suggestion(String term, int hits) {
    }
}

