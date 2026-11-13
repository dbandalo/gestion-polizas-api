package com.prueba.polizas.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearRiesgoRequest(
        @NotBlank(message = "La descripción del riesgo es obligatoria")
        String descripcion
) {
}
