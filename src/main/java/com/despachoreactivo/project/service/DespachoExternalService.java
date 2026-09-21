package com.despachoreactivo.project.service;

import com.despachoreactivo.project.external.ClimaResponse;
import com.despachoreactivo.project.external.ExternalService;
import com.despachoreactivo.project.external.RiesgoResponse;
import com.despachoreactivo.project.external.TarifaResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DespachoExternalService {

    private final ExternalService externalService;

    public DespachoExternalService(ExternalService externalService) {
        this.externalService = externalService;
    }

    public Mono<ResultadoExternos> consultarServicios(
            Long clienteId,
            Integer peso,
            String ciudad) {

        Mono<TarifaResponse> tarifa =
                externalService.consultarTarifa(clienteId, peso);

        Mono<ClimaResponse> clima =
                externalService.consultarClima(ciudad);

        Mono<RiesgoResponse> riesgo =
                externalService.consultarRiesgo(clienteId, ciudad);

        return Mono.zip(tarifa, clima, riesgo)
                .map(resultado -> new ResultadoExternos(
                        resultado.getT1(),
                        resultado.getT2(),
                        resultado.getT3()
                ));
    }

    public record ResultadoExternos(
            TarifaResponse tarifa,
            ClimaResponse clima,
            RiesgoResponse riesgo
    ) {
    }
}