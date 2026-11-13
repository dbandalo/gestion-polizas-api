package com.prueba.polizas.controller;

import com.prueba.polizas.dto.CoreEventoRequest;
import com.prueba.polizas.service.CoreIntegrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/core-mock")
public class CoreMockController {

    private final CoreIntegrationService coreIntegrationService;

    public CoreMockController(CoreIntegrationService coreIntegrationService) {
        this.coreIntegrationService = coreIntegrationService;
    }

    @PostMapping("/evento")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> registrarEvento(@Valid @RequestBody CoreEventoRequest request) {
        coreIntegrationService.registrarEvento(request.evento(), request.polizaId());
        return Map.of(
                "status", "REGISTRADO",
                "evento", request.evento(),
                "polizaId", request.polizaId()
        );
    }
}
