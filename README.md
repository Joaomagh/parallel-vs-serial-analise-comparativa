# Análise Comparativa de Algoritmos de Contagem de Palavras com Uso de Paralelismo

## Resumo

Este projeto realiza uma análise comparativa do desempenho de diferentes algoritmos para a contagem de ocorrências de uma palavra específica em arquivos de texto. Foram implementadas e avaliadas três abordagens distintas: uma execução puramente serial em CPU, uma execução paralela utilizando múltiplas threads em CPU, e uma execução paralela utilizando a capacidade de processamento de uma GPU através da tecnologia OpenCL (com a biblioteca JOCL em Java). O objetivo principal é identificar as condições sob as quais cada abordagem oferece maior eficiência computacional, considerando fatores como o tamanho do dataset e a sobrecarga inerente a cada técnica de paralelismo. Os resultados são registrados em formato CSV e visualizados através de gráficos gerados por uma aplicação Swing.

## 1. Introdução

A busca por eficiência em processamento de dados é um desafio constante na computação. Este trabalho foca na tarefa de contagem de palavras em grandes volumes de texto, um problema comum que pode se beneficiar significativamente de técnicas de paralelização. A abordagem adotada consiste no desenvolvimento e comparação de três métodos distintos em Java:

* **Método SerialCPU (`com.projeto.serial.SerialCPU`):** Uma implementação sequencial tradicional. O texto é lido e as palavras são verificadas uma após a outra em um único fluxo de execução. Serve como linha de base para as comparações de desempenho.

* **Método ParallelCPU (`com.projeto.parallelcpu.ParallelCPU`):** Utiliza o paralelismo em nível de thread para dividir a tarefa de contagem entre os múltiplos núcleos disponíveis em uma CPU moderna. O arquivo de texto é dividido em segmentos (ou a lista de palavras é dividida), e cada segmento é processado por uma thread separada gerenciada por um `ExecutorService`. O número de threads pode ser configurado pelo usuário.

* **Método ParallelGPU (`com.projeto.parallelgpu.ParallelGPU`):** Explora o paralelismo massivo oferecido por Unidades de Processamento Gráfico (GPUs). Utiliza a API OpenCL, através da biblioteca JOCL (Java Bindings for OpenCL), para executar um kernel customizado na GPU. O kernel é projetado para verificar ocorrências da palavra-alvo em paralelo, considerando as fronteiras das palavras para uma contagem precisa.

A comparação visa entender os trade-offs entre a simplicidade do código serial, o paralelismo gerenciado pela JVM na CPU, e o poder de processamento bruto (mas com maior overhead de comunicação) da GPU.

## 2. Metodologia

A metodologia empregada neste estudo seguiu as diretrizes propostas, focando em uma análise quantitativa e comparativa dos algoritmos.

* **Análise Estatística dos Resultados:** "Análise estatística dos resultados obtidos para identificar padrões de desempenho e comparar os algoritmos sob diferentes condições." Os tempos médios de execução de múltiplas amostras são utilizados para a comparação principal.

* **Implementação de Algoritmos:**
    * Todos os algoritmos foram implementados na linguagem Java (JDK 17).
    * A interação com a GPU foi realizada utilizando JOCL (versão 2.0.4), que fornece bindings Java para a API OpenCL.
    * A lógica de contagem de palavras foi padronizada para ser case-insensitive e para identificar palavras inteiras (delimitadas por caracteres não alfanuméricos).

* **Framework de Teste:**
    * **`TestLauncherSwing.java` (`com.projeto.gui`):** Uma interface gráfica Swing foi desenvolvida para facilitar a execução dos testes. Permite ao usuário:
        * Selecionar o arquivo de dataset (.txt) através de um `JFileChooser`.
        * Digitar a palavra a ser buscada.
        * Escolher quais algoritmos executar (Serial CPU, Parallel CPU, Parallel GPU).
        * Configurar o número de threads para a execução Parallel CPU através de um `JSpinner`.
        * Disparar a execução dos testes selecionados (por padrão, 3 amostras para cada configuração).
    * **`CSVExportar.java` (`com.projeto.util`):** Classe utilitária responsável por registrar os resultados de cada execução em um arquivo CSV.

* **Execução em Ambientes Variados:** (Esta seção deve ser preenchida pelo(s) autor(es) com detalhes sobre o ambiente de teste: especificações do processador, GPU, sistema operacional, versão do Java, e como as variações de ambiente foram (ou poderiam ser) consideradas). O objetivo é observar a variação no desempenho sob diferentes condições.

