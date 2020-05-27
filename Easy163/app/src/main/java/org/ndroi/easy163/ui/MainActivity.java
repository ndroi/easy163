package org.ndroi.easy163.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
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

import org.ndroi.easy163.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import static android.support.v7.app.AlertDialog.*;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ToggleButton.OnCheckedChangeListener
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ToggleButton toggleButton = findViewById(R.id.bt_start);
        toggleButton.setOnCheckedChangeListener(this);
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_github)
        {
            Uri uri = Uri.parse("https://github.com/ndroi/easy163");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } else if (id == R.id.nav_usage)
        {
            Builder builder=new Builder(this);
            builder.setTitle("使用说明");
            builder.setMessage("开启本软件后，设置系统代理为：\n" +
                    "主机：127.0.0.1 \n" +
                    "端口：8080 \n" +
                    "如 MIUI：\n" +
                    "WLAN -> 连接的 WLAN -> 代理 -> 手动 。\n" +
                    "开启本软件后如遇到设备网络异常请取消代理设置并关闭本软件。");
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            builder.show();
        } else if (id == R.id.nav_statement)
        {
            Builder builder=new Builder(this);
            builder.setTitle("免责声明");
            builder.setMessage("本软件工作原理为在本地开启 HTTP 代理服务，拦截重定向网易云请求到其他平台。\n" +
                    "设备所有的 HTTP 请求皆由本软件代理，如质疑其安全性欢迎阅读源码。\n" +
                    "本软件为实验性项目，请勿用于非法用途。");
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            builder.show();
        } else if (id == R.id.nav_donate)
        {
            {
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        super.run();
                        Socket socket = new Socket();
                        try
                        {
                            socket.connect(new InetSocketAddress("zlpingguo.com", 80));
                            OutputStream outputStream = socket.getOutputStream();
                            InputStream inputStream = socket.getInputStream();
                            outputStream.write("GET / HTTP/1.1\r\nHost: www.zlpingguo.com\r\n\r\n".getBytes());
                            byte[] buffer = new byte[4096];
                            inputStream.read(buffer);
                            String txt = new String(buffer);
                            Log.d("zlpingguo", txt);
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
            Builder builder=new Builder(this);
            builder.setTitle("捐赠支持");
            builder.setMessage("暂未开放捐赠，欢迎 Github 点赞。");
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            builder.show();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        Intent intent = new Intent(this, ServerService.class);
        if(isChecked)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                startForegroundService(intent);
            } else
            {
                startService(intent);
            }
            Toast.makeText(this, "开启代理成功", Toast.LENGTH_SHORT).show();
        }else
        {
            stopService(intent);
            System.exit(0);
        }
    }
}