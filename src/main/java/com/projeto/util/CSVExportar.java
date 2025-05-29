package com.projeto.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets; // Boa prática especificar o charset
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CSVExportar {

    // Cabeçalho atualizado para incluir todas as informações
    private static final String CABECALHO = "tipo_execucao,id_config_cpu_gpu,tempo_ms,ocorrencias,dataset,palavra_buscada,num_execucao";
    private static final String ARQUIVO_SAIDA = "results/csv/resultados.csv"; // Certifique-se que a pasta 'results/csv' pode ser criada ou já existe

    // Cria o CSV se não existir, com cabeçalho
    private static void inicializarCSV() throws IOException {
        Path caminho = Paths.get(ARQUIVO_SAIDA);
        if (Files.notExists(caminho)) {
            // Cria os diretórios pais se não existirem
            if (caminho.getParent() != null) {
                Files.createDirectories(caminho.getParent());
            }
            // Escreve o cabeçalho no arquivo, usando UTF-8
            // false no construtor do FileWriter para sobrescrever/criar novo, se o objetivo é apenas escrever o cabeçalho uma vez.
            try (PrintWriter writer = new PrintWriter(new FileWriter(ARQUIVO_SAIDA, StandardCharsets.UTF_8, false))) {
                writer.println(CABECALHO);
            }
        }
    }

    // Adiciona uma linha de resultado no CSV
    public static void salvarResultado(String tipoExecucao, int idConfiguracaoGpuOuCpu, long tempoMs, int ocorrencias, String nomeArquivoDataset, String palavraBuscada, int numeroExecucao) {
        try {
            inicializarCSV(); // Garante que o arquivo e o cabeçalho existam
            // Abre o arquivo para adicionar conteúdo (append=true), usando UTF-8
            try (PrintWriter writer = new PrintWriter(new FileWriter(ARQUIVO_SAIDA, StandardCharsets.UTF_8, true))) {
                // Usa os parâmetros corretos e todos eles
                writer.printf("%s,%d,%d,%d,%s,%s,%d%n",
                        tipoExecucao,
                        idConfiguracaoGpuOuCpu,
                        tempoMs,
                        ocorrencias,
                        nomeArquivoDataset,
                        palavraBuscada,
                        numeroExecucao
                );
            }
        } catch (IOException e) {
            System.err.println("Erro ao escrever no arquivo CSV: " + e.getMessage());
            e.printStackTrace(); // Ajuda a depurar
        }
    }

    // Método main para teste rápido do CSVExportar (opcional)
    public static void main(String[] args) {
        System.out.println("Testando CSVExportar...");
        // Exemplo de como chamar (simulando dados)
        salvarResultado("gpu", 0, 150L, 1200, "datasets/texto_grande.txt", "unifor", 1);
        salvarResultado("cpu-serial", 1, 500L, 1190, "datasets/texto_grande.txt", "unifor", 1);
        salvarResultado("cpu-parallel", 8, 80L, 1205, "datasets/texto_grande.txt", "unifor", 1);
        System.out.println("Verifique o arquivo: " + Paths.get(ARQUIVO_SAIDA).toAbsolutePath());
    }
}
