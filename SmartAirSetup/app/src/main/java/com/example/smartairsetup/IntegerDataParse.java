package com.example.smartairsetup;

import android.widget.EditText;

public class IntegerDataParse {
    public int parsePEF(EditText editText) {
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) return 0;
        return Integer.parseInt(text);
    }
}
