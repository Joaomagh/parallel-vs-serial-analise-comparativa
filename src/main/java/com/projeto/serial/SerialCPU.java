package com.projeto.serial;

import com.projeto.util.CSVExportar;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SerialCPU {

    public static ResultadoContagem countOccurrences(String filePath, String word, int executionNumber) {
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Usando o mesmo regex robusto para consistência
                String[] words = line.split("[^a-zA-Z0-9áéíóúâêîôûãõçÁÉÍÓÚÂÊÎÔÛÃÕÇ]+");
                for (String w : words) {
                    if (w != null && !w.trim().isEmpty() && w.equalsIgnoreCase(word)) {
                        count++;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo em SerialCPU: " + filePath + " - " + e.getMessage());
            // Retorna um resultado, mas registra o erro
            long errorTime = System.currentTimeMillis() - startTime;
            // Tenta salvar um resultado de erro no CSV
            try {
                CSVExportar.salvarResultado("cpu-serial-erro", 1, errorTime, 0, filePath, word, executionNumber);
            } catch (Exception csvEx) {
                System.err.println("Erro ao salvar resultado de erro para SerialCPU no CSV: " + csvEx.getMessage());
            }
            return new ResultadoContagem(0, errorTime, executionNumber);
        }

        long tempoTotal = System.currentTimeMillis() - startTime;

        try {
            CSVExportar.salvarResultado(
                    "cpu-serial",
                    1, // Para serial, idConfig pode ser 1 (representando 1 thread/configuração)
                    tempoTotal,
                    count,
                    filePath,
                    word,
                    executionNumber
            );
        } catch (Exception e) {
            System.err.println("Erro ao salvar resultado para SerialCPU no CSV: " + e.getMessage());
            e.printStackTrace();
        }
        return new ResultadoContagem(count, tempoTotal, executionNumber);
    }

    // Main pode ser mantido para testes individuais, se desejado.
    public static void main(String[] args) {
        String caminhoArquivoDataset = "datasets/DonQuixote-388208.txt"; // Exemplo
        String palavraParaBuscar = "y";
        int numeroDeExecucoes = 3;

        System.out.println("--- Iniciando Testes Individuais SerialCPU ---");
        for (int i = 1; i <= numeroDeExecucoes; i++) {
            System.out.printf("\nExecução Serial CPU #%d para a palavra '%s'...\n", i, palavraParaBuscar);
            ResultadoContagem resultado = countOccurrences(caminhoArquivoDataset, palavraParaBuscar, i);
            if (resultado != null) {
                System.out.printf("SerialCPU: %d ocorrências em %d ms (execução %d)%n",
                        resultado.ocorrencias, resultado.tempoMs, resultado.executionNumber);
            }
        }
        System.out.println("\n--- Testes Individuais SerialCPU Concluídos ---");
    }

    // CLASSE INTERNA ResultadoContagem MODIFICADA
    public static class ResultadoContagem {
        public int ocorrencias;       // Campo tornado público
        public long tempoMs;          // Campo tornado público
        public int executionNumber;   // Campo tornado público

        public ResultadoContagem(int ocorrencias, long tempoMs, int executionNumber) {
            this.ocorrencias = ocorrencias;
            this.tempoMs = tempoMs;
            this.executionNumber = executionNumber;
        }
    }
}
