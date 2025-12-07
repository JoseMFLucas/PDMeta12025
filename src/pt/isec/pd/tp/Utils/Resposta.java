package pt.isec.pd.tp.Utils;

import java.io.Serializable;

public class Resposta implements Serializable {
    private static final long serialVersionUID = 1L;

    private String enunciado;
    private String data;
    private String opcao;
    private String opcao_escolhida;
    private String resultado;

    public Resposta(String enunciado, String data, String opcao, String opcao_escolhida, String resultado) {
        this.enunciado = enunciado;
        this.data = data;
        this.opcao = opcao;
        this.opcao_escolhida = opcao_escolhida;
        this.resultado = resultado;
    }

    // Getters and Setters
    public String getEnunciado() {
        return enunciado;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getOpcao() {
        return opcao;
    }

    public void setOpcao(String opcao) {
        this.opcao = opcao;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }
}
