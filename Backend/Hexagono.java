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

    public void atualizarNivelPerigoPorHora(double mediaMapa, int hora) {
        int pesoHexagono = this.OcorrenciasHorario[hora];

        // Se a média for 0 (nenhum crime no mapa nessa hora), todos ficam em nível 1 ou 3
        if (mediaMapa == 0) {
            this.nivelPerigo = (pesoHexagono == 0) ? 1 : 3;
            return;
        }

        if (pesoHexagono >= mediaMapa * 1.75) {
            this.nivelPerigo = 5; // >= 50% acima da média
        } else if (pesoHexagono >= mediaMapa * 1.50) {
            this.nivelPerigo = 4; // >= 25% acima da média
        } else if (pesoHexagono <= mediaMapa * 0.50) {
            this.nivelPerigo = 1; // <= 50% abaixo da média
        } else if (pesoHexagono <= mediaMapa * 0.25) {
            this.nivelPerigo = 2; // <= 25% abaixo da média
        } else {
            this.nivelPerigo = 3; // Na faixa entre -25% e +25% da média
        }
    }

    // Getters e Setters
    public String getH3Index() { return h3Index; }
    public List<Ocorrencia> getOcorrencias() { return ocorrencias; }
    public int[] getOcorrenciasHorario() { return OcorrenciasHorario; }
    public boolean isTemViaturaAlocada() { return temViaturaAlocada; }
    public void setTemViaturaAlocada(boolean temViaturaAlocada) { this.temViaturaAlocada = temViaturaAlocada; }
    public int getNivelPerigo() { return nivelPerigo; }
    public void setNivelPerigo(int nivelPerigo){this.nivelPerigo = nivelPerigo;}
}