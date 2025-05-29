package com.projeto.parallelgpu;

import org.jocl.*;
import com.projeto.util.CSVExportar; // Certifique-se que esta classe existe e está correta
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.jocl.CL.*;

public class ParallelGPU {
    // Código do kernel OpenCL MODIFICADO para contar palavras inteiras (comentários internos removidos)
    private static final String KERNEL_SOURCE =
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
                    "\n" +
                    "    if (wordLength == 0) return;\n" +
                    "\n" +
                    "    if (gid <= textLength - wordLength) {\n" +
                    "        int potential_match = 1;\n" +
                    "        for (int i = 0; i < wordLength; i++) {\n" +
                    "            if (text[gid + i] != word[i]) {\n" +
                    "                potential_match = 0;\n" +
                    "                break;\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        if (potential_match) {\n" +
                    "            int prev_char_is_delimiter = (gid == 0) || (!is_alphanumeric(text[gid - 1]));\n" +
                    "            int next_char_is_delimiter = (gid + wordLength == textLength) || (!is_alphanumeric(text[gid + wordLength]));\n" +
                    "\n" +
                    "            if (prev_char_is_delimiter && next_char_is_delimiter) {\n" +
                    "                results[gid] = 1;\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";

    public static ResultadoContagem countOccurrences(String filePath, String word, int executionNumber) {
        long startTime = System.currentTimeMillis();
        int count = 0;
        String originalText = readFile(filePath);
        if (originalText == null) {
            System.err.println("Não foi possível ler o arquivo: " + filePath);
            return new ResultadoContagem(0, 0, executionNumber);
        }

        String textToProcess = originalText.toLowerCase();
        String wordToSearch = word.toLowerCase();

        CL.setExceptionsEnabled(true);

        cl_platform_id platform = null;
        cl_device_id device = null;
        cl_context context = null;
        cl_command_queue commandQueue = null;
        cl_program program = null;
        cl_kernel kernel = null;
        cl_mem textBuffer = null;
        cl_mem wordBuffer = null;
        cl_mem resultsBuffer = null;
        cl_event kernelEvent = null;

        try {
            platform = getPlatform();
            device = getDevice(platform);
            context = createContext(platform, device);
            commandQueue = createCommandQueue(context, device);

            byte[] textBytes = textToProcess.getBytes(StandardCharsets.UTF_8);
            byte[] wordBytes = wordToSearch.getBytes(StandardCharsets.UTF_8);
            int[] resultsArray = new int[textBytes.length];


            textBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    (long)textBytes.length * Sizeof.cl_char, Pointer.to(textBytes), null);
            wordBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    (long)wordBytes.length * Sizeof.cl_char, Pointer.to(wordBytes), null);
            resultsBuffer = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                    (long)resultsArray.length * Sizeof.cl_int, null, null);


            program = createProgram(context, KERNEL_SOURCE);
            buildProgram(program, device);

            kernel = createKernel(program, "countOccurrences");
            setKernelArgs(kernel, textBuffer, wordBuffer, wordBytes.length, textBytes.length, resultsBuffer);

            long globalWorkSize = 0;
            if (textBytes.length > 0 && wordBytes.length > 0 && textBytes.length >= wordBytes.length) {
                globalWorkSize = (long)textBytes.length - wordBytes.length + 1;
            }


            if (globalWorkSize > 0) {
                kernelEvent = new cl_event();
                clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                        new long[]{globalWorkSize}, null, 0, null, kernelEvent);
                clWaitForEvents(1, new cl_event[]{kernelEvent});
                clEnqueueReadBuffer(commandQueue, resultsBuffer, CL_TRUE, 0,
                        (long)resultsArray.length * Sizeof.cl_int, Pointer.to(resultsArray), 0, null, null);

                for (int i = 0; i < globalWorkSize; i++) {
                    count += resultsArray[i];
                }
            } else {
                count = 0;
            }

        } catch (CLException e) {
            System.err.println("Erro JOCL: " + e.getMessage());
            e.printStackTrace();
            return new ResultadoContagem(count, System.currentTimeMillis() - startTime, executionNumber);
        } finally {
            if (kernelEvent != null) clReleaseEvent(kernelEvent);
            if (kernel != null) clReleaseKernel(kernel);
            if (program != null) clReleaseProgram(program);
            if (resultsBuffer != null) clReleaseMemObject(resultsBuffer);
            if (wordBuffer != null) clReleaseMemObject(wordBuffer);
            if (textBuffer != null) clReleaseMemObject(textBuffer);
            if (commandQueue != null) clReleaseCommandQueue(commandQueue);
            if (context != null) clReleaseContext(context);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        return new ResultadoContagem(count, totalTime, executionNumber);
    }

    private static cl_platform_id getPlatform() {
        cl_platform_id[] platforms = new cl_platform_id[1];
        int[] numPlatforms = new int[1];
        int err = clGetPlatformIDs(1, platforms, numPlatforms);
        if (err != CL_SUCCESS) {
            throw new CLException("Falha ao obter ID da plataforma: " + stringFor_errorCode(err));
        }
        if (numPlatforms[0] == 0) {
            throw new RuntimeException("Nenhuma plataforma OpenCL encontrada.");
        }
        return platforms[0];
    }

    private static cl_device_id getDevice(cl_platform_id platform) {
        cl_device_id[] devices = new cl_device_id[1];
        int[] numDevices = new int[1];
        int err = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 1, devices, numDevices);
        if (err == CL_DEVICE_NOT_FOUND) {
            System.out.println("Nenhum dispositivo GPU encontrado, tentando CPU...");
            err = clGetDeviceIDs(platform, CL_DEVICE_TYPE_CPU, 1, devices, numDevices);
        }
        if (err != CL_SUCCESS) {
            throw new CLException("Falha ao obter ID do dispositivo: " + stringFor_errorCode(err));
        }
        if (numDevices[0] == 0) {
            throw new RuntimeException("Nenhum dispositivo OpenCL (GPU ou CPU) encontrado.");
        }
        return devices[0];
    }

    private static cl_context createContext(cl_platform_id platform, cl_device_id device) {
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
        int[] errcode_ret = new int[1];
        cl_context context = clCreateContext(contextProperties, 1, new cl_device_id[]{device}, null, null, errcode_ret);
        if (errcode_ret[0] != CL_SUCCESS) {
            throw new CLException("Falha ao criar contexto OpenCL: " + stringFor_errorCode(errcode_ret[0]));
        }
        return context;
    }

    private static cl_command_queue createCommandQueue(cl_context context, cl_device_id device) {
        long properties = 0;
        int[] errcode_ret = new int[1];
        cl_command_queue commandQueue = clCreateCommandQueue(context, device, properties, errcode_ret);
        if (errcode_ret[0] != CL_SUCCESS) {
            throw new CLException("Falha ao criar fila de comando: " + stringFor_errorCode(errcode_ret[0]));
        }
        return commandQueue;
    }

    private static cl_program createProgram(cl_context context, String kernelSource) {
        int[] errcode_ret = new int[1];
        cl_program program = clCreateProgramWithSource(context, 1, new String[]{kernelSource}, null, errcode_ret);
        if (errcode_ret[0] != CL_SUCCESS) {
            throw new CLException("Falha ao criar programa: " + stringFor_errorCode(errcode_ret[0]));
        }
        return program;
    }

    private static void buildProgram(cl_program program, cl_device_id device) {
        int err = clBuildProgram(program, 1, new cl_device_id[]{device}, null, null, null);
        if (err != CL_SUCCESS) {
            long[] logSize = new long[1];
            clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG, 0, null, logSize);
            if (logSize[0] == 0 && err != CL_SUCCESS) {
                throw new CLException("Falha ao construir programa: " + stringFor_errorCode(err) + " (Log de Build vazio ou não disponível)");
            }
            byte[] logData = new byte[(int)logSize[0]];
            clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG, logSize[0], Pointer.to(logData), null);
            String buildLog = new String(logData, 0, (logData.length > 0 && logData[logData.length-1] == 0) ? logData.length - 1 : logData.length, StandardCharsets.UTF_8);
            throw new CLException("Falha ao construir programa: " + stringFor_errorCode(err) + "\nLog de Build:\n" + buildLog);
        }
    }

    private static cl_kernel createKernel(cl_program program, String kernelName) {
        int[] errcode_ret = new int[1];
        cl_kernel kernel = clCreateKernel(program, kernelName, errcode_ret);
        if (errcode_ret[0] != CL_SUCCESS) {
            throw new CLException("Falha ao criar kernel '" + kernelName + "': " + stringFor_errorCode(errcode_ret[0]));
        }
        return kernel;
    }

    private static void setKernelArgs(cl_kernel kernel, cl_mem textBuffer, cl_mem wordBuffer,
                                      int wordLength, int textLength, cl_mem resultsBuffer) {
        int argIndex = 0;
        int err;
        err = clSetKernelArg(kernel, argIndex++, Sizeof.cl_mem, Pointer.to(textBuffer));
        if (err != CL_SUCCESS) throw new CLException("clSetKernelArg falhou para textBuffer: " + stringFor_errorCode(err));
        err = clSetKernelArg(kernel, argIndex++, Sizeof.cl_mem, Pointer.to(wordBuffer));
        if (err != CL_SUCCESS) throw new CLException("clSetKernelArg falhou para wordBuffer: " + stringFor_errorCode(err));
        err = clSetKernelArg(kernel, argIndex++, Sizeof.cl_int, Pointer.to(new int[]{wordLength}));
        if (err != CL_SUCCESS) throw new CLException("clSetKernelArg falhou para wordLength: " + stringFor_errorCode(err));
        err = clSetKernelArg(kernel, argIndex++, Sizeof.cl_int, Pointer.to(new int[]{textLength}));
        if (err != CL_SUCCESS) throw new CLException("clSetKernelArg falhou para textLength: " + stringFor_errorCode(err));
        err = clSetKernelArg(kernel, argIndex++, Sizeof.cl_mem, Pointer.to(resultsBuffer));
        if (err != CL_SUCCESS) throw new CLException("clSetKernelArg falhou para resultsBuffer: " + stringFor_errorCode(err));
    }

    private static String readFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo do sistema: " + filePath + " - " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        String caminhoArquivo = "projeto.analise.comparativa/datasets/DonQuixote-388208.txt";
        String palavraBuscar = "y";

        for (int i = 1; i <= 3; i++) {
            System.out.printf("\nExecução GPU #%d para a palavra '%s'...\n", i, palavraBuscar);
            ResultadoContagem resultado = countOccurrences(caminhoArquivo, palavraBuscar, i);
            if (resultado != null) {
                System.out.printf("ParallelGPU: %d ocorrências em %d ms (execução %d)%n",
                        resultado.ocorrencias, resultado.tempoMs, resultado.executionNumber);
                try {
                    CSVExportar.salvarResultado("gpu", 0, resultado.tempoMs, resultado.ocorrencias,
                            caminhoArquivo, palavraBuscar, resultado.executionNumber);
                    System.out.println("Resultado salvo no CSV.");
                } catch (Exception e) {
                    System.err.println("Erro ao salvar resultado no CSV: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Falha na execução GPU #" + i);
            }
        }
    }

    static class ResultadoContagem {
        int ocorrencias;
        long tempoMs;
        int executionNumber;

        public ResultadoContagem(int ocorrencias, long tempoMs, int executionNumber) {
            this.ocorrencias = ocorrencias;
            this.tempoMs = tempoMs;
            this.executionNumber = executionNumber;
        }
    }
}
