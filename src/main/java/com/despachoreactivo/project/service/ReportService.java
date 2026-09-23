package com.despachoreactivo.project.service;

import com.despachoreactivo.project.model.Despacho;
import com.despachoreactivo.project.repository.DespachoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class ReportService {

    private final DespachoRepository despachoRepository;

    public ReportService(DespachoRepository despachoRepository) {
        this.despachoRepository = despachoRepository;
    }

    public Mono<Map<String, ReporteCiudad>> obtenerReportePorCiudad() {
        return despachoRepository.findAll()
                .filter(d -> "EN_RUTA".equals(d.getEstado()) || "ENTREGADO".equals(d.getEstado()))
                .collectList()
                .map(despachos -> {
                    Map<String, ReporteCiudad> resultado = new HashMap<>();

                    for (Despacho despacho : despachos) {
                        int pesoTotal = despacho.getPaquetes() != null ? 
                                despacho.getPaquetes().stream()
                                        .mapToInt(p -> p.getPesoKg() != null ? p.getPesoKg() : 0)
                                        .sum() : 0;
                        
                        double valorTotal = despacho.getTarifaTotal() != null ? 
                                despacho.getTarifaTotal() : 0.0;

                        resultado.computeIfAbsent(despacho.getCiudad(), k -> new ReporteCiudad(k, 0, 0.0))
                                .agregarDatos(pesoTotal, valorTotal);
                    }

                    return resultado;
                });
    }

    public Flux<ReporteCiudad> obtenerReportePorCiudadStream() {
        return despachoRepository.findAll()
                .filter(d -> "EN_RUTA".equals(d.getEstado()) || "ENTREGADO".equals(d.getEstado()))
                .scan(new HashMap<String, ReporteCiudad>(), (acumulado, despacho) -> {
                    int pesoTotal = despacho.getPaquetes() != null ? 
                            despacho.getPaquetes().stream()
                                    .mapToInt(p -> p.getPesoKg() != null ? p.getPesoKg() : 0)
                                    .sum() : 0;
                    
                    double valorTotal = despacho.getTarifaTotal() != null ? 
                            despacho.getTarifaTotal() : 0.0;

                    acumulado.computeIfAbsent(despacho.getCiudad(), k -> new ReporteCiudad(k, 0, 0.0))
                            .agregarDatos(pesoTotal, valorTotal);
                    
                    return acumulado;
                })
                .skip(1)
                .flatMap(mapa -> Flux.fromIterable(mapa.values()))
                .distinct()
                .limitRate(100);
    }

    public static class ReporteCiudad {
        public String ciudad;
        public int pesoKgTotal;
        public double valorTotal;

        public ReporteCiudad(String ciudad, int pesoKgTotal, double valorTotal) {
            this.ciudad = ciudad;
            this.pesoKgTotal = pesoKgTotal;
            this.valorTotal = valorTotal;
        }

        public void agregarDatos(int peso, double valor) {
            this.pesoKgTotal += peso;
            this.valorTotal += valor;
        }
    }
}
