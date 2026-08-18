package com.search.service.impl;

import com.aicourse.model.Course;
import com.aicourse.repo.CourseRepo;
import com.auth.model.Users;
import com.auth.repo.UserRepo;
import com.search.dto.*;
import com.search.index.PrefixTrie;
import com.search.model.SearchDocument;
import com.search.service.SearchService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private final CourseRepo courseRepo;
    private final UserRepo userRepo;
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(SearchServiceImpl.class.getName());

    private final Map<String, SearchDocument> documents = new HashMap<>();
    private final Map<String, Set<String>> invertedIndex = new HashMap<>();
    private final PrefixTrie trie = new PrefixTrie();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public SearchServiceImpl(CourseRepo courseRepo, UserRepo userRepo) {
        this.courseRepo = courseRepo;
        this.userRepo = userRepo;
    }

    @PostConstruct
    public void initialize() {
        refreshAllIndices();
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            return new SearchResponse(Collections.emptyList(), 0);
        }

        List<String> tokens = tokenize(request.getQuery());
        if (tokens.isEmpty()) {
            return new SearchResponse(Collections.emptyList(), 0);
        }

        Set<ResultType> typeFilter = new HashSet<>(request.getTypes());
        Set<Long> excludeUsers = new HashSet<>(request.getExcludeUserIds());

        lock.readLock().lock();
        try {
            LOGGER.log(java.util.logging.Level.INFO, "Searching for: {0} with tokens: {1} types: {2}",
                    new Object[]{request.getQuery(), tokens, typeFilter});
            Set<String> candidateKeys = new HashSet<>();
            for (String token : tokens) {
                Set<String> keys = invertedIndex.get(token);
                if (keys != null) {
                    candidateKeys.addAll(keys);
                }
            }

            List<SearchResultItem> scored = new ArrayList<>();
            OffsetDateTime now = OffsetDateTime.now();

            for (String key : candidateKeys) {
                SearchDocument doc = documents.get(key);
                if (doc == null) {
                    continue;
                }
                if (!typeFilter.isEmpty() && !typeFilter.contains(doc.getType())) {
                    continue;
                }
                if (doc.getType() == ResultType.USER && excludeUsers.contains(doc.getId())) {
                    continue; // skip users that were already picked
                }
                double score = computeScore(doc, tokens, now);
                scored.add(new SearchResultItem(doc.getId(), doc.getType(), doc.getTitle(), doc.getDescription(), score, doc.getHandle()));
            }

            scored.sort((a, b) -> {
                int scoreCompare = Double.compare(b.score(), a.score());
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
                return a.label().compareToIgnoreCase(b.label());
            });

            int offset = request.getOffset();
            int limit = request.getLimit();
            int from = Math.min(offset, scored.size());
            int to = Math.min(from + limit - 1, scored.size());
            List<SearchResultItem> paged = scored.subList(from, to);

            LOGGER.log(java.util.logging.Level.INFO, "Found {0} total results, returning {1} results.",
                    new Object[]{scored.size(), paged.size()});
            return new SearchResponse(paged, scored.size());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public AutocompleteResponse autocomplete(String prefix, List<ResultType> types, int limit, Set<Long> excludeUserIds) {
        int resolvedLimit = Math.max(1, Math.min(limit, 20));
        Set<ResultType> typeFilter = types == null ? Collections.emptySet() : new HashSet<>(types);

        lock.readLock().lock();
        try {
            List<String> suggestions = trie.suggest(prefix, resolvedLimit);
            LOGGER.log(java.util.logging.Level.INFO, "Autocomplete for prefix: {0} suggestions: {1}",
                    new Object[]{prefix, suggestions});
            SearchRequest quickRequest = new SearchRequest(prefix, new ArrayList<>(typeFilter), 0, resolvedLimit, excludeUserIds);
            SearchResponse quickResults = search(quickRequest);
            return new AutocompleteResponse(suggestions, quickResults.results());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void refreshAllIndices() {
        lock.writeLock().lock();
        try {
            documents.clear();
            invertedIndex.clear();

            for (Course course : courseRepo.findAll()) {
                if (!course.isActive()) {
                    continue;
                }
                indexCourse(course);
            }

            for (Users user : userRepo.findAll()) {
                indexUser(user);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void indexCourse(Course course) {
        String title = safe(course.getTitle());
        String description = safe(course.getDescription());
        Set<String> tokens = tokenize(title + " " + description).stream().collect(Collectors.toSet());
        double popularityWeight = 1.0;

        SearchDocument doc = new SearchDocument(
                course.getId(),
                ResultType.COURSE,
                title,
                description,
                null,
                course.getCreatedAt(),
                popularityWeight,
                tokens
        );
        indexDocument(doc);
    }

    private void indexUser(Users user) {
        String handle = safe(user.getUsername());
        String displayName = safe(user.getDisplayName());
        String userIdStr = String.valueOf(user.getId());
        
        if (displayName.isBlank()) {
            displayName = handle;
        }
        String description = handle.isBlank() ? "User" : "@" + handle;

        // Include ID in tokens so users can be searched by their numeric ID as well
        String combined = displayName + " " + handle + " " + userIdStr;
        Set<String> tokens = tokenize(combined).stream().collect(Collectors.toSet());
        double popularityWeight = 0.5;

        LOGGER.log(java.util.logging.Level.INFO, "Indexing user: {0} ({1}) ID: {2} with tokens: {3}",
                new Object[]{displayName, handle, userIdStr, tokens});

        SearchDocument doc = new SearchDocument(
                user.getId(),
                ResultType.USER,
                displayName,
                description,
                handle,
                user.getCreatedAt(),
                popularityWeight,
                tokens
        );
        indexDocument(doc);
    }

    private void indexDocument(SearchDocument doc) {
        documents.put(doc.getKey(), doc);
        for (String token : doc.getTokens()) {
            invertedIndex.computeIfAbsent(token, k -> new HashSet<>()).add(doc.getKey());
            trie.insert(token);
        }
    }

    private List<String> tokenize(String input) {
        if (input == null || input.isBlank()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = Character.toLowerCase(input.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '_') {
                currentToken.append(c);
            } else {
                if (currentToken.length() >= 2) {
                    tokens.add(currentToken.toString());
                }
                currentToken.setLength(0);
            }
        }

        if (currentToken.length() >= 2) {
            tokens.add(currentToken.toString());
        }

        return tokens;
    }

    private double computeScore(SearchDocument doc, List<String> queryTokens, OffsetDateTime now) {
        if (doc.getTokens().isEmpty()) {
            return 0.0;
        }

        int matches = 0;
        for (String token : queryTokens) {
            if (doc.getTokens().contains(token)) {
                matches++;
            }
        }

        if (matches == 0) return 0.0;

        double coverage = matches / (double) doc.getTokens().size();

        // Time decay: 1.0 for now, decaying to 0.5 over 30 days
        long daysOld = ChronoUnit.DAYS.between(doc.getCreatedAt(), now);
        double recency = 1.0 / (1.0 + (Math.max(0, daysOld) / 30.0));

        // Exact match bonus for handles or labels
        double exactBonus = 0.0;
        String rawQuery = String.join(" ", queryTokens).toLowerCase();
        if (doc.getHandle() != null && doc.getHandle().toLowerCase().contains(rawQuery) || doc.getTitle().toLowerCase().contains(rawQuery)) {
            exactBonus = 0.5;
        }

        return (coverage * 0.6) + (recency * 0.2) + (doc.getPopularityWeight() * 0.2) + exactBonus;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
