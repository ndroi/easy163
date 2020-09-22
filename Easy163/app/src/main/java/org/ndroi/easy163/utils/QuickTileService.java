package org.ndroi.easy163.utils;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.net.VpnService;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.ndroi.easy163.R;
import org.ndroi.easy163.core.Server;
import org.ndroi.easy163.ui.MainActivity;
import org.ndroi.easy163.vpn.LocalVPNService;

import java.lang.ref.SoftReference;
import java.util.List;

public class QuickTileService extends TileService
{

    @Override
    public void onTileAdded()
    {
        getQsTile().setState(Tile.STATE_INACTIVE);
        super.onTileAdded();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

    }

//    @Override
//    public void onCreate()
//    {
//        super.onCreate();
//        registerReceiver(tileReceiver, new IntentFilter("activity"));
//    }

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile();
        int State = tile.getState();
        if(State == Tile.STATE_INACTIVE)
        {
            Intent vpnIntent = VpnService.prepare(this);
            if (vpnIntent != null)
            {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                Toast.makeText(this,"请先在软件内开启一次VPN！",Toast.LENGTH_LONG).show();
                return;
            }
            else
            {
                Intent cmdIntent = new Intent("activity");
                cmdIntent.putExtra("cmd", "start");
                LocalBroadcastManager.getInstance(this).sendBroadcast(cmdIntent);
                Intent intent = new Intent(this, LocalVPNService.class);
                startService(intent);
            }
            tile.setState(Tile.STATE_ACTIVE);
        }
        else
        {
            Intent intent = new Intent("activity");
            intent.putExtra("cmd", "stop");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            Log.d("stopVPN", "try to stopVPN");
            tile.setState(Tile.STATE_INACTIVE);
        }
        getQsTile().updateTile();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getQsTile().setState(Tile.STATE_INACTIVE);
        getQsTile().updateTile();
    }
}
