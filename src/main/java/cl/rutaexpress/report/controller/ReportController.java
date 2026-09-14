package cl.rutaexpress.report.controller;

import cl.rutaexpress.report.dto.KpiResponse;
import cl.rutaexpress.report.dto.TopServiceRow;
import cl.rutaexpress.report.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Solo Admin (ver Caso 3, sección 6 y SecurityConfig). Endpoints tal cual
 * los pide la sección 5 del caso.
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/kpis")
    public KpiResponse kpis(@RequestParam(defaultValue = "last24h") String range) {
        return service.kpis(range);
    }

    @GetMapping("/top-services")
    public List<TopServiceRow> topServices(@RequestParam(defaultValue = "last7d") String range) {
        return service.topServices(range);
    }
}