* **Registro de Dados:**
    * Os tempos de execução e contagens de ocorrências são armazenados no arquivo `results/csv/resultados.csv`.
    * As colunas do CSV são: `tipo_execucao`, `id_config_cpu_gpu` (nº de threads para CPU paralelo, 0 para GPU, 1 para serial), `tempo_ms`, `ocorrencias`, `dataset` (caminho do arquivo), `palavra_buscada`, `num_execucao` (número da amostra).

* **Análise Estatística e Visualização:**
    * **`VisualizadorGraficosSwing.java` (`com.projeto.gui`):** Outra interface gráfica Swing foi desenvolvida para a análise visual dos dados.
        * Lê o arquivo `resultados.csv`.
        * Permite ao usuário selecionar dinamicamente o `dataset` e a `palavra_buscada` cujos resultados deseja visualizar, através de `JComboBoxes`.
        * Ao clicar em "Atualizar Gráfico", calcula o tempo médio de execução para cada configuração de algoritmo (para o dataset e palavra selecionados).
        * Gera e exibe um gráfico de barras (utilizando a biblioteca JFreeChart) comparando esses tempos médios.

## 3. Resultados e Discussão

Nesta seção, são apresentados e discutidos os resultados obtidos através da execução dos algoritmos sob diferentes configurações e para diversos datasets e palavras-alvo. Os gráficos gerados pela aplicação `VisualizadorGraficosSwing` são a principal ferramenta para esta análise.

*(**Nota para o(s) autor(es):** Substitua o texto abaixo e as imagens de exemplo pelos seus próprios resultados e análises.)*

**Exemplo de Análise (a ser substituído):**

Foram realizados testes utilizando o dataset `DonQuixote-388208.txt` para diferentes palavras e configurações de threads para o `ParallelCPU`.

**Gráfico 1: Comparação de Tempo Médio para a palavra "y" no dataset DonQuixote-388208.txt**

**Discussão do Gráfico 1:**
* Observa-se que para a palavra "y", a implementação `cpu-parallel` com 4 threads apresentou o menor tempo médio (XXX ms), superando a execução serial (YYY ms) e a execução com 2, 6, 8 e 10 threads.
* A execução com 6 threads (ZZZ ms) mostrou um desempenho ligeiramente inferior à de 4 e 8 threads, o que pode indicar que para esta tarefa e dataset específicos, o overhead de gerenciamento de um número maior de threads começa a impactar, ou pode ser devido a variações na execução.
* A implementação GPU (média de AAA ms, incluindo warm-up) apresentou um tempo [maior/menor/comparável] em relação às melhores configurações da CPU. Isso pode ser atribuído [ao overhead de transferência de dados para a GPU / à natureza da tarefa não ser suficientemente massiva para explorar todo o potencial da GPU / ao efeito de warm-up da primeira execução da GPU].

*(Continue com mais gráficos e discussões para diferentes palavras, diferentes datasets, e diferentes números de threads. Analise a escalabilidade, os pontos de saturação, e compare o desempenho relativo de Serial, Parallel CPU e Parallel GPU.)*

**Observações Gerais:**
* **Escalabilidade do ParallelCPU:** Analise como o tempo de execução varia com o aumento do número de threads. Existe um ponto ótimo? O desempenho degrada após um certo número de threads?
* **Desempenho da GPU:** A GPU foi consistentemente mais rápida? Em que cenários? O efeito de "warm-up" (primeira execução mais lenta) foi significativo? O overhead de transferência de dados parece ser um fator limitante?
* **Consistência das Ocorrências:** As contagens de palavras foram consistentes entre as diferentes implementações para as mesmas entradas? (Discutir a pequena diferença observada anteriormente, se pertinente).

## 4. Conclusão

*(**Nota para o(s) autor(es):** Resuma suas principais descobertas aqui.)*

Com base nos testes realizados e na análise dos resultados, pode-se concluir que:

* Para tarefas de contagem de palavras em datasets de tamanho [pequeno/médio/grande], a abordagem [SerialCPU / ParallelCPU com X threads / ParallelGPU] demonstrou ser a mais eficiente em termos de tempo de execução.
* O paralelismo em CPU apresentou ganhos de desempenho até um certo número de threads (aproximadamente [X] threads para os datasets testados), após o qual o overhead de gerenciamento de threads [começou a anular os ganhos / levou a uma pequena degradação].
* A utilização da GPU para esta tarefa específica [mostrou-se vantajosa / não superou as implementações otimizadas para CPU / foi competitiva apenas para datasets muito grandes] devido a [fatores como overhead de transferência de dados, latência de inicialização do kernel, natureza da computação por palavra].
* O framework de teste desenvolvido permitiu uma coleta sistemática de dados, e a ferramenta de visualização gráfica facilitou a análise comparativa dos resultados.

