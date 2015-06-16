package ninja.hein.codestore;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String PREF_CODE = "CODE";
    private static final String PREF_ERROR = "ERROR";
    private static final String PREF_PIN = "PIN";
    private static String pinstr = new String();

    SharedPreferences myPrefs;
    Editor myEditor;
    LinearLayout topLayout;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initialize();

    }

    private void initialize() {
        myPrefs = getPreferences(MODE_PRIVATE);
        myEditor = myPrefs.edit();
        topLayout = (LinearLayout) findViewById(R.id.linearMain);

        // Layout des Buttons parameter dafür
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        // Button existiert in einem Linear Layout
        final LinearLayout linlay = new LinearLayout(this);
        linlay.setOrientation(LinearLayout.HORIZONTAL);
        // returns the String "ERROR" if PREF_ERROR key does not exist
        final String noentry = myPrefs.getString(PREF_ERROR, "ERROR");
        String code = new String();

        if (!myPrefs.contains(PREF_CODE)) {

            final Button btn = new Button(this);
            btn.setId(1);
            btn.setText("new Codestore");
            btn.setLayoutParams(params);

            btn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    topLayout.removeView(linlay);
                    displayCodeStore(noentry);

                }
            });
            linlay.addView(btn);
            topLayout.addView(linlay);
        } else {
            pincheck();
        }
    }

    protected void displayCodeStore(String data) {

        topLayout = (LinearLayout) findViewById(R.id.linearMain);

        final EditText txt_eingabe = new EditText(this);
        txt_eingabe.setHint("Hier bitte den Text eingeben");
        txt_eingabe.setSingleLine(false);
        txt_eingabe.setHorizontalScrollBarEnabled(false);
        txt_eingabe.setMinLines(3);
        txt_eingabe.setGravity(Gravity.TOP | Gravity.LEFT);

        // funktionieren nicht wirklich
        txt_eingabe.setVerticalScrollBarEnabled(true);
        txt_eingabe.setMovementMethod(new ScrollingMovementMethod());
        topLayout.addView(txt_eingabe);

        LinearLayout myButtons = (LinearLayout) getLayoutInflater().inflate(
                R.layout.del_save, null);
        topLayout.addView(myButtons);
        buttoninit(txt_eingabe);

        if (data == "ERROR") {
            txt_eingabe.setText("");
        } else {
            try {
                txt_eingabe.setText(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void buttoninit(EditText txt_eingabe) {
        myPrefs = getPreferences(MODE_PRIVATE);
        myEditor = myPrefs.edit();

        final EditText txt_eingabe1 = txt_eingabe;

        Button btn_del = (Button) findViewById(R.id.btn_del);
        Button btn_save = (Button) findViewById(R.id.btn_save);
        btn_del.setText("Delete");
        btn_save.setText("Save");
        btn_del.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                areyousure();
            }
        });

        btn_save.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                // save it in shared Prefs
                try {
                    pincreate();
                    String input = new String();

                    input = txt_eingabe1.getText().toString();
                    myEditor.clear();
                    myEditor.putString(PREF_CODE, input);
                    myEditor.commit();

                    // killmyapp();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

    private void areyousure() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm");
        builder.setMessage("Are you sure?");
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                dialog.dismiss();
            }
        });

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // Do nothing but close the dialog
                // delete it from Shared Prefs
                Toast.makeText(MainActivity.this, "Deleted", Toast.LENGTH_SHORT)
                        .show();
                myEditor.clear();
                myEditor.commit();
                dialog.dismiss();
                killmyapp();
            }

        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    /*
     * Methode die den Pin erstellt, und falls er zu kurz ist erneut aufruft
     */
    private void pincreate() {
        myPrefs = getPreferences(MODE_PRIVATE);
        myEditor = myPrefs.edit();

        setContentView(R.layout.pin_entry);
        final EditText pin_field = (EditText) findViewById(R.id.pin_field);

        pin_field.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                pinstr = pin_field.getText().toString();
                if (pinstr.length() != 4) {
                    Toast.makeText(MainActivity.this, "Pin ist zu kurz",
                            Toast.LENGTH_SHORT).show();
                    return false;
                } else {
                    myEditor.putString(PREF_PIN, pinstr);
                    myEditor.commit();

                    // Das ist echt unschön, oder?
                    setContentView(R.layout.activity_main);
                    String code = myPrefs.getString(PREF_CODE, null);
                    displayCodeStore(code);

                    try {
                        EncryptDecrypt encryptor = new EncryptDecrypt(pinstr);
                        //encryptor.encrypt(code);
                        String encrypted = new String();
                        encrypted = encryptor.encrypt(code);

                        String decrypted = new String();
                        decrypted = encryptor.decrypt(encrypted);

                        Toast.makeText(MainActivity.this, decrypted,
                                Toast.LENGTH_SHORT).show();
                    } catch (InvalidKeyException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (NoSuchPaddingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalBlockSizeException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (BadPaddingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });
    }

    private void pincheck() {
        myPrefs = getPreferences(MODE_PRIVATE);
        myEditor = myPrefs.edit();

        final String pin = myPrefs.getString(PREF_PIN, "PIN");
        // neuer codestore
        if (pin == "PIN") {
            setContentView(R.layout.activity_main);
            String code = myPrefs.getString(PREF_CODE, null);
            displayCodeStore(code);
        }

        setContentView(R.layout.pin_entry);
        final EditText pin_field = (EditText) findViewById(R.id.pin_field);

        pin_field.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                String pinstr = pin_field.getText().toString();

                if (pinstr.length() != 4) {
                    Toast.makeText(MainActivity.this, "Pin ist zu kurz",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }

                if (pinstr.equals(pin)) {
                    setContentView(R.layout.activity_main);
                    String code = myPrefs.getString(PREF_CODE, null);
                    displayCodeStore(code);
                }

                return false;
            }
        });
    }

    private void killmyapp() {
        finish();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public class EncryptDecrypt {
        private SecretKeySpec skeySpec;
        private Cipher cipher;

        EncryptDecrypt(String password) throws NoSuchAlgorithmException,
                UnsupportedEncodingException, NoSuchPaddingException,
                IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] key = Arrays.copyOf(sha
                            .digest(("ThisisMySalt1234" + password).getBytes("UTF-8")),
                    16);
            skeySpec = new SecretKeySpec(key, "AES");
            cipher = Cipher.getInstance("AES");
        }

        String encrypt(String clear) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
            String encrypted;
            cipher.init(Cipher.ENCRYPT_MODE,skeySpec);
            byte[] encryptedBytes = null;
            try {
                encryptedBytes = cipher.doFinal(clear.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            encrypted = Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
            return encrypted;
        }

        String decrypt(String encryptedBase64) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
            String decrypted = null;
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] decodedBytes = null;

            decodedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT);

            try {
                decrypted = new String(cipher.doFinal(decodedBytes), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            return decrypted;

        }

    }
}
