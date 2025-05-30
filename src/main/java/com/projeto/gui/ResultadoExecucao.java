package com.projeto.gui;

class ResultadoExecucao { // Removido 'public' para permitir múltiplas classes de topo no mesmo bloco de exemplo
    String tipoExecucao;
    int idConfigCpuGpu;
    long tempoMs;
    int ocorrencias;
    String dataset;
    String palavraBuscada;
    int numExecucao;

    public ResultadoExecucao(String tipoExecucao, int idConfigCpuGpu, long tempoMs, int ocorrencias, String dataset, String palavraBuscada, int numExecucao) {
        this.tipoExecucao = tipoExecucao;
        this.idConfigCpuGpu = idConfigCpuGpu;
        this.tempoMs = tempoMs;
        this.ocorrencias = ocorrencias;
        this.dataset = dataset;
        this.palavraBuscada = palavraBuscada;
        this.numExecucao = numExecucao;
    }

    // Getters
    public String getTipoExecucao() { return tipoExecucao; }
    public int getIdConfigCpuGpu() { return idConfigCpuGpu; }
    public long getTempoMs() { return tempoMs; }
    public int getOcorrencias() { return ocorrencias; }
    public String getDataset() { return dataset; }
    public String getPalavraBuscada() { return palavraBuscada; }
    public int getNumExecucao() { return numExecucao; }

    @Override
    public String toString() {
        return "ResultadoExecucao{" +
                "tipoExecucao='" + tipoExecucao + '\'' +
                ", idConfigCpuGpu=" + idConfigCpuGpu +
                ", tempoMs=" + tempoMs +
                ", ocorrencias=" + ocorrencias +
                ", dataset='" + dataset + '\'' +
                ", palavraBuscada='" + palavraBuscada + '\'' +
                ", numExecucao=" + numExecucao +
                '}';
    }
}