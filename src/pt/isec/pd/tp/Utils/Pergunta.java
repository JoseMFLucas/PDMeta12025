package pt.isec.pd.tp.Utils;

import java.io.Serializable;

public class Pergunta implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String enunciado;
    private String[] opcoes;
    private int opcaoCorreta;
    private int idDocente;
    private String codigo;

    public Pergunta(String enunciado, String[] opcoes, int opcaoCorreta) {
        this.enunciado = enunciado;
        this.opcoes = opcoes;
        this.opcaoCorreta = opcaoCorreta;
    }

    // Getters
    public int getId() { return id; }
    public String getEnunciado() { return enunciado; }
    public String[] getOpcoes() { return opcoes; }
    public int getOpcaoCorreta() { return opcaoCorreta; }
    public int getIdDocente() { return idDocente; }
    public String getCodigo() { return codigo; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }
    public void setOpcoes(String[] opcoes) { this.opcoes = opcoes; }
    public void setOpcaoCorreta(int opcaoCorreta) { this.opcaoCorreta = opcaoCorreta; }
    public void setIdDocente(int idDocente) { this.idDocente = idDocente; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    @Override
    public String toString() {
        return String.format("ID: %d - %s", id, enunciado);
    }
}
