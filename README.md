# Concurrent cache

Your primary objective is to design and implement a concurrent cache system. This cache should be optimized for scenarios where the majority of operations are read-based, yet it must handle write operations efficiently and safely. It is recommended to utilize a `ReadWriteLock` for managing access to the cache.

## Requirements:

1. Implement the concurrent cache in the `mate/academy/Cache.java` class.

2. The cache should be optimized for high read concurrency. It should allow multiple read operations to proceed concurrently, while write operations should be managed to avoid conflicts and data corruption.

3. Key Methods:
   - Implement the following methods in the `Cache` class:
       - `V get(K key)`: Retrieves an item from the cache.
       - `void put(K key, V value)`: Adds or updates an item in the cache.
       - `void remove(K key)`: Removes an item from the cache.
       - `int size()`: Returns the current number of items in the cache.
   - Ensure thread safety and efficient access for these methods.
