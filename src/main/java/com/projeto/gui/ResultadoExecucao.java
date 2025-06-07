package com.projeto.gui;

class ResultadoExecucao {
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

    public String getTipoExecucao() { return tipoExecucao; }
    public int getIdConfigCpuGpu() { return idConfigCpuGpu; }
    public long getTempoMs() { return tempoMs; }
    public int getOcorrencias() { return ocorrencias; }
    public String getDataset() { return dataset; } 
    public String getPalavraBuscada() { return palavraBuscada; }
    public int getNumExecucao() { return numExecucao; }

    public String getDatasetFileName() {
        if (dataset == null || dataset.isEmpty()) {
            return "";
        }
        String normalizedPath = dataset.replace("\\", "/");
        if (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        int lastSlash = normalizedPath.lastIndexOf('/');
        return (lastSlash == -1) ? normalizedPath : normalizedPath.substring(lastSlash + 1);
    }

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