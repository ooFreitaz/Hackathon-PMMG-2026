import com.uber.h3core.H3Core;
import java.io.IOException;
import java.util.*;

public class MapaController {

    private final LeitorDados leitorDados = new LeitorDados();
    private final ProcessadorPerigo perigoService = new ProcessadorPerigo();
    private final AgrupadorBloco agrupadorService = new AgrupadorBloco();
    private H3Core h3;

    public MapaController() {
        try {
            this.h3 = H3Core.newInstance();
        } catch (IOException e) {
            System.err.println("Erro ao inicializar H3Core: " + e.getMessage());
        }
    }

    public Map<String, Object> getDadosMapa(int hora, List<String> viaturasAtivas) {
        Map<String, Object> resposta = new HashMap<>();

        try {
            List<Ocorrencia> ocorrencias = leitorDados.lerTodasOcorrencias("ocorrencias.txt");

            Map<String, Hexagono> mapaHexagonos = new HashMap<>();
            for (Ocorrencia oc : ocorrencias) {
                String h3Index = h3.geoToH3Address(oc.getLatitude(), oc.getLongitude(), 9);
                mapaHexagonos.putIfAbsent(h3Index, new Hexagono(h3Index));
                mapaHexagonos.get(h3Index).adicionarOcorrencia(oc);
            }

            List<Hexagono> listaHexagonos = new ArrayList<>(mapaHexagonos.values());

            // Recalcula o nível de perigo
            perigoService.calcularPerigoDoMapaPorHora(listaHexagonos, hora);

            // Distribui as viaturas ativas
            List<Map<String, Object>> viaturasDTO = new ArrayList<>();
            int qtdAtivas = (viaturasAtivas != null) ? viaturasAtivas.size() : 0;

            if (qtdAtivas > 0) {
                List<Bloco> blocos = agrupadorService.criarBlocosEquilibrados(listaHexagonos, qtdAtivas, hora);

                for (int i = 0; i < blocos.size(); i++) {
                    Bloco b = blocos.get(i);
                    if (!b.getRegiaoHexagonos().isEmpty()) {
                        Map<String, Object> vtrMap = new HashMap<>();
                        vtrMap.put("idViatura", viaturasAtivas.get(i));
                        vtrMap.put("idBloco", b.getIdBloco());
                        vtrMap.put("h3Index", b.getRegiaoHexagonos().get(0).getH3Index());
                        viaturasDTO.add(vtrMap);
                    }
                }
            }

            // Monta o DTO com Nível de Perigo e Peso Horário
            List<Map<String, Object>> hexDTO = new ArrayList<>();
            for (Hexagono h : listaHexagonos) {
                Map<String, Object> item = new HashMap<>();
                item.put("h3Index", h.getH3Index());
                item.put("nivelPerigo", h.getNivelPerigo());

                int pesoNaHora = h.getOcorrenciasHorario()[hora];
                item.put("pesoHorario", pesoNaHora);

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

    public Map<String, Object> getDadosMapa(int hora) {
        return getDadosMapa(hora, Arrays.asList("VTR-1", "VTR-2", "VTR-3", "VTR-4", "VTR-5"));
    }

    /**
 * Gera o Cartão Programa de 24 horas para uma viatura específica,
 * considerando a hora atual e as viaturas ativas no sistema.
 */
public CartaoPrograma gerarCartaoProgramaViatura(String idViatura, int hora) {
    CartaoPrograma cp = new CartaoPrograma(idViatura);

    try {
        // 1. Lê as ocorrências
        List<Ocorrencia> ocorrencias = leitorDados.lerTodasOcorrencias("ocorrencias.txt");

        // 2. Mapeia as ocorrências nos hexágonos H3
        Map<String, Hexagono> mapaHexagonos = new HashMap<>();
        for (Ocorrencia oc : ocorrencias) {
            String h3Index = h3.geoToH3Address(oc.getLatitude(), oc.getLongitude(), 9);
            mapaHexagonos.putIfAbsent(h3Index, new Hexagono(h3Index));
            mapaHexagonos.get(h3Index).adicionarOcorrencia(oc);
        }

        List<Hexagono> listaHexagonos = new ArrayList<>(mapaHexagonos.values());

        // 3. Classifica as 24 horas do dia nas 3 Classes (8 faixas cada)
        Map<Integer, Integer> mapaClasses = CartaoPrograma.calcularClasses24Horas(listaHexagonos);

        // 4. Gera a distribuição de blocos para todas as 24 horas do dia
        List<Bloco> blocosDoDia = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            perigoService.calcularPerigoDoMapaPorHora(listaHexagonos, h);
            // Calcula com base em 5 VTRs operacionais padrão
            List<Bloco> blocosHora = agrupadorService.criarBlocosEquilibrados(listaHexagonos, 5, h);
            blocosDoDia.addAll(blocosHora);
        }

        // 5. Preenche o Cartão Programa com as 24h
        cp.gerarCartaoCompleto(mapaClasses, blocosDoDia);

    } catch (Exception e) {
        System.err.println("Erro ao gerar Cartão Programa para " + idViatura + ": " + e.getMessage());
        e.printStackTrace();
    }

    return cp;
}
}