package coffee;

import coffee.exception.CoffeeShopException;
import coffee.model.*;
import coffee.repository.MenuRepository;
import coffee.repository.OrderQueue;
import coffee.service.CouponService;
import coffee.service.OrderService;
import coffee.service.VirtualThreadBarista;
import coffee.util.FileLogger;
import coffee.util.SalesAnalytics;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class BaristaUI {

    private final OrderService  orderService;
    private final CouponService couponService;
    private final FileLogger    logger;
    private final Scanner       scanner;
    private final List<Order>   sessionOrders = new ArrayList<>();

    public BaristaUI(OrderService orderService, CouponService couponService,
                     FileLogger logger, Scanner scanner) {
        this.orderService  = orderService;
        this.couponService = couponService;
        this.logger        = logger;
        this.scanner       = scanner;
    }

    public void run() {
        System.out.println("\n  BREW & BEAN  /  barista\n");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("").trim();
            running = switch (choice) {
                case "1"      -> { showQueue();         yield true; }
                case "2"      -> { processNext();       yield true; }
                case "3"      -> { cancelOrder();       yield true; }
                case "4"      -> { showStock();         yield true; }
                case "5"      -> { restock();           yield true; }
                case "6"      -> { manageCoupons();     yield true; }
                case "7"      -> { showAnalytics();     yield true; }
                case "8"      -> { logger.printLog();   yield true; }
                case "9"      -> { threadDemo();        yield true; }
                case "q", "Q" -> false;
                default       -> { System.out.println("  invalid"); yield true; }
            };
        }
    }

    // Queue

    private void showQueue() {
        OrderQueue q = orderService.getOrderQueue();
        System.out.println();
        System.out.printf("  pending: %d   completed: %d   total: %d%n",
                q.pendingCount(), q.completedCount(), q.totalCount());
        q.peek().ifPresentOrElse(
                o -> System.out.printf("  next:  #%d  %s  ($%.2f)%n",
                        o.getOrderId(), o.getCustomerName(), o.getTotal()),
                () -> System.out.println("  queue empty"));
        q.lastCompleted().ifPresent(o ->
                System.out.printf("  last completed:  #%d  %s%n",
                        o.getOrderId(), o.getCustomerName()));
    }

    private void processNext() {
        orderService.processNext().ifPresentOrElse(order -> {
            sessionOrders.add(order);
            System.out.println(order.getReceipt());
            order.getItems().forEach(oi -> System.out.println("  " + oi.item().prepareDescription()));
            System.out.println();
            prompt("press enter when ready");
            orderService.markReady(order);
            orderService.completeOrder(order, logger);
            System.out.println("  order #" + order.getOrderId() + " done — call " + order.getCustomerName());
        }, () -> System.out.println("  nothing in queue"));
    }

    private void cancelOrder() {
        String input = prompt("order id").trim();
        try {
            int id = Integer.parseInt(input);
            orderService.getOrderQueue().findById(id).ifPresentOrElse(order -> {
                if (order.getStatus() == Order.Status.COMPLETED) {
                    System.out.println("  already completed, cannot cancel");
                    return;
                }
                System.out.println(order.getReceipt());
                if (prompt("cancel? y/n").trim().equalsIgnoreCase("y")) {
                    orderService.cancelOrder(order, logger);
                    System.out.println("  cancelled — stock restored");
                }
            }, () -> System.out.println("  order not found"));
        } catch (NumberFormatException e) {
            System.out.println("  invalid id");
        }
    }

    // Stock

    private void showStock() {
        System.out.println();
        orderService.getMenuRepo().findAll().stream()
            .collect(Collectors.groupingBy(MenuItem::getCategory,
                    LinkedHashMap::new, Collectors.toList()))
            .forEach((cat, items) -> {
                System.out.println("  " + cat.toLowerCase());
                items.stream()
                     .sorted(Comparator.comparing(MenuItem::getId))
                     .forEach(i -> System.out.println("  " + i));
                System.out.println();
            });
    }

    private void restock() {
        showStock();
        String itemId = prompt("item id").trim().toUpperCase();
        orderService.getMenuRepo().findById(itemId).ifPresentOrElse(item -> {
            System.out.printf("  %s — current stock: %d%n", item.getName(), item.getStockCount());
            try {
                int qty = Integer.parseInt(prompt("add qty").trim());
                item.restock(qty);
                System.out.printf("  restocked — new stock: %d%n", item.getStockCount());
                logger.logRestock(item.getName(), qty);
            } catch (NumberFormatException e) {
                System.out.println("  invalid qty");
            } catch (IllegalArgumentException e) {
                System.out.println("  " + e.getMessage());
            }
        }, () -> System.out.println("  item not found"));
    }

    // Coupons

    private void manageCoupons() {
        System.out.println();
        System.out.println("  coupons");
        System.out.println("  1  create");
        System.out.println("  2  list all");
        System.out.println("  3  end coupon early");
        System.out.println("  any  back");
        switch (prompt("").trim()) {
            case "1" -> createCoupon();
            case "2" -> listCoupons();
            case "3" -> deactivateCoupon();
        }
    }

    private void createCoupon() {
        String code = prompt("code").trim();
        if (code.isBlank()) { System.out.println("  code required"); return; }
        if (couponService.exists(code)) { System.out.println("  code already exists"); return; }

        int pct;
        try {
            pct = Integer.parseInt(prompt("percent off (1-100)").trim());
        } catch (NumberFormatException e) {
            System.out.println("  invalid number");
            return;
        }

        // Max uses
        int maxUses;
        String usesInput = prompt("max uses  (leave blank = unlimited)").trim();
        if (usesInput.isBlank()) {
            maxUses = Coupon.UNLIMITED;
        } else {
            try {
                maxUses = Integer.parseInt(usesInput);
                if (maxUses < 1) { System.out.println("  must be at least 1"); return; }
            } catch (NumberFormatException e) {
                System.out.println("  invalid number");
                return;
            }
        }

        // Expiry
        LocalDate expiry = null;
        String expiryStr = prompt("expiry YYYY-MM-DD  (leave blank = no expiry)").trim();
        if (!expiryStr.isBlank()) {
            try {
                expiry = LocalDate.parse(expiryStr);
            } catch (DateTimeParseException e) {
                System.out.println("  invalid date format — use YYYY-MM-DD");
                return;
            }
        }

        try {
            Coupon coupon = couponService.create(code, pct, expiry, maxUses);
            System.out.println("  created: " + coupon);
            logger.logCoupon(coupon);
        } catch (IllegalArgumentException | CoffeeShopException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void listCoupons() {
        List<Coupon> list = couponService.listAll();
        if (list.isEmpty()) { System.out.println("  no coupons"); return; }
        System.out.println();
        list.forEach(c -> System.out.println("  " + c));
    }

    private void deactivateCoupon() {
        listCoupons();
        String code = prompt("code to end").trim();
        if (code.isBlank()) return;
        if (couponService.deactivate(code)) {
            System.out.println("  coupon " + code.toUpperCase() + " deactivated");
            logger.logCouponDeactivated(code.toUpperCase());
        } else {
            System.out.println("  code not found");
        }
    }

    // Analytics

    private void showAnalytics() {
        if (sessionOrders.isEmpty()) { System.out.println("  no orders this session"); return; }
        System.out.println();
        DoubleSummaryStatistics stats = SalesAnalytics.orderStats(sessionOrders);
        System.out.printf("  orders:   %d%n", sessionOrders.size());
        System.out.printf("  revenue:  $%.2f%n", stats.getSum());
        System.out.printf("  average:  $%.2f%n", stats.getAverage());
        System.out.printf("  largest:  $%.2f%n", stats.getMax());
        System.out.println();

        List<Map.Entry<String, Double>> top = SalesAnalytics.topItemsByRevenue(sessionOrders, 3);
        if (!top.isEmpty()) {
            System.out.println("  top items");
            top.forEach(e -> System.out.printf("  %-24s $%.2f%n", e.getKey(), e.getValue()));
            System.out.println();
        }

        System.out.println("  by category");
        SalesAnalytics.itemCountByCategory(sessionOrders)
                .forEach((cat, cnt) -> System.out.printf("  %-14s %d%n", cat, cnt));

        System.out.println();
        int n = sessionOrders.size();
        System.out.printf("  loyalty bonus order #%d: %d pts%n", n, SalesAnalytics.loyaltyBonus(n));
    }

    // Thread demo

    private void threadDemo() {
        List<Order> orders = sessionOrders.isEmpty()
                ? new ArrayList<>(orderService.getOrderQueue().getCompleted())
                : sessionOrders;
        if (orders.isEmpty()) { System.out.println("  process some orders first"); return; }

        System.out.println("\n  virtual threads (Project Loom, Java 21)");
        System.out.println("  running " + orders.size() + " orders concurrently...");
        try {
            VirtualThreadBarista.processOrdersConcurrently(orders)
                    .forEach(r -> System.out.println("  " + r));

            double total = orders.stream().mapToDouble(Order::getTotal).sum();
            String pay = VirtualThreadBarista.verifyPaymentAsync(total).get();
            System.out.println("  " + pay);
        } catch (Exception e) {
            System.out.println("  error: " + e.getMessage());
        }
    }

    // Helpers

    private String prompt(String label) {
        if (!label.isBlank()) System.out.print("  " + label + ": ");
        else System.out.print("  > ");
        return scanner.nextLine();
    }

    private void printMenu() {
        System.out.println("  1  queue");
        System.out.println("  2  process next");
        System.out.println("  3  cancel order");
        System.out.println("  4  stock");
        System.out.println("  5  restock");
        System.out.println("  6  coupons");
        System.out.println("  7  analytics");
        System.out.println("  8  log");
        System.out.println("  9  thread demo");
        System.out.println("  q  logout");
        System.out.println();
    }
}
