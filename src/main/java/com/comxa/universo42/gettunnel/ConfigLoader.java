package com.comxa.universo42.gettunnel;

import com.comxa.universo42.embaralhador.Embaralhador;
import com.comxa.universo42.embaralhador.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class ConfigLoader {
    private static final String KEY_EDITABLE = "editable";
    private static final String KEY_HOST_HEADER = "hostHeader";
    private static final String KEY_TARGET = "target";
    private static final String KEY_SERVER = "server";
    private static final String KEY_PASS = "pass";
    private static final String KEY_BODY = "body";
    private static final String KEY_LOCAL_PORT = "localPort";

    public static final String DEFAULT_HOST_HEADER = "";
    public static final String DEFAULT_SERVER_ADDR = "";
    public static final String DEFAULT_TARGET_ADDR = "";
    public static final String DEFAULT_PASS = "";
    public static final String DEFAULT_BODY_INJECT = "";
    public static final int DEFAULT_LOCAL_PORT = 4242;

    private ConfigPref pref;
    private String file;
    private int localPort = DEFAULT_LOCAL_PORT;
    private String hostHeader = DEFAULT_HOST_HEADER;
    private String serverAddr = DEFAULT_SERVER_ADDR;
    private int serverPort;
    private String targetAddr = DEFAULT_TARGET_ADDR;
    private int targetPort;
    private String pass = DEFAULT_PASS;
    private String bodyInject = DEFAULT_BODY_INJECT;
    private boolean isEditable;

    public ConfigLoader(String file) {
        this.file = file;
        this.isEditable = true;
    }

    public ConfigLoader(ConfigPref pref) {
        this.pref = pref;
        this.isEditable = true;
    }

    public boolean setConfig(String externFile) {
        try {
            loadFile(new File(externFile));
            save();
            return true;
        } catch(Throwable e) {
            return false;
        }
    }

    public boolean isEditable() {
        return this.isEditable;
    }

    public void setEditable(boolean isEditable) {
        this.isEditable = isEditable;
    }

    public void setEditable(String isEditable) {
        this.isEditable = "true".equals(isEditable);
    }

    public String getBodyInject() {
        return bodyInject;
    }

    public void setBodyInject(String bodyInject) {
        this.bodyInject = bodyInject;
    }

    public int getLocalPort() {
        return this.localPort;
    }

    public boolean setLocalPort(int localPort) {
        if (localPort < 0 || localPort > 65535)
            return false;

        this.localPort = localPort;
        return true;
    }

    public boolean setLocalPort(String str) {
        try {
            int localPort = Integer.parseInt(str);

            return setLocalPort(localPort);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String getServer() {
        if (serverPort == 0 || serverAddr == null || serverAddr.length() == 0)
            return "";

        return serverAddr + ":" + serverPort;
    }

    public boolean setServer(String server) {
        if (server == null || server.length() < 3)
            return false;

        int aux = server.indexOf(':');

        if (aux == -1 || aux == server.length()-1 || aux == 0)
            return false;

        try {
            this.serverPort = Integer.parseInt(server.substring(aux+1));
        } catch(NumberFormatException e) {
            return false;
        }

        this.serverAddr = server.substring(0, aux);
        return true;
    }

    public String getServerAddr() {
        return this.serverAddr;
    }

    public int getServerPort() {
        return this.serverPort;
    }

    public String getTarget() {
        if (targetPort == 0 || targetAddr == null || targetAddr.length() == 0)
            return "";

        return targetAddr + ":" + targetPort;
    }

    public boolean setTarget(String target) {
        if (target == null || target.length() < 3)
            return false;

        int aux = target.indexOf(':');

        if (aux == -1 || aux == target.length()-1 || aux == 0)
            return false;

        try {
            this.targetPort = Integer.parseInt(target.substring(aux+1));
        } catch(NumberFormatException e) {
            return false;
        }

        this.targetAddr = target.substring(0, aux);
        return true;
    }

    public String getHostHeader() {
        return this.hostHeader;
    }

    public void setHostHeader(String hostHeader) {
        this.hostHeader = hostHeader;
    }

    public String getPass() {
        return this.pass;
    }

    public String getPassDesembaralhada() {
        if (getPass() == null || getPass().isEmpty()) {
            return null;
        }
        return new String(Base64.decode(getPass()));
    }

    public void setPass(String pass) {
        if (pass != null) {
            if (pass.isEmpty()) {
                this.pass = null;
            } else {
                this.pass = Base64.encode(pass);
            }
        }
    }


    public boolean reset() {
        this.isEditable = true;
        this.localPort = DEFAULT_LOCAL_PORT;
        this.hostHeader = DEFAULT_HOST_HEADER;
        this.pass = DEFAULT_PASS;
        this.serverAddr = DEFAULT_SERVER_ADDR;
        this.serverPort = 0;
        this.targetPort = 0;

        try {
            save();
            return true;
        } catch(Throwable e) {
            return false;
        }
    }

    public boolean exportToFile(File f, boolean editable) {
        boolean editSave = this.isEditable;

        try {
            this.isEditable = editable;
            saveFile(f);
            return true;
        } catch(Throwable e) {
            e.printStackTrace();
            return false;
        } finally {
            this.isEditable = editSave;
        }
    }

    public void save() throws IOException {
        if (pref != null) {
            savePref(this.pref);
        } else {
            saveFile(new File(this.file));
        }
    }

    private void saveFile(File f) throws IOException {
        StringBuilder builder = new StringBuilder();

        builder.append(this.isEditable);
        builder.append("\n");
        builder.append(this.localPort);
        builder.append("\n");
        builder.append(getServer());
        builder.append("\n");
        builder.append(getTarget());
        builder.append("\n");
        builder.append(this.hostHeader);
        builder.append("\n");
        builder.append(this.pass);
        builder.append("\n");
        builder.append(this.bodyInject.length() > 0 ? Embaralhador.embaralhar(this.bodyInject)
                                                : this.bodyInject);
        builder.append("\n");

        FileOutputStream file = new FileOutputStream(f);
        file.write(Embaralhador.embaralhar(builder.toString()).getBytes());
        file.close();
    }

    private void savePref(ConfigPref pref) {
        pref.putString(KEY_BODY, getBodyInject());
        pref.putString(KEY_HOST_HEADER, getHostHeader());
        pref.putString(KEY_SERVER, getServer());
        pref.putString(KEY_TARGET, getTarget());
        pref.putInt(KEY_LOCAL_PORT, getLocalPort());
        pref.putString(KEY_PASS, getPass());
        pref.putBoolean(KEY_EDITABLE, isEditable());
        pref.save();
    }

    public boolean load() throws IOException {
        if (this.pref != null) {
            loadPref(this.pref);
            return true;
        }
        return loadFile(new File(this.file));
    }

    private boolean loadFile(File f) throws IOException {
        boolean ret = false;

        if (f.exists()) {
            byte[] bytes = getFileBytes(f, 1024 * 1000);
            Scanner scanner = new Scanner(new String(Embaralhador.desembaralhar(bytes)));

            if (scanner.hasNextLine()) {
                setEditable(scanner.nextLine());

                if (scanner.hasNextLine()) {
                    if (setLocalPort(scanner.nextLine())) {

                        if (scanner.hasNextLine()) {
                            if (setServer(scanner.nextLine())) {

                                if (scanner.hasNextLine()) {
                                    if (setTarget(scanner.nextLine())) {

                                        if (scanner.hasNextLine()) {
                                            this.hostHeader = scanner.nextLine();

                                            if (scanner.hasNextLine()) {
                                                this.pass = scanner.nextLine();

                                                if (scanner.hasNextLine()) {
                                                    String str = scanner.nextLine();
                                                    if (str != null && str.length() > 0) {
                                                        this.bodyInject = new String(Embaralhador.desembaralhar(str.getBytes()));
                                                    }
                                                    ret = true;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            scanner.close();
        }

        return ret;
    }

    private void loadPref(ConfigPref pref) {
        setEditable(pref.getBoolean(KEY_EDITABLE, true));
        setLocalPort(pref.getInt(KEY_LOCAL_PORT, DEFAULT_LOCAL_PORT));
        setHostHeader(pref.getString(KEY_HOST_HEADER, DEFAULT_HOST_HEADER));
        this.pass = pref.getString(KEY_PASS, DEFAULT_PASS);
        setBodyInject(pref.getString(KEY_BODY, DEFAULT_BODY_INJECT));
        setTarget(pref.getString(KEY_TARGET, DEFAULT_TARGET_ADDR));
        setServer(pref.getString(KEY_SERVER, DEFAULT_SERVER_ADDR));
    }

    private byte[] getFileBytes(File file, int maxBytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream in = new FileInputStream(file);

        try {
            byte[] buffer = new byte[1024];
            int len, count = 0;
            while ((len = in.read(buffer)) != -1) {
                count += len;

                if (count >= maxBytes) {
                    break;
                }

                baos.write(buffer, 0, len);
            }
        } finally {
            in.close();
        }

        return baos.toByteArray();
    }
}
