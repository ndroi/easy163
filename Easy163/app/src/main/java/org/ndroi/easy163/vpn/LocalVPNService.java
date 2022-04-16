package org.ndroi.easy163.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.service.quicksettings.TileService;
import android.util.Log;
import org.ndroi.easy163.R;
import org.ndroi.easy163.core.Cache;
import org.ndroi.easy163.core.Local;
import org.ndroi.easy163.core.Server;
import org.ndroi.easy163.ui.EasyTileService;
import org.ndroi.easy163.ui.MainActivity;
import org.ndroi.easy163.utils.EasyLog;
import org.ndroi.easy163.vpn.bio.BioTcpHandler;
import org.ndroi.easy163.vpn.bio.BioUdpHandler;
import org.ndroi.easy163.vpn.tcpip.Packet;
import org.ndroi.easy163.vpn.util.ByteBufferPool;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalVPNService extends VpnService
{
    private static final String TAG = LocalVPNService.class.getSimpleName();
    private static final String VPN_ADDRESS = "10.0.0.2"; // Only IPv4 support for now
    private static final String VPN_ROUTE = "0.0.0.0"; // Intercept everything
    private ParcelFileDescriptor vpnInterface = null;
    private BlockingQueue<Packet> deviceToNetworkUDPQueue;
    private BlockingQueue<Packet> deviceToNetworkTCPQueue;
    private BlockingQueue<ByteBuffer> networkToDeviceQueue;
    private ExecutorService executorService;
    private static Boolean isRunning = false;
    private static Context context = null;

    public static Context getContext()
    {
        return context;
    }

    public static Boolean getIsRunning()
    {
        return isRunning;
    }

    private BroadcastReceiver stopReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String cmd = intent.getStringExtra("cmd");
            if(cmd.equals("stop"))
            {
                executorService.shutdownNow();
                cleanup();
                LocalVPNService.this.stopSelf();
            }else if(cmd.equals("check"))
            {
                Log.i(TAG, "checkServiceState received");
                sendState();
            }
        }
    };

    @Override
    public void onCreate()
    {
        super.onCreate();
        context = getApplicationContext();
        setupVPN();
        LocalBroadcastManager.getInstance(this).registerReceiver(stopReceiver, new IntentFilter("control"));
        registerReceiver(stopReceiver, new IntentFilter("control"));
        deviceToNetworkUDPQueue = new ArrayBlockingQueue<Packet>(1000);
        deviceToNetworkTCPQueue = new ArrayBlockingQueue<Packet>(1000);
        networkToDeviceQueue = new ArrayBlockingQueue<>(1000);
        executorService = Executors.newFixedThreadPool(3);
        executorService.submit(new BioUdpHandler(deviceToNetworkUDPQueue, networkToDeviceQueue, this));
        executorService.submit(new BioTcpHandler(deviceToNetworkTCPQueue, networkToDeviceQueue, this));
        executorService.submit(new VPNRunnable(vpnInterface.getFileDescriptor(),
                deviceToNetworkUDPQueue, deviceToNetworkTCPQueue, networkToDeviceQueue));
        startNotification();
        Server.getInstance().start();
        Cache.init();
        Local.load();
        isRunning = true;
        sendState();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(this, new ComponentName(this, EasyTileService.class));
        }
        Log.i(TAG, "Easy163 VPN 启动");
    }

    private void startNotification()
    {
        String notificationId = "easy163";
        String notificationName = "easy163";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(notificationId, notificationName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 100, intent, PendingIntent.FLAG_IMMUTABLE);
        Intent stopIntent = new Intent("control");
        stopIntent.putExtra("cmd", "stop");
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 101, stopIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, notificationId)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.icon)
                .setContentTitle("Easy163")
                .setContentText("正在运行...")
                .addAction(R.mipmap.icon, "停止", stopPendingIntent);
        Notification notification = builder.build();
        startForeground(1, notification);
    }

    private void setupVPN()
    {
        try
        {
            if (vpnInterface == null)
            {
                Builder builder = new Builder();
                builder.addAddress(VPN_ADDRESS, 32);
                builder.addRoute(VPN_ROUTE, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                    try
                    {
                        builder.addAllowedApplication("com.netease.cloudmusic");
                    }catch (PackageManager.NameNotFoundException e)
                    {
                        Log.d(TAG, "未检测到网易云音乐");
                    }
                    try
                    {
                        builder.addAllowedApplication("com.netease.cloudmusic.lite");
                    }catch (PackageManager.NameNotFoundException e)
                    {
                        Log.d(TAG, "未检测到网易云音乐极速版");
                    }
                }
                vpnInterface = builder.setSession(getString(R.string.app_name)).establish();
            }
        } catch (Exception e)
        {
            Log.e(TAG, "Easy163 VPN 启动失败");
            EasyLog.log("Easy163 VPN 启动失败");
            System.exit(0);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        executorService.shutdownNow();
        cleanup();
        isRunning = false;
        sendState();
        unregisterReceiver(stopReceiver);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(this, new ComponentName(this, EasyTileService.class));
        }
        Log.i(TAG, "Stopped");
    }

    private void cleanup()
    {
        deviceToNetworkTCPQueue = null;
        deviceToNetworkUDPQueue = null;
        networkToDeviceQueue = null;
        closeResources(vpnInterface);
    }

    private void sendState()
    {
        MainActivity.resetBroadcastReceivedState();
        Intent replyIntent=  new Intent("service");
        replyIntent.putExtra("isRunning", isRunning);
        LocalBroadcastManager.getInstance(this).sendBroadcast(replyIntent);
        Log.i(TAG, "sendState");
    }

    private static void closeResources(Closeable... resources)
    {
        for (Closeable resource : resources)
        {
            try
            {
                resource.close();
            } catch (IOException e)
            {
                // Ignore
            }
        }
    }

    private static class VPNRunnable implements Runnable
    {
        private static final String TAG = VPNRunnable.class.getSimpleName();

        private FileDescriptor vpnFileDescriptor;

        private BlockingQueue<Packet> deviceToNetworkUDPQueue;
        private BlockingQueue<Packet> deviceToNetworkTCPQueue;
        private BlockingQueue<ByteBuffer> networkToDeviceQueue;

        public VPNRunnable(FileDescriptor vpnFileDescriptor,
                           BlockingQueue<Packet> deviceToNetworkUDPQueue,
                           BlockingQueue<Packet> deviceToNetworkTCPQueue,
                           BlockingQueue<ByteBuffer> networkToDeviceQueue)
        {
            this.vpnFileDescriptor = vpnFileDescriptor;
            this.deviceToNetworkUDPQueue = deviceToNetworkUDPQueue;
            this.deviceToNetworkTCPQueue = deviceToNetworkTCPQueue;
            this.networkToDeviceQueue = networkToDeviceQueue;
        }

        static class WriteVpnThread implements Runnable
        {
            FileChannel vpnOutput;
            private BlockingQueue<ByteBuffer> networkToDeviceQueue;

            WriteVpnThread(FileChannel vpnOutput, BlockingQueue<ByteBuffer> networkToDeviceQueue)
            {
                this.vpnOutput = vpnOutput;
                this.networkToDeviceQueue = networkToDeviceQueue;
            }

            @Override
            public void run()
            {
                while (!Thread.interrupted())
                {
                    try
                    {
                        ByteBuffer bufferFromNetwork = networkToDeviceQueue.take();
                        bufferFromNetwork.flip();
                        while (bufferFromNetwork.hasRemaining())
                        {
                            int w = vpnOutput.write(bufferFromNetwork);
                        }
                    } catch (InterruptedException e)
                    {
                        break;
                    }
                    catch (Exception e)
                    {
                        Log.i(TAG, "WriteVpnThread fail", e);
                    }
                }
            }
        }

        @Override
        public void run()
        {
            FileChannel vpnInput = new FileInputStream(vpnFileDescriptor).getChannel();
            FileChannel vpnOutput = new FileOutputStream(vpnFileDescriptor).getChannel();
            Thread t = new Thread(new WriteVpnThread(vpnOutput, networkToDeviceQueue));
            t.start();
            try
            {
                while (!Thread.interrupted())
                {
                    ByteBuffer bufferToNetwork = ByteBufferPool.acquire();
                    int readBytes = vpnInput.read(bufferToNetwork);
                    if (readBytes > 0)
                    {
                        bufferToNetwork.flip();
                        Packet packet = new Packet(bufferToNetwork);
                        if (packet.isUDP())
                        {
                            deviceToNetworkUDPQueue.offer(packet);
                        } else if (packet.isTCP())
                        {
                            deviceToNetworkTCPQueue.offer(packet);
                        } else
                        {
                            //Log.w(TAG, String.format("Unknown packet protocol type %d", packet.ip4Header.protocolNum));
                        }
                    } else
                    {
                        Thread.sleep(50);
                    }
                }
            } catch (InterruptedException ignored)
            {

            }catch (IOException e)
            {
                Log.w(TAG, e.toString(), e);
            } finally
            {
                t.interrupt();
                closeResources(vpnInput, vpnOutput);
            }
        }
    }
}
