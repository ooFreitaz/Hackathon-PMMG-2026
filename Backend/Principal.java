import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Principal {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        MapaController controller = new MapaController();

        server.createContext("/api/dados-mapa", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json");

                String query = exchange.getRequestURI().getQuery();
                int hora = 20; // Hora padrão
                if (query != null && query.contains("hora=")) {
                    try {
                        hora = Integer.parseInt(query.split("hora=")[1].split("&")[0]);
                    } catch (Exception ignored) {}
                }

                Map<String, Object> dados = controller.getDadosMapa(hora);
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
        StringBuilder sb = new StringBuilder("{");

        // Serializa a lista de Hexágonos
        sb.append("\"hexagonos\":[");
        List<Map<String, Object>> hexs = (List<Map<String, Object>>) mapa.get("hexagonos");
        if (hexs != null) {
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

        // Serializa a lista de Viaturas
        sb.append("\"viaturas\":[");
        List<Map<String, Object>> vtrs = (List<Map<String, Object>>) mapa.get("viaturas");
        if (vtrs != null) {
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