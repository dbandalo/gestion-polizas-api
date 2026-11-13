package com.prueba.polizas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CoreIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(CoreIntegrationService.class);

    public void registrarEvento(String evento, Long polizaId) {
        log.info("CORE-MOCK: se intentó enviar el evento {} de la póliza {} al CORE", evento, polizaId);
    }
}
