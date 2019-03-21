package com.comxa.universo42.gettunnel;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class CrashActivity extends Activity {

    private TextView txtViewData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);

        txtViewData = (TextView) findViewById(R.id.txtData);

        Intent intent = getIntent();

        if (intent != null) {
            String error = intent.getStringExtra("error");

            if (error != null) {
                txtViewData.setText(error);
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
        finish();
    }

    public void onClickBtnShare(View view) {
        Intent i = new Intent();
        i.setAction(Intent.ACTION_SEND);
        i.setType("text/html");
        i.putExtra(Intent.EXTRA_TEXT, txtViewData.getText().toString());

        startActivity(i);
    }

    public void onClickBtnCopy(View view) {
        copyToClipboard(txtViewData.getText().toString());

        Toast.makeText(this, getString(R.string.msg_copiado), Toast.LENGTH_SHORT).show();
    }

    private void copyToClipboard(String str) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);

            clipboard.setText(str);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);

            clipboard.setPrimaryClip(ClipData.newPlainText("simple text", str));
        }
    }
}