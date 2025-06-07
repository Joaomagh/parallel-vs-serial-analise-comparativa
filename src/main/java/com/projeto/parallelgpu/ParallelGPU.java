package com.projeto.parallelgpu;

import org.jocl.*;
import com.projeto.util.CSVExportar;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.jocl.CL.*;

public class ParallelGPU {
    private static final String CODIGO_KERNEL =
            "inline int is_alphanumeric(char c) {\n" +
                    "    return ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'));\n" +
                    "}\n" +
                    "\n" +
                    "__kernel void countOccurrences(__global const char* text, \n" +
                    "                             __global const char* word, \n" +
                    "                             const int wordLength, \n" +
                    "                             const int textLength, \n" +
                    "                             __global int* results) {\n" +
                    "    int gid = get_global_id(0);\n" +
                    "    results[gid] = 0;\n" +
                    "    if (wordLength == 0) return;\n" +
                    "    if (gid <= textLength - wordLength) {\n" +
                    "        int potential_match = 1;\n" +
                    "        for (int i = 0; i < wordLength; i++) {\n" +
                    "            if (text[gid + i] != word[i]) {\n" +
                    "                potential_match = 0;\n" +
                    "                break;\n" +
                    "            }\n" +
                    "        }\n" +
                    "        if (potential_match) {\n" +
                    "            int prev_char_is_delimiter = (gid == 0) || (!is_alphanumeric(text[gid - 1]));\n" +
                    "            int next_char_is_delimiter = (gid + wordLength == textLength) || (!is_alphanumeric(text[gid + wordLength]));\n" +
                    "            if (prev_char_is_delimiter && next_char_is_delimiter) {\n" +
                    "                results[gid] = 1;\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";

    public static ResultadoContagem contarOcorrencias(String caminhoArquivo, String palavra, int numExecucao) {
        long tempoInicial = System.currentTimeMillis();
        int contagem = 0;
        String textoOriginal = lerArquivo(caminhoArquivo);

        if (textoOriginal == null) {
            System.err.println("Não foi possível ler o arquivo em GPUParalelo: " + caminhoArquivo);
            long tempoErro = System.currentTimeMillis() - tempoInicial;
            try {
                CSVExportar.salvarResultado("gpu-erro", 0, tempoErro, 0, caminhoArquivo, palavra, numExecucao);
            } catch (Exception csvEx) { System.err.println("Erro ao salvar resultado de erro para GPUParalelo no CSV: " + csvEx.getMessage()); }
            return new ResultadoContagem(0, tempoErro, numExecucao);
        }

        String textoParaProcessar = textoOriginal.toLowerCase();
        String palavraParaBuscar = palavra.toLowerCase();

        CL.setExceptionsEnabled(true);
        cl_platform_id plataforma = null;
        cl_device_id dispositivo = null;
        cl_context contexto = null;
        cl_command_queue filaComandos = null;
        cl_program programa = null;
        cl_kernel kernel = null;
        cl_mem bufferTexto = null;
        cl_mem bufferPalavra = null;
        cl_mem bufferResultados = null;
        cl_event eventoKernel = null;

        try {
            plataforma = obterPlataforma();
            dispositivo = obterDispositivo(plataforma);
            contexto = criarContexto(plataforma, dispositivo);
            filaComandos = criarFilaDeComandos(contexto, dispositivo);

            byte[] bytesTexto = textoParaProcessar.getBytes(StandardCharsets.UTF_8);
            byte[] bytesPalavra = palavraParaBuscar.getBytes(StandardCharsets.UTF_8);
            int[] arrayResultados = new int[bytesTexto.length];

            bufferTexto = clCreateBuffer(contexto, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, (long)bytesTexto.length * Sizeof.cl_char, Pointer.to(bytesTexto), null);
            bufferPalavra = clCreateBuffer(contexto, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, (long)bytesPalavra.length * Sizeof.cl_char, Pointer.to(bytesPalavra), null);
            bufferResultados = clCreateBuffer(contexto, CL_MEM_WRITE_ONLY, (long)arrayResultados.length * Sizeof.cl_int, null, null);

            programa = criarPrograma(contexto, CODIGO_KERNEL);
            compilarPrograma(programa, dispositivo);
            kernel = criarKernel(programa, "countOccurrences");
            definirArgumentosKernel(kernel, bufferTexto, bufferPalavra, bytesPalavra.length, bytesTexto.length, bufferResultados);

            long tamanhoGlobalTrabalho = 0;
            if (bytesTexto.length > 0 && bytesPalavra.length > 0 && bytesTexto.length >= bytesPalavra.length) {
                tamanhoGlobalTrabalho = (long)bytesTexto.length - bytesPalavra.length + 1;
            }

            if (tamanhoGlobalTrabalho > 0) {
                eventoKernel = new cl_event();
                clEnqueueNDRangeKernel(filaComandos, kernel, 1, null, new long[]{tamanhoGlobalTrabalho}, null, 0, null, eventoKernel);
                clWaitForEvents(1, new cl_event[]{eventoKernel});
                clEnqueueReadBuffer(filaComandos, bufferResultados, CL_TRUE, 0, (long)arrayResultados.length * Sizeof.cl_int, Pointer.to(arrayResultados), 0, null, null);
                for (int i = 0; i < tamanhoGlobalTrabalho; i++) {
                    contagem += arrayResultados[i];
                }
            }
        } catch (CLException e) {
            System.err.println("Erro JOCL em GPUParalelo: " + e.getMessage());
            e.printStackTrace();
            long tempoErro = System.currentTimeMillis() - tempoInicial;
            try {
                CSVExportar.salvarResultado("gpu-erro", 0, tempoErro, contagem, caminhoArquivo, palavra, numExecucao);
            } catch (Exception csvEx) { System.err.println("Erro ao salvar resultado de erro para GPUParalelo no CSV: " + csvEx.getMessage()); }
            return new ResultadoContagem(contagem, tempoErro, numExecucao);
        } finally {
            if (eventoKernel != null) clReleaseEvent(eventoKernel);
            if (kernel != null) clReleaseKernel(kernel);
            if (programa != null) clReleaseProgram(programa);
            if (bufferResultados != null) clReleaseMemObject(bufferResultados);
            if (bufferPalavra != null) clReleaseMemObject(bufferPalavra);
            if (bufferTexto != null) clReleaseMemObject(bufferTexto);
            if (filaComandos != null) clReleaseCommandQueue(filaComandos);
            if (contexto != null) clReleaseContext(contexto);
        }

        long tempoTotal = System.currentTimeMillis() - tempoInicial;
        try {
            CSVExportar.salvarResultado("gpu", 0, tempoTotal, contagem, caminhoArquivo, palavra, numExecucao);
        } catch (Exception e) {
            System.err.println("Erro ao salvar resultado para GPUParalelo no CSV: " + e.getMessage());
            e.printStackTrace();
        }
        return new ResultadoContagem(contagem, tempoTotal, numExecucao);
    }

