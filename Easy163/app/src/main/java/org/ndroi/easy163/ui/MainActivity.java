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
import org.ndroi.easy163.core.Cache;
import org.ndroi.easy163.core.Local;
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
    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        } else
        {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.nav_github)
        {
            Uri uri = Uri.parse("https://github.com/ndroi/easy163");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } else if (id == R.id.nav_usage)
        {
            Builder builder = new Builder(this);
            builder.setTitle("使用说明");
            builder.setMessage("开启本软件 VPN 服务后即可使用\n" +
                    "如音乐软件无法联网请重启手机\n" +
                    "清空音乐软件缓存后请重启本软件\n" +
                    "更多问题请查阅 Github");
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            builder.show();
        } else if (id == R.id.nav_statement)
        {
            Builder builder = new Builder(this);
            builder.setTitle("免责声明");
            builder.setMessage("本软件为实验性项目\n" +
                    "仅提供技术研究使用\n" +
                    "本软件完全免费\n" +
                    "作者不承担用户因软件造成的一切责任");
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            builder.show();
        } else if (id == R.id.nav_clear_cache)
        {
            Cache.clear();
            Local.clear();
            Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_about)
        {
            Builder builder = new Builder(this);
            builder.setTitle("关于");
            builder.setMessage("当前版本 " + BuildConfig.VERSION_NAME + "\n" +
                    "版本更新关注 Github Release");
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            builder.show();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        if (isChecked)
        {
            startVPN();
        } else
        {
            stopVPN();
        }
    }

    private void syncServiceState()
    {
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