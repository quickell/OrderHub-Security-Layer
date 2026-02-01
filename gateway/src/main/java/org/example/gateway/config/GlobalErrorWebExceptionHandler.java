package org.example.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import org.springframework.boot.web.error.ErrorAttributeOptions;

import java.util.Map;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    public GlobalErrorWebExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties.Resources resources,
            ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, resources, applicationContext);
        setMessageWriters(serverCodecConfigurer.getWriters());
        setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorAttributes = getErrorAttributes(request, ErrorAttributeOptions.defaults());
        Throwable error = getError(request);
        HttpStatus status = getHttpStatus(errorAttributes, error);

        if (error != null) {
            log.warn("Gateway error for {} {}: {} - {}", request.method(), request.path(), status, error.getMessage());
        }

        String message = (String) errorAttributes.getOrDefault("message", "Service temporarily unavailable");
        if (error != null && (message == null || message.isBlank())) {
            message = isConnectionError(error) ? "Backend service unavailable. Is the service running?" : "An unexpected error occurred.";
        }

        Map<String, Object> body = Map.of(
                "error", status.getReasonPhrase(),
                "status", status.value(),
                "message", message,
                "path", errorAttributes.getOrDefault("path", request.path())
        );

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body));
    }

    private HttpStatus getHttpStatus(Map<String, Object> errorAttributes, Throwable error) {
        Integer statusCode = (Integer) errorAttributes.get("status");
        if (statusCode != null) {
            try {
                return HttpStatus.valueOf(statusCode);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (isConnectionError(error)) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static boolean isConnectionError(Throwable error) {
        if (error == null) return false;
        String msg = error.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            if (lower.contains("connection refused") || lower.contains("connection reset")
                    || lower.contains("connection closed") || lower.contains("no route to host")) {
                return true;
            }
        }
        return false;
    }
}
