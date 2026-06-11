package coffee.util;

import coffee.model.Order;
import coffee.model.OrderItem;

import java.util.*;
import java.util.stream.*;

/**
 * SalesAnalytics
 * Demonstrates: Stream terminal ops, Collectors, sorting algorithms (manual + built-in),
 *               TreeMap (BST), recursive methods, Big-O annotations
 */
public class SalesAnalytics {

    /**
     * Top N items by revenue — Stream + sorted + limit
     * Time: O(n log n) due to sorting
     */
    public static List<Map.Entry<String, Double>> topItemsByRevenue(
            List<Order> orders, int topN) {

        // Flatten all orders → all items, group by name, sum subtotals
        Map<String, Double> revenueMap = orders.stream()
                .filter(o -> o.getStatus() == Order.Status.COMPLETED)
                .flatMap(o -> o.getItems().stream())           // flatten nested lists
                .collect(Collectors.groupingBy(
                        oi -> oi.item().getName(),
                        Collectors.summingDouble(OrderItem::subtotal)
                ));

        // Sort entries by value descending, take top N
        return revenueMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    /**
     * Revenue summary stats — IntSummaryStatistics
     */
    public static DoubleSummaryStatistics orderStats(List<Order> orders) {
        return orders.stream()
                .filter(o -> o.getStatus() == Order.Status.COMPLETED)
                .mapToDouble(Order::getTotal)
                .summaryStatistics();  // count, sum, min, max, average in one pass — O(n)
    }

    /**
     * Manual insertion sort on Order list by total — O(n²)
     * Shown for contrast with Stream's Timsort below
     */
    public static List<Order> insertionSortByTotal(List<Order> orders) {
        List<Order> arr = new ArrayList<>(orders);
        // O(n²) — good for small/nearly-sorted lists
        for (int i = 1; i < arr.size(); i++) {
            Order key = arr.get(i);
            int j = i - 1;
            while (j >= 0 && arr.get(j).getTotal() > key.getTotal()) {
                arr.set(j + 1, arr.get(j));
                j--;
            }
            arr.set(j + 1, key);
        }
        return arr;
    }

    /**
     * Binary search for order by ID in a sorted list — O(log n)
     * Demonstrates divide-and-conquer recursion
     */
    public static Optional<Order> binarySearchById(List<Order> sortedOrders, int targetId) {
        return binarySearch(sortedOrders, targetId, 0, sortedOrders.size() - 1);
    }

    private static Optional<Order> binarySearch(List<Order> orders, int target, int lo, int hi) {
        if (lo > hi) return Optional.empty();                  // base case — not found
        int mid = lo + (hi - lo) / 2;                         // avoids integer overflow
        int midId = orders.get(mid).getOrderId();

        if (midId == target)  return Optional.of(orders.get(mid));
        if (midId < target)   return binarySearch(orders, target, mid + 1, hi);  // right half
        return binarySearch(orders, target, lo, mid - 1);                         // left half
    }

    /**
     * Fibonacci-based loyalty points (demonstrates memoized recursion / DP)
     * Every Nth order gives bonus points = fib(N)
     * Time without memo: O(2^n) | With memo: O(n)
     */
    public static long loyaltyBonus(int orderNumber) {
        Map<Integer, Long> memo = new HashMap<>();
        return fib(orderNumber, memo);
    }

    private static long fib(int n, Map<Integer, Long> memo) {
        if (n <= 1) return n;
        if (memo.containsKey(n)) return memo.get(n);          // O(1) cache hit
        long result = fib(n - 1, memo) + fib(n - 2, memo);
        memo.put(n, result);
        return result;
    }

    /**
     * Count categories in a TreeMap (BST-backed) — keys automatically sorted
     * TreeMap: Red-Black BST — O(log n) put/get, but iterates in key order
     */
    public static Map<String, Long> itemCountByCategory(List<Order> orders) {
        return orders.stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(
                        oi -> oi.item().getCategory(),
                        TreeMap::new,                  // BST-backed map — sorted keys
                        Collectors.counting()
                ));
    }
}
