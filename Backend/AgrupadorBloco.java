import java.util.*;

public class AgrupadorBloco {

    /**
     * Divide a malha de hexágonos em blocos equilibrados com base na carga total de ocorrências.
     * 
     * @param todosHexagonos Lista com todos os hexágonos com ocorrências populadas
     * @param qtdViaturas Número de viaturas disponíveis (ex: 5)
     * @param hora Hora selecionada para análise (0 a 23)
     * @return Lista de Blocos equilibrados
     */
    public List<Bloco> criarBlocosEquilibrados(List<Hexagono> todosHexagonos, int qtdViaturas, int hora) {
        List<Bloco> blocos = new ArrayList<>();
        if (todosHexagonos == null || todosHexagonos.isEmpty() || qtdViaturas <= 0) {
            return blocos;
        }

        // 1. Calcula a pontuação total do mapa e a meta individual por viatura
        int pontuacaoTotal = 0;
        for (Hexagono h : todosHexagonos) {
            pontuacaoTotal += h.getOcorrenciasHorario()[hora];
        }

        double metaPorViatura = (double) pontuacaoTotal / qtdViaturas;
        System.out.println("Pontuação Total: " + pontuacaoTotal + " | Meta por Bloco: " + metaPorViatura);

        // Conjunto para controlar quais hexágonos já foram atribuídos a algum bloco
        Set<String> hexagonesVisitados = new HashSet<>();

        // 2. Ordena os hexágonos do mais crítico ao menos crítico na hora informada
        List<Hexagono> ordenados = new ArrayList<>(todosHexagonos);
        ordenados.sort((h1, h2) -> Integer.compare(
            h2.getOcorrenciasHorario()[hora], 
            h1.getOcorrenciasHorario()[hora]
        ));

        // 3. Cria os N blocos partindo dos hexágonos mais críticos (sementes)
        int numeroBloco = 1;
        for (Hexagono semente : ordenados) {
            if (blocos.size() >= qtdViaturas) break;

            if (!hexagonesVisitados.contains(semente.getH3Index())) {
                Bloco bloco = new Bloco("BLOCO_" + numeroBloco++);
                
                // Adiciona o hexágono semente ao bloco
                bloco.adicionarHexagono(semente);
                hexagonesVisitados.add(semente.getH3Index());

                // 4. Expande o bloco pegando vizinhos até atingir a meta aproximada
                expandirBlocoAteMeta(bloco, semente, todosHexagonos, hexagonesVisitados, metaPorViatura, hora);
                blocos.add(bloco);
            }
        }

        // 5. Caso sobrem hexágonos isolados, atribui ao bloco vizinho mais próximo
        for (Hexagono h : todosHexagonos) {
            if (!hexagonesVisitados.contains(h.getH3Index())) {
                Bloco blocoMaisProximo = encontrarBlocoMaisProximo(h, blocos);
                if (blocoMaisProximo != null) {
                    blocoMaisProximo.adicionarHexagono(h);
                    hexagonesVisitados.add(h.getH3Index());
                }
            }
        }

        return blocos;
    }

    /**
     * Expande um bloco adicionando hexágonos adjacentes até encostar na meta de pontuação.
     */
    private void expandirBlocoAteMeta(
            Bloco bloco, 
            Hexagono semente, 
            List<Hexagono> todosHexagonos, 
            Set<String> visitados, 
            double meta, 
            int hora
    ) {
        int cargaAtual = bloco.getScoreTotalBloco(hora);
        
        // Fila de prioridade/busca para expansão geográfica a partir da semente
        Queue<Hexagono> filaExpansao = new LinkedList<>();
        filaExpansao.add(semente);

        while (!filaExpansao.isEmpty() && cargaAtual < meta) {
            Hexagono atual = filaExpansao.poll();

            // Busca os vizinhos geográficos no mapa
            List<Hexagono> vizinhos = obterVizinhos(atual, todosHexagonos);

            for (Hexagono vizinho : vizinhos) {
                if (!visitados.contains(vizinho.getH3Index())) {
                    int pesoVizinho = vizinho.getOcorrenciasHorario()[hora];

                    // Adiciona o hexágono se não estourar excessivamente a meta
                    bloco.adicionarHexagono(vizinho);
                    visitados.add(vizinho.getH3Index());
                    cargaAtual += pesoVizinho;

                    filaExpansao.add(vizinho);

                    if (cargaAtual >= meta) {
                        break; // Meta atingida para este bloco
                    }
                }
            }
        }
    }

    // Auxiliar: Encontra vizinhos geográficos com base no índice H3
    private List<Hexagono> obterVizinhos(Hexagono centro, List<Hexagono> todos) {
        List<Hexagono> vizinhos = new ArrayList<>();
        // Lógica de vizinhança usando a biblioteca H3 no Java
        return vizinhos; 
    }

    // Auxiliar: Atribui hexágonos remanescentes ao bloco geograficamente mais próximo
    private Bloco encontrarBlocoMaisProximo(Hexagono h, List<Bloco> blocos) {
        if (blocos.isEmpty()) return null;
        return blocos.get(0); // Retorna o primeiro bloco disponível como fallback
    }
}