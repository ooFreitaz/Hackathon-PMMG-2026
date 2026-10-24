import java.util.*;

public class AgrupadorBloco {

    public List<Bloco> criarBlocosEquilibrados(List<Hexagono> todosHexagonos, int qtdViaturas, int hora) {
        List<Bloco> blocos = new ArrayList<>();
        if (todosHexagonos == null || todosHexagonos.isEmpty() || qtdViaturas <= 0) {
            return blocos;
        }

        // 1. Instancia os N blocos
        for (int i = 1; i <= qtdViaturas; i++) {
            blocos.add(new Bloco("BLOCO_" + i));
        }

        // 2. Calcula a pontuação total e a meta ideal por bloco
        int pontuacaoTotal = 0;
        for (Hexagono h : todosHexagonos) {
            pontuacaoTotal += h.getOcorrenciasHorario()[hora];
        }
        double metaPorBloco = (double) pontuacaoTotal / qtdViaturas;

        // 3. Ordena os hexágonos do maior para o menor peso na hora
        List<Hexagono> ordenados = new ArrayList<>(todosHexagonos);
        ordenados.sort((h1, h2) -> Integer.compare(
            h2.getOcorrenciasHorario()[hora], 
            h1.getOcorrenciasHorario()[hora]
        ));

        // 4. Distribui os hexágonos atribuindo sempre ao bloco que estiver com a MENOR CARGA ATUAL
        for (Hexagono hex : ordenados) {
            Bloco blocoComMenorCarga = blocos.get(0);
            
            for (Bloco b : blocos) {
                if (b.getScoreTotalBloco(hora) < blocoComMenorCarga.getScoreTotalBloco(hora)) {
                    blocoComMenorCarga = b;
                }
            }
            
            blocoComMenorCarga.adicionarHexagono(hex);
        }

        return blocos;
    }
}