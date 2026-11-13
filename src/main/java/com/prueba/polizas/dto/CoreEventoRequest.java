package com.prueba.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CoreEventoRequest(
        @NotBlank(message = "El evento es obligatorio")
        String evento,
        @NotNull(message = "El id de la póliza es obligatorio")
        Long polizaId
) {
}
