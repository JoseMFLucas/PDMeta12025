package pt.isec.pd.tp.Client;

import java.io.Serial;
import java.io.Serializable;

public class Client implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;


    public enum Tipo {
        DOCENTE,
        ESTUDANTE,
    }

    private int id;
    private String nome;
    private String email;
    private String password;
    private Tipo tipo;
    private int numero;
    private String codigoRegisto;

    public Client(String email, String password, String nome) {
        this.email = email;
        this.password = password;
        this.nome = nome;
    }

    public Client(int id, String email, String password, Tipo tipo) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.tipo = tipo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Tipo getTipo() { return tipo; }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getCodigoRegisto() {
        return codigoRegisto;
    }

    public void setCodigoRegisto(String codigoRegisto) {
        this.codigoRegisto = codigoRegisto;
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", tipo=" + tipo +
                ", numero=" + numero +
                ", codigoRegisto='" + codigoRegisto + '\'' +
                '}';
    }
}
