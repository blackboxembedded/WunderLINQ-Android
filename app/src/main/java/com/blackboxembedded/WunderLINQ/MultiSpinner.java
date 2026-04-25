package com.blackboxembedded.WunderLINQ;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;

public class MultiSpinner extends androidx.appcompat.widget.AppCompatSpinner {

    private final CharSequence[] entries;
    public boolean[] selected;
    private MultiSpinnerListener listener;

    public MultiSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiSpinner);
        entries = a.getTextArray(R.styleable.MultiSpinner_android_entries);
        if (entries != null) {
            selected = new boolean[entries.length]; // false-filled by default
        }
        a.recycle();
    }

    private final DialogInterface.OnMultiChoiceClickListener mOnMultiChoiceClickListener = new DialogInterface.OnMultiChoiceClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            selected[which] = isChecked;
        }
    };

    private final DialogInterface.OnClickListener mOnClickListener = (dialog, which) -> {
        // build new spinner text & delimiter management
        updateText();
        dialog.dismiss();
    };

    @Override
    public boolean performClick() {
        new AlertDialog.Builder(getContext())
                .setMultiChoiceItems(entries, selected, mOnMultiChoiceClickListener)
                .setPositiveButton(android.R.string.ok, mOnClickListener)
                .show();
        return true;
    }

    public void updateText(){
        StringBuilder spinnerBuffer = new StringBuilder();
        for (int i = 0; i < entries.length; i++) {
            if (selected[i]) {
                spinnerBuffer.append(entries[i]);
                spinnerBuffer.append(", ");
            }
        }

        // Remove trailing comma
        if (spinnerBuffer.length() > 2) {
            spinnerBuffer.setLength(spinnerBuffer.length() - 2);
        }

        // display new text
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                R.layout.item_hwsettings_spinners,
                new String[] { spinnerBuffer.toString() });
        setAdapter(adapter);

        if (listener != null) {
            listener.onItemsSelected(selected);
        }
    }

    public void setMultiSpinnerListener(MultiSpinnerListener listener) {
        this.listener = listener;
    }

    public interface MultiSpinnerListener {
        void onItemsSelected(boolean[] selected);
    }
}
