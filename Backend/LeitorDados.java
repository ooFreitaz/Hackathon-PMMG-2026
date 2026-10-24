import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LeitorDados {
    private static final Pattern PADRAO_OCORRENCIA = Pattern.compile(
            "\\{\\s*\"id\"\\s*:\\s*\"[^\"]+\"\\s*,\\s*"
                    + "\"natureza\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*"
                    + "\"dataHora\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*"
                    + "\"latLon\"\\s*:\\s*\\{\\s*"
                    + "\"lon\"\\s*:\\s*(-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\s*,\\s*"
                    + "\"lat\"\\s*:\\s*(-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\s*\\}\\s*\\}",
            Pattern.DOTALL);
    private static final Pattern PADRAO_TOTAL = Pattern.compile("\"total\"\\s*:\\s*(\\d+)");
    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public List<Ocorrencia> lerTodasOcorrencias(String caminhoArquivo) throws IOException {
        Path arquivo = Path.of(caminhoArquivo);
        if (!Files.isRegularFile(arquivo) && !arquivo.isAbsolute()) {
            Path caminhoNoBackend = Path.of("Backend").resolve(arquivo);
            if (Files.isRegularFile(caminhoNoBackend)) {
                arquivo = caminhoNoBackend;
            }
        }
        if (!Files.isRegularFile(arquivo)) {
            throw new IOException("Arquivo não encontrado: " + arquivo.toAbsolutePath()
                    + " (diretório de execução: " + Path.of("").toAbsolutePath() + ")");
        }

        String json = Files.readString(arquivo, StandardCharsets.UTF_8);
        List<Ocorrencia> listaOcorrencias = new ArrayList<>();
        Matcher matcher = PADRAO_OCORRENCIA.matcher(json);
        while (matcher.find()) {
            String naturezaTexto = matcher.group(1);
            if (naturezaTexto.length() != 1) {
                throw new IOException("Natureza inválida no registro " + (listaOcorrencias.size() + 1));
            }

            try {
                char natureza = naturezaTexto.charAt(0);
                LocalDateTime dataHora = LocalDateTime.parse(matcher.group(2), FORMATO_DATA_HORA);
                double lon = Double.parseDouble(matcher.group(3));
                double lat = Double.parseDouble(matcher.group(4));
                listaOcorrencias.add(new Ocorrencia(lat, lon, dataHora.toLocalTime(), natureza));
            } catch (RuntimeException e) {
                throw new IOException("Não foi possível interpretar o registro "
                        + (listaOcorrencias.size() + 1), e);
            }
        }

        Matcher totalMatcher = PADRAO_TOTAL.matcher(json);
        if (totalMatcher.find()) {
            int totalDeclarado = Integer.parseInt(totalMatcher.group(1));
            if (listaOcorrencias.size() != totalDeclarado) {
                throw new IOException("Foram lidas " + listaOcorrencias.size()
                        + " ocorrências, mas o arquivo declara " + totalDeclarado + ".");
            }
        }
        return listaOcorrencias;
    }
}
