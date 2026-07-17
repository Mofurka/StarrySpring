package irden.space.proxy.plugin.site.web.client.exceptions;

import org.springframework.http.HttpStatusCode;

public class IrdenAppClientException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String responseBody;

    public IrdenAppClientException(
            HttpStatusCode statusCode,
            String message,
            String responseBody
    ) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public HttpStatusCode statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }
}