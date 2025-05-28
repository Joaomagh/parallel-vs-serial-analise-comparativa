package com.projeto.serial;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SerialCPU {

    public static int countOccurrences(String filePath, String word) {
        int count = 0;
        long startTime = System.currentTimeMillis();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\W+"); // separa por palavras (ignora pontuação)
                for (String w : words) {
                    if (w.equalsIgnoreCase(word)) {
                        count++;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        System.out.printf("SerialCPU: %d ocorrências em %d ms%n", count, (endTime - startTime));
        return count;
    }

    // Método de teste rápido
    public static void main(String[] args) {
        String filePath = "datasets/DonQuixote-388208.txt"; // caminho relativo
        String wordToCount = "y";

        countOccurrences(filePath, wordToCount);
    }
}