package octi.wanparty.cloudflare;

public class TunnelRegistrationException extends Exception {
    public final long retryAfter;

    public TunnelRegistrationException(String cause, long retryAfter) {
        super(cause);
        this.retryAfter = retryAfter;
    }

    public boolean shouldRetry() {
        return this.retryAfter >= 0;
    }
}
