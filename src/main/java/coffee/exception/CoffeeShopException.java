package coffee.exception;

public class CoffeeShopException extends RuntimeException {
    public CoffeeShopException(String message) {
        super(message);
    }
    public CoffeeShopException(String message, Throwable cause) {
        super(message, cause);
    }
}
