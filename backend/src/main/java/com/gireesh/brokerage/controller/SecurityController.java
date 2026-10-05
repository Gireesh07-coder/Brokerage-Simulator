package com.gireesh.brokerage.controller;

import com.gireesh.brokerage.dto.PageResponse;
import com.gireesh.brokerage.dto.SecurityResponse;
import com.gireesh.brokerage.service.SecurityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/securities")
public class SecurityController {
    private final SecurityService securityService;

    public SecurityController(SecurityService securityService) {
        this.securityService = securityService;
    }

    @GetMapping
    public PageResponse<SecurityResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sector,
            @RequestParam(defaultValue = "symbol") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return securityService.search(q, sector, sort, direction, page, size);
    }

    @GetMapping("/{idOrSymbol}")
    public SecurityResponse getByIdOrSymbol(@PathVariable String idOrSymbol) {
        return securityService.getByIdOrSymbol(idOrSymbol);
    }
}
