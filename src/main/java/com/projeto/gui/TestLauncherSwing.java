package com.projeto.gui; // Ou o pacote onde estão suas outras classes de GUI

import com.projeto.serial.SerialCPU;
import com.projeto.parallelcpu.ParallelCPU;
import com.projeto.parallelgpu.ParallelGPU;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List; // <<<--- IMPORT CORRETO PARA java.util.List

public class TestLauncherSwing extends JFrame {

    private JTextField filePathField;
    private JButton chooseFileButton;
    private JTextField searchWordField;
    private JCheckBox serialCPUCheckbox;
    private JCheckBox parallelCPUCheckbox;
    private JSpinner numThreadsSpinner;
    private JCheckBox parallelGPUCheckbox;
    private JButton startTestsButton;
    private JTextArea logArea;

    private static final int EXECUTIONS_PER_TEST_RUN = 3;

    public TestLauncherSwing(String title) {
        super(title);
        initializeUIComponents();
        setupLayout();
        setupActionListeners();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 550);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeUIComponents() {
        filePathField = new JTextField(35);
        filePathField.setToolTipText("Caminho para o arquivo de dataset (.txt)");
        chooseFileButton = new JButton("Escolher Arquivo...");

        searchWordField = new JTextField(20);
        searchWordField.setToolTipText("Palavra a ser buscada no texto");

        serialCPUCheckbox = new JCheckBox("Serial CPU", true);
        parallelCPUCheckbox = new JCheckBox("Parallel CPU", true);
        parallelGPUCheckbox = new JCheckBox("Parallel GPU", true);

        SpinnerModel threadSpinnerModel = new SpinnerNumberModel(
                Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
                1, // valor mínimo
                Math.max(1, Runtime.getRuntime().availableProcessors() * 2),
                1  // passo
        );
        numThreadsSpinner = new JSpinner(threadSpinnerModel);
        numThreadsSpinner.setToolTipText("Número de threads para Parallel CPU");
        numThreadsSpinner.setEnabled(parallelCPUCheckbox.isSelected());
        parallelCPUCheckbox.addActionListener(e -> numThreadsSpinner.setEnabled(parallelCPUCheckbox.isSelected()));

        startTestsButton = new JButton("Iniciar Testes e Salvar no CSV");

        logArea = new JTextArea(15, 60);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
    }

    private void setupLayout() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; inputPanel.add(new JLabel("Arquivo Dataset:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; inputPanel.add(filePathField, gbc);
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; inputPanel.add(chooseFileButton, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(new JLabel("Palavra a Buscar:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; inputPanel.add(searchWordField, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 2; inputPanel.add(new JLabel("Algoritmos:"), gbc);
        JPanel checksPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        checksPanel.add(serialCPUCheckbox);
        checksPanel.add(parallelCPUCheckbox);
        checksPanel.add(new JLabel("Threads:"));
        Dimension spinnerSize = numThreadsSpinner.getPreferredSize();
        spinnerSize.width = 60;
        numThreadsSpinner.setPreferredSize(spinnerSize);
        checksPanel.add(numThreadsSpinner);
        checksPanel.add(parallelGPUCheckbox);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2; inputPanel.add(checksPanel, gbc);
        gbc.gridwidth = 1;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(startTestsButton);

        setLayout(new BorderLayout(10, 10));
        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void setupActionListeners() {
        chooseFileButton.addActionListener(e -> selectDatasetFile());
        startTestsButton.addActionListener(e -> runSelectedTests());
    }

    private void selectDatasetFile() {
        JFileChooser fileChooser = new JFileChooser(".");
        fileChooser.setDialogTitle("Selecione o arquivo de dataset (.txt)");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Arquivos de Texto (.txt)", "txt"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void runSelectedTests() {
        String filePath = filePathField.getText().trim();
        String searchWord = searchWordField.getText().trim();

        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, selecione um arquivo de dataset.", "Erro de Entrada", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (searchWord.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, digite uma palavra para buscar.", "Erro de Entrada", JOptionPane.ERROR_MESSAGE);
            return;
        }

        logArea.setText("");
        logMessage("Iniciando testes...");
        logMessage("Dataset: " + filePath);
        logMessage("Palavra: " + searchWord);

        startTestsButton.setEnabled(false);

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (serialCPUCheckbox.isSelected()) {
                    publish("--- Executando Serial CPU ---");
                    for (int i = 1; i <= EXECUTIONS_PER_TEST_RUN; i++) {
                        publish(String.format("Execução Serial #%d...", i));
                        SerialCPU.ResultadoContagem resultado = SerialCPU.countOccurrences(filePath, searchWord, i);
                        publish(String.format("SerialCPU: %d ocorrências em %d ms (execução %d)",
                                resultado.ocorrencias, resultado.tempoMs, resultado.executionNumber));
                    }
                    publish("--- Serial CPU Concluído ---");
                }

                if (parallelCPUCheckbox.isSelected()) {
                    publish("--- Executando Parallel CPU ---");
                    int numThreads = (Integer) numThreadsSpinner.getValue();
                    publish(String.format("Configuração: %d thread(s)", numThreads));
                    for (int i = 1; i <= EXECUTIONS_PER_TEST_RUN; i++) {
                        publish(String.format("Execução Parallel CPU #%d...", i));
                        // CORREÇÃO AQUI: De contarOcorrencias para countOccurrences
                        ParallelCPU.ResultadoContagem resultado = ParallelCPU.countOccurrences(filePath, searchWord, numThreads, i);
                        publish(String.format("ParallelCPU: %d ocorrências em %d ms (threads: %d, execução %d)",
                                resultado.ocorrencias, resultado.tempoMs, numThreads, resultado.executionNumber));
                    }
                    publish("--- Parallel CPU Concluído ---");
                }

                if (parallelGPUCheckbox.isSelected()) {
                    publish("--- Executando Parallel GPU ---");
                    for (int i = 1; i <= EXECUTIONS_PER_TEST_RUN; i++) {
                        publish(String.format("Execução Parallel GPU #%d...", i));
                        ParallelGPU.ResultadoContagem resultado = ParallelGPU.countOccurrences(filePath, searchWord, i);
                        publish(String.format("ParallelGPU: %d ocorrências em %d ms (execução %d)",
                                resultado.ocorrencias, resultado.tempoMs, resultado.executionNumber));
                    }
                    publish("--- Parallel GPU Concluído ---");
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) { // Usa java.util.List<String>
                for (String msg : chunks) {
                    logMessage(msg);
                }
            }

            @Override
            protected void done() {
                startTestsButton.setEnabled(true);
                logMessage("\nTodos os testes selecionados foram concluídos!");
                JOptionPane.showMessageDialog(TestLauncherSwing.this,
                        "Testes concluídos! Os resultados foram salvos em resultados.csv.",
                        "Concluído", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TestLauncherSwing("Lançador de Testes de Desempenho"));
    }
}
