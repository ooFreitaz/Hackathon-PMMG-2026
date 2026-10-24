import java.util.List;

public class ProcessadorPerigo {

    /**
     * Recalcula a média e aplica a classificação de perigo em todos os hexágonos para a hora informada.
     * @param todosHexagonos Lista com todos os hexágonos da malha
     * @param hora Hora selecionada para análise (0 a 23)
     */
    public void calcularPerigoDoMapaPorHora(List<Hexagono> todosHexagonos, int hora) {
        if (todosHexagonos == null || todosHexagonos.isEmpty()) {
            return;
        }

        // 1. Calcula a soma total dos pesos de todos os hexágonos na hora especificada
        int somaPesosHexagonos = 0;
        for (Hexagono hex : todosHexagonos) {
            somaPesosHexagonos += hex.getOcorrenciasHorario()[hora];
        }

        // 2. Calcula a média do mapa
        double mediaMapa = (double) somaPesosHexagonos / todosHexagonos.size();

        // 3. Atualiza o nível de perigo de cada hexágono individualmente com base na média obtida
        for (Hexagono hex : todosHexagonos) {
            hex.atualizarNivelPerigoPorHora(mediaMapa, hora);
        }

        System.out.println("Processamento Concluído para a Hora: " + hora + "h | Média do Mapa: " + mediaMapa);
    }
}