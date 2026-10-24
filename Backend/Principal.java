import java.io.IOException;
import java.util.*;

public class Principal {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO SISTEMA DE POSICIONAMENTO DE VIATURAS (5 VIATURAS) ===");

        // 1. Instancia o leitor fornecido e lê a lista de ocorrências do arquivo TXT/JSON
        LeitorDados leitor = new LeitorDados();
        List<Ocorrencia> ocorrenciasLidas;

        try {
            ocorrenciasLidas = leitor.lerTodasOcorrencias("ocorrencias.txt");
            System.out.println("-> Arquivo processado com sucesso!");
            System.out.println("-> Total de Ocorrências Importadas: " + ocorrenciasLidas.size());
        } catch (IOException e) {
            System.err.println("Erro ao carregar o arquivo de ocorrências: " + e.getMessage());
            return;
        }

        // 2. Simulação da malha de hexágonos cadastrados no mapa (Índices H3)
        Map<String, Hexagono> mapaHexagonos = new HashMap<>();
        
        String[] h3Exemplos = {
            "89a8a0233b3ffff", "89a8a0233b7ffff", "89a8a0233bbffff", 
            "89a8a0233c3ffff", "89a8a0233c7ffff", "89a8a0233cbffff",
            "89a8a0233d3ffff", "89a8a0233d7ffff", "89a8a0233dbffff",
            "89a8a0233e3ffff"
        };

        for (String idH3 : h3Exemplos) {
            mapaHexagonos.put(idH3, new Hexagono(idH3));
        }

        // 3. Distribui as ocorrências lidas para os hexágonos da malha
        // (Em produção, o H3 mapeia cada lat/lon para seu h3Index exato via h3.latLngToCell)
        int index = 0;
        for (Ocorrencia oc : ocorrenciasLidas) {
            String targetH3 = h3Exemplos[index % h3Exemplos.length];
            Hexagono hex = mapaHexagonos.get(targetH3);
            hex.adicionarOcorrencia(oc);
            index++;
        }

        List<Hexagono> listaHexagonos = new ArrayList<>(mapaHexagonos.values());

        // 4. Avaliação de Perigo Relativo por Média para o Horário Escolhido (ex: 20h)
        int horaAnalise = 20;
        System.out.println("\n--- 1. AVALIAÇÃO DE PERIGO RELATIVO PARA AS " + horaAnalise + "h ---");
        
        ProcessadorPerigo perigoService = new ProcessadorPerigo();
        perigoService.calcularPerigoDoMapaPorHora(listaHexagonos, horaAnalise);

        for (Hexagono hex : listaHexagonos) {
            int pesoNaHora = hex.getOcorrenciasHorario()[horaAnalise];
            System.out.println("Hexágono [" + hex.getH3Index() 
                    + "] | Peso Acumulado (" + horaAnalise + "h): " + pesoNaHora 
                    + " | Nível de Perigo: " + hex.getNivelPerigo());
        }

        // 5. Agrupamento em 5 Blocos Equilibrados e Alocação de 5 Viaturas
        int qtdViaturas = 5;
        System.out.println("\n--- 2. DISTRIBUIÇÃO EM " + qtdViaturas + " BLOCOS DE PATRULHAMENTO ---");

        AgrupadorBloco agrupadorService = new AgrupadorBloco();
        List<Bloco> blocosGerados = agrupadorService.criarBlocosEquilibrados(listaHexagonos, qtdViaturas, horaAnalise);

        // 6. Exibição da atribuição dos blocos e viaturas
        for (int i = 0; i < blocosGerados.size(); i++) {
            Bloco bloco = blocosGerados.get(i);
            
            Viatura viatura = new Viatura("VTR-" + (i + 1), bloco);
            viatura.atualizarComportamentoPelaHora(horaAnalise);

            if (!bloco.getRegiaoHexagonos().isEmpty()) {
                Hexagono posicaoEstrategica = bloco.getRegiaoHexagonos().get(0);
                viatura.setPosicaoAtual(posicaoEstrategica);
                posicaoEstrategica.setTemViaturaAlocada(true);
            }

            System.out.println("\n------------------------------------------------");
            System.out.println("ID do Bloco: " + bloco.getIdBloco());
            System.out.println("Hexágonos vinculados: " + bloco.getRegiaoHexagonos().size());
            System.out.println("Carga de Perigo Total do Bloco: " + bloco.getScoreTotalBloco(horaAnalise));
            System.out.println("Viatura Alocada: " + viatura.getIdViatura());
            System.out.println("Posição Atual (H3): " + (viatura.getPosicaoAtual() != null ? viatura.getPosicaoAtual().getH3Index() : "N/A"));
            System.out.println("Comportamento Operacional: " + viatura.getComportamentoAtual());
        }

        System.out.println("\n=== EXECUÇÃO CONCLUÍDA COM SUCESSO ===");
    }
}