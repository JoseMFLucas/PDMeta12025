package pt.isec.pd.tp.Server.dados;

import pt.isec.pd.tp.Client.Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DBManager {

    private static final String DB_FILE_NAME = "pdtp.db";
    private final String dbPath;
    private Connection conn;
    private int versaoDB;

    public DBManager(String dbPath) {
        this.dbPath = dbPath;
        this.conn = connectDB();
        versaoDB = 0;
    }

    private Connection connectDB() {
        Path providedPath = Paths.get(this.dbPath).normalize();
        Path dbFilePath = providedPath;

        if (Files.isDirectory(providedPath)) {
            dbFilePath = providedPath.resolve(DB_FILE_NAME);
        }

        Path dbFolder = dbFilePath.getParent();
        String dbUrl = "jdbc:sqlite:" + dbFilePath.toAbsolutePath();

        Connection connection = null;
        boolean dbExists = Files.exists(dbFilePath);

        try {
            if (dbFolder != null && !Files.exists(dbFolder)) {
                Files.createDirectories(dbFolder);
                System.out.println("Pasta " + dbFolder + " criada");
            }

            connection = DriverManager.getConnection(dbUrl);

            if (!dbExists) {
                System.out.println("Db nova. A criar tabelas...");
                createTables(connection);
            }
        } catch (IOException e) {
            System.err.println("Erro IO: " + e.getMessage());
            return null;
        } catch (SQLException e) {
            System.err.println("Erro SQL: " + e.getMessage());
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    System.err.println("Erro ao fechar a ligação á base de dados: " + ex.getMessage());
                }
            }
            return null;
        }
        return connection;
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
                        ");" +

                        "INSERT INTO Configuracao (codigoregisto, versao)" +
                        "    VALUES ('INIT', '0');"
                ;
    }

    private static void createTables(Connection conn){
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

    public String login(Client client){
        if(conn == null){
            System.err.println("Login falhou: Conexão com a base de dados não estabelecida.");
            return null;
        }
        String email = client.getEmail();
        String password = client.getPassword();

        if (checkLogin("Docente", email, password)) {
            return "DOCENTE";
        }

        else if (checkLogin("Estudante", email, password)) {
            return "ESTUDANTE";
        }

        return null;
    }

    public boolean checkLogin(String userType, String email, String password){

        if (!userType.equals("Docente") && !userType.equals("Estudante")) {
            throw new IllegalArgumentException("Tipo de utilizador inválido.");
        }

        String sql = "SELECT * FROM " + userType + " WHERE email = ? AND password = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("Erro ao verificar login: " + e.getMessage());
        }
        return false;
    }

    public boolean registarEstudante(String[] msg) {
        if (conn == null || msg.length != 4) {
            return false;
        }

        String sql = "INSERT INTO Estudante (numero_estudante, nome, email, password) VALUES (?, ?, ?, ?)";
        boolean originalAutoCommit = false;

        try {
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false); // Inicia a transação

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, msg[0]);
                pstmt.setString(2, msg[1]);
                pstmt.setString(3, msg[2]);
                pstmt.setString(4, msg[3]);

                int linhasAfetadas = pstmt.executeUpdate();

                if (linhasAfetadas > 0) {
                    conn.commit(); // Confirma a transação
                    return true;
                } else {
                    conn.rollback(); // Desfaz se nada foi inserido
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao adicionar Estudante: " + e.getMessage());
            try {
                conn.rollback(); // Desfaz em caso de erro
            } catch (SQLException ex) {
                System.err.println("Erro ao realizar rollback: " + ex.getMessage());
            }
            return false;
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit); // Restaura o modo original
            } catch (SQLException e) {
                System.err.println("Erro ao restaurar auto-commit: " + e.getMessage());
            }
        }
    }


    public boolean registarDocente(String[] msg) {
        if (conn == null || msg.length != 4) {
            return false;
        }

        String sql = "INSERT INTO Docente (nome, email, password) VALUES (?, ?, ?)";
        String sql_codigounico = "SELECT codigoregisto FROM Configuracao";
        boolean originalAutoCommit = false;

        try {
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false); // Inicia a transação

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql_codigounico)) {

                if (rs.next() && rs.getString("codigoregisto").equals(msg[3])) {
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, msg[0]);
                        pstmt.setString(2, msg[1]);
                        pstmt.setString(3, msg[2]);

                        int linhasAfetadas = pstmt.executeUpdate();

                        if (linhasAfetadas > 0) {
                            conn.commit(); // Confirma a transação
                            return true;
                        }
                    }
                }
                conn.rollback(); // Desfaz se o código estiver incorreto ou a inserção falhar
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao adicionar Docente: " + e.getMessage());
            try {
                conn.rollback(); // Desfaz em caso de erro
            } catch (SQLException ex) {
                System.err.println("Erro ao realizar rollback: " + ex.getMessage());
            }
            return false;
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit); // Restaura o modo original
            } catch (SQLException e) {
                System.err.println("Erro ao restaurar auto-commit: " + e.getMessage());
            }
        }
    }


    public static boolean criaPergunta(String msg){

        return false;
    }

    public boolean eliminarPergunta(String msg){

        int idpergunta = Integer.parseInt(msg);

        if (temRespostasAssociadas(idpergunta)) {
            System.out.println("Não é possível eliminar a pergunta. Já tem respostas associadas.");
            return false;
        }

        try {
            conn.setAutoCommit(false);

            // Elimina primeiro as Opções
            String sqlDeleteOpcoes = "DELETE FROM Opcao WHERE idpergunta = ?";

            try (PreparedStatement pstmtOpcoes = conn.prepareStatement(sqlDeleteOpcoes)) {
                pstmtOpcoes.setInt(1, idpergunta);
                pstmtOpcoes.executeUpdate();
                System.out.println("Opções associadas á questão são eliminadas.");
            }

            // Eliminar depois a pergunta
            String sqlDeletePergunta = "DELETE FROM Pergunta WHERE idpergunta = ?";

            try (PreparedStatement pstmtPergunta = conn.prepareStatement(sqlDeletePergunta)) {
                pstmtPergunta.setLong(1, idpergunta);
                int linhasAfetadas = pstmtPergunta.executeUpdate();

                if (linhasAfetadas > 0) {
                    conn.commit(); // Confirma ambas as eliminações
                    System.out.println("Pergunta eliminada com sucesso.");
                    return true;
                } else {
                    conn.rollback(); // Se a pergunta não foi eliminada, desfaz a eliminação das opções
                    System.out.println("Pergunta não encontrada ou falha na eliminação.");
                    return false;
                }
            }
        } catch (SQLException e) {
            try {
                conn.rollback(); // Desfaz em caso de qualquer erro
            } catch (SQLException ex) {
                System.err.println("Erro ao realizar rollback: " + ex.getMessage());
            }
            System.err.println("Erro SQL ao eliminar pergunta: " + e.getMessage());
            return false;
        } finally {
            try {
                conn.setAutoCommit(true); // Restaura o modo auto-commit
            } catch (SQLException e) {
                System.err.println("Erro ao restaurar auto-commit: " + e.getMessage());
            }
        }
    }

    private boolean temRespostasAssociadas(int idpergunta) {
        String sqlCheck = "SELECT COUNT(*) FROM Resposta WHERE idpergunta = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlCheck)) {
            pstmt.setLong(1, idpergunta);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Se a contagem for maior que zero, existem respostas.
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao verificar respostas associadas: " + e.getMessage());
            // Se houver um erro, assumimos que não deve ser eliminada por segurança.
            return true;
        }
        return false;
    }

    public List<String[]> listarPerguntasDocente(int id_docente, String msg){
        List<String[]> Perguntas = new ArrayList<>();

        LocalDateTime agora = LocalDateTime.now();

        Timestamp datahoraatualTimestamp = Timestamp.valueOf(agora);

        String sql = "SELECT idpergunta, enunciado, codigo_acesso, data_hora_inicio, data_hora_fim, opcao_correta_indice " +
                "FROM Pergunta " +
                "WHERE iddocente = ?";

        StringBuilder where = new StringBuilder(sql);


            return null; // perguntas
    }

    public String getDbPath() {
        return dbPath;
    }

    public void setVersaoDB(int versaoDB) {
        this.versaoDB = versaoDB;
    }

    public int getVersaoDB() {
        return versaoDB;
    }
}