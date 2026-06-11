package coffee;

import coffee.repository.*;
import coffee.service.*;
import coffee.util.FileLogger;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        MenuRepository menuRepo   = new MenuRepository();
        OrderQueue     orderQueue = new OrderQueue();
        MenuDataLoader.load(menuRepo);

        Scanner       scanner       = new Scanner(System.in);
        CouponService couponService = new CouponService();

        AccountService accountService;
        try {
            accountService = new AccountService("data");
        } catch (IOException e) {
            System.err.println("could not initialise account storage: " + e.getMessage());
            return;
        }

        try (FileLogger logger = new FileLogger("logs")) {
            OrderService orderService = new OrderService(menuRepo, orderQueue);
            boolean running = true;

            while (running) {
                System.out.println("\n  BREW & BEAN");
                System.out.println("  1  customer");
                System.out.println("  2  barista");
                System.out.println("  q  quit");
                System.out.println();
                System.out.print("  > ");
                String choice = scanner.nextLine().trim().toLowerCase();

                running = switch (choice) {
                    case "1" -> {
                        new CustomerUI(orderService, couponService,
                                accountService, logger, scanner).run();
                        yield true;
                    }
                    case "2" -> {
                        new BaristaUI(orderService, couponService, logger, scanner).run();
                        yield true;
                    }
                    case "q" -> false;
                    default  -> { System.out.println("  invalid"); yield true; }
                };
            }

        } catch (Exception e) {
            System.err.println("error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
