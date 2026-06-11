package coffee;

import coffee.exception.CoffeeShopException;
import coffee.model.*;
import coffee.payment.PaymentService;
import coffee.service.AccountService;
import coffee.service.CouponService;
import coffee.service.OrderService;
import coffee.util.FileLogger;
import coffee.util.Validator;

import java.util.*;
import java.util.stream.Collectors;

public class CustomerUI {

    private final OrderService      orderService;
    private final CouponService     couponService;
    private final AccountService    accountService;
    private final PaymentService    paymentService;
    private final FileLogger        logger;
    private final Scanner           scanner;

    // set after login; null = guest
    private CustomerAccount         account = null;
    private final List<Order>       sessionOrders = new ArrayList<>();

    public CustomerUI(OrderService orderService, CouponService couponService,
                      AccountService accountService, FileLogger logger, Scanner scanner) {
        this.orderService   = orderService;
        this.couponService  = couponService;
        this.accountService = accountService;
        this.paymentService = new PaymentService(scanner);
        this.logger         = logger;
        this.scanner        = scanner;
    }

    public void run() {
        System.out.println("\n  BREW & BEAN  /  customer\n");
        if (!authFlow()) return;  // user pressed esc at auth menu — back to main

        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("").trim();
            running = switch (choice) {
                case "1"      -> { newOrder();     yield true; }
                case "2"      -> { showMenu();     yield true; }
                case "3"      -> { showMyOrders(); yield true; }
                case "4"      -> { showAccount();  yield true; }
                case "q", "Q" -> false;
                default       -> { System.out.println("  invalid"); yield true; }
            };
        }
        account = null;
    }

    // Auth

    /** Returns true to proceed into the customer UI, false to go back to the main menu. */
    private boolean authFlow() {
        System.out.println("  1  login");
        System.out.println("  2  create account");
        System.out.println("  3  continue as guest");
        System.out.println("  esc  back");
        System.out.println();

        while (true) {
            String choice = prompt("").trim();
            if (choice.equalsIgnoreCase(ESC)) { System.out.println(); return false; }
            switch (choice) {
                case "1" -> doLogin();
                case "2" -> doRegister();
                case "3" -> { System.out.println("  continuing as guest\n"); return true; }
                default  -> { System.out.println("  invalid"); continue; }
            }
            if (account != null) { System.out.println(); return true; }  // auth succeeded
            // ESC inside login/register or failed login — re-show the menu
            System.out.println();
            System.out.println("  1  login");
            System.out.println("  2  create account");
            System.out.println("  3  continue as guest");
            System.out.println("  esc  back");
            System.out.println();
        }
    }

    private static final String ESC = "esc";

    private void doLogin() {
        System.out.println("  (type esc at any prompt to go back)\n");

        // username
        String username;
        while (true) {
            username = prompt("username").trim();
            if (username.equalsIgnoreCase(ESC)) { System.out.println("  cancelled"); return; }
            if (!username.isBlank()) break;
            System.out.println("  username cannot be empty");
        }

        // password
        String password;
        while (true) {
            password = prompt("password").trim();
            if (password.equalsIgnoreCase(ESC)) { System.out.println("  cancelled"); return; }
            if (!password.isBlank()) break;
            System.out.println("  password cannot be empty");
        }

        String u = username, p = password;
        accountService.login(u, p).ifPresentOrElse(a -> {
            this.account = a;
            System.out.println("  logged in as " + a.getUsername());
        }, () -> System.out.println("  incorrect username or password"));
    }

    private void doRegister() {
        System.out.println("  (type esc at any prompt to go back)\n");

        // username
        String username;
        while (true) {
            username = prompt("username").trim();
            if (username.equalsIgnoreCase(ESC)) { System.out.println("  cancelled"); return; }

            Validator.ValidationResult r = Validator.validateUsername(username);
            if (!r.valid()) {
                System.out.println("  username requirements not met:");
                Validator.printErrors(r);
                continue;
            }
            if (accountService.usernameExists(username)) {
                System.out.println("  username already taken — try another");
                continue;
            }
            break;
        }

        // password
        System.out.println();
        System.out.println("  password requirements:");
        System.out.println("    - 8+ characters");
        System.out.println("    - uppercase and lowercase letters");
        System.out.println("    - at least one number");
        System.out.println("    - at least one special character  (!@#$%^&* etc.)");
        System.out.println("    - no spaces");
        System.out.println();

        String password;
        while (true) {
            password = prompt("password").trim();
            if (password.equalsIgnoreCase(ESC)) { System.out.println("  cancelled"); return; }

            Validator.ValidationResult r = Validator.validatePassword(password);
            if (!r.valid()) {
                System.out.println("  password does not meet requirements:");
                Validator.printErrors(r);
                continue;
            }

            // confirm
            String confirm = prompt("confirm password").trim();
            if (confirm.equalsIgnoreCase(ESC)) { System.out.println("  cancelled"); return; }
            if (!confirm.equals(password)) {
                System.out.println("  passwords do not match — try again");
                continue;
            }
            break;
        }

        // email
        String email;
        while (true) {
            email = prompt("email").trim();
            if (email.equalsIgnoreCase(ESC)) { System.out.println("  cancelled"); return; }

            Validator.ValidationResult r = Validator.validateEmail(email);
            if (!r.valid()) {
                System.out.println("  invalid email:");
                Validator.printErrors(r);
                continue;
            }
            break;
        }

        try {
            this.account = accountService.register(username, password, email);
            System.out.println("  account created — logged in as " + account.getUsername());
        } catch (CoffeeShopException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    // Menu

    private void showMenu() {
        System.out.println();
        orderService.getMenuRepo().findAll().stream()
            .collect(Collectors.groupingBy(MenuItem::getCategory,
                    LinkedHashMap::new, Collectors.toList()))
            .forEach((cat, items) -> {
                System.out.println("  " + cat.toLowerCase());
                items.stream()
                     .sorted(Comparator.comparing(MenuItem::getPrice))
                     .forEach(i -> System.out.println("  " + i));
                System.out.println();
            });
        System.out.println("  sizes:  s = base price   m = +$0.50   l = +$1.00");
    }

    // Order flow

    private void newOrder() {
        String name = account != null ? account.getUsername() : prompt("name").trim();
        if (name.isBlank()) { System.out.println("  name required"); return; }

        Cart cart = new Cart(name);
        boolean shopping = true;

        while (shopping) {
            cart.print();
            System.out.println();
            String actions = account != null
                    ? "  add / update / menu / coupon / checkout / cancel"
                    : "  add / update / menu / checkout / cancel";
            System.out.println(actions);

            String action = prompt("").trim().toLowerCase();
            shopping = switch (action) {
                case "add"      -> { addItem(cart);    yield true; }
                case "update"   -> { updateItem(cart); yield true; }
                case "menu"     -> { showMenu();       yield true; }
                case "coupon"   -> { applyCoupon(cart); yield true; }
                case "checkout" -> !doCheckout(cart);
                case "cancel"   -> { System.out.println("  order cancelled"); yield false; }
                default         -> {
                    if (action.equals("coupon") && account == null)
                        System.out.println("  login required to use coupons");
                    else
                        System.out.println("  unknown action");
                    yield true;
                }
            };
        }
    }

    private void addItem(Cart cart) {
        showMenu();

        // Step 1 — validate item ID immediately before asking anything else
        String itemId;
        while (true) {
            itemId = prompt("item id").trim().toUpperCase();
            if (itemId.isBlank()) return;
            if (itemId.equalsIgnoreCase(ESC)) return;
            if (orderService.getMenuRepo().findById(itemId).isPresent()) break;
            System.out.println("  item not found: \"" + itemId + "\" — check the ID and try again");
        }

        // Step 2 — size (validated in chooseSize)
        Size size = chooseSize();
        if (size == null) return;  // ESC

        // Step 3 — quantity (validated in chooseQty)
        int qty = chooseQty();
        if (qty == 0) return;  // ESC

        try {
            orderService.addToCart(cart, itemId, size, qty);
            System.out.println("  added");
        } catch (CoffeeShopException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    /**
     * updateItem — unified edit/remove for a cart line.
     * Options per line: change size, change quantity, remove some or all.
     */
    private void updateItem(Cart cart) {
        if (cart.isEmpty()) { System.out.println("  cart is empty"); return; }
        cart.print();

        int idx = parseInt(prompt("line number"));
        List<Cart.CartEntry> entries = cart.getEntries();
        if (idx < 1 || idx > entries.size()) { System.out.println("  invalid line"); return; }

        Cart.CartEntry entry = entries.get(idx - 1);
        System.out.printf("  selected: %s [%s] x%d%n",
                entry.item().getName(), entry.size().getLabel(), entry.qty());
        System.out.println();
        System.out.println("  1  change size");
        System.out.println("  2  change quantity");
        System.out.println("  3  remove some");
        System.out.println("  4  remove all");
        System.out.println("  any  back");

        switch (prompt("").trim()) {
            case "1" -> {
                Size newSize = chooseSize();
                try {
                    // remove old line, re-add with new size
                    int qty = entry.qty();
                    cart.removeByIndex(idx);
                    orderService.addToCart(cart, entry.item().getId(), newSize, qty);
                    System.out.println("  size updated");
                } catch (CoffeeShopException e) {
                    System.out.println("  " + e.getMessage());
                }
            }
            case "2" -> {
                System.out.printf("  current qty: %d%n", entry.qty());
                int newQty = parseInt(prompt("new qty"));
                if (newQty < 0) { System.out.println("  invalid"); return; }
                try {
                    cart.updateQty(idx, newQty, entry.item());
                    System.out.println(newQty == 0 ? "  item removed" : "  quantity updated");
                } catch (CoffeeShopException e) {
                    System.out.println("  " + e.getMessage());
                }
            }
            case "3" -> {
                System.out.printf("  current qty: %d  — remove how many?%n", entry.qty());
                int removeQty = parseInt(prompt("qty to remove"));
                if (removeQty <= 0) { System.out.println("  invalid"); return; }
                int remaining = entry.qty() - removeQty;
                if (remaining < 0) {
                    System.out.println("  cannot remove more than you have");
                    return;
                }
                try {
                    cart.updateQty(idx, remaining, entry.item());
                    System.out.println(remaining == 0 ? "  item removed" : "  removed " + removeQty + ", " + remaining + " remaining");
                } catch (CoffeeShopException e) {
                    System.out.println("  " + e.getMessage());
                }
            }
            case "4" -> {
                cart.removeByIndex(idx);
                System.out.println("  item removed");
            }
            default -> System.out.println("  back");
        }
    }

    private void applyCoupon(Cart cart) {
        if (account == null) {
            System.out.println("  login required to use coupons");
            return;
        }
        if (cart.getCoupon().isPresent()) {
            String replace = prompt("coupon " + cart.getCoupon().get().getCode()
                    + " applied — replace? y/n").trim();
            if (!replace.equalsIgnoreCase("y")) return;
            cart.removeCoupon();
        }
        String code = prompt("coupon code").trim();
        couponService.validate(code).ifPresentOrElse(c -> {
            cart.applyCoupon(c);
            String uses = c.getUsesRemaining() == Coupon.UNLIMITED
                    ? "unlimited uses" : c.getUsesRemaining() + " uses remaining";
            System.out.printf("  applied — %d%% off  (%s)%n", c.getPercentOff(), uses);
        }, () -> System.out.println("  invalid, expired, or used-up code"));
    }

    private boolean doCheckout(Cart cart) {
        if (cart.isEmpty()) { System.out.println("  cart is empty"); return false; }

        System.out.println("\n  -- order summary --");
        cart.print();

        if (!prompt("confirm? y/n").trim().equalsIgnoreCase("y")) {
            System.out.println("  back to cart");
            return false;
        }

        Order order;
        try {
            order = cart.checkout();
        } catch (CoffeeShopException e) {
            System.out.println("  " + e.getMessage());
            return false;
        }

        boolean paid = paymentService.collectAndProcess(order, order.getTotal());
        if (!paid) {
            System.out.println("  payment failed — order not placed");
            for (OrderItem oi : order.getItems()) oi.item().restock(oi.quantity());
            return false;
        }

        // Record coupon use only after successful payment
        cart.getCoupon().ifPresent(c -> couponService.recordUse(c.getCode()));

        orderService.getOrderQueue().enqueue(order);
        logger.logOrder(order);

        if (account != null) accountService.recordOrder(account);

        System.out.println(order.getReceipt());
        System.out.println("  position in queue: " + orderService.getOrderQueue().pendingCount());
        sessionOrders.add(order);
        return true;
    }

    // Account / Orders

    private void showMyOrders() {
        if (sessionOrders.isEmpty()) { System.out.println("\n  no orders yet"); return; }
        System.out.println();
        sessionOrders.forEach(o ->
                System.out.printf("  #%d  $%.2f  %s%n",
                        o.getOrderId(), o.getTotal(), o.getStatus()));
    }

    private void showAccount() {
        System.out.println();
        if (account == null) {
            System.out.println("  not logged in  (guest session)");
            while (true) {
                System.out.println("  1  login    2  create account    any  back");
                switch (prompt("").trim()) {
                    case "1" -> doLogin();
                    case "2" -> doRegister();
                    default  -> { return; }
                }
                if (account != null) return;  // auth succeeded — leave account view
                // ESC or failed login — re-show the mini menu
                System.out.println();
            }
        } else {
            System.out.println("  " + account);
            System.out.println();
            System.out.println("  1  change password");
            System.out.println("  2  logout");
            System.out.println("  any  back");
            switch (prompt("").trim()) {
                case "1" -> doChangePassword();
                case "2" -> { System.out.println("  logged out"); account = null; }
            }
        }
    }

    private void doChangePassword() {
        System.out.println("  (type esc at any prompt to cancel)\n");

        // confirm old password
        String oldPassword;
        while (true) {
            oldPassword = prompt("current password").trim();
            if (oldPassword.equalsIgnoreCase(ESC)) { System.out.println("  cancelled"); return; }
            if (!oldPassword.isBlank()) break;
            System.out.println("  password cannot be empty");
        }

        if (!account.checkPassword(oldPassword)) {
            System.out.println("  incorrect password");
            return;
        }

        // new password
        System.out.println();
        System.out.println("  password requirements:");
        System.out.println("    - 8+ characters, uppercase + lowercase");
        System.out.println("    - at least one number and one special character");
        System.out.println("    - no spaces");
        System.out.println();

        String newPassword;
        while (true) {
            newPassword = prompt("new password").trim();
            if (newPassword.equalsIgnoreCase(ESC)) { System.out.println("  cancelled"); return; }

            Validator.ValidationResult r = Validator.validatePassword(newPassword);
            if (!r.valid()) {
                System.out.println("  password does not meet requirements:");
                Validator.printErrors(r);
                continue;
            }
            if (newPassword.equals(oldPassword)) {
                System.out.println("  new password must be different from current password");
                continue;
            }

            String confirm = prompt("confirm new password").trim();
            if (confirm.equalsIgnoreCase(ESC)) { System.out.println("  cancelled"); return; }
            if (!confirm.equals(newPassword)) {
                System.out.println("  passwords do not match — try again");
                continue;
            }
            break;
        }

        String np = newPassword;
        accountService.changePassword(account, oldPassword, np).ifPresentOrElse(updated -> {
            this.account = updated;
            System.out.println("  password changed");
        }, () -> System.out.println("  failed — please try again"));
    }

    // Helpers

    /** Returns null if the user types ESC. */
    private Size chooseSize() {
        System.out.println("  size  s / m / l");
        while (true) {
            String input = prompt("").trim().toLowerCase();
            if (input.equalsIgnoreCase(ESC)) return null;
            switch (input) {
                case "s" -> { return Size.SMALL; }
                case "m" -> { return Size.MEDIUM; }
                case "l" -> { return Size.LARGE; }
                default  -> System.out.println("  enter s, m, or l");
            }
        }
    }

    /** Returns 0 if the user types ESC. */
    private int chooseQty() {
        while (true) {
            String input = prompt("qty").trim();
            if (input.equalsIgnoreCase(ESC)) return 0;
            try {
                int qty = Integer.parseInt(input);
                if (qty > 0) return qty;
                System.out.println("  quantity must be at least 1");
            } catch (NumberFormatException e) {
                System.out.println("  enter a whole number (e.g. 1, 2, 3)");
            }
        }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    private String prompt(String label) {
        if (!label.isBlank()) System.out.print("  " + label + ": ");
        else System.out.print("  > ");
        return scanner.nextLine();
    }

    private void printMenu() {
        System.out.println("  1  new order");
        System.out.println("  2  menu");
        System.out.println("  3  my orders");
        System.out.println("  4  account" + (account != null ? "  (" + account.getUsername() + ")" : "  (guest)"));
        System.out.println("  q  exit");
        System.out.println();
    }
}
