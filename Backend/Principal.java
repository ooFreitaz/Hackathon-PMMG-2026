import java.util.List;

public class Principal {
    public static void main(String[] args) {
        LeitorDados leitor = new LeitorDados();
        
        List<Ocorrencia> lista = leitor.lerTodasOcorrencias("ocorrencias.txt");

        System.out.println("Total de ocorrências carregadas na lista: " + lista.size());

        if (!lista.isEmpty()) {
            Ocorrencia primeira = lista.get(0);
            Ocorrencia ultima = lista.get(lista.size() - 1);

            System.out.println("Primeira Ocorrência -> Data: " + primeira.getData() + " | Natureza: " + primeira.getNatureza()+" | Hora: "+primeira.getHora());
            System.out.println("Última Ocorrência   -> Data: " + ultima.getData() + " | Natureza: " + ultima.getNatureza());
        }
    }
}
