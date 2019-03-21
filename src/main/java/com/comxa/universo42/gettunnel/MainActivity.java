package com.comxa.universo42.gettunnel;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import com.comxa.universo42.gettunnel.modelo.listener.ByteCounter;
import com.comxa.universo42.gettunnel.modelo.ClientServer;
import com.comxa.universo42.gettunnel.modelo.listener.CounterListener;
import com.comxa.universo42.gettunnel.modelo.listener.LogBox;
import com.comxa.universo42.gettunnel.modelo.listener.LogListener;

public class MainActivity extends AppCompatActivity implements ServiceConnection, CounterListener, LogListener {
    public static final int FILE_EXPLORER_REQUEST_CODE = 42;
    public static final int MILLISECONDS_BYTE_COUNTER = 1000;
    public static final int MILLISECONDS_LOG_REFRESH = 500;
    public static final int LOG_BOX_SIZE = 50;
    public static final String LISTENING_ADDR = "127.0.0.1";

    private EditText txtServer;
    private EditText txtTarget;
    private EditText txtLocalPort;
    private Button btnRun;
    private Button btnHost;
    private Button btnPass;
    private TextView txtViewUp;
    private TextView txtViewDown;
    private TextView txtViewLog;

    private ConfigLoader config;
    private boolean needUnbind;
    private ServiceControl serviceControl;
    private ClientServer server;
    private boolean menuEnable = true;
    private boolean menuExportEnable = true;
    private LogBox logBox;
    private ByteCounter counter;
    private DrawerLayout drawerLayout;

