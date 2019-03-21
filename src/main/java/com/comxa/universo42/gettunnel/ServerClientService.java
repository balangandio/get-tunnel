package com.comxa.universo42.gettunnel;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.app.NotificationManager;
import android.support.v7.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import com.comxa.universo42.gettunnel.modelo.listener.ByteCounter;
import com.comxa.universo42.gettunnel.modelo.ClientServer;
import com.comxa.universo42.gettunnel.modelo.listener.LogBox;

public class ServerClientService extends Service implements ServiceControl {
    public static final int NOTINICATION_ID = 4242;

    private boolean isOnForegroud;
    private Controller controller = new Controller();
    private ClientServer server;
    private LogBox logBox;
    private ByteCounter counter;

    private NotificationManager notificationManager;
    private Builder notificationBuilder;

    @Override
    public void onDestroy() {
        if (server != null)
            server.close();
        if (notificationManager != null)
            notificationManager.cancel(NOTINICATION_ID);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return controller;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        if (isOnForegroud)
            return START_NOT_STICKY;

        isOnForegroud = true;

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.tunnel)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(pendingIntent);

        startForeground(NOTINICATION_ID, notificationBuilder.build());

        if (server != null)
            server.start();

        return START_NOT_STICKY;
    }


    public void setClientServer(String listeningAddr, int listeningPort, String target, String serverAddr, int serverPort) {
        this.server = new ClientServer(listeningAddr, listeningPort, target, serverAddr, serverPort) {
            @Override
            public void onLog(String str) {
                if (notificationBuilder != null) {
                    notificationBuilder.setContentText(str);
                    notificationManager.notify(NOTINICATION_ID, notificationBuilder.build());
                }
                if (logBox != null) {
                    logBox.addLog(str);
                }
            }
        };
        if (this.counter != null) {
            this.server.setByteCounter(this.counter);
        }
    }

    public ClientServer getClientServer() {
        return this.server;
    }

    public LogBox getLogBox() {
        return this.logBox;
    }

    public void setLogBox(LogBox logBox) {
        this.logBox = logBox;
    }

    @Override
    public ByteCounter getByteCounter() {
        return this.counter;
    }

    @Override
    public void setByteCounter(ByteCounter counter) {
        this.counter = counter;

        if (this.server != null) {
            this.server.setByteCounter(this.counter);
        }
    }

    public class Controller extends Binder {
        public ServiceControl getControl() {
            return ServerClientService.this;
        }
    }
}
