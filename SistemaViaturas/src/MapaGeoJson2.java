import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MapaGeoJson2 extends JPanel {

    // ============================
    // Classe que representa um ponto
    // ============================
    public static class Ponto {
        double lon;
        double lat;

        public Ponto(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
        }
    }

    private final List<Ponto> pontos;

    public MapaGeoJson2(List<Ponto> pontos) {
        this.pontos = pontos;
    }

    // ============================
    // Leitura do GeoJSON
    // ============================
    public static List<Ponto> lerGeoJson(String caminhoArquivo) {

        List<Ponto> pontos = new ArrayList<>();

        try {

            ObjectMapper mapper = new ObjectMapper();

            JsonNode root = mapper.readTree(new File(caminhoArquivo));

            JsonNode features = root.get("features");

            if (features == null || !features.isArray()) {
                System.out.println("Nenhuma feature encontrada.");
                return pontos;
            }

            int contador = 0;

            for (JsonNode feature : features) {

                JsonNode geometry = feature.get("geometry");

                if (geometry == null) {
                    continue;
                }

                String tipo = geometry.get("type").asText();

                if ("Polygon".equals(tipo)) {

                    JsonNode coordinates = geometry.get("coordinates");

                    for (JsonNode anel : coordinates) {

                        for (JsonNode coordenada : anel) {

                            double lon = coordenada.get(0).asDouble();
                            double lat = coordenada.get(1).asDouble();

                            pontos.add(new Ponto(lon, lat));

                            contador++;

                            System.out.printf(
                                    "Ponto %d -> Longitude: %.15f | Latitude: %.15f%n",
                                    contador,
                                    lon,
                                    lat
                            );
                        }
                    }
                }
            }

            System.out.println();
            System.out.println("Total de coordenadas lidas: " + contador);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return pontos;
    }

    // ============================
    // Desenho do polígono
    // ============================
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        if (pontos == null || pontos.isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;

        double minLon = pontos.stream()
                .mapToDouble(p -> p.lon)
                .min()
                .orElse(0);

        double maxLon = pontos.stream()
                .mapToDouble(p -> p.lon)
                .max()
                .orElse(0);

        double minLat = pontos.stream()
                .mapToDouble(p -> p.lat)
                .min()
                .orElse(0);

        double maxLat = pontos.stream()
                .mapToDouble(p -> p.lat)
                .max()
                .orElse(0);

        int margem = 30;

        int largura = getWidth() - (margem * 2);
        int altura = getHeight() - (margem * 2);

        Polygon polygon = new Polygon();

        for (Ponto p : pontos) {

            int x = (int) (((p.lon - minLon)
                    / (maxLon - minLon))
                    * largura) + margem;

            int y = (int) (((maxLat - p.lat)
                    / (maxLat - minLat))
                    * altura) + margem;

            polygon.addPoint(x, y);
        }

        g2.setColor(Color.LIGHT_GRAY);
        g2.fillPolygon(polygon);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        g2.drawPolygon(polygon);
    }

    // ============================
    // Main
    // ============================
    public static void main(String[] args) {

        String arquivo =
               "dados\\39_cia.geojson";  //arquivo com coordenadas

        List<Ponto> pontos = lerGeoJson(arquivo);

        System.out.println("Total de pontos carregados: " + pontos.size());

        JFrame frame = new JFrame("Visualizador GeoJSON");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 900);

        frame.add(new MapaGeoJson2(pontos));

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
