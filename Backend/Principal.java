import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Principal {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        MapaController controller = new MapaController();

        server.createContext("/api/dados-mapa", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                // Configuração dos cabeçalhos CORS
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

                String query = exchange.getRequestURI().getQuery();
                int hora = 20; // Hora padrão de análise
                String ativasParam = "VTR-1,VTR-2,VTR-3,VTR-4,VTR-5"; // Padrão: todas operacionais

                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("hora=")) {
                            try { 
                                hora = Integer.parseInt(param.split("=")[1]); 
                            } catch (Exception ignored) {}
                        } else if (param.startsWith("ativas=")) {
                            String[] partes = param.split("=");
                            if (partes.length > 1) {
                                ativasParam = partes[1];
                            } else {
                                ativasParam = "";
                            }
                        }
                    }
                }

                // Cria a lista de viaturas operacionais
                List<String> viaturasAtivas = new ArrayList<>();
                if (!ativasParam.trim().isEmpty()) {
                    viaturasAtivas = Arrays.asList(ativasParam.split(","));
                }

                // Obtém os dados do MapaController
                Map<String, Object> dados = controller.getDadosMapa(hora, viaturasAtivas);
                String jsonResposta = serializarParaJSON(dados);

                byte[] respostaBytes = jsonResposta.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, respostaBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(respostaBytes);
                os.close();
            }
        });

        server.start();
        System.out.println("=== SERVIDOR PMMG OPERACIONAL NA PORTA 8080 ===");
    }

    @SuppressWarnings("unchecked")
    private static String serializarParaJSON(Map<String, Object> mapa) {
        if (mapa == null) return "{\"hexagonos\":[],\"viaturas\":[]}";

        StringBuilder sb = new StringBuilder("{");

        // 1. Serializa a lista de Hexágonos com Nível de Perigo
        sb.append("\"hexagonos\":[");
        List<Map<String, Object>> hexs = (List<Map<String, Object>>) mapa.get("hexagonos");
        if (hexs != null && !hexs.isEmpty()) {
            for (int i = 0; i < hexs.size(); i++) {
                Map<String, Object> h = hexs.get(i);
                sb.append("{")
                  .append("\"h3Index\":\"").append(h.get("h3Index")).append("\",")
                  .append("\"nivelPerigo\":").append(h.get("nivelPerigo"))
                  .append("}");
                if (i < hexs.size() - 1) sb.append(",");
            }
        }
        sb.append("],");

        // 2. Serializa as Viaturas
        sb.append("\"viaturas\":[");
        List<Map<String, Object>> vtrs = (List<Map<String, Object>>) mapa.get("viaturas");
        if (vtrs != null && !vtrs.isEmpty()) {
            for (int i = 0; i < vtrs.size(); i++) {
                Map<String, Object> v = vtrs.get(i);
                sb.append("{")
                  .append("\"idViatura\":\"").append(v.get("idViatura")).append("\",")
                  .append("\"idBloco\":\"").append(v.get("idBloco")).append("\",")
                  .append("\"h3Index\":\"").append(v.get("h3Index")).append("\"")
                  .append("}");
                if (i < vtrs.size() - 1) sb.append(",");
            }
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }
}