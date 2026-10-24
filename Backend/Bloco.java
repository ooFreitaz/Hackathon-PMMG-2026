package Backend;

import java.util.ArrayList;
import java.util.List;

public class Bloco {
    private String idBloco;
    private List<Hexagono> regiaoHexagonos;

    public Bloco(String idBloco) {
        this.idBloco = idBloco;
        this.regiaoHexagonos = new ArrayList<>();
    }

    public void adicionarHexagono(Hexagono h) {
        this.regiaoHexagonos.add(h);
    }

    // Agrega todas as ocorrências de todos os hexágonos do bloco
    public List<Ocorrencia> getTodasOcorrencias() {
        List<Ocorrencia> todas = new ArrayList<>();
        for (Hexagono h : regiaoHexagonos) {
            todas.addAll(h.getOcorrencias());
        }
        return todas;
    }

    // Getters
    public String getIdBloco() { return idBloco; }
    public List<Hexagono> getRegiaoHexagonos() { return regiaoHexagonos; }
}
