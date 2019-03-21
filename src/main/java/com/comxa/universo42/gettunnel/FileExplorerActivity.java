package com.comxa.universo42.gettunnel;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.widget.TextView;
import android.widget.Toast;

import com.comxa.universo42.gettunnel.R;
import com.comxa.universo42.gettunnel.view.FileListAdapter;

public class FileExplorerActivity extends Activity {
    public static final int RESULT_CODE_CANCELED = 2;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 42;

    private Button btnOk;
    private Button btnParent;
    private TextView txtViewSelected;
    private ListView lista;
    private FileListAdapter adapter;
    private List<File> filesLista = new ArrayList<File>();
    private File selecionado;
    private File dirAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        this.txtViewSelected = (TextView) findViewById(R.id.txtViewSelected);
        this.btnOk = (Button) findViewById(R.id.btnOk);
        this.btnOk.setEnabled(false);
        this.btnParent = (Button) findViewById(R.id.btnParent);
        this.lista =  (ListView) findViewById(R.id.lista);
        this.lista.setOnItemClickListener(getOnItemClickLista());

        this.dirAtual = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            makeLista(this.dirAtual);
        }
    }

    @Override
    public void onBackPressed() {
        finish(RESULT_CODE_CANCELED, new Intent());
    }

    public void onClickBtnParent(View view) {
        if (dirAtual.getParentFile() != null) {
            dirAtual = dirAtual.getParentFile();
            makeLista(dirAtual);
        }
    }

    public void onClickBtnOk(View view) {
        Intent data = new Intent();

        data.putExtra("file", selecionado.toString());

        finish(RESULT_OK, data);
    }

    public AdapterView.OnItemClickListener getOnItemClickLista() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int posicaoLinhaSelecionada, long id) {
                File fileSelecionado = filesLista.get(posicaoLinhaSelecionada);

                if(fileSelecionado.isDirectory()) {
                    dirAtual = fileSelecionado;
                    makeLista(dirAtual);
                } else {
                    selecionado = fileSelecionado;
                    txtViewSelected.setText(selecionado.getName());
                    btnOk.setEnabled(true);
                }
            }
        };
    }

    public void makeLista(File dir) {
        File[] files = dir.listFiles();

        if (files != null) {
            this.filesLista.clear();
            ArrayList<File> fileOnlyList = new ArrayList<File>();

            for (File file : files) {
                if (!file.getName().startsWith(".")) {
                    if (file.isDirectory()) {
                        this.filesLista.add(file);
                    } else {
                        fileOnlyList.add(file);
                    }
                }
            }

            Comparator<File> comparator = new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().toUpperCase().compareTo(o2.getName().toUpperCase());
                }
            };

            Collections.sort(this.filesLista, comparator);
            Collections.sort(fileOnlyList, comparator);

            this.filesLista.addAll(fileOnlyList);

            this.adapter = new FileListAdapter(this.filesLista, this);
            this.lista.setAdapter(this.adapter);
        } else {
            Toast.makeText(this, getString(R.string.msg_sempermissao), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode ==MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makeLista(this.dirAtual);
            } else {
                Toast.makeText(this, getString(R.string.msg_sempermissao), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void finish(int retCod, Intent data) {
        setResult(retCod, data);

        this.finish();
    }
}
