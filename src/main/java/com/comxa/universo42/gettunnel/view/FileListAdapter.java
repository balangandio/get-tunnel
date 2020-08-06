package com.comxa.universo42.gettunnel.view;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.comxa.universo42.gettunnel.R;

import java.io.File;
import java.util.List;

public class FileListAdapter extends BaseAdapter {

    private Activity act;
    private List<File> fileList;

    public FileListAdapter(List<File> fileList, Activity act) {
        this.act = act;
        this.fileList = fileList;
    }

    @Override
    public int getCount() {
        return this.fileList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.fileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater = this.act.getLayoutInflater();

        View listaLayout = layoutInflater.inflate(R.layout.list_view_files_layout, parent, false);

        File file = fileList.get(position);

        ImageView imgView = (ImageView) listaLayout.findViewById(R.id.file_list_img);
        if (file.isDirectory()) {
            imgView.setImageResource(R.mipmap.folder);
        } else {
            if (file.getName().endsWith(".conf")) {
                imgView.setImageResource(R.mipmap.ic_launcher);
            } else {
                imgView.setImageResource(R.mipmap.file);
            }
        }

        TextView txtNome = (TextView) listaLayout.findViewById(R.id.file_list_nome);
        txtNome.setText(file.getName());

        return listaLayout;
    }
}
