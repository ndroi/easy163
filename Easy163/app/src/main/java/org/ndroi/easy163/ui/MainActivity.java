package org.ndroi.easy163.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.ndroi.easy163.R;
import org.ndroi.easy163.utils.EasyLog;
import org.ndroi.easy163.vpn.LocalVPNService;

public class MainActivity extends AppCompatActivity {

  private static final int VPN_REQUEST_CODE = 0x0F;
  private ViewGroup serviceLayout = null;
  private boolean isRunning = false;

  private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      boolean isServiceRunning = intent.getBooleanExtra("isRunning", false);
      Log.d("MainActivity", "BroadcastReceiver service isRunning: " + isServiceRunning);
      isRunning = isServiceRunning;
      setServiceLayout(isServiceRunning);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    IntentFilter filter = new IntentFilter();
    filter.addAction("service");
    filter.addAction("activity");
    LocalBroadcastManager.getInstance(this).registerReceiver(serviceReceiver, filter);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    serviceLayout = findViewById(R.id.layout_service);

    serviceLayout.setOnClickListener(view -> {
      if (!isRunning) {
        startVPN();
      } else {
        stopVPN();
      }
    });
    syncServiceState();
    EasyLog.setTextView(findViewById(R.id.log));
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

  private void setServiceLayout(boolean isServiceRunning) {
    TextView info = serviceLayout.findViewById(R.id.tv_info);
    TextView tip = serviceLayout.findViewById(R.id.tv_tip);
    ImageView icon = serviceLayout.findViewById(R.id.iv_icon);

    if (isServiceRunning) {
      info.setText(R.string.service_is_running);
      tip.setText(R.string.tap_to_stop_service);
      icon.setImageResource(R.drawable.ic_done);
    } else {
      info.setText(R.string.service_is_not_running);
      tip.setText(R.string.tap_to_start_service);
      icon.setImageResource(R.drawable.ic_no);
    }
  }
}