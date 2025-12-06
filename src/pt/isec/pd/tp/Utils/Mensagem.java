package pt.isec.pd.tp.Utils;

import java.io.Serial;
import java.io.Serializable;

public class Mensagem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum Tipo {
        // Cliente -> Servidor
        LOGIN,
        REGISTO_DOCENTE,
        REGISTO_ESTUDANTE,
        EDITAR_PERFIL_ESTUDANTE,
        EDITAR_PERFIL_DOCENTE,
        CRIAR_PERGUNTA,
        VISUALIZAR_PERGUNTA,
        EDITAR_PERGUNTA,
        ELIMINAR_PERGUNTA,
        LISTAR_PERGUNTAS_DOCENTE,
        VER_ESTATISTICAS,
        EXPORTAR_CSV,
        LISTAR_RESULTADOS_PERGUNTA,
        OBTER_PERGUNTA, // Submeter código da pergunta
        DETALHES_PERGUNTA_ESTUDANTE,
        SUBMETER_RESPOSTA,
        LISTAR_PERGUNTAS_RESPONDIDAS,
        LISTA_PERGUNTAS_RESPONDIDAS,
        PEDIR_LISTA_CLIENTES,
        LOGOUT,
        EXIT,

        // Servidor -> Cliente
        LOGIN_SUCESSO,
        LOGIN_FALHOU,
        REGISTO_SUCESSO,
        REGISTO_FALHOU,
        OPERACAO_SUCESSO,
        OPERACAO_FALHOU,
        LISTA_PERGUNTAS,
        LISTA_CLIENTES,
        DETALHES_PERGUNTA, // Para o estudante responder
        ESTATISTICAS_PERGUNTA,
        FICHEIRO_CSV,
        LISTA_RESPOSTAS,
        RESULTADOS_PERGUNTA, // Para o docente
        NOTIFICACAO_ASSINCRONA, // [cite: 143, 169]
        ERRO
    }

    private Tipo tipo;
    private Object payload; // Objeto que transporta os dados (ex: um objeto User, Pergunta, String, etc.)

    public Mensagem(Tipo tipo, Object payload) {
        this.tipo = tipo;
        this.payload = payload;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public Object getPayload() {
        return payload;
    }
}