    private DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
    {
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        formatter.setDecimalFormatSymbols(symbols);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        txtServer = (EditText) findViewById(R.id.editTxtServer);
        txtTarget = (EditText) findViewById(R.id.editTxtTarget);
        txtLocalPort = (EditText) findViewById(R.id.editTxtLocalPort);
        btnRun = (Button) findViewById(R.id.btnRun);
        btnHost = (Button) findViewById(R.id.btnHost);
        btnPass = (Button) findViewById(R.id.btnPass);
        txtViewUp = (TextView) findViewById(R.id.txtViewUp);
        txtViewDown = (TextView) findViewById(R.id.txtViewDown);
        txtViewLog = (TextView) findViewById(R.id.txtViewLog);

        config = new ConfigLoader(new ConfigPref(getPreferences(MODE_PRIVATE)));
        loadConfig();
        menuExportEnable = config.isEditable();

    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, ServerClientService.class), this, 0);
        needUnbind = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLogBox();
        stopByteCounter();
        unbindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveConfig();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_log:
                if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                    drawerLayout.closeDrawer(Gravity.LEFT);
                } else {
                    drawerLayout.openDrawer(Gravity.LEFT);
                }
                break;
            case R.id.menu_import:
                Intent intent = new Intent(this, FileExplorerActivity.class);
                startActivityForResult(intent, FILE_EXPLORER_REQUEST_CODE);
                break;
            case R.id.menu_export:
                showExportDialog();
                break;
            case R.id.menu_clear_config:
                showClearConfigDialog();
                break;
            case R.id.menu_clear_log:
                if (logBox != null) {
                    logBox.clearLog();
                }
                txtViewLog.setText("");
                break;
            case R.id.menu_about:
                showAboutDialog();
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        menu.getItem(1).setEnabled(menuEnable);
        menu.getItem(2).setEnabled(menuEnable && menuExportEnable);
        menu.getItem(3).setEnabled(menuEnable);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_EXPLORER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (config.setConfig(data.getStringExtra("file"))) {
                    loadConfig();
                    menuExportEnable = config.isEditable();
                    showMsg(getString(R.string.msgLoaded));
                } else {
                    showMsg(getString(R.string.msgLoadError));
                }
            }
        }
    }

    public void onClickBtnHostHeader(View view) {
        showHostInput();
    }

    public void onClickBtnPass(View view) {
        showPassInput();
    }

    public void onClickBtnRun(View view) {
        if (server == null) {
            if (!config.isEditable() || config.setLocalPort(txtLocalPort.getText().toString())) {
                if (!config.isEditable() || config.setServer(txtServer.getText().toString())) {
                    if (!config.isEditable() || config.setTarget(txtTarget.getText().toString())) {

                        bindService(new Intent(this, ServerClientService.class), this, BIND_AUTO_CREATE);
                        btnRun.setEnabled(false);
                    } else {
                        showMsg(getString(R.string.msgTargetInvalido));
                    }
                } else {
                    showMsg(getString(R.string.msgServerInvalido));
                }
            } else {
                showMsg(getString(R.string.msgLocalPortInvalido));
            }
        } else {
            server.close();
            server = null;
            stopService();
            stopLogBox();
            stopByteCounter();
            btnRun.setEnabled(true);
            btnRun.setText(getString(R.string.btnRun));
            setEnableInput(true);
            setEnableMenu(true);
        }
    }

    public void setBtnStop() {
        btnRun.setEnabled(true);
        btnRun.setText(getString(R.string.btnRunStop));
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        needUnbind = true;
        setEnableInput(false);
        setEnableMenu(false);

        ServerClientService.Controller controller = (ServerClientService.Controller) service;
        serviceControl = controller.getControl();

        server = serviceControl.getClientServer();

        if (server == null) {
            serviceControl.setClientServer(LISTENING_ADDR, config.getLocalPort(), config.getTarget(), config.getServerAddr(), config.getServerPort());
            server = serviceControl.getClientServer();

            String configHost = config.getHostHeader();
            if (configHost != null && configHost.length() > 0)
                server.getConfig().setHostHeader(configHost);
            String configPass = config.getPass();
            if (configPass != null && configPass.length() > 0)
                server.getConfig().setPass(configPass);
            String bodyInject = config.getBodyInject();
            if (bodyInject != null && bodyInject.length() > 0)
                server.getConfig().setBodyInject(bodyInject.replace("\\r","\r").replace("\\n","\n"));

            serviceControl.setByteCounter(new ByteCounter(MILLISECONDS_BYTE_COUNTER));
            serviceControl.setLogBox(new LogBox(MILLISECONDS_LOG_REFRESH));

            startService();
        }

        startLogBox(serviceControl.getLogBox());
        startByteCounter(serviceControl.getByteCounter());

        setBtnStop();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (server != null)
            server.close();
        server = null;
        serviceControl = null;
        stopLogBox();
        stopByteCounter();
        stopService(new Intent(this, ServerClientService.class));
        showMsg(getString(R.string.msgServiceDisconnect));
    }

    private void unbindService() {
        if (needUnbind) {
            needUnbind = false;
            unbindService(this);
        }
    }

    private void startService() {
        Intent i = new Intent(this, ServerClientService.class);
        startService(i);
    }

    private void stopService() {
        unbindService();

        serviceControl = null;

        Intent i = new Intent(this, ServerClientService.class);
        stopService(i);
    }

    private void startLogBox(LogBox logBox) {
        this.logBox = logBox;
        this.logBox.setBoxSize(LOG_BOX_SIZE);
        this.logBox.setListener(this);
        onLog(this.logBox.toString());
        this.logBox.start();
    }

    private void stopLogBox() {
        if (logBox != null) {
            logBox.stop();
            logBox = null;
        }
    }

    private void startByteCounter(ByteCounter counter) {
        this.counter = counter;
        this.counter.setListener(this);
        this.counter.start();
    }

    private void stopByteCounter() {
        if (counter != null) {
            counter.stop();
            counter = null;
        }
    }

    private void saveConfig() {
        config.setLocalPort(txtLocalPort.getText().toString());
        config.setServer(txtServer.getText().toString());
        config.setTarget(txtTarget.getText().toString());

        try {
            config.save();
        } catch(IOException e) {
            showMsg(e.getMessage());
        }
    }

    private void loadConfig() {
        try {
            config.load();

            txtServer.setText(config.getServer());
            txtTarget.setText(config.getTarget());
            if (config.getLocalPort() != 0)
                txtLocalPort.setText(String.valueOf(config.getLocalPort()));

            if (!config.isEditable()) {
                txtServer.setText("*************************************************************");
                txtTarget.setText("*************************************************************");
            }

            txtServer.setEnabled(config.isEditable());
            txtTarget.setEnabled(config.isEditable());
            btnHost.setEnabled(config.isEditable());
            btnPass.setEnabled(config.isEditable());
        } catch(IOException e) {
            showMsg(e.getMessage());
        }
    }

    private void setEnableInput(boolean enable) {
        txtLocalPort.setEnabled(enable);
        txtServer.setEnabled(config.isEditable() && enable);
        txtTarget.setEnabled(config.isEditable() && enable);
        btnHost.setEnabled(config.isEditable() && enable);
        btnPass.setEnabled(config.isEditable() && enable);
    }

    private void setEnableMenu(boolean enable) {
        this.menuEnable = enable;
    }


    @Override
    public void countBytes(final long uploadBytes, final long downloadBytes) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String upStr = formatter.format(uploadBytes) + "B";
                String downStr = formatter.format(downloadBytes) + "B";

                txtViewUp.setText(upStr);
                txtViewDown.setText(downStr);
            }
        });
    }

    @Override
    public void onLog(final String log) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtViewLog.setText(log);
            }
        });
    }

    private void showClearConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.titleClearConfigDialog));

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (config.reset()) {
                    loadConfig();
                    showMsg(getString(R.string.msgReset));
                } else {
                    showMsg(getString(R.string.msgResetError));
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void showHostInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.titleHostHeaderInput));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(config.getHostHeader());
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                config.setHostHeader(input.getText().toString());
            }
        });

        builder.show();
    }

    private void showPassInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.titlePassInput));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(config.getPassDesembaralhada());
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                config.setPass(input.getText().toString());
            }
        });

        builder.show();
    }

    private void showExportDialog() {
        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(LinearLayout.VERTICAL);

        final CheckBox boxEditable = new CheckBox(this);
        boxEditable.setText(getString(R.string.labelCheckBoxEditable));
        boxEditable.setChecked(true);
        layout.addView(boxEditable);

        final EditText fileName = new EditText(this);
        fileName.setHint(getString(R.string.labelTextFieldFileName));
        layout.addView(fileName);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.exportFileDialog));
        builder.setView(layout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = fileName.getText().toString();

                if (!name.isEmpty()) {
                    File root = new File(Environment.getExternalStorageDirectory(), "GetTunnel");
                    File file = new File(root, name + ".conf");

                    root.mkdirs();

                    if (config.exportToFile(file, boxEditable.isChecked())) {
                        showMsg(getString(R.string.msgExport) + " " + file.getAbsolutePath());
                    } else {
                        showMsg(getString(R.string.msgExportError));
                    }
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.menu_about));

        TextView txtAbout = new TextView(this);
        txtAbout.setText(getString(R.string.txtAbout).replace("[nl]", "\n").replace("[tb]", "\t"));

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(txtAbout);

        builder.setView(scrollView);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        /*builder.setNegativeButton("Source", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://universo42.comxa.com/Files/Sources/MySuperTunnelGet/"));
                startActivity(browserIntent);
            }
        });*/

        builder.show();
    }

    private void showMsg(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }
}
