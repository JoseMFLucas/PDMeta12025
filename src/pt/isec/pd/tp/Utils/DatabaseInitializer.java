package pt.isec.pd.tp.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    private static final String DB_FILE_NAME = "pdtp.db";

    private static final Path DB_FOLDER_PATH = Paths.get("db");

    private static final Path FULL_DB_PATH = DB_FOLDER_PATH.resolve(DB_FILE_NAME).normalize();

    private static final String DB_URL = "jdbc:sqlite:" + FULL_DB_PATH.toAbsolutePath();

    public static Connection connectDB() {

        boolean dbExists = Files.exists(FULL_DB_PATH);
        Connection conn = null;

        try{
            if(!Files.exists(DB_FOLDER_PATH)){
                Files.createDirectories(DB_FOLDER_PATH);
                System.out.println("Pasta db criada");
            }

            conn = DriverManager.getConnection(DB_URL);

            if(!dbExists){
                System.out.println("Db nova. A criar tabelas...");
                createTables(conn);
            }
        } catch (IOException e) {
            System.err.println("Erro IO: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Erro SQL: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("Erro ao fechar a ligação á base de dados: " + ex.getMessage());
                }
                conn = null;
            }
        }


        return conn;
    }


    private static String sqlTables() {
        return
                "CREATE TABLE Configuracao (" +
                        "    codigoregisto TEXT PRIMARY KEY NOT NULL," +
                        "    versao TEXT NOT NULL" +
                        ");" +

                        // Tabela Docente
                        "CREATE TABLE Docente (" +
                        "    iddocente INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    nome TEXT NOT NULL," +
                        "    email TEXT UNIQUE NOT NULL," +
                        "    password TEXT NOT NULL" +
                        ");" +

                        // Tabela Estudante
                        "CREATE TABLE Estudante (" +
                        "    idestudante INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    numero_estudante TEXT UNIQUE NOT NULL," +
                        "    nome TEXT NOT NULL," +
                        "    email TEXT UNIQUE NOT NULL," +
                        "    password TEXT NOT NULL" +
                        ");" +

                        // Tabela Pergunta
                        "CREATE TABLE Pergunta (" +
                        "    idpergunta INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    iddocente INTEGER NOT NULL," +
                        "    enunciado TEXT NOT NULL," +
                        "    codigo_acesso TEXT UNIQUE NOT NULL," +
                        "    data_hora_inicio DATETIME NOT NULL," +
                        "    data_hora_fim DATETIME NOT NULL," +
                        "    opcao_correta_indice INTEGER NOT NULL," +
                        "    FOREIGN KEY (iddocente) REFERENCES Docente(iddocente)" +
                        ");" +

                        // Tabela Opcao
                        "CREATE TABLE Opcao (" +
                        "    idopcao INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    idpergunta INTEGER UNIQUE NOT NULL," +
                        "    indice INTEGER UNIQUE NOT NULL," +
                        "    texto TEXT NOT NULL," +
                        "    FOREIGN KEY (idpergunta) REFERENCES Pergunta(idpergunta)" +
                        ");" +

                        // Tabela Resposta
                        "CREATE TABLE Resposta (" +
                        "    idpergunta INTEGER NOT NULL," +
                        "    idestudante INTEGER NOT NULL," +
                        "    data_hora_realizacao DATETIME NOT NULL," +
                        "    opcao_escolhida_indice INTEGER NOT NULL," +
                        "    esta_certa BOOLEAN NOT NULL," +
                        "    PRIMARY KEY (idpergunta, idestudante, data_hora_realizacao)," +
                        "    FOREIGN KEY (idpergunta) REFERENCES Pergunta(idpergunta)," +
                        "    FOREIGN KEY (idestudante) REFERENCES Estudante(estudante_id)" +
                        ");";
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON;");

            String sql = sqlTables();

            String[] statements = sql.split(";");
            for (String statement : statements) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement.trim());
                }
            }
            System.out.println("Todas as tabelas foram criadas com sucesso.");

        } catch (SQLException e) {
            System.err.println("Erro durante a criação das tabelas: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try (Connection conn = connectDB()) {
            if (conn != null) {
                System.out.println("Conexão estabelecida com sucesso.");
            } else {
                System.out.println("Falha na conexão.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao fechar a conexão: " + e.getMessage());
        }
    }
}