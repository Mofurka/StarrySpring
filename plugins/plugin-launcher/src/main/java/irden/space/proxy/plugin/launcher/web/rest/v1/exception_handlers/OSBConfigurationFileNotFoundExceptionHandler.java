package irden.space.proxy.plugin.launcher.web.rest.v1.exception_handlers;

import irden.space.proxy.plugin.launcher.exceptions.OSBConfigurationFileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OSBConfigurationFileNotFoundExceptionHandler {

    @ExceptionHandler(OSBConfigurationFileNotFoundException.class)
    public ResponseEntity<String> handleOSBConfigurationFileNotFoundException(OSBConfigurationFileNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }


}
