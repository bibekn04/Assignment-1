package edu.sezg534.codereview.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sezg534.codereview.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LocalDeterministicLlmEngine implements LlmClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean isLiveApi() {
        return false;
    }

    @Override
    public String generateReview(ReviewRequest request) throws Exception {
        ReviewResponse response = new ReviewResponse();
        response.setIntent(request.getIntent() != null ? request.getIntent() : Intent.BOTH);
        response.setClarificationNeeded(false);

        String code = request.getCode() != null ? request.getCode() : "";
        List<Finding> findings = new ArrayList<>();
        List<RefactoringSuggestion> refactorings = new ArrayList<>();

        Quality quality = Quality.ACCEPTABLE;
        StringBuilder summaryBuilder = new StringBuilder();

        // 1. SQL Injection pattern
        if (code.contains("Statement stmt") || code.contains("SELECT * FROM users WHERE username = '\" +")) {
            findings.add(new Finding(
                    "FINDING-SEC-01",
                    Severity.CRITICAL,
                    Category.SECURITY,
                    "3-5",
                    "CWE-89: SQL Injection via String Concatenation",
                    "User input 'username' is concatenated directly into the SQL query string, enabling arbitrary SQL injection attacks.",
                    "Use PreparedStatement with parameterized query placeholders (?) instead of dynamic string concatenation."
            ));
            refactorings.add(new RefactoringSuggestion(
                    "REFACTOR-SEC-01",
                    "Convert String Concatenation to Parameterized PreparedStatement",
                    "Removes SQL injection vulnerability by enforcing strict parameter binding.",
                    "String sql = \"SELECT * FROM users WHERE username = '\" + username + \"'\";\nResultSet rs = stmt.executeQuery(sql);",
                    "String sql = \"SELECT * FROM users WHERE username = ?\";\ntry (PreparedStatement ps = conn.prepareStatement(sql)) {\n    ps.setString(1, username);\n    ResultSet rs = ps.executeQuery();\n}"
            ));
            quality = Quality.POOR;
            summaryBuilder.append("Critical SQL injection vulnerability detected in query construction. ");
        }

        // 2. Hardcoded secret / credential pattern
        if (code.contains("AWS_SECRET") || code.contains("DB_PASS") || code.contains("SuperSecret")) {
            findings.add(new Finding(
                    "FINDING-SEC-02",
                    Severity.CRITICAL,
                    Category.SECURITY,
                    "2-3",
                    "CWE-798: Use of Hardcoded Credentials",
                    "Sensitive production credentials (AWS Secret Key and Database Password) are committed directly in source code.",
                    "Externalize credentials into environment variables or a secret vault (e.g., AWS Secrets Manager, HashiCorp Vault)."
            ));
            refactorings.add(new RefactoringSuggestion(
                    "REFACTOR-SEC-02",
                    "Externalize Credentials to Environment Variables",
                    "Eliminates hardcoded secret leakage risks in version control.",
                    "private static final String AWS_SECRET = \"[REDACTED_SECRET]\";\nprivate static final String DB_PASS = \"[REDACTED_SECRET]\";",
                    "private final String awsSecret = System.getenv(\"AWS_SECRET_KEY\");\nprivate final String dbPass = System.getenv(\"DB_PASSWORD\");"
            ));
            quality = Quality.POOR;
            summaryBuilder.append("Critical hardcoded credentials detected. ");
        }

        // 3. Path Traversal
        if (code.contains("baseDir, fileName") || code.contains("/var/data/reports")) {
            findings.add(new Finding(
                    "FINDING-SEC-03",
                    Severity.CRITICAL,
                    Category.SECURITY,
                    "3",
                    "CWE-22: Path Traversal Vulnerability",
                    "Constructing File target directly with unsanitized user-supplied filename allows directory traversal via '../' sequences.",
                    "Validate and normalize the path using Path.normalize() and verify target.getCanonicalPath() starts with baseDir.getCanonicalPath()."
            ));
            refactorings.add(new RefactoringSuggestion(
                    "REFACTOR-SEC-03",
                    "Enforce Canonical Path Sandboxing",
                    "Guarantees requested file resides strictly within the designated base directory.",
                    "File target = new File(baseDir, fileName);",
                    "File target = new File(baseDir, fileName).getCanonicalFile();\nif (!target.getPath().startsWith(baseDir.getCanonicalPath())) {\n    throw new SecurityException(\"Access denied: path traversal detected\");\n}"
            ));
            quality = Quality.POOR;
            summaryBuilder.append("Severe path traversal vulnerability identified. ");
        }

        // 4. Concurrency Race Condition
        if (code.contains("visitCount") || code.contains("++visitCount")) {
            findings.add(new Finding(
                    "FINDING-CORR-01",
                    Severity.HIGH,
                    Category.CORRECTNESS,
                    "3-5",
                    "Race Condition on Shared Mutable State in Singleton Service",
                    "The primitive integer 'visitCount' is not thread-safe. Concurrent requests will cause lost updates during '++visitCount'.",
                    "Replace 'int' with java.util.concurrent.atomic.AtomicInteger and use incrementAndGet()."
            ));
            refactorings.add(new RefactoringSuggestion(
                    "REFACTOR-CORR-01",
                    "Use AtomicInteger for Thread-Safe Counter",
                    "Ensures atomic lock-free increments under concurrent multi-threaded execution.",
                    "private int visitCount = 0;\npublic int increment() { return ++visitCount; }",
                    "private final AtomicInteger visitCount = new AtomicInteger(0);\npublic int increment() { return visitCount.incrementAndGet(); }"
            ));
            quality = Quality.NEEDS_IMPROVEMENT;
            summaryBuilder.append("Thread-safety issue detected on shared mutable singleton field. ");
        }

        // 5. N+1 Performance Query
        if (code.contains("N+1") || code.contains("orderRepository.findByUserId(id)")) {
            findings.add(new Finding(
                    "FINDING-PERF-01",
                    Severity.HIGH,
                    Category.PERFORMANCE,
                    "4-6",
                    "N+1 Database Query Bottleneck in Iteration",
                    "Executing orderRepository.findByUserId inside a loop causes N separate database roundtrips, degrading response latency under scale.",
                    "Perform a single bulk batch query (e.g., findByUserIdsIn) or fetch with a SQL JOIN."
            ));
            refactorings.add(new RefactoringSuggestion(
                    "REFACTOR-PERF-01",
                    "Batch Fetch User Orders",
                    "Replaces N database calls with a single bulk fetch indexed by userId in memory.",
                    "for (Long id : userIds) {\n    User user = userRepository.findById(id);\n    List<Order> orders = orderRepository.findByUserId(id);\n    result.add(new UserDTO(user, orders));\n}",
                    "Map<Long, List<Order>> ordersByUser = orderRepository.findByUserIdIn(userIds).stream()\n    .collect(Collectors.groupingBy(Order::getUserId));\nfor (Long id : userIds) {\n    User user = userRepository.findById(id);\n    result.add(new UserDTO(user, ordersByUser.getOrDefault(id, List.of())));\n}"
            ));
            quality = Quality.NEEDS_IMPROVEMENT;
            summaryBuilder.append("Performance bottleneck: N+1 query loop identified. ");
        }

        // 6. Arrow Anti-pattern / Deep Nesting
        if (code.contains("if (o != null)") && code.contains("if (o.getItems() != null)")) {
            findings.add(new Finding(
                    "FINDING-MAINT-01",
                    Severity.MEDIUM,
                    Category.MAINTAINABILITY,
                    "2-11",
                    "Arrow Anti-Pattern: Excessive Nested Conditional Logic",
                    "Deeply nested if-statements inflate cyclomatic complexity and hinder code readability.",
                    "Invert conditional checks and use guard clauses with early returns."
            ));
            refactorings.add(new RefactoringSuggestion(
                    "REFACTOR-MAINT-01",
                    "Flatten Nested Conditionals with Guard Clauses",
                    "Reduces indentation depth and clarifies preconditions.",
                    "if (o != null) {\n    if (o.getItems() != null) {\n        ...\n        return true;\n    }\n}\nreturn false;",
                    "if (o == null || o.getItems() == null || o.getItems().isEmpty()) return false;\nif (o.getCustomer() == null || !o.getCustomer().isActive()) return false;\nreturn true;"
            ));
            quality = Quality.ACCEPTABLE;
            summaryBuilder.append("Deep nesting and high cyclomatic complexity detected. ");
        }

        // 7. Imperative Loop to Java Stream
        if (code.contains("getActiveCustomerNames") || code.contains("for (Customer c : customers)")) {
            findings.add(new Finding(
                    "FINDING-STYLE-01",
                    Severity.LOW,
                    Category.STYLE,
                    "2-7",
                    "Imperative Filtering Loop",
                    "Traditional imperative for-loops with accumulator lists can be written more declaratively using the Java Stream API.",
                    "Refactor loop to customers.stream().filter(...).map(...).toList()."
            ));
            refactorings.add(new RefactoringSuggestion(
                    "REFACTOR-STYLE-01",
                    "Convert Loop to Java Stream Pipeline",
                    "Expresses domain filtering and mapping intent clearly and concisely.",
                    "List<String> names = new ArrayList<>();\nfor (Customer c : customers) {\n    if (c.isActive() && c.getAge() >= 18) {\n        names.add(c.getName().toUpperCase());\n    }\n}\nreturn names;",
                    "return customers.stream()\n    .filter(c -> c.isActive() && c.getAge() >= 18)\n    .map(c -> c.getName().toUpperCase())\n    .toList();"
            ));
            quality = Quality.ACCEPTABLE;
            summaryBuilder.append("Imperative collection loop suitable for Stream refactoring. ");
        }

        // 8. Unhandled division by zero and poor method name
        if (code.contains("prc(int x)") || code.contains("100 / x")) {
            findings.add(new Finding(
                    "FINDING-CORR-02",
                    Severity.HIGH,
                    Category.CORRECTNESS,
                    "4",
                    "Potential Unchecked ArithmeticException: Division by Zero",
                    "Passing x=0 will trigger an unhandled ArithmeticException, crashing order processing.",
                    "Add defensive validation for x != 0 before division."
            ));
            findings.add(new Finding(
                    "FINDING-STYLE-02",
                    Severity.LOW,
                    Category.STYLE,
                    "2",
                    "Non-descriptive Method Name 'prc'",
                    "Method name 'prc' violates clean code naming conventions and obscures intent.",
                    "Rename method to a clear action verb such as 'processOrder'."
            ));
            refactorings.add(new RefactoringSuggestion(
                    "REFACTOR-CORR-02",
                    "Add Zero-Division Guard and Descriptive Method Name",
                    "Prevents runtime crashes and clarifies business purpose.",
                    "public void prc(int x) {\n    int y = 100 / x;\n    System.out.println(\"Processed: \" + y);\n}",
                    "public void processOrder(int units) {\n    if (units <= 0) {\n        throw new IllegalArgumentException(\"Units must be positive\");\n    }\n    int result = 100 / units;\n    System.out.println(\"Processed: \" + result);\n}"
            ));
            quality = Quality.NEEDS_IMPROVEMENT;
            summaryBuilder.append("Potential division by zero and non-descriptive naming detected. ");
        }

        // 9. Resource leak (unclosed file streams)
        if (code.contains("FileInputStream") && !code.contains("try (")) {
            findings.add(new Finding(
                    "FINDING-CORR-03",
                    Severity.HIGH,
                    Category.CORRECTNESS,
                    "2-3",
                    "Resource Leak: Unclosed I/O Streams",
                    "FileInputStream and FileOutputStream are instantiated without try-with-resources or finally close() blocks, causing OS file descriptor leaks.",
                    "Wrap stream allocations in a try-with-resources statement."
            ));
            refactorings.add(new RefactoringSuggestion(
                    "REFACTOR-CORR-03",
                    "Convert File Streams to Try-With-Resources",
                    "Guarantees deterministic stream closure even when I/O exceptions are thrown.",
                    "FileInputStream in = new FileInputStream(src);\nFileOutputStream out = new FileOutputStream(dest);",
                    "try (FileInputStream in = new FileInputStream(src);\n     FileOutputStream out = new FileOutputStream(dest)) {\n    // buffer operations\n}"
            ));
            quality = Quality.NEEDS_IMPROVEMENT;
            summaryBuilder.append("Resource leak detected on unclosed file streams. ");
        }

        // 10. Memory leak in static unbounded cache
        if (code.contains("EventCache") || code.contains("CACHE.put(key, val)")) {
            findings.add(new Finding(
                    "FINDING-PERF-02",
                    Severity.HIGH,
                    Category.PERFORMANCE,
                    "2-5",
                    "Unbounded Static Cache Causing Memory Leak",
                    "Static HashMap 'CACHE' stores items indefinitely without eviction policies or sizing limits, inevitably exhausting heap memory (OutOfMemoryError).",
                    "Utilize an LRU cache or bounded cache library such as Caffeine or WeakHashMap."
            ));
            refactorings.add(new RefactoringSuggestion(
                    "REFACTOR-PERF-02",
                    "Implement Bounded Cache with Eviction",
                    "Restricts memory footprint and evicts stale cache entries.",
                    "private static final Map<String, Object> CACHE = new HashMap<>();",
                    "private static final Map<String, Object> CACHE = Collections.synchronizedMap(new LinkedHashMap<>(100, 0.75f, true) {\n    @Override\n    protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {\n        return size() > 1000;\n    }\n});"
            ));
            quality = Quality.NEEDS_IMPROVEMENT;
            summaryBuilder.append("Unbounded static cache memory leak identified. ");
        }

        // 11. Clean record Money
        if (code.contains("record Money") && code.contains("Objects.requireNonNull")) {
            quality = Quality.EXCELLENT;
            findings.add(new Finding(
                    "FINDING-STYLE-03",
                    Severity.INFO,
                    Category.STYLE,
                    "1-10",
                    "Exemplary Immutable Value Object Design",
                    "The record properly enforces immutability, validates input arguments compactly, and handles currency safety.",
                    "Maintain current architectural practices."
            ));
            summaryBuilder.append("Well-structured, clean immutable record with robust input validation. ");
        }

        // 12. Underspecified Handler
        if (code.contains("class Handler") && code.contains("doSomething")) {
            findings.add(new Finding(
                    "FINDING-MAINT-02",
                    Severity.LOW,
                    Category.MAINTAINABILITY,
                    "1",
                    "Package-Private Visibility and Missing Error Handling",
                    "Class and method use default package-private visibility with no contract or logging.",
                    "Declare explicit public/private modifiers and document method contracts."
            ));
            quality = Quality.ACCEPTABLE;
            summaryBuilder.append("Generic handler with underspecified context and package-private visibility. ");
        }

        // Fallback if no specific pattern matched
        if (findings.isEmpty()) {
            findings.add(new Finding(
                    "FINDING-GEN-01",
                    Severity.INFO,
                    Category.STYLE,
                    "1",
                    "General Code Review",
                    "Code syntax inspected. No major architectural anti-patterns or critical security issues detected.",
                    "Ensure thorough unit testing and boundary validation."
            ));
            summaryBuilder.append("General code inspection completed. ");
        }

        response.setFindings(findings);
        response.setRefactoringSuggestions(refactorings);
        response.setOverallQuality(quality);
        response.setSummary(summaryBuilder.toString().trim());

        return objectMapper.writeValueAsString(response);
    }
}
