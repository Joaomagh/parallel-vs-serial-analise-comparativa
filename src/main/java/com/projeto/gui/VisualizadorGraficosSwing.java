package com.projeto.gui; // Pacote ajustado para com.projeto.gui

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;     // Import para ArrayList
import java.util.Comparator;
import java.util.List;          // Import para List
import java.util.Map;
import java.util.Set;           // Import para Set
import java.util.TreeSet;       // Import para TreeSet
import java.util.stream.Collectors; // Import para Collectors
import java.nio.file.Paths;         // Import para Paths

public class VisualizadorGraficosSwing extends JFrame {

    private static final String CAMINHO_CSV = "results/csv/resultados.csv";
    private List<ResultadoExecucao> todosOsResultados;

    private JComboBox<String> comboDatasetsDisplay;
    private JComboBox<String> comboPalavras;
    private JButton botaoAtualizarGrafico;
    private ChartPanel painelDoGraficoJFree;
    private JPanel painelPrincipal;

    public VisualizadorGraficosSwing(String tituloJanela) {
        super(tituloJanela);

        todosOsResultados = LeitorCSVResultados.lerResultados(CAMINHO_CSV);

        if (todosOsResultados.isEmpty()) { // List.isEmpty() é um método válido
            JOptionPane.showMessageDialog(this,
                    "Nenhum dado encontrado no arquivo CSV ou o arquivo não foi encontrado.\n" +
                            "Verifique o caminho: " + Paths.get(CAMINHO_CSV).toAbsolutePath(),
                    "Erro de Dados", JOptionPane.ERROR_MESSAGE);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(600, 150);
            setLocationRelativeTo(null);
            setVisible(true);
            return;
        }

        inicializarComponentesUI();
        if (!todosOsResultados.isEmpty()) {
            atualizarGrafico();
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void inicializarComponentesUI() {
        painelPrincipal = new JPanel(new BorderLayout());
        JPanel painelControles = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        comboDatasetsDisplay = new JComboBox<>();
        // Collectors.toCollection e TreeSet são válidos com os imports corretos
        Set<String> datasetsDisplayUnicos = todosOsResultados.stream()
                .map(ResultadoExecucao::getDatasetFileName)
                .collect(Collectors.toCollection(TreeSet::new));
        for (String dsName : datasetsDisplayUnicos) {
            comboDatasetsDisplay.addItem(dsName);
        }
        comboDatasetsDisplay.addActionListener(e -> atualizarComboPalavras());

        comboPalavras = new JComboBox<>();
        if (!datasetsDisplayUnicos.isEmpty()) {
            atualizarComboPalavras();
        }

        botaoAtualizarGrafico = new JButton("Atualizar Gráfico");
        botaoAtualizarGrafico.addActionListener(e -> atualizarGrafico());

        painelControles.add(new JLabel("Dataset:"));
        painelControles.add(comboDatasetsDisplay);
        painelControles.add(new JLabel("Palavra:"));
        painelControles.add(comboPalavras);
        painelControles.add(botaoAtualizarGrafico);

        painelPrincipal.add(painelControles, BorderLayout.NORTH);

        painelDoGraficoJFree = new ChartPanel(null);
        painelDoGraficoJFree.setPreferredSize(new Dimension(800, 600));
        painelPrincipal.add(painelDoGraficoJFree, BorderLayout.CENTER);

        setContentPane(painelPrincipal);
    }

    private void atualizarComboPalavras() {
        String datasetSelecionadoDisplay = (String) comboDatasetsDisplay.getSelectedItem();
        comboPalavras.removeAllItems();

        if (datasetSelecionadoDisplay == null || todosOsResultados == null) {
            return;
        }

        Set<String> palavrasUnicas = todosOsResultados.stream()
                .filter(r -> datasetSelecionadoDisplay.equals(r.getDatasetFileName()))
                .map(ResultadoExecucao::getPalavraBuscada)
                .collect(Collectors.toCollection(TreeSet::new));

        for (String palavra : palavrasUnicas) {
            comboPalavras.addItem(palavra);
        }
    }

    private void atualizarGrafico() {
        String datasetSelecionadoDisplay = (String) comboDatasetsDisplay.getSelectedItem();
        String palavraSelecionada = (String) comboPalavras.getSelectedItem();

        if (datasetSelecionadoDisplay == null || palavraSelecionada == null || todosOsResultados == null) {
            if(painelDoGraficoJFree != null) {
                JFreeChart chartVazio = ChartFactory.createBarChart(
                        "Seleção Inválida", "Configuração", "Tempo", null, PlotOrientation.VERTICAL, false, false, false);
                CategoryPlot plotVazio = chartVazio.getCategoryPlot();
                plotVazio.setNoDataMessage("Por favor, selecione um dataset e uma palavra válidos.");
                painelDoGraficoJFree.setChart(chartVazio);
            }
            return;
        }

        DefaultCategoryDataset datasetParaGrafico = new DefaultCategoryDataset();
        // Collectors.groupingBy e Collectors.averagingLong são válidos com os imports corretos
        Map<String, Double> mediasPorConfiguracao = todosOsResultados.stream()
                .filter(r -> datasetSelecionadoDisplay.equals(r.getDatasetFileName()) &&
                        palavraSelecionada.equalsIgnoreCase(r.getPalavraBuscada()))
                .collect(Collectors.groupingBy(
                        r -> { // ResultadoExecucao.getIdConfigCpuGpu() é válido
                            if ("cpu-parallel".equals(r.getTipoExecucao())) {
                                return r.getTipoExecucao() + " (" + r.getIdConfigCpuGpu() + " thr)";
                            } else if ("gpu".equals(r.getTipoExecucao())) {
                                return r.getTipoExecucao();
                            } else {
                                return r.getTipoExecucao();
                            }
                        },
                        Collectors.averagingLong(ResultadoExecucao::getTempoMs)
                ));

        List<Map.Entry<String, Double>> listaOrdenada = new ArrayList<>(mediasPorConfiguracao.entrySet());
        listaOrdenada.sort(Map.Entry.comparingByKey(Comparator.naturalOrder()));

        for (Map.Entry<String, Double> entrada : listaOrdenada) {
            datasetParaGrafico.addValue(entrada.getValue(), "Tempo Médio (ms)", entrada.getKey());
        }

        JFreeChart graficoDeBarras;
        if (datasetParaGrafico.getColumnCount() == 0) {
            System.out.println("Nenhum dado para plotar após filtros para dataset: " + datasetSelecionadoDisplay + ", palavra: " + palavraSelecionada);
            graficoDeBarras = ChartFactory.createBarChart(
                    "Tempo: Dataset '" + datasetSelecionadoDisplay + "', Palavra '" + palavraSelecionada + "'",
                    "Configuração de Execução", "Tempo Médio (ms)", null, PlotOrientation.VERTICAL, true, true, false);
            CategoryPlot plotVazio = graficoDeBarras.getCategoryPlot();
            plotVazio.setNoDataMessage("Nenhum dado para exibir com os filtros selecionados.");
        } else {
            graficoDeBarras = ChartFactory.createBarChart(
                    "Comparativo de Tempo de Execução Médio",
                    "Configuração de Execução",
                    "Tempo Médio (ms)",
                    datasetParaGrafico,
                    PlotOrientation.VERTICAL,
                    true, true, false);

            CategoryPlot plot = graficoDeBarras.getCategoryPlot();
            plot.setBackgroundPaint(Color.lightGray);
            plot.setDomainGridlinePaint(Color.white);
            plot.setRangeGridlinePaint(Color.white);

            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setDrawBarOutline(false);
            GradientPaint gp0 = new GradientPaint(0.0f, 0.0f, new Color(255, 100, 100), 0.0f, 0.0f, new Color(200, 50, 50));
            renderer.setSeriesPaint(0, gp0);

            CategoryAxis domainAxis = plot.getDomainAxis();
            domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));
            graficoDeBarras.setTitle("Tempo: Dataset '" + datasetSelecionadoDisplay + "', Palavra '" + palavraSelecionada + "'");
        }

        painelDoGraficoJFree.setChart(graficoDeBarras);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VisualizadorGraficosSwing("Análise de Desempenho de Algoritmos");
        });
    }
}