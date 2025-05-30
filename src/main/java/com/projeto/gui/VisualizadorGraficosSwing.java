package com.projeto.gui;

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
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Classe principal para exibir os gráficos em uma janela Swing.
 */
public class VisualizadorGraficosSwing extends JFrame {

    private static final String CAMINHO_CSV = "results/csv/resultados.csv";

    public VisualizadorGraficosSwing(String tituloJanela) {
        super(tituloJanela);

        List<ResultadoExecucao> todosResultados = LeitorCSVResultados.lerResultados(CAMINHO_CSV);

        if (todosResultados.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Nenhum dado encontrado no arquivo CSV ou o arquivo não foi encontrado.\n" +
                            "Verifique o caminho: " + Paths.get(CAMINHO_CSV).toAbsolutePath(), // Mostra o caminho absoluto para ajudar na depuração
                    "Erro de Dados", JOptionPane.ERROR_MESSAGE);

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(500, 150);
            setLocationRelativeTo(null);
            setVisible(true);
            return;
        }

        JPanel painelGrafico = criarPainelComGraficoDeBarras(todosResultados);
        setContentPane(painelGrafico);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel criarPainelComGraficoDeBarras(List<ResultadoExecucao> resultados) {
        DefaultCategoryDataset datasetParaGrafico = new DefaultCategoryDataset();

        // --- FILTROS E AGRUPAMENTO DE DADOS ---
        // Defina aqui o dataset e a palavra que você quer usar para o gráfico.
        // Se quiser que seja dinâmico, precisará adicionar componentes de UI (ex: JComboBox) para seleção.
        String datasetAlvo = "projeto.analise.comparativa/datasets/DonQuixote-388208.txt"; // Defina o dataset alvo
        String palavraAlvo = "y"; // Defina a palavra alvo

        Map<String, Double> mediasPorConfiguracao = resultados.stream()
                .filter(r -> {
                    String datasetNoCsv = r.getDataset();
                    // Normaliza o caminho do dataset no CSV removendo uma possível barra no final
                    if (datasetNoCsv != null && datasetNoCsv.endsWith("/")) {
                        datasetNoCsv = datasetNoCsv.substring(0, datasetNoCsv.length() - 1);
                    }
                    // Compara com o datasetAlvo e palavraAlvo definidos
                    return datasetAlvo.equals(datasetNoCsv) && palavraAlvo.equals(r.getPalavraBuscada());
                })
                .collect(Collectors.groupingBy(
                        r -> {
                            if ("cpu-parallel".equals(r.getTipoExecucao())) {
                                return r.getTipoExecucao() + " (" + r.getIdConfigCpuGpu() + " thr)";
                            } else if ("gpu".equals(r.getTipoExecucao())) {
                                return r.getTipoExecucao(); // GPU idConfig é 0, não precisa mostrar "ID 0"
                            } else {
                                return r.getTipoExecucao(); // Captura "cpu-serial"
                            }
                        },
                        Collectors.averagingLong(ResultadoExecucao::getTempoMs)
                ));

        mediasPorConfiguracao.forEach((configDescricao, mediaTempo) -> {
            datasetParaGrafico.addValue(mediaTempo, "Tempo Médio (ms)", configDescricao);
        });

        if (datasetParaGrafico.getColumnCount() == 0) {
            System.out.println("Nenhum dado para plotar após filtros para dataset: " + datasetAlvo + ", palavra: " + palavraAlvo);
            JPanel painelVazio = new JPanel(new BorderLayout());
            painelVazio.add(new JLabel("Nenhum dado para exibir com os filtros (Dataset: " +
                    datasetAlvo.substring(datasetAlvo.lastIndexOf('/') + 1) +
                    ", Palavra: " + palavraAlvo + "). Verifique o CSV e os filtros.", SwingConstants.CENTER), BorderLayout.CENTER);
            return painelVazio;
        }

        JFreeChart graficoDeBarras = ChartFactory.createBarChart(
                "Comparativo de Tempo de Execução Médio",
                "Configuração de Execução",
                "Tempo Médio (ms)",
                datasetParaGrafico,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        CategoryPlot plot = graficoDeBarras.getCategoryPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        // Para definir cores por categoria (configuração) em vez de por série:
        // Você precisaria de um CustomRenderer ou iterar e setar a cor para cada item.
        // Por simplicidade, todas as barras da série "Tempo Médio (ms)" terão a mesma cor base.
        // renderer.setSeriesPaint(0, new Color(255, 102, 102)); // Exemplo de cor avermelhada para a série
        // Se quiser cores diferentes para cada barra (categoria), é mais complexo:
        GradientPaint gp0 = new GradientPaint(0.0f, 0.0f, new Color(255, 100, 100),
                0.0f, 0.0f, new Color(200, 50, 50));
        renderer.setSeriesPaint(0, gp0); // Aplica o gradiente para a primeira (e única) série


        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(
                CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0)
        );

        String nomeDatasetCurto = datasetAlvo.substring(datasetAlvo.lastIndexOf('/') + 1);
        graficoDeBarras.setTitle("Tempo: Dataset '" + nomeDatasetCurto + "', Palavra '" + palavraAlvo + "'");

        return new ChartPanel(graficoDeBarras);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VisualizadorGraficosSwing("Análise de Desempenho de Algoritmos");
        });
    }
}