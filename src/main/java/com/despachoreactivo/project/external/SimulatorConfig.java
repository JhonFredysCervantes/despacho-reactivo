package com.despachoreactivo.project.external;

public class SimulatorConfig {

    private int fallasTarifa;
    private long latenciaRiesgoMs;
    private int scoreRiesgo;

    public SimulatorConfig() {
    }

    public SimulatorConfig(int fallasTarifa, long latenciaRiesgoMs, int scoreRiesgo) {
        this.fallasTarifa = fallasTarifa;
        this.latenciaRiesgoMs = latenciaRiesgoMs;
        this.scoreRiesgo = scoreRiesgo;
    }

    public int getFallasTarifa() {
        return fallasTarifa;
    }

    public void setFallasTarifa(int fallasTarifa) {
        this.fallasTarifa = fallasTarifa;
    }

    public long getLatenciaRiesgoMs() {
        return latenciaRiesgoMs;
    }

    public void setLatenciaRiesgoMs(long latenciaRiesgoMs) {
        this.latenciaRiesgoMs = latenciaRiesgoMs;
    }

    public int getScoreRiesgo() {
        return scoreRiesgo;
    }

    public void setScoreRiesgo(int scoreRiesgo) {
        this.scoreRiesgo = scoreRiesgo;
    }
}