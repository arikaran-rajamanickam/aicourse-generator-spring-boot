package com.challenge.q1;

import com.aicourse.model.Course;
import com.aicourse.repo.CourseRepo;
import com.auth.model.Users;
import com.auth.repo.UserRepo;
import com.search.dto.AutocompleteResponse;
import com.search.dto.ResultType;
import com.search.dto.SearchRequest;
import com.search.dto.SearchResponse;
import com.search.dto.SearchResultItem;
import com.search.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Graded behaviour suite for the Search & Discovery subsystem.
 *
 * Everything here is black box: the service is built directly with mocked
 * repositories, the in-memory index is populated once, and only the values
 * returned by the public search / autocomplete methods are asserted on.
 */
class SearchAndDiscoveryBehaviourTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.now().minusDays(4);

    // ------------------------------------------------------------------
    // paging behaviour
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a page returns as many results as the caller asked for when more matches exist")
    void searchReturnsEveryMatchingResultOnAPage() {
        SearchServiceImpl service = serviceWith(kubernetesCatalogue(12), List.of());

        SearchResponse page = service.search(request("kubernetes", 0, 10));

        assertThat(page.results())
                .as("q=kubernetes&offset=0&limit=10 matches 12 courses, so the first page must contain 10 results")
                .hasSize(10);
        assertThat(page.total())
                .as("total must report every match in the index, not just the size of the page")
                .isEqualTo(12);
    }

    @Test
    @DisplayName("walking the pages yields every match exactly once")
    void paginationVisitsEveryMatchingResultExactlyOnce() {
        SearchServiceImpl service = serviceWith(kubernetesCatalogue(12), List.of());

        SearchResponse first = service.search(request("kubernetes", 0, 10));
        SearchResponse second = service.search(request("kubernetes", 10, 10));

        assertThat(second.results())
                .as("with 12 matches and limit=10, the page at offset=10 must contain the remaining 2 results")
                .hasSize(2);

        List<Long> walked = new ArrayList<>();
        first.results().forEach(item -> walked.add(item.id()));
        second.results().forEach(item -> walked.add(item.id()));

        assertThat(walked)
                .as("paging through offset=0 then offset=10 must visit all 12 matching course ids, with no gaps and no repeats")
                .containsExactlyInAnyOrderElementsOf(expectedCourseIds(12));
    }

    @Test
    @DisplayName("a smaller page size is honoured exactly")
    void smallerPageSizeReturnsExactlyThatManyResults() {
        SearchServiceImpl service = serviceWith(kubernetesCatalogue(12), List.of());

        SearchResponse page = service.search(request("kubernetes", 0, 4));

        assertThat(page.results())
                .as("q=kubernetes&limit=4 must return exactly 4 of the 12 matching courses")
                .hasSize(4);
        assertThat(page.results())
                .extracting(SearchResultItem::id)
                .as("results on a single page must be distinct")
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("all matches are returned when the limit is larger than the number of matches")
    void searchReturnsAllMatchesWhenLimitExceedsMatchCount() {
        SearchServiceImpl service = serviceWith(kubernetesCatalogue(3), List.of());

        SearchResponse page = service.search(request("kubernetes", 0, 25));

        assertThat(page.results())
                .as("only 3 courses match, so all 3 must come back")
                .hasSize(3);
        assertThat(page.total())
                .as("total must equal the number of matching documents")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("total is the size of the whole result set, independent of the page size")
    void totalReportsAllMatchesEvenForASmallPage() {
        SearchServiceImpl service = serviceWith(kubernetesCatalogue(12), List.of());

        SearchResponse page = service.search(request("kubernetes", 0, 2));

        assertThat(page.total())
                .as("12 courses match q=kubernetes, so total must be 12 even when limit=2")
                .isEqualTo(12);
    }

    // ------------------------------------------------------------------
    // relevance ordering
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an item matching every word of the query outranks an item matching only one word")
    void resultMatchingAllQueryWordsRanksAboveResultMatchingOneWord() {
        Course bothWords = course(201L, "Java Programming: Testing And Debugging",
                "A hands on course about writing reliable software");
        Course oneWord = course(202L, "Java Basics", "");
        SearchServiceImpl service = serviceWith(List.of(bothWords, oneWord), List.of());

        SearchResponse response = service.search(request("java testing", 0, 10));

        assertThat(response.results())
                .extracting(SearchResultItem::id)
                .as("both courses contain at least one query word, so both must be returned")
                .contains(201L, 202L);
        assertThat(response.results().get(0).id())
                .as("\"Java Programming: Testing And Debugging\" matches both words of \"java testing\", "
                        + "so it must rank above \"Java Basics\", which matches only \"java\" — "
                        + "a longer description must not push a fully matching item down")
                .isEqualTo(201L);
    }

    @Test
    @DisplayName("results are ordered by how much of the query they match")
    void resultsAreOrderedByHowMuchOfTheQueryTheyMatch() {
        Course allThree = course(301L, "Spring Framework Boot And Testing Guide", "");
        Course twoOfThree = course(302L, "Spring Boot", "");
        Course oneOfThree = course(303L, "Spring", "");
        SearchServiceImpl service = serviceWith(List.of(allThree, twoOfThree, oneOfThree), List.of());

        SearchResponse response = service.search(request("spring boot testing", 0, 10));

        assertThat(response.results())
                .extracting(SearchResultItem::id)
                .as("for q=\"spring boot testing\" the course matching all three words must come first, "
                        + "then the one matching two words, then the one matching a single word")
                .startsWith(301L, 302L);
    }

    // ------------------------------------------------------------------
    // filtering / matching basics
    // ------------------------------------------------------------------

    @Test
    @DisplayName("queries are case insensitive")
    void searchMatchesRegardlessOfLetterCase() {
        SearchServiceImpl service = serviceWith(kubernetesCatalogue(3), List.of());

        SearchResponse upper = service.search(request("KUBERNETES", 0, 25));

        assertThat(upper.results())
                .as("an upper-case query must match the same documents as a lower-case one")
                .hasSize(3);
    }

    @Test
    @DisplayName("a blank query returns nothing")
    void blankQueryReturnsNoResultsAndZeroTotal() {
        SearchServiceImpl service = serviceWith(kubernetesCatalogue(3), List.of());

        SearchResponse response = service.search(request("   ", 0, 10));

        assertThat(response.results()).as("a blank query must not return results").isEmpty();
        assertThat(response.total()).as("a blank query must report a total of 0").isZero();
    }

    @Test
    @DisplayName("a query that matches no indexed term returns nothing")
    void unmatchedQueryReturnsNoResults() {
        SearchServiceImpl service = serviceWith(kubernetesCatalogue(3), List.of());

        SearchResponse response = service.search(request("quantumchromodynamics", 0, 10));

        assertThat(response.results()).as("an unknown term must not match any document").isEmpty();
        assertThat(response.total()).as("an unknown term must report a total of 0").isZero();
    }

    @Test
    @DisplayName("the type filter restricts results to the requested type")
    void typeFilterReturnsOnlyTheRequestedType() {
        SearchServiceImpl service = serviceWith(
                List.of(course(401L, "Designer Workflows", "")),
                List.of(user(7L, "designerdan", "Dan Designer")));

        SearchResponse usersOnly = service.search(
                new SearchRequest("designer", List.of(ResultType.USER), 0, 10, Set.of()));
        SearchResponse coursesOnly = service.search(
                new SearchRequest("designer", List.of(ResultType.COURSE), 0, 10, Set.of()));

        assertThat(usersOnly.results())
                .as("types=USER must return user results only")
                .isNotEmpty()
                .allMatch(item -> item.type() == ResultType.USER);
        assertThat(usersOnly.results())
                .extracting(SearchResultItem::id)
                .as("the matching user must be present when types=USER")
                .contains(7L);
        assertThat(coursesOnly.results())
                .as("types=COURSE must return course results only")
                .isNotEmpty()
                .allMatch(item -> item.type() == ResultType.COURSE);
    }

    @Test
    @DisplayName("excluded user ids are left out of the results")
    void excludedUserIdsAreNotReturned() {
        SearchServiceImpl service = serviceWith(
                List.of(course(402L, "Designer Workflows", "")),
                List.of(user(7L, "designerdan", "Dan Designer"),
                        user(8L, "designersam", "Sam Designer")));

        SearchResponse response = service.search(
                new SearchRequest("designer", List.of(ResultType.USER), 0, 10, Set.of(8L)));

        assertThat(response.results())
                .extracting(SearchResultItem::id)
                .as("excludeIds=8 must remove user 8 while keeping the other matching user")
                .contains(7L)
                .doesNotContain(8L);
    }

    // ------------------------------------------------------------------
    // autocomplete
    // ------------------------------------------------------------------

    @Test
    @DisplayName("autocomplete suggests indexed terms that share the prefix")
    void autocompleteSuggestsIndexedTermsSharingThePrefix() {
        SearchServiceImpl service = serviceWith(
                List.of(course(501L, "Java Basics", ""), course(502L, "JavaScript Essentials", "")),
                List.of());

        AutocompleteResponse response = service.autocomplete("jav", List.of(), 8, Set.of());

        assertThat(response.suggestions())
                .as("both indexed terms beginning with \"jav\" must be suggested")
                .contains("java", "javascript");
    }

    @Test
    @DisplayName("autocomplete returns nothing for a prefix that matches no indexed term")
    void autocompleteReturnsNothingForUnknownPrefix() {
        SearchServiceImpl service = serviceWith(List.of(course(503L, "Java Basics", "")), List.of());

        AutocompleteResponse response = service.autocomplete("zzz", List.of(), 8, Set.of());

        assertThat(response.suggestions()).as("an unknown prefix must yield no suggestions").isEmpty();
        assertThat(response.topResults()).as("an unknown prefix must yield no top results").isEmpty();
    }

    @Test
    @DisplayName("autocomplete fills the requested number of top results when enough matches exist")
    void autocompleteTopResultsFillTheRequestedLimit() {
        SearchServiceImpl service = serviceWith(kubernetesCatalogue(12), List.of());

        AutocompleteResponse response = service.autocomplete("kubernetes", List.of(), 5, Set.of());

        assertThat(response.topResults())
                .as("12 courses match \"kubernetes\", so a limit of 5 must produce 5 top results")
                .hasSize(5);
        assertThat(response.topResults())
                .extracting(SearchResultItem::id)
                .as("top results must be distinct")
                .doesNotHaveDuplicates();
    }

    // ------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------

    private static SearchRequest request(String query, int offset, int limit) {
        return new SearchRequest(query, Collections.emptyList(), offset, limit, Set.of());
    }

    private static SearchServiceImpl serviceWith(List<Course> courses, List<Users> users) {
        CourseRepo courseRepo = mock(CourseRepo.class);
        UserRepo userRepo = mock(UserRepo.class);
        when(courseRepo.findAll()).thenReturn(courses);
        when(userRepo.findAll()).thenReturn(users);

        SearchServiceImpl service = new SearchServiceImpl(courseRepo, userRepo);
        service.refreshAllIndices();
        return service;
    }

    /** {@code count} courses that all match the term "kubernetes" and share the same shape. */
    private static List<Course> kubernetesCatalogue(int count) {
        List<Course> courses = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            courses.add(course(100L + i, "Kubernetes Guide " + String.format("%02d", i),
                    "Cluster operations handbook"));
        }
        return courses;
    }

    private static List<Long> expectedCourseIds(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            ids.add(100L + i);
        }
        return ids;
    }

    private static Course course(long id, String title, String description) {
        Course course = new Course() {
            @Override
            public OffsetDateTime getCreatedAt() {
                return CREATED_AT;
            }
        };
        course.setId(id);
        course.setTitle(title);
        course.setDescription(description);
        course.setActive(true);
        return course;
    }

    private static Users user(long id, String username, String displayName) {
        Users user = new Users();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setCreatedAt(CREATED_AT);
        return user;
    }
}
