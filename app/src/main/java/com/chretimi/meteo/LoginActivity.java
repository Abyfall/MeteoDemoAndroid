package com.chretimi.meteo;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private String login;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final Button button = findViewById(R.id.button_login);
        final EditText loginField = findViewById(R.id.login);
        final EditText passwordField = findViewById(R.id.password);


        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Context context = getApplicationContext();
                CharSequence text = getString(R.string.passWordAsk);
                int duration = Toast.LENGTH_SHORT;

                login = loginField.getText().toString();
                password = passwordField.getText().toString();

                Toast toast;

                if((login.equals("Michel") && password.equals("pass")) || 1==1){
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("userLogin", login);
                    startActivity(intent);
                }else{
                    toast = Toast.makeText(context, text, duration);
                    toast.show();
                }



            }
        });
    }
}
