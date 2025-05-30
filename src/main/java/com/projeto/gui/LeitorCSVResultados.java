package com.projeto.gui;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê os dados do arquivo CSV de resultados.
 */
class LeitorCSVResultados { // Removido 'public'

    public static List<ResultadoExecucao> lerResultados(String caminhoArquivoCsv) {
        List<ResultadoExecucao> resultados = new ArrayList<>();
        Path caminho = Paths.get(caminhoArquivoCsv);

        if (!Files.exists(caminho)) {
            System.err.println("Arquivo CSV não encontrado: " + caminhoArquivoCsv);
            return resultados;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivoCsv, StandardCharsets.UTF_8))) {
            String linha;
            boolean cabecalhoLido = false;
            while ((linha = br.readLine()) != null) {
                if (!cabecalhoLido) {
                    cabecalhoLido = true;
                    continue;
                }
                if (linha.trim().isEmpty()) {
                    continue;
                }

                String[] valores = linha.split(",");
                if (valores.length == 7) {
                    try {
                        String tipoExecucao = valores[0].trim();
                        int idConfigCpuGpu = Integer.parseInt(valores[1].trim());
                        long tempoMs = Long.parseLong(valores[2].trim());
                        int ocorrencias = Integer.parseInt(valores[3].trim());
                        String dataset = valores[4].trim();
                        String palavraBuscada = valores[5].trim();
                        int numExecucao = Integer.parseInt(valores[6].trim());

                        resultados.add(new ResultadoExecucao(tipoExecucao, idConfigCpuGpu, tempoMs, ocorrencias, dataset, palavraBuscada, numExecucao));
                    } catch (NumberFormatException e) {
                        System.err.println("Erro ao converter número na linha do CSV: \"" + linha + "\". Detalhe: " + e.getMessage());
                    }
                } else {
                    System.err.println("Linha com número incorreto de colunas (" + valores.length + ") ignorada: " + linha);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo CSV '" + caminhoArquivoCsv + "': " + e.getMessage());
            e.printStackTrace();
        }
        return resultados;
    }
}