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

    public Map<String, Object> getDadosMapa(int hora, List<String> viaturasAtivas) {
    Map<String, Object> resposta = new HashMap<>();

    try {
        // 1. Lê as ocorrências salvas em memória
        List<Ocorrencia> ocorrencias = leitorDados.lerTodasOcorrencias("ocorrencias.txt");

        // 2. Mapeia as ocorrências para os hexágonos H3
        Map<String, Hexagono> mapaHexagonos = new HashMap<>();
        for (Ocorrencia oc : ocorrencias) {
            String h3Index = h3.geoToH3Address(oc.getLatitude(), oc.getLongitude(), 9);
            mapaHexagonos.putIfAbsent(h3Index, new Hexagono(h3Index));
            mapaHexagonos.get(h3Index).adicionarOcorrencia(oc);
        }

        List<Hexagono> listaHexagonos = new ArrayList<>(mapaHexagonos.values());

        // 3. RECALCULA A MÉDIA DA HORA
        perigoService.calcularPerigoDoMapaPorHora(listaHexagonos, hora);

        // 4. DISTRIBUI APENAS AS VIATURAS OPERACIONAIS
        List<Map<String, Object>> viaturasDTO = new ArrayList<>();
        int qtdAtivas = (viaturasAtivas != null) ? viaturasAtivas.size() : 0;

        if (qtdAtivas > 0) {
            List<Bloco> blocos = agrupadorService.criarBlocosEquilibrados(listaHexagonos, qtdAtivas, hora);

            for (int i = 0; i < blocos.size(); i++) {
                Bloco b = blocos.get(i);
                if (!b.getRegiaoHexagonos().isEmpty()) {
                    Map<String, Object> vtrMap = new HashMap<>();
                    // Atribui o ID da viatura operacional (ex: VTR-1, VTR-3)
                    vtrMap.put("idViatura", viaturasAtivas.get(i));
                    vtrMap.put("idBloco", b.getIdBloco());
                    vtrMap.put("h3Index", b.getRegiaoHexagonos().get(0).getH3Index());
                    viaturasDTO.add(vtrMap);
                }
            }
        }

        // 5. MONTA DTO DOS HEXÁGONOS (Garante que nivelPerigo seja retornado)
        List<Map<String, Object>> hexDTO = new ArrayList<>();
        for (Hexagono h : listaHexagonos) {
            Map<String, Object> item = new HashMap<>();
            item.put("h3Index", h.getH3Index());
            item.put("nivelPerigo", h.getNivelPerigo()); // Valores de 1 a 5
            hexDTO.add(item);
        }

        resposta.put("hexagonos", hexDTO);
        resposta.put("viaturas", viaturasDTO);

    } catch (Exception e) {
        System.err.println("Erro ao calcular dados do mapa: " + e.getMessage());
        e.printStackTrace();
        resposta.put("hexagonos", new ArrayList<>());
        resposta.put("viaturas", new ArrayList<>());
    }

    return resposta;
}

// Sobrecarga para evitar que chamadas antigas sem a lista de VTRs quebrem
public Map<String, Object> getDadosMapa(int hora) {
    return getDadosMapa(hora, Arrays.asList("VTR-1", "VTR-2", "VTR-3", "VTR-4", "VTR-5"));
}
}