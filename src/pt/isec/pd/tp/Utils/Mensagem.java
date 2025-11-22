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
        CRIAR_PERGUNTA,
        EDITAR_PERGUNTA,
        ELIMINAR_PERGUNTA,
        LISTAR_PERGUNTAS_DOCENTE,
        LISTAR_RESULTADOS_PERGUNTA,
        EXPORTAR_CSV,
        ENTRAR_PERGUNTA_ESTUDANTE, // Submeter código da pergunta
        SUBMETER_RESPOSTA,
        LISTAR_PERGUNTAS_RESPONDIDAS,
        LOGOUT,

        // Servidor -> Cliente
        LOGIN_SUCESSO,
        LOGIN_FALHOU,
        REGISTO_SUCESSO,
        REGISTO_FALHOU,
        OPERACAO_SUCESSO,
        OPERACAO_FALHOU,
        LISTA_PERGUNTAS,
        DETALHES_PERGUNTA, // Para o estudante responder
        LISTA_RESPOSTAS,
        RESULTADOS_PERGUNTA, // Para o docente
        FICHEIRO_CSV,
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