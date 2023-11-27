package mate.academy;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 200, unit = TimeUnit.MILLISECONDS) // if you have very slow PC, please increase the current value
public class CacheTest {
    private volatile Cache<Integer, String> cache = new Cache<>();

    @BeforeEach
    void setUp() {
        cache = new Cache<>();
    }

    @RepeatedTest(1000)
    @DisplayName("Concurrent access with multiple reads and writes should maintain data consistency")
    void concurrentAccess_MultipleReadsWrites_DataConsistency() throws InterruptedException {
        // Executor service for concurrent tasks
        ExecutorService executor = Executors.newFixedThreadPool(10);
        // Perform multiple read and write operations
        for (int i = 0; i < 100; i++) {
            int key = i;
            executor.submit(() -> {
                // Write operation
                cache.put(key, "Value" + key);

                // Read operation
                String value = cache.get(key);
                Assertions.assertEquals("Value" + key, value);
            });
        }

        // Shutdown executor and await termination
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);

        // Check if all tasks completed
        Assertions.assertTrue(finished);

        // Verify cache size
        Assertions.assertEquals(100, cache.size());
    }

    @RepeatedTest(1000)
    @DisplayName("Simultaneous reads of the same value should return consistent results")
    void simultaneousReads_SameValue_ConsistentResults() throws InterruptedException {
        cache.put(1, "Value1");

        // Use a CountDownLatch to start all reading threads at the same time
        CountDownLatch startSignal = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    startSignal.await();
                    Assertions.assertEquals("Value1", cache.get(1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startSignal.countDown();
        executor.shutdown();
        Assertions.assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

    }

    @RepeatedTest(1000)
    @DisplayName("Exclusive writes should result in correct final values")
    void exclusiveWrites_MultipleThreads_CorrectValues() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            int key = i;
            executor.submit(() -> cache.put(key, "Value" + key));
        }

        executor.shutdown();
        Assertions.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

        // Verify that all values are written correctly
        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals("Value" + i, cache.get(i));
        }
    }

    @RepeatedTest(1000)
    @DisplayName("Write operation should reflect updated value after lock behavior")
    void put_AfterWriteLock_NewValueReflected() throws InterruptedException {
        cache.put(1, "InitialValue");

        CountDownLatch writeSignal = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        // Write operation in one thread
        executor.submit(() -> {
            cache.put(1, "NewValue");
            writeSignal.countDown();
        });

        // Read operation in another thread
        executor.submit(() -> {
            try {
                writeSignal.await();
                Assertions.assertEquals("NewValue", cache.get(1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        executor.shutdown();
        Assertions.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

    }

    @RepeatedTest(1000)
    @DisplayName("Clear method should result in an empty cache")
    void clear_AfterMultiplePuts_EmptyCache() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            cache.put(i, "Value" + i);
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 5; i++) {
            executor.submit(cache::clear);
        }

        executor.shutdown();
        Assertions.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

        // Check if cache is empty
        for (int i = 0; i < 10; i++) {
            Assertions.assertNull(cache.get(i));
        }

    }

    @RepeatedTest(1000)
    @DisplayName("Concurrent writes to the same key should result in one of the written values")
    void put_ConcurrentWritesToSameKey_OneOfValuesPresent() throws InterruptedException {
        final int threadCount = 100;
        final Integer key = 1;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Callable<Void>> threads = new CopyOnWriteArrayList<>();

        IntStream.range(0, threadCount).forEach(i -> threads.add(() -> {
            cache.put(key, "Thread" + i); // Overwriting the same key
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
            return null;
        }));

        executor.invokeAll(threads);

        latch.await();
        executor.shutdown();
        String value = cache.get(1);
        boolean isExpectedValue = IntStream.range(0, threadCount)
                .anyMatch(i -> value.equals("Thread" + i));
        Assertions.assertTrue(isExpectedValue, "Value was not written by any thread.");

    }

    @RepeatedTest(1000)
    @DisplayName("Mixed concurrent reads and writes should result in consistent cache state")
    void mixedOperations_ConcurrentReadsWrites_ConsistentState() throws InterruptedException {
        final int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger counter = new AtomicInteger(0);
        IntStream.range(0, threadCount).forEach(i -> executor.submit(() -> {
            if (i % 2 == 0) {
                cache.put(i, "Value" + i);
            } else {
                cache.get(i - 1);
            }
            counter.incrementAndGet();
        }));

        executor.shutdown();
        // awaitTermination means that all tasks have completed within the given timeout period.
        Assertions.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

        Assertions.assertEquals(threadCount, counter.get()); // Ensure all threads completed

    }

    @RepeatedTest(1000)
    @DisplayName("Concurrent overwrites to the same key should result in the last value written")
    void put_ConcurrentOverwrites_LastValuePresent() throws InterruptedException {
        final int iterations = 100000;
        cache.put(1, "InitialValue");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Runnable writeTask = () -> IntStream.range(0, iterations).forEach(i -> cache.put(1, "Value" + i));
        executor.submit(writeTask);
        executor.submit(writeTask);

        executor.shutdown();
        // awaitTermination means that all tasks have completed within the given timeout period.
        Assertions.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

        String finalValue = cache.get(1);
        Assertions.assertEquals("Value" + (iterations - 1), finalValue); // Check if the final value is as expected
    }
}
