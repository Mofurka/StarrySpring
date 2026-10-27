package irden.space.proxy.plugin.irden.integration.web.rest.v1.transactions;

import irden.space.proxy.plugin.irden.integration.web.dto.player_app_id.PlayerAppIdParam;
import irden.space.proxy.plugin.irden.integration.web.rest.v1.constants.RestRoutes;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@PreAuthorize("hasRole('SITE')")
@RequestMapping(RestRoutes.TransactionsV1.PRIVATE)
@RequiredArgsConstructor
public class TransactionsController {
    private final TransactionsHandler handler;

    @GetMapping(PlayerAppIdParam.PATH)
    public ResponseEntity<PagedModel<PlayerTransactionDto>> getPlayerTransactions(

            @PathVariable(PlayerAppIdParam.NAME)
            long playerAppId,

            @RequestParam(name = TransactionsDateRange.START_DATE, required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(name = TransactionsDateRange.END_DATE, required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(handler.handlePlayerTransactions(
                playerAppId,
                TransactionsDateRange.of(startDate, endDate),
                pageable
        ));
    }

}
