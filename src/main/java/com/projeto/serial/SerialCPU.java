package com.projeto.serial;

import com.projeto.util.CSVExportar; // Certifique-se que esta classe está no local correto e compilada
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SerialCPU {

    // Método para contar ocorrências de forma serial
    public static ResultadoContagem countOccurrences(String filePath, String word, int executionNumber) {
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Divide a linha em palavras. Usar um regex mais robusto como em ParallelCPU.
                // Para simplicidade aqui, mantemos o split básico, mas o ideal seria o mesmo de ParallelCPU.
                // String[] words = line.split("\\W+"); // Original
                String[] words = line.split("[^a-zA-Z0-9áéíóúâêîôûãõçÁÉÍÓÚÂÊÎÔÛÃÕÇ]+"); // Melhorado
                for (String w : words) {
                    if (w != null && !w.trim().isEmpty() && w.equalsIgnoreCase(word)) {
                        count++;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo: " + filePath + " - " + e.getMessage());
            // Retorna um resultado com erro, mas com o número da execução
            return new ResultadoContagem(0, System.currentTimeMillis() - startTime, executionNumber);
        }

        long tempoTotal = System.currentTimeMillis() - startTime;
        return new ResultadoContagem(count, tempoTotal, executionNumber);
    }

    // Método principal para executar os testes seriais
    public static void main(String[] args) {
        // Ajuste o caminho se necessário.
        String caminhoArquivoDataset = "projeto.analise.comparativa/datasets/DonQuixote-388208.txt/";
        String palavraParaBuscar = "y"; // Palavra a ser buscada

        System.out.println("--- Iniciando Testes SerialCPU ---");
        // O trabalho pede "Pelo menos 3 amostras de cada execução"
        int numeroDeExecucoesPorConfiguracao = 3;

        for (int i = 1; i <= numeroDeExecucoesPorConfiguracao; i++) {
            System.out.printf("\nExecução Serial CPU #%d para a palavra '%s'...\n", i, palavraParaBuscar);

            // Chama countOccurrences passando o número da execução atual
            ResultadoContagem resultado = countOccurrences(caminhoArquivoDataset, palavraParaBuscar, i);

            if (resultado != null) {
                System.out.printf("SerialCPU: %d ocorrências em %d ms (execução %d)%n",
                        resultado.ocorrencias, resultado.tempoMs, resultado.executionNumber);

                // Chamada corrigida para CSVExportar.salvarResultado com todos os 7 argumentos
                try {
                    CSVExportar.salvarResultado(
                            "cpu-serial",            // tipoExecucao
                            1,                       // idConfiguracaoGpuOuCpu (para serial, podemos usar 1 thread como referência)
                            resultado.tempoMs,       // tempoMs
                            resultado.ocorrencias,   // ocorrencias
                            caminhoArquivoDataset,   // nomeArquivoDataset
                            palavraParaBuscar,       // palavraBuscada
                            resultado.executionNumber // numeroExecucao
                    );
                    System.out.println("Resultado da execução #" + i + " salvo no CSV.");
                } catch (Exception e) {
                    System.err.println("Erro ao salvar resultado da execução #" + i + " no CSV: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Falha na execução Serial CPU #" + i);
            }
        }
        System.out.println("\n--- Testes SerialCPU Concluídos ---");
    }

    // Classe interna para armazenar o resultado da contagem
    static class ResultadoContagem {
        int ocorrencias;
        long tempoMs;
        int executionNumber; // Adicionado para rastrear o número da execução

        public ResultadoContagem(int ocorrencias, long tempoMs, int executionNumber) {
            this.ocorrencias = ocorrencias;
            this.tempoMs = tempoMs;
            this.executionNumber = executionNumber;
        }
    }
}



