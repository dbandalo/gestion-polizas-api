package com.prueba.polizas.dto;

import com.prueba.polizas.domain.EstadoPoliza;
import com.prueba.polizas.domain.Poliza;
import com.prueba.polizas.domain.TipoPoliza;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolizaResponse(
        Long id,
        TipoPoliza tipo,
        EstadoPoliza estado,
        BigDecimal canonMensual,
        BigDecimal prima,
        Integer mesesVigencia,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int cantidadRiesgos
) {
    public static PolizaResponse from(Poliza poliza) {
        return new PolizaResponse(
                poliza.getId(),
                poliza.getTipo(),
                poliza.getEstado(),
                poliza.getCanonMensual(),
                poliza.getPrima(),
                poliza.getMesesVigencia(),
                poliza.getFechaInicio(),
                poliza.getFechaFin(),
                poliza.getRiesgos() == null ? 0 : poliza.getRiesgos().size()
        );
    }
}
