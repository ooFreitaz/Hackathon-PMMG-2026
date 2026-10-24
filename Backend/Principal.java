import java.io.IOException;
import java.util.List;

public class Principal {
    public static void main(String[] args) throws IOException {
        LeitorDados leitor = new LeitorDados();
        
        List<Ocorrencia> lista = leitor.lerTodasOcorrencias("ocorrencias.txt");

        System.out.println("Total de ocorrências carregadas na lista: " + lista.size());

        if (!lista.isEmpty()) {
            Ocorrencia primeira = lista.get(0);
            Ocorrencia ultima = lista.get(lista.size() - 1);

            System.out.println("Primeira Ocorrência -> Natureza: " + primeira.getNatureza()
                    + " | Hora: " + primeira.getHora()
                    + " | Latitude: " + primeira.getLatitude()
                    + " | Longitude: " + primeira.getLongitude());
            System.out.println("Última Ocorrência   -> Natureza: " + ultima.getNatureza()
                    + " | Hora: " + ultima.getHora()
                    + " | Latitude: " + ultima.getLatitude()
                    + " | Longitude: " + ultima.getLongitude());
        }
    }
}
