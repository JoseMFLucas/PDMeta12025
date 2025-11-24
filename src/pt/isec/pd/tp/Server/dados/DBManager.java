package pt.isec.pd.tp.Server.dados;

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

    private static final Path DB_FOLDER_PATH = Paths.get("db");

    private static final Path FULL_DB_PATH = DB_FOLDER_PATH.resolve(DB_FILE_NAME).normalize();

    private static final String DB_URL = "jdbc:sqlite:" + FULL_DB_PATH.toAbsolutePath();

    private static Connection conn;

    public static Connection connectDB() {

        boolean dbExists = Files.exists(FULL_DB_PATH);

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
                        ");" +

                        "INSERT INTO Configuracao (codigoregisto, versao)" +
                        "    VALUES ('INIT', '0');"
                ;
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

    public static boolean login(/*String msg*/){
      /*  String[] dados = msg.split(" ", 2);

        if(dados.length != 2){
            return false;
        }

        String email = dados[0];
        String password = dados[1];

        if (checkLogin(conn, "Docente", email, password)) {
            userType = "DOCENTE";
        }

        else if (checkLogin(conn, "Estudante", email, password)) {
            userType = "ESTUDANTE";
        }

        if (userType == null) {
            System.out.println("Login falhou. Credenciais inválidas.");
        } else {
            System.out.println("Login bem-sucedido como: " + userType);
        }
    }

    public static boolean checkLogin(Connection conn, String userType, String email, String password){

        if (!userType.equals("Docente") && !userType.equals("Estudante")) {
            throw new IllegalArgumentException("Tipo de utilizador inválido.");
        }

        String sql = "SELECT * FROM " + userType + "WHERE email = " + email + " AND password = " + password;

        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

            if(rs.next()){
                return true;
            }
                return false;

        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }*/
        return false;
    }

    public static boolean registarEstudante(String msg){
        String[] dados = msg.split(" ", 4);

        if(dados.length != 4){
            return false;
        }

        String numero_estudante = dados[0];
        String nome = dados[1];
        String email = dados[2];
        String password = dados[3];

        String sql = "INSERT INTO Estudante (numero_estudante, nome, email, password) VALUES (?, ?, ?, ?)";

        /*try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, numero_estudante);
            pstmt.setString(2, nome);
            pstmt.setString(3, email);
            pstmt.setString(4, password);

            int linhasAfetadas = pstmt.executeUpdate();

            if(linhasAfetadas > 0){
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Erro ao adicionar Estudante: " + e.getMessage());
            return false;
        }*/
        return false;
    }

    public static boolean registarDocente(String msg){
        String[] dados = msg.split(" ", 4);

        if(dados.length != 4){
            return false;
        }

        String nome = dados[0];
        String email = dados[1];
        String password = dados[2];
        String codigo_unico = dados[3];

        String sql = "INSERT INTO Docente (nome, email, password) VALUES (?, ?, ?)";

        String sql_codigounico = "SELECT codigoregisto FROM Configuracao";
/*
        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql_codigounico)) {

            if(rs.next()){
                if(rs.getString("codigoregisto").equals(codigo_unico))
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                        pstmt.setString(1, nome);
                        pstmt.setString(2, email);
                        pstmt.setString(3, password);

                        int linhasAfetadas = pstmt.executeUpdate();

                        if(linhasAfetadas > 0){
                            return true;
                        }
                        return false;

                    } catch (SQLException e) {
                        System.err.println("Erro ao adicionar Docente: " + e.getMessage());
                        return false;
                    }
            }
            return false;

        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

             */
            return false;
    }

    public static boolean criaPergunta(String msg){

        return false;
    }

    public static boolean eliminarPergunta(String msg){

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

    private static boolean temRespostasAssociadas(int idpergunta) {
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
/*
        ArrayList<Object> parametros = new ArrayList<>();
        parametros.add(iddocente);

        switch (msg){
            case ATIVA:
                where.append(" AND data_hora_inicio <= ? AND data_hora_fim >= ?");
                parametros.add(datahoraatual);
                parametros.add(datahoraatual);
                break;
            case FUTURAS:
                where.append(" AND data_hora_inicio > ?");
                parametros.add(datahoraatual);
                parametros.add(datahoraatual);
                break;
            case EXPIRADAS:
                where.append(" data_hora_fim < ?");
                parametros.add(datahoraatual);
                break;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(whereClause.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                // O primeiro parâmetro é o id_docente
                if (i == 0) {
                    pstmt.setInt(i + 1, (Integer) parametros.get(i));
                } else {
                    // Os restantes são as Strings da data/hora - Depois converte para DATETIME
                    pstmt.setString(i + 1, (String) parametros.get(i));
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        // Array de resultados
                        String[] perguntaData = {
                                String.valueOf(rs.getLong("idpergunta")),
                                rs.getString("enunciado"),
                                rs.getString("codigo_acesso"),
                                rs.getString("data_hora_inicio"),
                                rs.getString("data_hora_fim"),
                                String.valueOf(rs.getInt("opcao_correta_indice"))
                        };
                        perguntas.add(perguntaData);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Erro SQL ao listar perguntas: " + e.getMessage());
            }

 */

            return null; // perguntas
    }
}