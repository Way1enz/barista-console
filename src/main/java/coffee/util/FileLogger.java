package coffee.util;

import coffee.model.Coupon;
import coffee.model.Coupon;
import coffee.model.Order;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogger implements AutoCloseable {

    private final Path logFile;
    private BufferedWriter writer;

    public FileLogger(String directory) throws IOException {
        Path dir = Path.of(directory);
        Files.createDirectories(dir);
        String fileName = "orders_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".log";
        this.logFile = dir.resolve(fileName);
        this.writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(logFile.toFile(), true),
                        StandardCharsets.UTF_8));
    }

    public void logOrder(Order order) {
        writeEntry(String.format("ORDER    #%d  %-15s  $%6.2f  %s",
                order.getOrderId(), order.getCustomerName(),
                order.getTotal(), order.getStatus()));
    }

    public void logRestock(String itemName, int qty) {
        writeEntry(String.format("RESTOCK  %-22s  +%d", itemName, qty));
    }

    public void logCoupon(Coupon coupon) {
        String uses = coupon.getUsesRemaining() == Coupon.UNLIMITED
                ? "unlimited" : coupon.getUsesRemaining() + " uses";
        writeEntry(String.format("COUPON   %-12s  %d%% off  %s",
                coupon.getCode(), coupon.getPercentOff(), uses));
    }

    public void logCouponDeactivated(String code) {
        writeEntry(String.format("COUPON   %-12s  deactivated", code));
    }

    private void writeEntry(String msg) {
        try {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            writer.write("[" + ts + "]  " + msg + System.lineSeparator());
            writer.flush();
        } catch (IOException e) {
            System.err.println("log write failed: " + e.getMessage());
        }
    }

    public void printLog() {
        try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            System.out.println();
            System.out.println("  -- log --");
            reader.lines().forEach(line -> System.out.println("  " + line));
            System.out.println("  ---------");
        } catch (IOException e) {
            System.out.println("  no log entries yet");
        }
    }

    @Override
    public void close() throws IOException {
        if (writer != null) writer.close();
    }
}
