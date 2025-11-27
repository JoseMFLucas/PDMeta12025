package pt.isec.pd.tp.Client;

public class Client{
    
    public enum Tipo {
        DOCENTE,
        ESTUDANTE
    }

    private final String email;
    private final String password;
    private Tipo tipo;

    public Client(String email, String password, Tipo tipo) {
        this.email = email;
        this.password = password;
        this.tipo = tipo;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }
}