Este trabalho contribui para a compreensão prática da aplicação de diferentes técnicas de paralelismo em um problema comum de processamento de texto, destacando que a escolha da melhor abordagem depende intrinsecamente das características da tarefa, do volume de dados e da arquitetura de hardware disponível.


## 6. Anexos

### Códigos das Implementações

As principais classes desenvolvidas para este projeto incluem:

* **Lógica de Contagem:**
    * `com.projeto.serial.SerialCPU.java`: Implementação serial.
    * `com.projeto.parallelcpu.ParallelCPU.java`: Implementação paralela em CPU.
    * `com.projeto.parallelgpu.ParallelGPU.java`: Implementação paralela em GPU (contém o `KERNEL_SOURCE` OpenCL).
* **Utilitários:**
    * `com.projeto.util.CSVExportar.java`: Responsável por salvar os resultados dos testes em formato CSV.
* **Interface Gráfica e Visualização:**
    * `com.projeto.gui.TestLauncherSwing.java`: Interface para configurar e disparar os testes.
    * `com.projeto.gui.VisualizadorGraficosSwing.java`: Interface para ler o CSV e exibir os gráficos de desempenho.
    * `com.projeto.gui.ResultadoExecucao.java`: Classe de modelo para os dados de resultado.
    * `com.projeto.gui.LeitorCSVResultados.java`: Classe para ler e processar o arquivo CSV.

### Link do Projeto no GitHub

https://github.com/Joaomagh/parallel-vs-serial-analise-comparativa


---

## 7. Execução

### 1. Pré-requisitos

- Java JDK 17+
    - Oracle JDK 
    - Variável de ambiente JAVA_HOME e PATH configuradas

- Maven 3.6.x+
    - https://maven.apache.org
    - Comando mvn no terminal/shell

- Git
    - https://git-scm.com

- Drivers de GPU com suporte a OpenCL (apenas necessário para execução GPU)
    - Drivers atualizados da NVIDIA, AMD ou Intel


### 1.1 Clonagem

```bash
git clone https://github.com/Joaomagh/parallel-vs-serial-analise-comparativa.git
cd parallel-vs-serial-analise-comparativa
```

### 1.2 Compilação

O processo de compilação é feito pela propria IDE. vá para o passo 3.4

```bash
mvn clean package
```

Gera `target/analise-comparativa-1.0-SNAPSHOT.jar`

### 1.3 Execução com IDE (Recomendado)

Abra o projeto com sua IDE (IntelliJ, Eclipse, VSCode com extensão Maven) e:
- Importe o projeto como um projeto Maven.
- Execute `TestLauncherSwing` para iniciar os testes
- Execute `VisualizadorGraficosSwing` para visualizar os resultados

1. Execute `TestLauncherSwing`
2. Escolha o dataset `.txt`
3. Digite a palavra alvo
4. Escolha os algoritmos e defina número de threads
5. Clique em "Iniciar Testes"
6. Execute `VisualizadorGraficosSwing` para gerar os gráficos comparativos e escolha sua pasta e palavra procurada


### 1.4 Execução via Terminal

obs: caso não queira executar pela IDE

```bash
java -cp target/analise-comparativa-1.0-SNAPSHOT.jar com.projeto.gui.TestLauncherSwing
java -cp target/analise-comparativa-1.0-SNAPSHOT.jar com.projeto.gui.VisualizadorGraficosSwing
```

> Para uso com `java -jar`, configure o MANIFEST.MF ou gere um fat JAR.

### 2. Estrutura de Pastas

```
datasets/                    # Arquivos de entrada
results/csv/resultados.csv   # Resultados gerados (CSV)
src/main/java/               # arquivos Java
pom.xml                      # Configuração Maven
```

## 7. Tecnologias

- Java 17, Maven, Swing
- JOCL (OpenCL em Java)
- JFreeChart
- IDEs que recomendo: IntelliJ IDEA, Eclipse, VSCode (com Maven)

## 9. GitHub

[Repositório do Github](https://github.com/Joaomagh/parallel-vs-serial-analise-comparativa)



<h2 id="colab">🤝 Colaboradores</h2>

Pessoas que colaboraram

<table>
  <tr>
    <td align="center">
      <a href="#">
        <img src="https://avatars.githubusercontent.com/u/116520638?s=400&u=1321b810211cc12bfbe259adb1a664b0031aa710&v=4" width="100px;" alt="João Pedro Foto"/><br>
        <sub>
          <b>Joao Pedro Rego Magalhães</b>
        </sub>
      </a>
    </td>
    <td align="center">
      <a href="#">
        <img src="https://avatars.githubusercontent.com/u/111661983?v=4" width="100px;" alt="João Pedro Foto"/><br>
        <sub>
          <b>Guilherme silva</b>
        </sub>
      </a>
    </td>
  </tr>
</table>




