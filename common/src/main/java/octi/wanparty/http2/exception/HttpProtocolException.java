package octi.wanparty.http2.exception;

public class HttpProtocolException extends Exception {
    public HttpProtocolException() {
    }

    public HttpProtocolException(String message) {
        super(message);
    }

    public HttpProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpProtocolException(Throwable cause) {
        super(cause);
    }

    public HttpProtocolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
