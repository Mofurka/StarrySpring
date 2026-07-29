package irden.space.proxy.plugin.site.web.rest.v1.exception_handlers;

import irden.space.proxy.plugin.irden.service.exception.AccountNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice("irden.space.proxy.plugin.site.web.rest.v1.player_balance")
public class PlayerBalanceExceptionHandlers {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Void> handleAccountNotFoundException(AccountNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }


}