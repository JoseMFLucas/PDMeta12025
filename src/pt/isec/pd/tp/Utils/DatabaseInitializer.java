import java.sql.*;

public class DatabaseInitializer {
    private static final String DB_URL = "jdbc:sqlite:db/pdtp.db";

    static final String USER = "username";
    static final String PASS = "password";

    private Connection connectdb(){
        Connection conn = null;
        try {
            // Registar JDBC driver
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            System.out.println("Conexão com SQLite estabelecida com sucesso.");
        } catch (ClassNotFoundException e) {
            System.err.println("Erro: Driver JDBC do SQLite não encontrado.");
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.err.println("Erro de conexão com a base de dados SQLite.");
            System.out.println(e.getMessage());
            System.err.println("A criar nova base de dados...");

            try{
                conn = DriverManager.getConnection(DB_URL);

            }
        }
        return conn;
    }
}