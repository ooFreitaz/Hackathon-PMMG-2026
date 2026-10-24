import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LeitorDados {
    public List<Ocorrencia> lerTodasOcorrencias(String caminhoArquivo) {
        List<Ocorrencia> listaOcorrencias = new ArrayList<>();
        StringBuilder conteudo = new StringBuilder();

        // 1. Lê todo o conteúdo do arquivo
        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                conteudo.append(linha);
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
            return listaOcorrencias;
        }

        String json = conteudo.toString();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // 2. Quebra o texto a cada abertura de chave '{'
        String[] blocos = json.split("\\{");

        for (String bloco : blocos) {
            // Processa apenas os blocos que contêm dados de uma ocorrência
            if (bloco.contains("\"natureza\":") && bloco.contains("\"dataHora\":")) {
                try {
                    double lat = Double.parseDouble(extrairValor(bloco, "\"lat\":"));
                    double lon = Double.parseDouble(extrairValor(bloco, "\"lon\":"));

                    String naturezaStr = extrairValorTexto(bloco, "\"natureza\":");
                    char natureza = naturezaStr.charAt(0);

                    String dataHoraStr = extrairValorTexto(bloco, "\"dataHora\":");
                    LocalDateTime dateTime = LocalDateTime.parse(dataHoraStr, formatter);

                    LocalDate data = dateTime.toLocalDate();
                    LocalTime hora = dateTime.toLocalTime();

                    // Cria o objeto e adiciona à lista
                    listaOcorrencias.add(new Ocorrencia(lat, lon, data, hora, natureza));

                } catch (Exception e) {
                    // Ignora blocos malformados
                }
            }
        }

        return listaOcorrencias;
    }

    private String extrairValor(String texto, String chave) {
        int inicio = texto.indexOf(chave);
        if (inicio == -1) return "0";
        inicio += chave.length();
        int fim = texto.indexOf(",", inicio);
        if (fim == -1) fim = texto.indexOf("}", inicio);
        return texto.substring(inicio, fim).replaceAll("[^0-9.-]", "").trim();
    }

    private String extrairValorTexto(String texto, String chave) {
        int inicio = texto.indexOf(chave);
        if (inicio == -1) return "";
        inicio += chave.length();
        int primeiraAspa = texto.indexOf("\"", inicio) + 1;
        int segundaAspa = texto.indexOf("\"", primeiraAspa);
        return texto.substring(primeiraAspa, segundaAspa);
    }
}
