import java.util.ArrayList;
import java.util.List;



public class Hexagono {
    private String h3Index;
    private List<Ocorrencia> ocorrencias;
    private boolean temViaturaAlocada;
    private int nivelPerigo; // Nível de 1 a 5 (calculado dinamicamente com base no score de ocorrências)

    public Hexagono(String h3Index) {
        this.h3Index = h3Index;
        this.ocorrencias = new ArrayList<>();
        this.temViaturaAlocada = false;
        this.nivelPerigo = 1;
    }

    public void adicionarOcorrencia(Ocorrencia o) {
        this.ocorrencias.add(o);
        atualizarNivelPerigo();
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
    public boolean isTemViaturaAlocada() { return temViaturaAlocada; }
    public void setTemViaturaAlocada(boolean temViaturaAlocada) { this.temViaturaAlocada = temViaturaAlocada; }
    public int getNivelPerigo() { return nivelPerigo; }
}