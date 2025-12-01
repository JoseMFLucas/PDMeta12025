package pt.isec.pd.tp.Server.dados;

import pt.isec.pd.tp.Client.Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import pt.isec.pd.tp.Utils.CodigoAcessoGenerator;

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

        Path dbFolder = providedPath;
        Path dbFilePath = dbFolder.resolve(DB_FILE_NAME);

        String dbUrl = "jdbc:sqlite:" + dbFilePath.toAbsolutePath();

        Connection connection = null;
        boolean dbExists = Files.exists(dbFilePath);

        try {
            if (!Files.exists(dbFolder)) {
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
                        "    numero_estudante INTEGER UNIQUE NOT NULL," +
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
                        "    idpergunta INTEGER NOT NULL," +
                        "    indice INTEGER NOT NULL," +
                        "    texto TEXT NOT NULL," +
                        "    FOREIGN KEY (idpergunta) REFERENCES Pergunta(idpergunta)," +
                        "    UNIQUE (idpergunta, indice)" +
                        ");" +

                        // Tabela Resposta
                        "CREATE TABLE Resposta (" +
                        "    idpergunta INTEGER NOT NULL," +
                        "    numero_estudante INTEGER NOT NULL," +
                        "    data_hora_realizacao DATETIME NOT NULL," +
                        "    opcao_escolhida_indice INTEGER NOT NULL," +
                        "    esta_certa BOOLEAN NOT NULL," +
                        "    PRIMARY KEY (idpergunta, numero_estudante)," +
                        "    FOREIGN KEY (idpergunta) REFERENCES Pergunta(idpergunta)" +
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

    public int getUserId(String email, String userType) {
        if (conn == null) {
            return -1;
        }
        String idColumn = userType.equals("DOCENTE") ? "iddocente" : "numero_estudante";
        String tableName = userType.equals("DOCENTE") ? "Docente" : "Estudante";
        String sql = "SELECT " + idColumn + " FROM " + tableName + " WHERE email = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao obter ID do utilizador: " + e.getMessage());
        }
        return -1;
    }

    public boolean registarEstudante(String[] msg) {
        if (conn == null || msg.length != 4) {
            return false;
        }

        String sql = "INSERT INTO Estudante (numero_estudante, nome, email, password) VALUES (?, ?, ?, ?)";
        boolean originalAutoCommit = false;

        try {
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, Integer.parseInt(msg[0]));
                pstmt.setString(2, msg[1]);
                pstmt.setString(3, msg[2]);
                pstmt.setString(4, msg[3]);

                int linhasAfetadas = pstmt.executeUpdate();

                if (linhasAfetadas > 0) {
                    conn.commit();
                    return true;
                }
            }
            conn.rollback();
            return false;
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


    public String[] criaPergunta(String[] msg){
        if (conn == null || msg.length != 6) {
            return null;
        }

        String sqlAddPergunta = "INSERT INTO Pergunta (iddocente, enunciado, codigo_acesso, data_hora_inicio, data_hora_fim, opcao_correta_indice) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlAddOpcao = "INSERT INTO Opcao (idpergunta, indice, texto) VALUES (?, ?, ?)";

        boolean originalAutoCommit = false;
        int idpergunta = -1;

        try {
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false); // Inicia a transação

            // 1. Inserir a pergunta
            try (PreparedStatement pstmt = conn.prepareStatement(sqlAddPergunta)) {
                pstmt.setInt(1, Integer.parseInt(msg[0]));
                pstmt.setString(2, msg[1]);
                pstmt.setString(3, CodigoAcessoGenerator.gerarCodigo());
                pstmt.setString(4, msg[3]);
                pstmt.setString(5, msg[4]);
                pstmt.setInt(6, Integer.parseInt(msg[5]));

                int linhasAfetadas = pstmt.executeUpdate();
                if (linhasAfetadas == 0) {
                    throw new SQLException("Falha ao inserir a pergunta, nenhuma linha afetada.");
                }
            }

            // 2. Obter o ID da pergunta gerado (forma correta para SQLite)
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    idpergunta = rs.getInt(1);
                } else {
                    throw new SQLException("Falha ao obter o ID da pergunta (last_insert_rowid).");
                }
            }

            // 3. Inserir as opções
            String opcoesString = msg[2].substring(1, msg[2].length() - 1);
            String[] opcoes = opcoesString.split(", ");

            try (PreparedStatement pstmtOpcao = conn.prepareStatement(sqlAddOpcao)) {
                for (int i = 0; i < opcoes.length; i++) {
                    pstmtOpcao.setInt(1, idpergunta);
                    pstmtOpcao.setInt(2, i + 1);
                    pstmtOpcao.setString(3, opcoes[i]);
                    pstmtOpcao.addBatch();
                }
                pstmtOpcao.executeBatch();
            }

            conn.commit(); // Confirma a transação

            // 4. Retorna os detalhes da pergunta recém-criada
            return visualizarPergunta(idpergunta);

        } catch (SQLException | NumberFormatException e) {
            System.err.println("Erro ao criar pergunta: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Erro ao realizar rollback: " + ex.getMessage());
            }
            return null;
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                System.err.println("Erro ao restaurar auto-commit: " + e.getMessage());
            }
        }
    }

    public String[] visualizarPergunta(int idpergunta) {
        if (conn == null) {
            return null;
        }

        String sqlPergunta = "SELECT iddocente, enunciado, codigo_acesso, data_hora_inicio, data_hora_fim, opcao_correta_indice FROM Pergunta WHERE idpergunta = ?";
        String sqlOpcoes = "SELECT texto FROM Opcao WHERE idpergunta = ? ORDER BY indice ASC";

        try {
            String idDocente, enunciado, codigoAcesso, dataInicio, dataFim, opcaoCorreta;
            List<String> opcoesList = new ArrayList<>();

            // 1. Obter dados da pergunta
            try (PreparedStatement pstmt = conn.prepareStatement(sqlPergunta)) {
                pstmt.setInt(1, idpergunta);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        idDocente = String.valueOf(rs.getInt("iddocente"));
                        enunciado = rs.getString("enunciado");
                        codigoAcesso = rs.getString("codigo_acesso");
                        dataInicio = rs.getString("data_hora_inicio");
                        dataFim = rs.getString("data_hora_fim");
                        opcaoCorreta = String.valueOf(rs.getInt("opcao_correta_indice"));
                    } else {
                        return null; // Pergunta não encontrada
                    }
                }
            }

            // 2. Obter as opções
            try (PreparedStatement pstmt = conn.prepareStatement(sqlOpcoes)) {
                pstmt.setInt(1, idpergunta);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        opcoesList.add(rs.getString("texto"));
                    }
                }
            }

            // 3. Montar o array de strings no formato desejado
            String opcoesString = Arrays.toString(opcoesList.toArray(new String[0]));

            return new String[]{String.valueOf(idpergunta), idDocente, enunciado, opcoesString, dataInicio, dataFim, opcaoCorreta, codigoAcesso};

        } catch (SQLException e) {
            System.err.println("Erro ao visualizar pergunta: " + e.getMessage());
            return null;
        }
    }

    public boolean editarPergunta(String[] dadosEdicao) {
        if (conn == null || dadosEdicao == null || dadosEdicao.length != 7) {
            return false;
        }

        boolean originalAutoCommit = false;
        try {
            int idPergunta = Integer.parseInt(dadosEdicao[0]);
            String idDocente = dadosEdicao[1]; // Não usado para update, mas vem no payload
            String enunciado = dadosEdicao[2];
            String opcoesString = dadosEdicao[3];
            String dataInicio = dadosEdicao[4];
            String dataFim = dadosEdicao[5];
            int opcaoCorreta = Integer.parseInt(dadosEdicao[6]);

            // Regra de negócio: não editar se já houver respostas
            if (temRespostasAssociadas(idPergunta)) {
                System.err.println("Tentativa de editar pergunta com respostas associadas.");
                return false;
            }

            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false); // Inicia a transação

            // 1. Atualizar a tabela Pergunta
            String sqlUpdatePergunta = "UPDATE Pergunta SET enunciado = ?, data_hora_inicio = ?, data_hora_fim = ?, opcao_correta_indice = ? WHERE idpergunta = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdatePergunta)) {
                pstmt.setString(1, enunciado);
                pstmt.setString(2, dataInicio);
                pstmt.setString(3, dataFim);
                pstmt.setInt(4, opcaoCorreta);
                pstmt.setInt(5, idPergunta);
                pstmt.executeUpdate();
            }

            // 2. Apagar as opções antigas
            String sqlDeleteOpcoes = "DELETE FROM Opcao WHERE idpergunta = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDeleteOpcoes)) {
                pstmt.setInt(1, idPergunta);
                pstmt.executeUpdate();
            }

            // 3. Inserir as novas opções
            String[] novasOpcoes = opcoesString.substring(1, opcoesString.length() - 1).split(", ");
            String sqlInsertOpcao = "INSERT INTO Opcao (idpergunta, indice, texto) VALUES (?, ?, ?)";
            try (PreparedStatement pstmtOpcao = conn.prepareStatement(sqlInsertOpcao)) {
                for (int i = 0; i < novasOpcoes.length; i++) {
                    pstmtOpcao.setInt(1, idPergunta);
                    pstmtOpcao.setInt(2, i + 1);
                    pstmtOpcao.setString(3, novasOpcoes[i]);
                    pstmtOpcao.addBatch();
                }
                pstmtOpcao.executeBatch();
            }

            conn.commit(); // Confirma a transação
            return true;

        } catch (SQLException | NumberFormatException e) {
            System.err.println("Erro ao editar pergunta: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Erro ao realizar rollback: " + ex.getMessage());
            }
            return false;
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                System.err.println("Erro ao restaurar auto-commit: " + e.getMessage());
            }
        }
    }

    public boolean editarPerfilDocente(String[] dadosEdicao) {
        if (conn == null || dadosEdicao == null || dadosEdicao.length != 4) {
            return false;
        }

        int idDocente = Integer.parseInt(dadosEdicao[0]);
        String campo = dadosEdicao[1];
        String novoValor = dadosEdicao[2];
        String codigoRegisto = dadosEdicao[3];

        String sqlCheckCode = "SELECT codigoregisto FROM Configuracao WHERE codigoregisto = ?";
        String sqlUpdate = "UPDATE Docente SET " + campo + " = ? WHERE iddocente = ?";

        try {
            // 1. Verificar o código de registo
            try (PreparedStatement pstmt = conn.prepareStatement(sqlCheckCode)) {
                pstmt.setString(1, codigoRegisto);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        // Código de registo inválido
                        System.err.println("Código de registo de docente inválido.");
                        return false;
                    }
                }
            }

            // 2. Se o código for válido, atualizar o perfil
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                pstmt.setString(1, novoValor);
                pstmt.setInt(2, idDocente);
                int linhasAfetadas = pstmt.executeUpdate();
                return linhasAfetadas > 0;
            }

        } catch (SQLException | NumberFormatException e) {
            System.err.println("Erro ao editar perfil do docente: " + e.getMessage());
            return false;
        }
    }


    public boolean eliminarPergunta(int idpergunta){

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

    public List<String[]> listarPerguntasDocente(int id_docente, String filtro) {
        if (conn == null) {
            return null;
        }

        List<String[]> perguntas = new ArrayList<>();
        String sql = "SELECT idpergunta, enunciado, codigo_acesso, data_hora_inicio, data_hora_fim FROM Pergunta WHERE iddocente = ?";

        String agora = "datetime('now', 'localtime')";

        switch (filtro.toLowerCase()) {
            case "ativas":
                sql += " AND datetime(replace(data_hora_inicio, '/', '-')) <= " + agora + " AND datetime(replace(data_hora_fim, '/', '-')) >= " + agora;
                break;
            case "futuras":
                sql += " AND datetime(replace(data_hora_inicio, '/', '-')) > " + agora;
                break;
            case "expiradas":
                sql += " AND datetime(replace(data_hora_fim, '/', '-')) < " + agora;
                break;
            case "todas":
                break;
            default:
                return null; // Filtro inválido
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id_docente);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int idPergunta = rs.getInt("idpergunta");
                    String[] pergunta;

                    if (filtro.equalsIgnoreCase("expiradas")) {
                        String sqlStats = "SELECT COUNT(*) as total_respostas, SUM(CASE WHEN esta_certa = 1 THEN 1 ELSE 0 END) as respostas_certas FROM Resposta WHERE idpergunta = ?";
                        try (PreparedStatement pstmtStats = conn.prepareStatement(sqlStats)) {
                            pstmtStats.setInt(1, idPergunta);
                            try (ResultSet rsStats = pstmtStats.executeQuery()) {
                                if (rsStats.next()) {
                                    int totalRespostas = rsStats.getInt("total_respostas");
                                    int respostasCertas = rsStats.getInt("respostas_certas");
                                    double percentagem = (totalRespostas > 0) ? ((double) respostasCertas / totalRespostas) * 100 : 0;
                                    pergunta = new String[]{
                                            String.valueOf(idPergunta),
                                            rs.getString("enunciado"),
                                            rs.getString("codigo_acesso"),
                                            rs.getString("data_hora_inicio"),
                                            rs.getString("data_hora_fim"),
                                            String.valueOf(totalRespostas),
                                            String.format("%.2f%%", percentagem)
                                    };
                                } else {
                                    pergunta = new String[]{
                                            String.valueOf(idPergunta),
                                            rs.getString("enunciado"),
                                            rs.getString("codigo_acesso"),
                                            rs.getString("data_hora_inicio"),
                                            rs.getString("data_hora_fim"),
                                            "0",
                                            "0.00%"
                                    };
                                }
                            }
                        }
                    } else {
                        pergunta = new String[]{
                                String.valueOf(idPergunta),
                                rs.getString("enunciado"),
                                rs.getString("codigo_acesso"),
                                rs.getString("data_hora_inicio"),
                                rs.getString("data_hora_fim")
                        };
                    }
                    perguntas.add(pergunta);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar perguntas do docente: " + e.getMessage());
            return null;
        }

        return perguntas;
    }

    public List<String[]> getEstatisticasPerguntaExpirada(int id_pergunta) {
        if (conn == null) {
            return null;
        }

        List<String[]> resultado = new ArrayList<>();
        String sqlPergunta = "SELECT p.enunciado, p.data_hora_fim, p.opcao_correta_indice, GROUP_CONCAT(o.texto, ';') as opcoes " +
                "FROM Pergunta p JOIN Opcao o ON p.idpergunta = o.idpergunta " +
                "WHERE p.idpergunta = ? " +
                "GROUP BY p.idpergunta";

        String sqlRespostas = "SELECT e.numero_estudante, e.nome, e.email, r.opcao_escolhida_indice, r.data_hora_realizacao, o.texto as resposta_texto " +
                "FROM Resposta r " +
                "JOIN Estudante e ON r.numero_estudante = e.numero_estudante " +
                "JOIN Opcao o ON r.idpergunta = o.idpergunta AND r.opcao_escolhida_indice = o.indice " +
                "WHERE r.idpergunta = ?";

        try {
            // 1. Obter detalhes da pergunta
            try (PreparedStatement pstmt = conn.prepareStatement(sqlPergunta)) {
                pstmt.setInt(1, id_pergunta);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String[] detalhesPergunta = {
                                rs.getString("enunciado"),
                                rs.getString("data_hora_fim"),
                                rs.getString("opcoes"),
                                String.valueOf(rs.getInt("opcao_correta_indice"))
                        };
                        resultado.add(detalhesPergunta);
                    } else {
                        return null; // Pergunta não encontrada
                    }
                }
            }

            // 2. Obter respostas dos estudantes e calcular estatísticas
            int totalRespostas = 0;
            int respostasCertas = 0;
            List<String[]> respostasAlunos = new ArrayList<>();

            try (PreparedStatement pstmt = conn.prepareStatement(sqlRespostas)) {
                pstmt.setInt(1, id_pergunta);
                try (ResultSet rs = pstmt.executeQuery()) {
                    int opcaoCorreta = Integer.parseInt(resultado.get(0)[3]);
                    while (rs.next()) {
                        totalRespostas++;
                        int opcaoEscolhida = rs.getInt("opcao_escolhida_indice");
                        if (opcaoEscolhida == opcaoCorreta) {
                            respostasCertas++;
                        }
                        String[] respostaAluno = {
                                String.valueOf(rs.getInt("numero_estudante")),
                                rs.getString("nome"),
                                rs.getString("email"),
                                rs.getString("resposta_texto"),
                                rs.getString("data_hora_realizacao")
                        };
                        respostasAlunos.add(respostaAluno);
                    }
                }
            }

            // 3. Adicionar estatísticas
            double percentagemCertas = (totalRespostas > 0) ? ((double) respostasCertas / totalRespostas) * 100 : 0;
            resultado.add(new String[]{String.format("%.2f%%", percentagemCertas)});

            // 4. Adicionar respostas dos alunos
            resultado.addAll(respostasAlunos);

        } catch (SQLException e) {
            System.err.println("Erro ao obter estatísticas da pergunta: " + e.getMessage());
            return null;
        }

        return resultado;
    }


    public List<String[]> listarPerguntasRespondidas(int idEstudante) {
        if (conn == null) {
            return null;
        }

        List<String[]> perguntasRespondidas = new ArrayList<>();
        String sql = "SELECT p.enunciado, r.data_hora_realizacao, r.opcao_escolhida_indice, r.esta_certa " +
                     "FROM Resposta r " +
                     "JOIN Pergunta p ON r.idpergunta = p.idpergunta " +
                     "WHERE r.numero_estudante = ? " +
                     "ORDER BY r.data_hora_realizacao DESC";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idEstudante);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String[] resposta = {
                            rs.getString("enunciado"),
                            rs.getString("data_hora_realizacao"),
                            String.valueOf(rs.getInt("opcao_escolhida_indice")),
                            rs.getBoolean("esta_certa") ? "Correta" : "Incorreta"
                    };
                    perguntasRespondidas.add(resposta);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar perguntas respondidas: " + e.getMessage());
            return null;
        }

        return perguntasRespondidas;
    }

    public String[] obterPerguntaPorCodigo(String codigo) {
        if (conn == null) return null;

        String sql = "SELECT idpergunta, enunciado FROM Pergunta WHERE codigo_acesso = ? AND datetime(replace(data_hora_inicio, '/', '-')) <= datetime('now', 'localtime') AND datetime(replace(data_hora_fim, '/', '-')) >= datetime('now', 'localtime')";
        String sqlOpcoes = "SELECT texto FROM Opcao WHERE idpergunta = ? ORDER BY indice ASC";

        try {
            int idPergunta;
            String enunciado;
            List<String> opcoesList = new ArrayList<>();

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, codigo);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        idPergunta = rs.getInt("idpergunta");
                        enunciado = rs.getString("enunciado");
                    } else {
                        return null; // Nenhuma pergunta ativa encontrada com este código
                    }
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlOpcoes)) {
                pstmt.setInt(1, idPergunta);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        opcoesList.add(rs.getString("texto"));
                    }
                }
            }

            String opcoesString = Arrays.toString(opcoesList.toArray(new String[0]));
            return new String[]{String.valueOf(idPergunta), enunciado, opcoesString};

        } catch (SQLException e) {
            System.err.println("Erro ao obter pergunta por código: " + e.getMessage());
            return null;
        }
    }

    public boolean submeterResposta(int idEstudante, int idPergunta, int opcaoEscolhida) {
        if (conn == null) return false;

        String sqlCheckRespostaExistente = "SELECT COUNT(*) FROM Resposta WHERE idpergunta = ? AND numero_estudante = ?";
        String sqlGetPerguntaStatus = "SELECT opcao_correta_indice, data_hora_inicio, data_hora_fim FROM Pergunta WHERE idpergunta = ?";
        String sqlInsertResposta = "INSERT INTO Resposta (idpergunta, numero_estudante, data_hora_realizacao, opcao_escolhida_indice, esta_certa) VALUES (?, ?, ?, ?, ?)";

        try {
            // 1. Verificar se o estudante já respondeu a esta pergunta
            try (PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheckRespostaExistente)) {
                pstmtCheck.setInt(1, idPergunta);
                pstmtCheck.setInt(2, idEstudante);
                try (ResultSet rsCheck = pstmtCheck.executeQuery()) {
                    if (rsCheck.next() && rsCheck.getInt(1) > 0) {
                        System.err.println("Erro: Estudante já respondeu a esta pergunta.");
                        return false; // Estudante já respondeu
                    }
                }
            }

            // 2. Obter detalhes da pergunta e verificar se está ativa
            int opcaoCorreta;
            LocalDateTime dataHoraInicio;
            LocalDateTime dataHoraFim;
            try (PreparedStatement pstmt = conn.prepareStatement(sqlGetPerguntaStatus)) {
                pstmt.setInt(1, idPergunta);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        opcaoCorreta = rs.getInt("opcao_correta_indice");
                        dataHoraInicio = LocalDateTime.parse(rs.getString("data_hora_inicio"), DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
                        dataHoraFim = LocalDateTime.parse(rs.getString("data_hora_fim"), DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
                    } else {
                        System.err.println("Erro: Pergunta não existe.");
                        return false; // Pergunta não existe
                    }
                }
            }

            LocalDateTime agora = LocalDateTime.now();
            if (agora.isBefore(dataHoraInicio) || agora.isAfter(dataHoraFim)) {
                System.err.println("Erro: A pergunta não está ativa no momento.");
                return false; // Pergunta não está ativa
            }

            // 3. Inserir a resposta
            boolean estaCerta = (opcaoEscolhida == opcaoCorreta);
            String dataHoraAtual = agora.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertResposta)) {
                pstmt.setInt(1, idPergunta);
                pstmt.setInt(2, idEstudante);
                pstmt.setString(3, dataHoraAtual);
                pstmt.setInt(4, opcaoEscolhida);
                pstmt.setBoolean(5, estaCerta);

                int linhasAfetadas = pstmt.executeUpdate();
                return linhasAfetadas > 0;
            }

        } catch (SQLException e) {
            System.err.println("Erro ao submeter resposta: " + e.getMessage());
            return false;
        }
    }

    public boolean editarPerfilEstudante(String[] dadosEdicao) {
        if (conn == null || dadosEdicao == null || dadosEdicao.length != 3) {
            return false;
        }

        int idEstudante = Integer.parseInt(dadosEdicao[0]);
        String campo = dadosEdicao[1];
        String novoValor = dadosEdicao[2];

        String sqlUpdate = "UPDATE Estudante SET " + campo + " = ? WHERE numero_estudante = ?";

        try {
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                if (campo.equals("numero_estudante")) {
                    pstmt.setInt(1, Integer.parseInt(novoValor));
                } else {
                    pstmt.setString(1, novoValor);
                }
                pstmt.setInt(2, idEstudante);
                int linhasAfetadas = pstmt.executeUpdate();
                return linhasAfetadas > 0;
            }
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Erro ao editar perfil do estudante: " + e.getMessage());
            return false;
        }
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