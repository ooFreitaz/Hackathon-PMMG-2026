import com.uber.h3core.H3Core;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapaController {

    private final LeitorDados leitorDados = new LeitorDados();
    private final ProcessadorPerigo perigoService = new ProcessadorPerigo();
    private final AgrupadorBloco agrupadorService = new AgrupadorBloco();
    private H3Core h3;

    public MapaController() {
        try {
            // Inicializa a biblioteca nativa da Uber H3
            this.h3 = H3Core.newInstance();
        } catch (IOException e) {
            System.err.println("Erro ao inicializar H3Core: " + e.getMessage());
        }
    }

    public Map<String, Object> getDadosMapa(int hora) {
        Map<String, Object> resposta = new HashMap<>();

        try {
            // 1. Lê todas as ocorrências do arquivo de texto
            List<Ocorrencia> ocorrencias = leitorDados.lerTodasOcorrencias("ocorrencias.txt");

            // 2. Mapeamento Dinâmico: Converte Lat/Lon em Hexágonos H3 de Resolução 9
            Map<String, Hexagono> mapaHexagonos = new HashMap<>();

            for (Ocorrencia oc : ocorrencias) {
                String h3Index;
                if (h3 != null) {
                    // Mapeia coordenadas reais para a célula H3
                    h3Index = h3.geoToH3Address(oc.getLatitude(), oc.getLongitude(), 9);
                } else {
                    // Fallback caso a lib H3 não esteja no classpath
                    h3Index = "89a8a0233b3ffff";
                }

                mapaHexagonos.putIfAbsent(h3Index, new Hexagono(h3Index));
                mapaHexagonos.get(h3Index).adicionarOcorrencia(oc);
            }

            List<Hexagono> listaHexagonos = new ArrayList<>(mapaHexagonos.values());

            // 3. Processa o Nível de Perigo Relativo (1 a 5) baseado na hora selecionada
            perigoService.calcularPerigoDoMapaPorHora(listaHexagonos, hora);

            // 4. Agrupa os blocos e aloca as 5 viaturas
            List<Bloco> blocos = agrupadorService.criarBlocosEquilibrados(listaHexagonos, 5, hora);

            // 5. Monta DTO das Viaturas
            List<Map<String, Object>> viaturasDTO = new ArrayList<>();
            for (int i = 0; i < blocos.size(); i++) {
                Bloco b = blocos.get(i);
                if (!b.getRegiaoHexagonos().isEmpty()) {
                    Map<String, Object> vtrMap = new HashMap<>();
                    vtrMap.put("idViatura", "VTR-" + (i + 1));
                    vtrMap.put("idBloco", b.getIdBloco());
                    vtrMap.put("h3Index", b.getRegiaoHexagonos().get(0).getH3Index());
                    viaturasDTO.add(vtrMap);
                }
            }

            // 6. Monta DTO dos Hexágonos com os Níveis de Perigo Calculados (1 a 5)
            List<Map<String, Object>> hexDTO = new ArrayList<>();
            for (Hexagono h : listaHexagonos) {
                Map<String, Object> item = new HashMap<>();
                item.put("h3Index", h.getH3Index());
                item.put("nivelPerigo", h.getNivelPerigo());
                item.put("ocorrenciasNaHora", h.getOcorrenciasHorario()[hora]);
                hexDTO.add(item);
            }

            resposta.put("hexagonos", hexDTO);
            resposta.put("viaturas", viaturasDTO);

        } catch (Exception e) {
            System.err.println("Erro no MapaController: " + e.getMessage());
            resposta.put("erro", e.getMessage());
        }

        return resposta;
    }
}