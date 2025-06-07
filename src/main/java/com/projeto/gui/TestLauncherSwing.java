package com.projeto.gui;

import com.projeto.serial.SerialCPU;
import com.projeto.parallelcpu.ParallelCPU;
import com.projeto.parallelgpu.ParallelGPU;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class TestLauncherSwing extends JFrame {

    // Componentes da UI
    private JTextField campoCaminhoArquivo;
    private JButton botaoEscolherArquivo;
    private JTextField campoPalavraBusca;
    private JCheckBox checkSerialCPU;
    private JCheckBox checkParaleloCPU;
    private JSpinner spinnerNumThreads;
    private JCheckBox checkParaleloGPU;
    private JButton botaoIniciarTestes;
    private JTextArea areaLog;

    private static final int NUMERO_EXECUCOES_POR_TESTE = 3;

    public TestLauncherSwing(String titulo) {
        super(titulo);
        inicializarComponentesUI();
        configurarLayout();
        configurarAcoes();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 550);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void inicializarComponentesUI() {
        campoCaminhoArquivo = new JTextField(35);
        campoCaminhoArquivo.setToolTipText("Caminho para o arquivo de dataset (.txt)");
        botaoEscolherArquivo = new JButton("Escolher Arquivo...");

        campoPalavraBusca = new JTextField(20);
        campoPalavraBusca.setToolTipText("Palavra a ser buscada no texto");

        checkSerialCPU = new JCheckBox("CPU Serial", true);
        checkParaleloCPU = new JCheckBox("CPU Paralelo", true);
        checkParaleloGPU = new JCheckBox("GPU Paralelo", true);

        SpinnerModel modeloSpinner = new SpinnerNumberModel(
                Math.max(1, Runtime.getRuntime().availableProcessors() / 2), // Valor inicial dinâmico
                1, // Valor mínimo
                Math.max(1, Runtime.getRuntime().availableProcessors() * 2), // Valor máximo sugerido
                1  // Passo
        );
        spinnerNumThreads = new JSpinner(modeloSpinner);
        spinnerNumThreads.setToolTipText("Número de threads para CPU Paralelo");
        spinnerNumThreads.setEnabled(checkParaleloCPU.isSelected());
        checkParaleloCPU.addActionListener(e -> spinnerNumThreads.setEnabled(checkParaleloCPU.isSelected()));

        botaoIniciarTestes = new JButton("Iniciar Testes e Salvar no CSV");

        areaLog = new JTextArea(15, 60);
        areaLog.setEditable(false);
        areaLog.setLineWrap(true);
        areaLog.setWrapStyleWord(true);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
    }

    private void configurarLayout() {
        JPanel painelEntrada = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Linha 1: Escolher Arquivo
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; painelEntrada.add(new JLabel("Arquivo Dataset:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; painelEntrada.add(campoCaminhoArquivo, gbc);
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; painelEntrada.add(botaoEscolherArquivo, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Linha 2: Palavra a Buscar
        gbc.gridx = 0; gbc.gridy = 1; painelEntrada.add(new JLabel("Palavra a Buscar:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; painelEntrada.add(campoPalavraBusca, gbc);
        gbc.gridwidth = 1;

        // Linha 3: Seleção de Algoritmos
        gbc.gridx = 0; gbc.gridy = 2; painelEntrada.add(new JLabel("Algoritmos:"), gbc);
        JPanel painelChecks = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        painelChecks.add(checkSerialCPU);
        painelChecks.add(checkParaleloCPU);
        painelChecks.add(new JLabel("Threads:"));
        Dimension tamanhoSpinner = spinnerNumThreads.getPreferredSize();
        tamanhoSpinner.width = 60;
        spinnerNumThreads.setPreferredSize(tamanhoSpinner);
        painelChecks.add(spinnerNumThreads);
        painelChecks.add(checkParaleloGPU);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2; painelEntrada.add(painelChecks, gbc);
        gbc.gridwidth = 1;

        // Botão Iniciar
        JPanel painelBotao = new JPanel(new FlowLayout(FlowLayout.CENTER));
        painelBotao.add(botaoIniciarTestes);

        // Montagem final
        setLayout(new BorderLayout(10, 10));
        add(painelEntrada, BorderLayout.NORTH);
        add(new JScrollPane(areaLog), BorderLayout.CENTER);
        add(painelBotao, BorderLayout.SOUTH);
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void configurarAcoes() {
        botaoEscolherArquivo.addActionListener(e -> selecionarArquivoDataset());
        botaoIniciarTestes.addActionListener(e -> executarTestesSelecionados());
    }

    private void selecionarArquivoDataset() {
        JFileChooser seletorArquivo = new JFileChooser(".");
        seletorArquivo.setDialogTitle("Selecione o arquivo de dataset (.txt)");
        seletorArquivo.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Arquivos de Texto (.txt)", "txt"));
        seletorArquivo.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int resultado = seletorArquivo.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File arquivoSelecionado = seletorArquivo.getSelectedFile();
            campoCaminhoArquivo.setText(arquivoSelecionado.getAbsolutePath());
        }
    }

    private void executarTestesSelecionados() {
        String caminhoArquivo = campoCaminhoArquivo.getText().trim();
        String palavraBusca = campoPalavraBusca.getText().trim();

        if (caminhoArquivo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, selecione um arquivo de dataset.", "Erro de Entrada", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (palavraBusca.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, digite uma palavra para buscar.", "Erro de Entrada", JOptionPane.ERROR_MESSAGE);
            return;
        }

        areaLog.setText("");
        registrarLog("Iniciando testes...");
        registrarLog("Dataset: " + caminhoArquivo);
        registrarLog("Palavra: " + palavraBusca);

        botaoIniciarTestes.setEnabled(false);

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (checkSerialCPU.isSelected()) {
                    publish("--- Executando CPU Serial ---");
                    for (int i = 1; i <= NUMERO_EXECUCOES_POR_TESTE; i++) {
                        publish(String.format("Execução Serial #%d...", i));
                        SerialCPU.ResultadoContagem resultado = SerialCPU.countOccurrences(caminhoArquivo, palavraBusca, i);
                        publish(String.format("CPU Serial: %d ocorrências em %d ms (execução %d)",
                                resultado.ocorrencias, resultado.tempoMs, resultado.executionNumber));
                    }
                    publish("--- CPU Serial Concluído ---");
                }

                if (checkParaleloCPU.isSelected()) {
                    publish("--- Executando CPU Paralelo ---");
                    int numThreads = (Integer) spinnerNumThreads.getValue();
                    publish(String.format("Configuração: %d thread(s)", numThreads));
                    for (int i = 1; i <= NUMERO_EXECUCOES_POR_TESTE; i++) {
                        publish(String.format("Execução CPU Paralelo #%d...", i));
                        ParallelCPU.ResultadoContagem resultado = ParallelCPU.countOccurrences(caminhoArquivo, palavraBusca, numThreads, i);
                        publish(String.format("CPU Paralelo: %d ocorrências em %d ms (threads: %d, execução %d)",
                                resultado.ocorrencias, resultado.tempoMs, numThreads, resultado.executionNumber));
                    }
                    publish("--- CPU Paralelo Concluído ---");
                }

                if (checkParaleloGPU.isSelected()) {
                    publish("--- Executando GPU Paralelo ---");
                    for (int i = 1; i <= NUMERO_EXECUCOES_POR_TESTE; i++) {
                        publish(String.format("Execução GPU Paralelo #%d...", i));
                        ParallelGPU.ResultadoContagem resultado = ParallelGPU.contarOcorrencias(caminhoArquivo, palavraBusca, i);
                        publish(String.format("GPU Paralelo: %d ocorrências em %d ms (execução %d)",
                                resultado.ocorrencias, resultado.tempoMs, resultado.executionNumber));
                    }
                    publish("--- GPU Paralelo Concluído ---");
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    registrarLog(msg);
                }
            }

            @Override
            protected void done() {
                botaoIniciarTestes.setEnabled(true);
                registrarLog("\nTodos os testes selecionados foram concluídos!");
                JOptionPane.showMessageDialog(TestLauncherSwing.this,
                        "Testes concluídos! Os resultados foram salvos em resultados.csv.",
                        "Concluído", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    private void registrarLog(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            areaLog.append(mensagem + "\n");
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TestLauncherSwing("Lançador de Testes de Desempenho"));
    }
}