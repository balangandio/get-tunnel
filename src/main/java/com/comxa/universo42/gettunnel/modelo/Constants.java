package com.comxa.universo42.gettunnel.modelo;

public class Constants {
    //Dado adicionado a fila para identificar fim de stream
    public static final byte[] END_OF_STREAM = new byte[0];

    //Headers
    public static final String DATA_HEADER = "X-Data: ";
    public static final String ACTION_HEADER = "X-Action: ";
    public static final String TARGET_HEADER = "X-Target: ";
    public static final String LENGTH_HEADER = "X-Len: ";
    public static final String ID_HEADER = "X-Id: ";
    public static final String PASS_HEADER = "X-Pass: ";
    public static final String BODY_HEADER = "X-Body: ";

    //Actions
    public static final String ACTION_CREATE = "create";
    public static final String ACTION_COMPLETE = "complete";
    public static final String ACTION_DATA = "data";
    public static final String ACTION_CLOSE = "close";

    //Status line string ao criar a conexão com o cliente
    public static final String MSG_CONNECTION_CREATED = "Created";
    //Status line string ao completar a conexão com o cliente
    public static final String MSG_CONNECTION_COMPLETED = "Completed";
    //Status line string ao fechar a conexão com o cliente
    public static final String MSG_CONNECTION_CLOSED = "Closed";
    //Status line string ao receber EOF por parte do target
    public static final String MSG_END_OF_STREAM = "EndOfStream";

    public class Client {
        //Tamanho da fila de dados do app
        public static final int TAM_SEND_QUEUE = 10;
        //Tamanho do buffer de recepção do app / tamanho máximo de um dado na fila
        public static final int TAM_RECEIVE_BUFFER_APP = 1024;

        //Tempo de espera pra colocar um dado quando a fila está cheia
        public static final int MAX_SECONDS_TIME_OUT_OFFER_SEND_QUEUE = 30;

        //Tamanho do buffer de recepção do conteúdo de uma server response
        public static final int TAM_BUFFER_SERVER_RESPONDE_CONTENT = 1024;
        //Tamanho máximo que a http head response enviada pode ter
        public static final int MAX_LEN_SERVER_RESPONSE_HEAD = 1024 * 1000;

        //Tamanho do buffer de recepção para a aplicação em base64
        public static final int LENGTH_RECEIVE_BUFFER_APP_BASE64 = ((4 * TAM_RECEIVE_BUFFER_APP / 3 + 3) & ~3 );

        //Tamanho máximo do texto em base64 enviado em uma requisição GET (pelo header X-Data)
        public static final int MAX_QTD_LENGTH_TO_SEND = 11000;

        //Quantidade máxima de bytes da aplicação que podem ser enviados por vez em uma requisição
        public static final int MAX_QTD_BYTES_TO_SEND = MAX_QTD_LENGTH_TO_SEND * 3 / 4;
    }
}