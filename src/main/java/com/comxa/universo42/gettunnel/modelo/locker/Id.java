package com.comxa.universo42.gettunnel.modelo.locker;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.comxa.universo42.embaralhador.Embaralhador;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.UUID;

public class Id {
    public static final String FILE_TAG = "infile=";
    public static final String ID_FILE_NAME = "device_id";

    private String id;
    private Context context;

    public Id(Context context) {
        this.context = context;
    }

    public Id(Context context, String id) {
        this(context);
        setId(id);
    }

    public String getId() {
        return new String(Embaralhador.desembaralhar(this.id));
    }

    public void setId(String id) {
        this.id = Embaralhador.embaralhar(id);
    }

    public void setDeviceId() {
        setId(getDeviceID(this.context));
    }

    public boolean isDeviceId() {
        return getDeviceID(this.context).equals(new String(Embaralhador.desembaralhar(this.id)));
    }

    public void load() throws FileNotFoundException {
        File file = new File(this.context.getFilesDir(), ID_FILE_NAME);

        Scanner scanner = new Scanner(file);
        try {
            String id = scanner.nextLine();

            id = new String(Embaralhador.desembaralhar(id));

            if (id.contains(FILE_TAG)) {
                this.id = Embaralhador.embaralhar(id.replace(FILE_TAG, ""));
            } else {
                throw new IllegalStateException("File ID não reconhecida.");
            }
        } finally {
            scanner.close();
        }
    }

    public void save() throws FileNotFoundException {
        File file = new File(this.context.getFilesDir(), ID_FILE_NAME);

        PrintWriter pw = new PrintWriter(file);

        String id = new String(Embaralhador.desembaralhar(this.id));
        id = Embaralhador.embaralhar(FILE_TAG + id);

        pw.println(id);

        pw.close();
    }


    public static String getDeviceID(Context context) {
        return getAndroidID(context) + getBuildID();
    }

    private static String getAndroidID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private static String getBuildID() {
        String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10) + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);

        String serial = null;

        try {
            serial = Build.class.getField("SERIAL").get(null).toString();

            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        } catch (Exception exception) {
            serial = "serial";
        }

        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }
}
