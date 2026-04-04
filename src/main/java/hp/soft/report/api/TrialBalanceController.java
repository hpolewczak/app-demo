package hp.soft.report.api;

import hp.soft.report.dto.TrialBalance;
import hp.soft.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class TrialBalanceController {
    private final ReportService reportService;

    @GetMapping("/trial-balance")
    public Mono<TrialBalance> getTrialBalance() {
        return reportService.getTrialBalance();
    }
}
