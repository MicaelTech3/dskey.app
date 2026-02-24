package com.dsigner.dskey;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Tela exibida apenas na PRIMEIRA abertura do app.
 * Pergunta se o usuário deseja iniciar o DSigner TV com o sistema.
 *
 * Como o app já está configurado como launcher (category HOME no Manifest),
 * ele JÁ abre automaticamente com o sistema — essa tela apenas informa isso
 * e registra que o onboarding foi concluído.
 */
public class WelcomeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "dskey_prefs";
    private static final String KEY_ONBOARDING = "onboarding_done";

    private Button btnSim;
    private Button btnNao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Se onboarding já foi feito, vai direto para o app
        if (isOnboardingDone()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_welcome);

        btnSim = findViewById(R.id.btnSim);
        btnNao = findViewById(R.id.btnNao);

        // ✅ SIM — marca como feito e vai para o app
        btnSim.setOnClickListener(v -> {
            markOnboardingDone(true);
            goToMain();
        });

        // ❌ NÃO — marca como feito (sem autostart) e vai para o app
        btnNao.setOnClickListener(v -> {
            markOnboardingDone(false);
            goToMain();
        });

        // Foco inicial no botão SIM (melhor para controle remoto)
        btnSim.requestFocus();
    }

    // Navega entre botões com o controle remoto (D-pad)
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_TAB) {
            if (btnSim.isFocused()) {
                btnNao.requestFocus();
                return true;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (btnNao.isFocused()) {
                btnSim.requestFocus();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean isOnboardingDone() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sp.getBoolean(KEY_ONBOARDING, false);
    }

    private void markOnboardingDone(boolean autostart) {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sp.edit()
                .putBoolean(KEY_ONBOARDING, true)
                .putBoolean("autostart_enabled", autostart)
                .apply();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish(); // remove WelcomeActivity da pilha de back
    }
}