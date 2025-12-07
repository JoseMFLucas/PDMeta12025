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
    private String dataInicio;
    private String dataFim;
    private int totalRespostas;
    private double percentagemCertas;


    public Pergunta(String enunciado, String[] opcoes, int opcaoCorreta) {
        this.enunciado = enunciado;
        this.opcoes = opcoes;
        this.opcaoCorreta = opcaoCorreta;
    }

    public Pergunta() {
        // Default constructor
    }

    // Getters
    public int getId() { return id; }
    public String getEnunciado() { return enunciado; }
    public String[] getOpcoes() { return opcoes; }
    public int getOpcaoCorreta() { return opcaoCorreta; }
    public int getIdDocente() { return idDocente; }
    public String getCodigo() { return codigo; }
    public String getDataInicio() { return dataInicio; }
    public String getDataFim() { return dataFim; }
    public int getTotalRespostas() { return totalRespostas; }
    public double getPercentagemCertas() { return percentagemCertas; }


    // Setters
    public void setId(int id) { this.id = id; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }
    public void setOpcoes(String[] opcoes) { this.opcoes = opcoes; }
    public void setOpcaoCorreta(int opcaoCorreta) { this.opcaoCorreta = opcaoCorreta; }
    public void setIdDocente(int idDocente) { this.idDocente = idDocente; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }
    public void setDataFim(String dataFim) { this.dataFim = dataFim; }
    public void setTotalRespostas(int totalRespostas) { this.totalRespostas = totalRespostas; }
    public void setPercentagemCertas(double percentagemCertas) { this.percentagemCertas = percentagemCertas; }


    @Override
    public String toString() {
        return String.format("ID: %d - %s", id, enunciado);
    }
}
