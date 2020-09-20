package org.ndroi.easy163.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;
import org.ndroi.easy163.BuildConfig;
import org.ndroi.easy163.R;
import org.ndroi.easy163.core.Cache;
import org.ndroi.easy163.core.Local;
import org.ndroi.easy163.utils.EasyLog;
import org.ndroi.easy163.vpn.LocalVPNService;
import static android.support.v7.app.AlertDialog.Builder;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ToggleButton.OnCheckedChangeListener
{
    private static final int VPN_REQUEST_CODE = 0x0F;
    ToggleButton toggleButton = null;

    private BroadcastReceiver serviceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            boolean isServiceRunning = intent.getBooleanExtra("isRunning", false);
            Log.d("MainActivity", "BroadcastReceiver service isRunning: " + isServiceRunning);
            toggleButton.setChecked(isServiceRunning);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceReceiver, new IntentFilter("service"));
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        toggleButton = findViewById(R.id.bt_start);
        toggleButton.setOnCheckedChangeListener(this);
        //syncServiceState();
        EasyLog.setTextView(findViewById(R.id.log));
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
            builder.setMessage("当前版本" + BuildConfig.VERSION_NAME + "\n" + "由@Revincx添加了对倒带的支持和通知栏快捷设置" + "\n" +
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

    private void startVPN()
    {
        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null)
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        else
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
    }

    private void stopVPN()
    {
        Intent intent = new Intent("activity");
        intent.putExtra("cmd", "stop");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d("stopVPN", "try to stopVPN");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK)
        {
            Intent intent = new Intent(this, LocalVPNService.class);
            startService(intent);
        }
    }
}