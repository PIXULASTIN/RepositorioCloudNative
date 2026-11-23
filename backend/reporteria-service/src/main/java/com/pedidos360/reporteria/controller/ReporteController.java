package com.pedidos360.reporteria.controller;

import com.pedidos360.reporteria.model.ResumenReporte;
import com.pedidos360.reporteria.service.ReporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Permisos en SecurityConfig: /api/reportes/** -> ADMIN u OPERADOR. */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenReporte> resumen() {
        return ResponseEntity.ok(reporteService.resumen());
    }
}
