package com.example.pictit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.TextView;
public class AlertDialogFrag extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_dialog);

        TextView tv = (TextView) findViewById(R.id.text);
        ((TextView)tv).setText("Alert!");
    }


    void showDialog() {
        DialogFragment newFragment = AlertDialogFragment.newInstance(
                "alert_dialog", null);
        newFragment.show(getFragmentManager(), "dialog");
    }

   public static class AlertDialogFragment extends DialogFragment {
        public static AlertDialogFragment newInstance(String title, String msg) {
			AlertDialogFragment frag = new AlertDialogFragment();
			frag.setStyle(STYLE_NO_FRAME, 0);
			Bundle args = new Bundle();
			args.putString("title", title);
			args.putString("message", msg);
			frag.setArguments(args);
			return frag;
        }
        public Dialog onCreateDialog(Bundle savedInstanceState) {
			String title = getArguments().getString("title");
			String message = getArguments().getString("message");
			return new AlertDialog.Builder(getActivity())
			        .setIcon(R.drawable.ic_launcher)
			        .setTitle(title)
			        .setMessage(message)
			        .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int id) {
			                //query_date = now_date;
			                    dialog.dismiss();
			                }
			            })
			            .create();
			   }
    }
}