public class Viatura {
    private String idViatura;
    private Hexagono posicaoAtual;
    private boolean disponivel;
    private Bloco blocoResponsavel;
    private int comportamentoAtual; // 1 a 3
    private CartaoPrograma cartaoPrograma;

    public Viatura(String idViatura, Bloco blocoResponsavel) {
        this.idViatura = idViatura;
        this.blocoResponsavel = blocoResponsavel;
        this.disponivel = true;
        this.comportamentoAtual = 1;
        this.cartaoPrograma = new CartaoPrograma();
    }

    // Atualiza a postura da viatura com base no plano de 24h para a hora informada
    public void atualizarComportamentoPelaHora(int horaDoDia) {
        this.comportamentoAtual = this.cartaoPrograma.getComportamentoNaHora(horaDoDia);
    }

    // Getters e Setters
    public String getIdViatura() { return idViatura; }
    public Hexagono getPosicaoAtual() { return posicaoAtual; }
    public void setPosicaoAtual(Hexagono posicaoAtual) { this.posicaoAtual = posicaoAtual; }
    public boolean isDisponivel() { return disponivel; }
    public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }
    public Bloco getBlocoResponsavel() { return blocoResponsavel; }
    public int getComportamentoAtual() { return comportamentoAtual; }
    public void setComportamentoAtual(int comportamentoAtual) { this.comportamentoAtual = comportamentoAtual; }
    public CartaoPrograma getCartaoPrograma() { return cartaoPrograma; }
    public void setCartaoPrograma(CartaoPrograma cartaoPrograma) { this.cartaoPrograma = cartaoPrograma; }
}
