package com.comxa.universo42.gettunnel.modelo.locker;

import android.util.Log;

import com.comxa.universo42.embaralhador.Embaralhador;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class IdListGetter implements Runnable {

    private String url;
    private List<String> idList = new LinkedList<String>();
    private boolean isObtida;

    HttpURLConnection conn;
    private boolean running;

    public IdListGetter(String url) {
        this.url = url;
    }

    public List<String> getIdList() {
        return this.idList;
    }

    public boolean isObtida() {
        return this.isObtida;
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean contains(String id) {
        for (String idList : this.idList) {
            String idOnly = new String(Embaralhador.desembaralhar(idList));
            idOnly = idOnly.substring("autorizada=".length());
            idOnly = new String(Embaralhador.desembaralhar(idOnly));

            if (idOnly.equals(id))
                return true;
        }

        return false;
    }

    public void stop() {
        if (this.running) {
            this.running = false;
            this.conn.disconnect();
        }
    }

    @Override
    public void run() {
        try {
            this.conn = (HttpURLConnection)new URL(this.url).openConnection();
            conn.setRequestMethod("GET");

            this.running = true;
            int code = conn.getResponseCode();

            if (isRunning() && code == 200) {
                onLogReceived("<-> Response code == 200", null);
                Scanner scanner = new Scanner(conn.getInputStream());

                while (scanner.hasNextLine()) {
                    String strId = scanner.nextLine();
                    if (strId.length() > 0)
                        this.idList.add(strId);
                }

                this.isObtida = true;
                scanner.close();
            }else{
                onLogReceived("<!> Response code != 200: " + code, null);
            }

        } catch(IOException e) {
            if (this.running)
                onLogReceived("<#> Connetion error!", e);
        } finally {
            if (this.conn != null)
                this.conn.disconnect();
        }

        if (isRunning()) {
            this.running = false;
            onConnectionDone();
        }
    }

    protected void onLogReceived(String log, Exception e) {}

    protected void onConnectionDone() {}
}
