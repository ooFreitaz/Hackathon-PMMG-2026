import java.util.ArrayList;
import java.util.List;

public class Hexagono {
    private String h3Index;
    private List<Ocorrencia> ocorrencias;
    private int[] OcorrenciasHorario; // Vetor de 24 posições (0 a 23 horas)
    private boolean temViaturaAlocada;
    private int nivelPerigo; // Nível de 1 a 5

    public Hexagono(String h3Index) {
        this.h3Index = h3Index;
        this.ocorrencias = new ArrayList<>();
        this.OcorrenciasHorario = new int[24]; // Inicializa as 24 horas com valor 0
        this.temViaturaAlocada = false;
        this.nivelPerigo = 1;
    }

    /**
     * Adiciona uma ocorrência, atualiza o vetor de horários e recalcula o perigo geral
     */
    public void adicionarOcorrencia(Ocorrencia o) {
        this.ocorrencias.add(o);
        
        // Pega a hora exata da ocorrência (0 a 23) e acumula o peso do crime na posição do vetor
        int hora = o.getHora().getHour();
        this.OcorrenciasHorario[hora] += o.getPeso();

        atualizarNivelPerigo();
    }

    /**
     * Recalcula todo o vetor de 24 horas a partir da lista de ocorrências atual.
     * Útil caso as ocorrências sejam carregadas em lote.
     */
    public void calcularOcorrenciasPorHorario() {
        // Reseta o vetor com zeros antes de reprocessar
        this.OcorrenciasHorario = new int[24];

        for (Ocorrencia o : this.ocorrencias) {
            int hora = o.getHora().getHour();
            this.OcorrenciasHorario[hora] += o.getPeso();
        }
    }

    private void atualizarNivelPerigo() {
        int scoreTotal = ocorrencias.stream().mapToInt(Ocorrencia::getPeso).sum();
        if (scoreTotal > 50) this.nivelPerigo = 5;
        else if (scoreTotal > 30) this.nivelPerigo = 4;
        else if (scoreTotal > 15) this.nivelPerigo = 3;
        else if (scoreTotal > 5) this.nivelPerigo = 2;
        else this.nivelPerigo = 1;
    }

    // Getters e Setters
    public String getH3Index() { return h3Index; }
    public List<Ocorrencia> getOcorrencias() { return ocorrencias; }
    public int[] getOcorrenciasHorario() { return OcorrenciasHorario; }
    public boolean isTemViaturaAlocada() { return temViaturaAlocada; }
    public void setTemViaturaAlocada(boolean temViaturaAlocada) { this.temViaturaAlocada = temViaturaAlocada; }
    public int getNivelPerigo() { return nivelPerigo; }
}