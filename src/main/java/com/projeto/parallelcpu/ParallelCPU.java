package com.projeto.parallelcpu;

import com.projeto.util.CSVExportar;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ParallelCPU {

    static class ContadorPalavras implements Callable<Integer> {
        private final String[] palavras;
        private final String palavraAlvo;
        private final int inicio;
        private final int fim;

        public ContadorPalavras(String[] palavras, String palavraAlvo, int inicio, int fim) {
            this.palavras = palavras;
            this.palavraAlvo = palavraAlvo.toLowerCase();
            this.inicio = inicio;
            this.fim = fim;
        }

        @Override
        public Integer call() {
            int contagemLocal = 0;
            for (int i = inicio; i < fim; i++) {
                if (palavras[i] != null && !palavras[i].isEmpty() && palavras[i].toLowerCase().equals(palavraAlvo)) {
                    contagemLocal++;
                }
            }
            return contagemLocal;
        }
    }

    public static ResultadoContagem countOccurrences(String caminhoArquivo, String palavra, int numThreads, int executionNumber) {
        long inicioTempo = System.currentTimeMillis();
        int totalOcorrencias = 0;

        String[] todasPalavras;
        try {
            todasPalavras = lerPalavrasDoArquivo(caminhoArquivo);
            if (todasPalavras == null || todasPalavras.length == 0) {
                System.err.println("Nenhuma palavra lida do arquivo ou arquivo vazio em ParallelCPU: " + caminhoArquivo);
                long errorTime = System.currentTimeMillis() - inicioTempo;
                try {
                    CSVExportar.salvarResultado("cpu-parallel-erro", numThreads, errorTime, 0, caminhoArquivo, palavra, executionNumber);
                } catch (Exception csvEx) {System.err.println("Erro ao salvar resultado de erro para ParallelCPU no CSV: " + csvEx.getMessage());}
                return new ResultadoContagem(0, errorTime, executionNumber);
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo em ParallelCPU: " + e.getMessage());
            long errorTime = System.currentTimeMillis() - inicioTempo;
            try {
                CSVExportar.salvarResultado("cpu-parallel-erro", numThreads, errorTime, 0, caminhoArquivo, palavra, executionNumber);
            } catch (Exception csvEx) {System.err.println("Erro ao salvar resultado de erro para ParallelCPU no CSV: " + csvEx.getMessage());}
            return new ResultadoContagem(0, errorTime, executionNumber);
        }

        if (numThreads <= 0) {
            System.err.println("Número de threads deve ser positivo para ParallelCPU.");
            long errorTime = System.currentTimeMillis() - inicioTempo;
            try {
                CSVExportar.salvarResultado("cpu-parallel-erro", numThreads, errorTime, totalOcorrencias, caminhoArquivo, palavra, executionNumber);
            } catch (Exception csvEx) {System.err.println("Erro ao salvar resultado de erro para ParallelCPU no CSV: " + csvEx.getMessage());}
            return new ResultadoContagem(totalOcorrencias, errorTime, executionNumber);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Integer>> listaDeResultadosFuturos = new ArrayList<>();

        int totalDePalavrasNoArquivo = todasPalavras.length;
        int tamanhoBlocoBase = totalDePalavrasNoArquivo / numThreads;
        int blocosComExtra = totalDePalavrasNoArquivo % numThreads;
        int indiceAtual = 0;

        for (int i = 0; i < numThreads; i++) {
            int inicioBloco = indiceAtual;
            int tamanhoBlocoAtual = tamanhoBlocoBase + (i < blocosComExtra ? 1 : 0);
            int fimBloco = inicioBloco + tamanhoBlocoAtual;
            if (inicioBloco < totalDePalavrasNoArquivo) {
                fimBloco = Math.min(fimBloco, totalDePalavrasNoArquivo);
                if (inicioBloco < fimBloco) {
                    Callable<Integer> tarefa = new ContadorPalavras(todasPalavras, palavra, inicioBloco, fimBloco);
                    listaDeResultadosFuturos.add(executor.submit(tarefa));
                }
            }
            indiceAtual = fimBloco;
        }

        for (Future<Integer> resultadoFuturo : listaDeResultadosFuturos) {
            try {
                totalOcorrencias += resultadoFuturo.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Erro ao obter resultado da thread em ParallelCPU: " + e.getMessage());
                e.printStackTrace();
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        long tempoTotal = System.currentTimeMillis() - inicioTempo;

        try {
            CSVExportar.salvarResultado(
                    "cpu-parallel",
                    numThreads,
                    tempoTotal,
                    totalOcorrencias,
                    caminhoArquivo,
                    palavra,
                    executionNumber
            );
        } catch (Exception e) {
            System.err.println("Erro ao salvar resultado para ParallelCPU no CSV: " + e.getMessage());
            e.printStackTrace();
        }
        return new ResultadoContagem(totalOcorrencias, tempoTotal, executionNumber);
    }

    private static String[] lerPalavrasDoArquivo(String caminhoArquivo) throws IOException {
        StringBuilder textoCompleto = new StringBuilder();
        try (BufferedReader leitor = new BufferedReader(new FileReader(caminhoArquivo, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                textoCompleto.append(linha).append(" ");
            }
        }
        String[] palavrasBrutas = textoCompleto.toString().split("[^a-zA-Z0-9áéíóúâêîôûãõçÁÉÍÓÚÂÊÎÔÛÃÕÇ]+");
        List<String> palavrasLimpas = new ArrayList<>();
        for (String p : palavrasBrutas) {
            if (p != null && !p.trim().isEmpty()) {
                palavrasLimpas.add(p.trim());
            }
        }
        return palavrasLimpas.toArray(new String[0]);
    }




/*
    public static void main(String[] args) {
        String caminhoArquivoDataset = "datasets/DonQuixote-388208.txt";
        String palavraParaBuscar = "y";
        int[] quantidadesDeThreads = {2, 4, 8};
        int numeroDeExecucoes = 3;

        System.out.println("--- Iniciando Testes Individuais ParallelCPU ---");
        for (int numThreads : quantidadesDeThreads) {
            System.out.printf("\nTestando com %d thread(s) para a palavra '%s'%n", numThreads, palavraParaBuscar);
            for (int i = 1; i <= numeroDeExecucoes; i++) {
                ResultadoContagem resultado = countOccurrences(caminhoArquivoDataset, palavraParaBuscar, numThreads, i);
                if (resultado != null) {
                    System.out.printf("ParallelCPU: %d ocorrências em %d ms (threads: %d, execução: %d)%n",
                            resultado.ocorrencias, resultado.tempoMs, numThreads, resultado.executionNumber);
                }
            }
        }
        System.out.println("\n--- Testes Individuais ParallelCPU Concluídos ---");
    }

 */


    public static class ResultadoContagem {
        public int ocorrencias;
        public long tempoMs;
        public int executionNumber;

        public ResultadoContagem(int ocorrencias, long tempoMs, int executionNumber) {
            this.ocorrencias = ocorrencias;
            this.tempoMs = tempoMs;
            this.executionNumber = executionNumber;
        }
    }
}
