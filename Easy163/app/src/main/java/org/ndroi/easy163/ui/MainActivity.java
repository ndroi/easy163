package org.ndroi.easy163.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ToggleButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.ndroi.easy163.R;
import org.ndroi.easy163.utils.EasyLog;
import org.ndroi.easy163.vpn.LocalVPNService;

public class MainActivity extends AppCompatActivity implements ToggleButton.OnCheckedChangeListener {

  private static final int VPN_REQUEST_CODE = 0x0F;
  private ToggleButton toggleButton = null;

  private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      boolean isServiceRunning = intent.getBooleanExtra("isRunning", false);
      Log.d("MainActivity", "BroadcastReceiver service isRunning: " + isServiceRunning);
      toggleButton.setChecked(isServiceRunning);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    LocalBroadcastManager.getInstance(this).registerReceiver(serviceReceiver, new IntentFilter("service"));

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    toggleButton = findViewById(R.id.bt_start);
    toggleButton.setOnCheckedChangeListener(this);
    syncServiceState();
    EasyLog.setTextView(findViewById(R.id.log));
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (isChecked) {
      startVPN();
    } else {
      stopVPN();
    }
  }

  private void syncServiceState() {
    Intent intent = new Intent("activity");
    intent.putExtra("cmd", "check");
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

  private void startVPN() {
    Intent vpnIntent = VpnService.prepare(this);
    if (vpnIntent != null) {
      startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
    } else {
      onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
    }
  }

  private void stopVPN() {
    Intent intent = new Intent("activity");
    intent.putExtra("cmd", "stop");
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    Log.d("stopVPN", "try to stopVPN");
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
      Intent intent = new Intent(this, LocalVPNService.class);
      startService(intent);
    }
  }
}