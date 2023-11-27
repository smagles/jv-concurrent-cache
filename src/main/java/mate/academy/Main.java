package mate.academy;

public class Main {
    public static void main(String[] args) {
        // If you want, you may use this method to test your code during the development
        Cache<Integer, String> cache = new Cache<>();

        // Writing to cache
        cache.put(1, "A");
        cache.put(2, "B");

        // Reading from cache
        System.out.println(cache.get(1)); // Expected output: A
        System.out.println(cache.get(2)); // Expected output: B
        System.out.println(cache.get(3)); // Expected output: null

        // Clearing cache
        cache.clear();

        // Reading from empty cache
        System.out.println(cache.get(1)); // Expected output: null
    }
}
