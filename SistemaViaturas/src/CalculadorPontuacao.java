import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CalculadorPontuacao {

    private static final Map<String, Integer> PONTOS = Map.of(
            "A", 4,
            "B", 5,
            "C", 1,
            "D", 1,
            "E", 2,
            "F", 3
    );

    public static int calcularPontuacaoTotal(String caminhoArquivo) throws IOException {

        Pattern patternNatureza = Pattern.compile("\"natureza\"\\s*:\\s*\"([A-F])\"");

        try (Stream<String> linhas = Files.lines(Path.of(caminhoArquivo))) {

            return linhas
                    .map(patternNatureza::matcher)
                    .filter(Matcher::find)
                    .map(matcher -> matcher.group(1))
                    .mapToInt(natureza -> PONTOS.getOrDefault(natureza, 0))
                    .sum();
        }
    }

    /*public static void main(String[] args) {

        String caminhoArquivo = "C:\\Users\\samuo\\Downloads\\ocorrencias.txt";

        try {
            int pontuacaoTotal = calcularPontuacaoTotal(caminhoArquivo);

            System.out.println("Pontuação Total = " + pontuacaoTotal);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
}