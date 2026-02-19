package com.punemetroautomation;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Spinner spinnerFrom, spinnerTo;
    private Button btnStart, btnEnableAccess;
    private TextView tvStatus, tvStepIndicator;

    private final String[] STATIONS = {
        "Pimpri", "Pimpri Chinchwad", "Sant Tukaram Nagar", "Nashik Phata",
        "Kasarwadi", "Phugewadi", "Dapodi", "Bopodi", "Khadki", "Range Hills",
        "Shivajinagar", "Civil Court", "Budhwar Peth", "Mandai", "Swargate",
        "Vanaz", "Anand Nagar", "Ideal Colony", "Nal Stop", "Garware College",
        "Deccan Gymkhana", "PMC", "Pune Railway Station", "Ruby Hall Clinic",
        "Bund Garden", "Yerawada", "Kalyani Nagar", "Ramwadi"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);

            spinnerFrom = findViewById(R.id.spinnerFrom);
            spinnerTo   = findViewById(R.id.spinnerTo);
            btnStart        = findViewById(R.id.btnStart);
            btnEnableAccess = findViewById(R.id.btnEnableAccess);
            tvStatus        = findViewById(R.id.tvStatus);
            tvStepIndicator = findViewById(R.id.tvStepIndicator);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_dropdown_item, STATIONS);
            spinnerFrom.setAdapter(adapter);
            spinnerTo.setAdapter(adapter);
            spinnerTo.setSelection(10);

            BotStatusManager.setCallback(status ->
                runOnUiThread(() -> {
                    if (tvStatus != null) tvStatus.setText(status);
                })
            );

            btnEnableAccess.setOnClickListener(v -> openAccessibilitySettings());
            btnStart.setOnClickListener(v -> startBot());

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAccessibilityAndUpdateUI();
    }

    private void checkAccessibilityAndUpdateUI() {
        if (isAccessibilityEnabled()) {
            btnEnableAccess.setVisibility(View.GONE);
            btnStart.setEnabled(true);
            btnStart.setAlpha(1.0f);
            tvStatus.setText("Bot ready! Select stations and tap START BOT");
            tvStatus.setBackgroundColor(0xFFE8F5E9);
            tvStatus.setTextColor(0xFF2E7D32);
        } else {
            btnEnableAccess.setVisibility(View.VISIBLE);
            btnStart.setEnabled(false);
            btnStart.setAlpha(0.5f);
            tvStatus.setText("Please enable Accessibility Permission first!");
            tvStatus.setBackgroundColor(0xFFFFF3E0);
            tvStatus.setTextColor(0xFFE65100);
        }
    }

    private void startBot() {
        try {
            String from = spinnerFrom.getSelectedItem().toString();
            String to   = spinnerTo.getSelectedItem().toString();

            if (from.equals(to)) {
                Toast.makeText(this, "From and To cannot be same!", Toast.LENGTH_SHORT).show();
                return;
            }

            MetroBotService service = MetroBotService.getInstance();
            if (service == null) {
                Toast.makeText(this,
                    "Bot service not running. Enable Accessibility first!",
                    Toast.LENGTH_LONG).show();
                openAccessibilitySettings();
                return;
            }

            tvStatus.setText("Bot starting...\nFrom: " + from + "\nTo: " + to);
            tvStatus.setBackgroundColor(0xFFE3F2FD);
            tvStatus.setTextColor(0xFF0D47A1);
            service.startBot(from, to);

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openAccessibilitySettings() {
        Toast.makeText(this,
            "Find Pune Metro Bot and turn it ON",
            Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private boolean isAccessibilityEnabled() {
        try {
            int enabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            if (enabled == 1) {
                String services = Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (services != null) {
                    return services.toLowerCase()
                            .contains(getPackageName().toLowerCase());
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
