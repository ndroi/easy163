package org.ndroi.easy163.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.net.VpnService;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import org.ndroi.easy163.vpn.LocalVPNService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class EasyTileService extends TileService
{
    @Override
    public void onStartListening()
    {
        super.onStartListening();
        Log.d("EasyTileService", "onStartListening");
        Tile tile = getQsTile();
        if (tile == null)
        {
            return;
        }
        if(LocalVPNService.getIsRunning())
        {
            tile.setState(Tile.STATE_ACTIVE);
            tile.updateTile();
        }else
        {
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        TileService.requestListeningState(this, new ComponentName(this, EasyTileService.class));
        return super.onBind(intent);
    }

    @Override
    public void onClick()
    {
        super.onClick();
        Tile tile = getQsTile();
        if (tile == null)
        {
            return;
        }
        switch (tile.getState())
        {
            case Tile.STATE_ACTIVE:
            {
                stopVPN();
                tile.setState(Tile.STATE_INACTIVE);
                tile.updateTile();
                break;
            }
            case Tile.STATE_INACTIVE:
            {
                startVPN();
                tile.setState(Tile.STATE_ACTIVE);
                tile.updateTile();
                break;
            }
            default:break;
        }
    }

    private void startVPN()
    {
        if (VpnService.prepare(this) == null)
        {
            Intent intent = new Intent(this, LocalVPNService.class);
            startService(intent);
        } else
        {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(intent);
        }
    }

    private void stopVPN()
    {
        Intent intent = new Intent("control");
        intent.putExtra("cmd", "stop");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d("stopVPN", "try to stopVPN");
    }
}
