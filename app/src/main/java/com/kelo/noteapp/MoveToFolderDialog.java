package com.kelo.noteapp;

import android.app.AlertDialog;
import android.content.Context;

public class MoveToFolderDialog {

    public interface OnFolderChosen {
        void onChosen(String categoryKey);
    }

    private static final String[] CAT_KEYS    = {"work","personal","family","errand","other","everyday"};
    private static final String[] CAT_DISPLAY = {"Работа","Личное","Семья","Поручение","Другое","Ежедневно"};

    public static void show(Context ctx, OnFolderChosen callback) {
        new AlertDialog.Builder(ctx)
                .setTitle("Переместить в папку")
                .setItems(CAT_DISPLAY, (d, which) -> {
                    if (callback != null) callback.onChosen(CAT_KEYS[which]);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}
