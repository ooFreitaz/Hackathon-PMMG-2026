
import java.util.List;

public class ProcessadorPerigo {

    public void calcularPerigoDoMapaPorHora(List<Hexagono> hexagonos, int hora) {
        if (hexagonos == null || hexagonos.isEmpty()) return;

        // 1. Soma do peso de TODOS os hexágonos do mapa na hora selecionada
        double somaPesosHorario = 0;
        int totalHexagonosMapa = hexagonos.size(); // Quantidade total (ex: 162 hexágonos)

        for (Hexagono hex : hexagonos) {
            somaPesosHorario += hex.getOcorrenciasHorario()[hora];
        }

        // 2. Média global do mapa inteiro (260 / 162.5 = 1.6)
        double mediaGeralMapa = somaPesosHorario / totalHexagonosMapa;

        // Se não houver nenhuma ocorrência na hora em todo o mapa
        if (mediaGeralMapa == 0) {
            for (Hexagono hex : hexagonos) {
                hex.setNivelPerigo(1);
            }
            return;
        }

        // 3. Classificação em relação à Média Global (Leva TODOS em consideração)
        for (Hexagono hex : hexagonos) {
            int pesoHex = hex.getOcorrenciasHorario()[hora];

            if (pesoHex == 0) {
                // Hexágono sem nenhuma ocorrência no horário
                hex.setNivelPerigo(1);
            } else {
                // Compara o peso do hexágono com a média global do mapa
                double razao = pesoHex / mediaGeralMapa;
                if(razao < 1){
                    hex.setNivelPerigo(2);
                }
                else if (razao < 2) {
                    hex.setNivelPerigo(2); // Abaixo da média do mapa (ex: peso 1)
                } else if (razao <= 3) {
                    hex.setNivelPerigo(3); // Na média ou pouco acima (ex: peso 2 ou 3)
                } else if (razao <= 4) {
                    hex.setNivelPerigo(4); // Relevante / Alto (ex: peso 4 a 6)
                } else {
                    hex.setNivelPerigo(5); // Hotspot / Crítico (peso muito acima da média)
                }
            }
        }

        System.out.println("=== AVALIAÇÃO DE PERIGO - " + hora + "h ===");
        System.out.printf("Peso Total do Mapa: %.0f | Total de Hexágonos: %d | Média Global: %.2f%n", 
            somaPesosHorario, totalHexagonosMapa, mediaGeralMapa);
    }
}