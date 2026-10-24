package Backend;

public class CartaoPrograma {
    // Array de 24 posições indicando a postura da viatura para cada hora do dia (0h às 23h)
    // Ex: 1 = Patrulhamento ostensivo, 2 = Ponto de Presença Fixo (PB), 3 = Permanência em Ponto Crítico
    private int[] comportamentoPorHora;

    public CartaoPrograma() {
        this.comportamentoPorHora = new int[24];
    }

    public void definirComportamentoHora(int hora, int comportamento) {
        if (hora >= 0 && hora < 24) {
            this.comportamentoPorHora[hora] = comportamento;
        }
    }

    public int getComportamentoNaHora(int hora) {
        if (hora >= 0 && hora < 24) {
            return this.comportamentoPorHora[hora];
        }
        return 1;
    }

    public int[] getComportamentoPorHora() { return comportamentoPorHora; }
}