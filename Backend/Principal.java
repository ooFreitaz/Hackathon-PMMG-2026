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

        // 1. ENDPOINT DADOS DO MAPA
        server.createContext("/api/dados-mapa", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

                String query = exchange.getRequestURI().getQuery();
                int hora = 20;
                String ativasParam = "VTR-1,VTR-2,VTR-3,VTR-4,VTR-5";

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

                List<String> viaturasAtivas = new ArrayList<>();
                if (!ativasParam.trim().isEmpty()) {
                    viaturasAtivas = Arrays.asList(ativasParam.split(","));
                }

                Map<String, Object> dados = controller.getDadosMapa(hora, viaturasAtivas);
                String jsonResposta = serializarParaJSON(dados);

                byte[] respostaBytes = jsonResposta.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, respostaBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(respostaBytes);
                os.close();
            }
        });

        // 2. ENDPOINT CARTÃO PROGRAMA
        server.createContext("/api/cartao-programa", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

                String query = exchange.getRequestURI().getQuery();
                String vtr = "VTR-1";
                int hora = 20;

                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("vtr=")) vtr = param.split("=")[1];
                        else if (param.startsWith("hora=")) {
                            try { hora = Integer.parseInt(param.split("=")[1]); } catch (Exception ignored) {}
                        }
                    }
                }

                CartaoPrograma cp = controller.gerarCartaoProgramaViatura(vtr, hora);

                StringBuilder sb = new StringBuilder("{");
                sb.append("\"idViatura\":\"").append(vtr).append("\",");
                sb.append("\"comportamentos\":[");
                int[] comps = cp.getComportamentoPorHora();
                for (int i = 0; i < 24; i++) {
                    sb.append(comps[i]);
                    if (i < 23) sb.append(",");
                }
                sb.append("]}");

                byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
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

        sb.append("\"hexagonos\":[");
        List<Map<String, Object>> hexs = (List<Map<String, Object>>) mapa.get("hexagonos");
        if (hexs != null && !hexs.isEmpty()) {
            for (int i = 0; i < hexs.size(); i++) {
                Map<String, Object> h = hexs.get(i);
                sb.append("{")
                  .append("\"h3Index\":\"").append(h.get("h3Index")).append("\",")
                  .append("\"nivelPerigo\":").append(h.get("nivelPerigo")).append(",")
                  .append("\"pesoHorario\":").append(h.get("pesoHorario") != null ? h.get("pesoHorario") : 0)
                  .append("}");
                if (i < hexs.size() - 1) sb.append(",");
            }
        }
        sb.append("],");

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