    private static cl_platform_id obterPlataforma() {
        cl_platform_id[] plataformas = new cl_platform_id[1];
        clGetPlatformIDs(1, plataformas, null);
        return plataformas[0];
    }
    private static cl_device_id obterDispositivo(cl_platform_id plataforma) {
        cl_device_id[] dispositivos = new cl_device_id[1];
        clGetDeviceIDs(plataforma, CL_DEVICE_TYPE_GPU, 1, dispositivos, null);
        return dispositivos[0];
    }
    private static cl_context criarContexto(cl_platform_id plataforma, cl_device_id dispositivo) {
        cl_context_properties propriedadesContexto = new cl_context_properties();
        propriedadesContexto.addProperty(CL_CONTEXT_PLATFORM, plataforma);
        return clCreateContext(propriedadesContexto, 1, new cl_device_id[]{dispositivo}, null, null, null);
    }
    private static cl_command_queue criarFilaDeComandos(cl_context contexto, cl_device_id dispositivo) {
        return clCreateCommandQueue(contexto, dispositivo, 0, null);
    }
    private static cl_program criarPrograma(cl_context contexto, String codigoFonte) {
        return clCreateProgramWithSource(contexto, 1, new String[]{codigoFonte}, null, null);
    }
    private static void compilarPrograma(cl_program programa, cl_device_id dispositivo) {
        clBuildProgram(programa, 1, new cl_device_id[]{dispositivo}, null, null, null);
    }
    private static cl_kernel criarKernel(cl_program programa, String nomeKernel) {
        return clCreateKernel(programa, nomeKernel, null);
    }
    private static void definirArgumentosKernel(cl_kernel kernel, cl_mem bufferTexto, cl_mem bufferPalavra, int tamanhoPalavra, int tamanhoTexto, cl_mem bufferResultados) {
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(bufferTexto));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(bufferPalavra));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{tamanhoPalavra}));
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{tamanhoTexto}));
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(bufferResultados));
    }
    private static String lerArquivo(String caminhoArquivo) {
        StringBuilder conteudo = new StringBuilder();
        try (BufferedReader leitor = new BufferedReader(new FileReader(caminhoArquivo, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                conteudo.append(linha).append("\n");
            }
            return conteudo.toString();
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo do sistema: " + caminhoArquivo + " - " + e.getMessage());
            return null;
        }
    }

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
