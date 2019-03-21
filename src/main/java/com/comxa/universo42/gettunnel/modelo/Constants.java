package com.comxa.universo42.gettunnel.modelo;

public class Constants {
    //Headers
    public static final String ACTION_HEADER = "X-Action: ";
    public static final String TARGET_HEADER = "X-Target: ";
    public static final String ID_HEADER = "X-Id: ";
    public static final String PASS_HEADER = "X-Pass: ";
    public static final String BODY_HEADER = "X-Body: ";
    public static final String CONTENT_HEADER = "Content-Length: ";

    //Actions
    public static final String ACTION_CREATE = "create";
    public static final String ACTION_COMPLETE = "complete";
    public static final String ACTION_DATA = "data";

    //Status line string ao criar a conexão com o cliente
    public static final String MSG_CONNECTION_CREATED = "Created";
    //Status line string ao completar a conexão com o cliente
    public static final String MSG_CONNECTION_COMPLETED = "Completed";

    public class Client {
        //Tamanho do buffer de recepção do app / tamanho máximo de um dado na fila
        public static final int TAM_RECEIVE_BUFFER_APP = 4096;

        //Tamanho do buffer de recepção do conteúdo de uma server response
        public static final int TAM_BUFFER_SERVER_RESPONSE_CONTENT = 4096;

        //Tamanho máximo que a http head response enviada pode ter
        public static final int MAX_LEN_SERVER_RESPONSE_HEAD = 1024 * 1000;
    }
}
