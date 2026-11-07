package br.edu.ifro.ji.controller;

import br.edu.ifro.ji.dto.DashboardResponse;
import br.edu.ifro.ji.security.UsuarioPrincipal;
import br.edu.ifro.ji.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    DashboardResponse obter(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) String mes) {
        return dashboardService.obter(principal, mes);
    }
}
