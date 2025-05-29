package com.projeto.parallelcpu;

import com.projeto.util.CSVExportar; // Certifique-se que esta classe está no local correto e compilada
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ParallelCPU {

    // Classe interna para a tarefa de contagem de palavras executada por cada thread
    static class ContadorPalavras implements Callable<Integer> {
        private final String[] palavras; // Segmento de palavras para esta thread
        private final String palavraAlvo;
        private final int inicio;
        private final int fim;

        public ContadorPalavras(String[] palavras, String palavraAlvo, int inicio, int fim) {
            this.palavras = palavras;
            this.palavraAlvo = palavraAlvo.toLowerCase(); // Converter palavra alvo para minúsculas uma vez
            this.inicio = inicio;
            this.fim = fim;
        }

        @Override
        public Integer call() {
            int contagemLocal = 0;
            for (int i = inicio; i < fim; i++) {
                // Verifica se a palavra não é nula ou vazia antes de comparar
                if (palavras[i] != null && !palavras[i].isEmpty() && palavras[i].toLowerCase().equals(palavraAlvo)) {
                    contagemLocal++;
                }
            }
            return contagemLocal;
        }
    }

    // Método principal para contar ocorrências usando paralelismo na CPU
    public static ResultadoContagem contarOcorrencias(String caminhoArquivo, String palavra, int numThreads, int executionNumber) {
        long inicioTempo = System.currentTimeMillis();
        int totalOcorrencias = 0;

        String[] todasPalavras;
        try {
            todasPalavras = lerPalavrasDoArquivo(caminhoArquivo);
            if (todasPalavras == null || todasPalavras.length == 0) {
                System.err.println("Nenhuma palavra lida do arquivo ou arquivo vazio: " + caminhoArquivo);
                return new ResultadoContagem(0, 0, executionNumber);
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo: " + e.getMessage());
            return new ResultadoContagem(0, 0, executionNumber);
        }

        // Se numThreads for 0 ou negativo, ou se não houver palavras, não faz sentido continuar
        if (numThreads <= 0) {
            System.err.println("Número de threads deve ser positivo.");
            return new ResultadoContagem(0, System.currentTimeMillis() - inicioTempo, executionNumber);
        }


        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Integer>> listaDeResultadosFuturos = new ArrayList<>();

        int totalDePalavrasNoArquivo = todasPalavras.length;
        int tamanhoBlocoBase = totalDePalavrasNoArquivo / numThreads;
        int blocosComExtra = totalDePalavrasNoArquivo % numThreads; // Quantos blocos terão uma palavra a mais

        int indiceAtual = 0;
        for (int i = 0; i < numThreads; i++) {
            int inicioBloco = indiceAtual;
            int tamanhoBlocoAtual = tamanhoBlocoBase + (i < blocosComExtra ? 1 : 0);
            int fimBloco = inicioBloco + tamanhoBlocoAtual;

            // Garante que não ultrapasse o limite e que haja trabalho a ser feito
            if (inicioBloco < totalDePalavrasNoArquivo) {
                fimBloco = Math.min(fimBloco, totalDePalavrasNoArquivo); // Garante que não ultrapasse o array
                if (inicioBloco < fimBloco) { // Só submete se houver palavras no bloco
                    Callable<Integer> tarefa = new ContadorPalavras(todasPalavras, palavra, inicioBloco, fimBloco);
                    Future<Integer> resultadoFuturo = executor.submit(tarefa);
                    listaDeResultadosFuturos.add(resultadoFuturo);
                }
            }
            indiceAtual = fimBloco;
        }

        // Consolidação dos resultados de cada thread
        for (Future<Integer> resultadoFuturo : listaDeResultadosFuturos) {
            try {
                totalOcorrencias += resultadoFuturo.get(); // Bloqueia até a thread terminar e retorna o resultado
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Erro ao obter resultado da thread: " + e.getMessage());
                e.printStackTrace();
                // Pode-se optar por continuar e obter uma contagem parcial ou relançar/tratar o erro de forma mais robusta
            }
        }

        executor.shutdown(); // Inicia o desligamento do executor
        try {
            // Espera um tempo para as tarefas finalizarem. Se não finalizarem, força o desligamento.
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        long tempoTotal = System.currentTimeMillis() - inicioTempo;
        return new ResultadoContagem(totalOcorrencias, tempoTotal, executionNumber);
    }

    // Método para ler todas as palavras de um arquivo de texto
    private static String[] lerPalavrasDoArquivo(String caminhoArquivo) throws IOException {
        StringBuilder textoCompleto = new StringBuilder();
        try (BufferedReader leitor = new BufferedReader(new FileReader(caminhoArquivo, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                textoCompleto.append(linha).append(" "); // Adiciona espaço para separar palavras entre linhas
            }
        }
        // Divide o texto por qualquer caractere que não seja uma letra ou número (melhor que \W+ para evitar vazios)
        // e filtra strings vazias que podem surgir de múltiplos delimitadores.
        String[] palavrasBrutas = textoCompleto.toString().split("[^a-zA-Z0-9áéíóúâêîôûãõçÁÉÍÓÚÂÊÎÔÛÃÕÇ]+");
        List<String> palavrasLimpas = new ArrayList<>();
        for (String p : palavrasBrutas) {
            if (p != null && !p.trim().isEmpty()) {
                palavrasLimpas.add(p.trim());
            }
        }
        return palavrasLimpas.toArray(new String[0]);
    }

    // Método principal para executar os testes
    public static void main(String[] args) {
        // Ajuste o caminho se necessário. Este caminho assume que a pasta 'datasets'
        // está no mesmo nível que a pasta 'src' do seu projeto Maven,
        // e você está executando a partir da raiz do projeto.
        String caminhoArquivoDataset = "projeto.analise.comparativa/datasets/DonQuixote-388208.txt"; // Caminho relativo à raiz do projeto
        String palavraParaBuscar = "y"; // Palavra a ser buscada

        // Quantidades de threads a serem testadas, conforme especificado no trabalho (ajustar o número de núcleos)
        // O trabalho pede para "ajustando o número de núcleos de processamento disponíveis".
        // Geralmente, testamos com 1 (serial dentro do framework paralelo), 2, 4, número de núcleos físicos, número de núcleos lógicos.
        int[] quantidadesDeThreads = {2, 4, 8}; // Exemplo, ajuste conforme seu CPU

        System.out.println("--- Iniciando Testes ParallelCPU ---");
        // O trabalho pede "Pelo menos 3 amostras de cada execução"
        int numeroDeExecucoesPorConfiguracao = 3;

        for (int numThreadsParaTeste : quantidadesDeThreads) {
            System.out.printf("\nConfiguração: Testando com %d thread(s) para a palavra '%s'%n", numThreadsParaTeste, palavraParaBuscar);
            for (int i = 1; i <= numeroDeExecucoesPorConfiguracao; i++) {
                System.out.printf("Execução CPU #%d com %d thread(s)...\n", i, numThreadsParaTeste);

                ResultadoContagem resultado = contarOcorrencias(caminhoArquivoDataset, palavraParaBuscar, numThreadsParaTeste, i);

                if (resultado != null) {
                    System.out.printf("ParallelCPU: %d ocorrências em %d ms (threads: %d, execução: %d)%n",
                            resultado.ocorrencias, resultado.tempoMs, numThreadsParaTeste, resultado.executionNumber);

                    // Chamada corrigida para CSVExportar.salvarResultado
                    try {
                        CSVExportar.salvarResultado(
                                "cpu-parallel",          // tipoExecucao
                                numThreadsParaTeste,     // idConfiguracaoGpuOuCpu (usando o número de threads aqui)
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
                    System.out.println("Falha na execução CPU #" + i + " com " + numThreadsParaTeste + " thread(s).");
                }
            }
        }
        System.out.println("\n--- Testes ParallelCPU Concluídos ---");
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
