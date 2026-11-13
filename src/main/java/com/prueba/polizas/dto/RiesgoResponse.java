package com.prueba.polizas.dto;

import com.prueba.polizas.domain.EstadoRiesgo;
import com.prueba.polizas.domain.Riesgo;

public record RiesgoResponse(
        Long id,
        String descripcion,
        EstadoRiesgo estado,
        Long polizaId
) {
    public static RiesgoResponse from(Riesgo riesgo) {
        return new RiesgoResponse(
                riesgo.getId(),
                riesgo.getDescripcion(),
                riesgo.getEstado(),
                riesgo.getPoliza().getId()
        );
    }
}
