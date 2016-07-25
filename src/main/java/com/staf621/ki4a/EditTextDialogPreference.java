package com.staf621.ki4a;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.preference.DialogPreference;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EditTextDialogPreference extends DialogPreference {

    //Layout Fields
    private final RelativeLayout layout = new RelativeLayout(this.getContext());
    private final EditText editText = new EditText(this.getContext());
    private final Button button = new Button(this.getContext());

    //Called when addPreferencesFromResource() is called. Initializes basic paramaters
    public EditTextDialogPreference(final Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
        button.setText(R.string.str_import);
        button.getBackground().setColorFilter(ContextCompat.getColor(context, R.color.colorDarkButton), PorterDuff.Mode.SRC);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, KeyImport.class);
                context.startActivity(intent);
                //Close the dialog
                getDialog().dismiss();
            }
        });
    }

    //Create the Dialog view
    @Override
    protected View onCreateDialogView() {
        layout.addView(editText);
        layout.addView(button);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) button.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        return layout;
    }

    //Attach persisted values to Dialog
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        editText.setText(getPersistedString("EditText"), TextView.BufferType.NORMAL);
    }

    //persist values and disassemble views
    @Override
    protected void onDialogClosed(boolean positiveresult) {
        super.onDialogClosed(positiveresult);
        if (positiveresult && shouldPersist()) {
            String value = editText.getText().toString();
            if (callChangeListener(value))
                persistString(value);
        }

        ((ViewGroup) editText.getParent()).removeView(editText);
        ((ViewGroup) button.getParent()).removeView(button);
        ((ViewGroup) layout.getParent()).removeView(layout);

        notifyChanged();
    }

    public void setValue(String value) {
        editText.setText(value);
    }
}