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
    private final String email;
    private final String password;
    private Tipo tipo;

    public Client(String email, String password, Tipo tipo) {
        this.email = email;
        this.password = password;
        this.tipo = tipo;
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

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Tipo getTipo() { return tipo; }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    // Inside the pt.isec.pd.tp.Utils.Client class

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", tipo=" + tipo +
                '}';
    }
}