import java.util.*;

public class CartaoPrograma {

    // 1. Array de 24 posições mantido da sua versão original
    // Mapeamento dos tipos de atividade/comportamento:
    // 1 = Ponto-Base 20min + Patrulhamento 40min (Raio 1km) [Classe 1 e metade Classe 2]
    // 2 = Patrulhamento no Entorno de 2km [Metade Classe 2]
    // 3 = Patrulhamento Preventivo no Entorno de 3km [Classe 3]
    private int[] comportamentoPorHora;

    // Novos campos para atender às regras de alocação espacial e classificação
    private String idViatura;
    private int[] classePorHora;          // Guarda a Classe (1, 2 ou 3) de cada hora
    private String[] h3IndexPrioritario;   // Quadrícula selecionada para cada hora (0h a 23h)
    private String[] descricaoAtividade;  // Descrição operacional detalhada para impressão

    public CartaoPrograma() {
        this.comportamentoPorHora = new int[24];
        this.classePorHora = new int[24];
        this.h3IndexPrioritario = new String[24];
        this.descricaoAtividade = new String[24];
    }

    public CartaoPrograma(String idViatura) {
        this();
        this.idViatura = idViatura;
    }

    // --- MÉTODOS ORIGINAIS PRESERVADOS ---
    public void definirComportamentoHora(int hora, int comportamento) {
        if (hora >= 0 && hora < 24) {
            this.comportamentoPorHora[hora] = comportamento;
        }
    }

    public int getComportamentoNaHora(int hora) {
        if (hora >= 0 && hora < 24) {
            return this.comportamentoPorHora[hora];
        }
        return 1;
    }

    public int[] getComportamentoPorHora() { 
        return comportamentoPorHora; 
    }

    // --- NOVOS GETTERS E SETTERS ---
    public String getIdViatura() { return idViatura; }
    public void setIdViatura(String idViatura) { this.idViatura = idViatura; }

    public int[] getClassePorHora() { return classePorHora; }
    public String[] getH3IndexPrioritario() { return h3IndexPrioritario; }
    public String[] getDescricaoAtividade() { return descricaoAtividade; }


    // =========================================================================
    // LÓGICA DE GERAÇÃO DINÂMICA DO CARTÃO PROGRAMA (REGRAS HACKATHON PMMG)
    // =========================================================================

    /**
     * Classifica as 24 faixas horárias da Cia em 3 Classes (8 faixas em cada)
     * com base na incidência criminal total calculada.
     */
    public static Map<Integer, Integer> calcularClasses24Horas(List<Hexagono> todosHexagonos) {
        Map<Integer, Double> pesoPorHora = new HashMap<>();

        for (int h = 0; h < 24; h++) {
            double soma = 0;
            for (Hexagono hex : todosHexagonos) {
                if (hex.getOcorrenciasHorario() != null) {
                    soma += hex.getOcorrenciasHorario()[h];
                }
            }
            pesoPorHora.put(h, soma);
        }

        // Ordena as horas do maior para o menor peso criminal
        List<Map.Entry<Integer, Double>> listaOrdenada = new ArrayList<>(pesoPorHora.entrySet());
        listaOrdenada.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        Map<Integer, Integer> mapaClasses = new HashMap<>();
        for (int i = 0; i < listaOrdenada.size(); i++) {
            int hora = listaOrdenada.get(i).getKey();
            if (i < 8) mapaClasses.put(hora, 1);       // Classe 1: 8 maiores incidências
            else if (i < 16) mapaClasses.put(hora, 2); // Classe 2: 8 incidências intermediárias
            else mapaClasses.put(hora, 3);             // Classe 3: 8 menores incidências
        }

        return mapaClasses;
    }

    /**
     * Preenche as 24 horas deste Cartão Programa de acordo com a viatura e seus blocos.
     */
    public void gerarCartaoCompleto(Map<Integer, Integer> mapaClasses, List<Bloco> blocosPorHora) {
        String h3Anterior = null;

        for (int h = 0; h < 24; h++) {
            int classe = mapaClasses.getOrDefault(h, 3);
            this.classePorHora[h] = classe;

            // Encontra o setor/bloco desta VTR na hora 'h'
            Bloco blocoAtual = encontrarBlocoDaViatura(this.idViatura, blocosPorHora);

            // Seleciona a quadrícula aplicando a REGRA DE ALTERNÂNCIA (sem repetição consecutiva)
            String hexPrioritario = selecionarQuadriculaAlternada(blocoAtual, h, h3Anterior);
            this.h3IndexPrioritario[h] = hexPrioritario;
            h3Anterior = hexPrioritario;

            // Aplica as regras de comportamento do edital
            if (classe == 1) {
                definirComportamentoHora(h, 1);
                this.descricaoAtividade[h] = "Ponto-Base 20min em " + hexPrioritario + " + Patrulhamento 40min (Raio 1km)";
            } else if (classe == 2) {
                // Classe 2: 4 faixas usam PB 20/40 (comportamento 1) e 4 faixas usam Entorno 2km (comportamento 2)
                if (h % 2 == 0) {
                    definirComportamentoHora(h, 1);
                    this.descricaoAtividade[h] = "Ponto-Base 20min em " + hexPrioritario + " + Patrulhamento 40min (Raio 1km)";
                } else {
                    definirComportamentoHora(h, 2);
                    this.descricaoAtividade[h] = "Patrulhamento em Polígono de até 2km no entorno de " + hexPrioritario;
                }
            } else { // Classe 3
                definirComportamentoHora(h, 3);
                this.descricaoAtividade[h] = "Patrulhamento Preventivo em Polígono de até 3km no entorno de " + hexPrioritario;
            }
        }
    }

    private String selecionarQuadriculaAlternada(Bloco bloco, int hora, String h3Anterior) {
        if (bloco == null || bloco.getRegiaoHexagonos() == null || bloco.getRegiaoHexagonos().isEmpty()) {
            return "SEM_INCIDENCIA";
        }

        List<Hexagono> ordenados = new ArrayList<>(bloco.getRegiaoHexagonos());
        ordenados.sort((a, b) -> Integer.compare(b.getOcorrenciasHorario()[hora], a.getOcorrenciasHorario()[hora]));

        String melhorHex = ordenados.get(0).getH3Index();

        // Se for idêntica à quadrícula da hora anterior, seleciona a segunda de maior peso
        if (melhorHex.equals(h3Anterior) && ordenados.size() > 1) {
            melhorHex = ordenados.get(1).getH3Index();
        }

        return melhorHex;
    }

    private Bloco encontrarBlocoDaViatura(String idVtr, List<Bloco> blocos) {
        if (blocos == null) return null;
        for (Bloco b : blocos) {
            if (idVtr != null && idVtr.equalsIgnoreCase(b.getIdBloco())) {
                return b;
            }
        }
        return null;
    }
}