package com.wynncraft.combination;

import com.wynncraft.AlgorithmRegistry;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import org.junit.jupiter.api.extension.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

final class CombinationTestSupport {

    private static final String ALGORITHM_PROPERTY = "algorithm";

    private CombinationTestSupport() { }

    private static List<AlgorithmRegistry.Entry> selectedAlgorithms() {
        String filter = System.getProperty(ALGORITHM_PROPERTY);
        if (filter == null || filter.isBlank()) {
            return AlgorithmRegistry.registry();
        }

        String normalizedFilter = normalize(filter);
        List<AlgorithmRegistry.Entry> entries = AlgorithmRegistry.registry()
            .stream()
            .filter(entry -> matches(entry, normalizedFilter))
            .toList();

        if (entries.isEmpty()) {
            throw new IllegalArgumentException("No algorithm matched -D" + ALGORITHM_PROPERTY + "=\"" + filter + "\"");
        }

        return entries;
    }

    private static boolean matches(AlgorithmRegistry.Entry entry, String normalizedFilter) {
        return normalize(entry.name()).contains(normalizedFilter)
            || normalize(entry.algorithm().getClass().getSimpleName()).contains(normalizedFilter)
            || normalize(entry.algorithm().getClass().getName()).contains(normalizedFilter);
    }

    private static String normalize(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    static final class InvocationContextProvider implements TestTemplateInvocationContextProvider {

        @Override
        public boolean supportsTestTemplate(ExtensionContext context) {
            return context.getTestMethod()
                .map(method -> method.isAnnotationPresent(CombinationTest.class))
                .orElse(false);
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
            List<AlgorithmRegistry.Entry> entries = selectedAlgorithms();
            SummaryTable.get(context, entries);
            return entries.stream()
                .map(InvocationContext::new);
        }

    }

    static final class InvocationContext implements TestTemplateInvocationContext {

        private final AlgorithmRegistry.Entry entry;

        private InvocationContext(AlgorithmRegistry.Entry entry) {
            this.entry = entry;
        }

        @Override
        public String getDisplayName(int invocationIndex) {
            return entry.name();
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            return List.of(
                new BeforeCombinationCallback(),
                new ParameterResolver(entry),
                new SummaryWatcher(entry.name())
            );
        }

    }

    static final class ParameterResolver implements org.junit.jupiter.api.extension.ParameterResolver {

        private final AlgorithmRegistry.Entry entry;

        private ParameterResolver(AlgorithmRegistry.Entry entry) {
            this.entry = entry;
        }

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            Class<?> type = parameterContext.getParameter().getType();
            return type == IAlgorithm.class || type == IPlayerBuilder.class;
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            Class<?> type = parameterContext.getParameter().getType();

            if (type == IAlgorithm.class) {
                return entry.algorithm();
            }

            if (type == IPlayerBuilder.class) {
                return entry.builder();
            }

            throw new ParameterResolutionException("Unsupported parameter type: " + type.getName());
        }

    }

    static final class BeforeCombinationCallback implements BeforeTestExecutionCallback {

        private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(BeforeCombinationCallback.class);

        private BeforeCombinationCallback() {

        }

        @Override
        public void beforeTestExecution(ExtensionContext context) throws Exception {
            String key = context.getRequiredTestClass().getName();
            ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
            synchronized (store) {
                if (store.get(key) != null) {
                    return;
                }

                Object testInstance = context.getRequiredTestInstance();
                Method[] methods = testInstance.getClass().getDeclaredMethods();

                for (Method method : methods) {
                    if (!method.isAnnotationPresent(BeforeCombination.class)) {
                        continue;
                    }

                    method.setAccessible(true);
                    try {
                        method.invoke(testInstance);
                    } catch (InvocationTargetException exception) {
                        Throwable cause = exception.getCause();
                        if (cause instanceof Exception checked) {
                            throw checked;
                        }

                        if (cause instanceof Error error) {
                            throw error;
                        }

                        throw exception;
                    }
                }

                store.put(key, true);
            }
        }

    }

    static final class SummaryWatcher implements TestWatcher {

        private final String algorithmName;

        private SummaryWatcher(String algorithmName) {
            this.algorithmName = algorithmName;
        }

        @Override
        public void testSuccessful(ExtensionContext context) {
            SummaryTable.get(context).pass(algorithmName);
        }

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            SummaryTable.get(context).fail(algorithmName);
        }

    }

    static final class SummaryTable implements ExtensionContext.Store.CloseableResource {

        private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(SummaryTable.class);
        private static final String KEY = "summary-table";

        private final Map<String, Counters> counts = new LinkedHashMap<>();

        private SummaryTable(List<AlgorithmRegistry.Entry> entries) {
            for (AlgorithmRegistry.Entry entry : entries) {
                counts.putIfAbsent(entry.name(), new Counters());
            }
        }

        static SummaryTable get(ExtensionContext context, List<AlgorithmRegistry.Entry> entries) {
            return context.getRoot()
                .getStore(NAMESPACE)
                .getOrComputeIfAbsent(KEY, key -> new SummaryTable(entries), SummaryTable.class);
        }

        static SummaryTable get(ExtensionContext context) {
            return get(context, selectedAlgorithms());
        }

        synchronized void pass(String algorithmName) {
            counts.get(algorithmName).pass++;
        }

        synchronized void fail(String algorithmName) {
            counts.get(algorithmName).fail++;
        }

        @Override
        public void close() {
            int width = "Algorithm".length();
            for (String name : counts.keySet()) {
                width = Math.max(width, name.length());
            }

            String border = "=".repeat(width + 28);
            System.out.println(border);
            for (Map.Entry<String, Counters> entry : counts.entrySet()) {
                Counters counter = entry.getValue();
                System.out.printf("%-" + width + "s    PASS: %d    FAIL: %d%n", entry.getKey(), counter.pass, counter.fail);
            }
            System.out.println(border);
        }

    }

    static final class Counters {
        int pass;
        int fail;
    }

}
