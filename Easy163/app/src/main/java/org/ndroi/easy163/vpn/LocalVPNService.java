package org.ndroi.easy163.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.ndroi.easy163.R;
import org.ndroi.easy163.core.Cache;
import org.ndroi.easy163.core.Local;
import org.ndroi.easy163.core.Server;
import org.ndroi.easy163.ui.MainActivity;
import org.ndroi.easy163.utils.EasyLog;
import org.ndroi.easy163.vpn.bio.BioTcpHandler;
import org.ndroi.easy163.vpn.bio.BioUdpHandler;
import org.ndroi.easy163.vpn.config.Config;
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
    private Boolean isRunning = false;
    private static Context context = null;

    public static Context getContext()
    {
        return context;
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
        LocalBroadcastManager.getInstance(this).registerReceiver(stopReceiver, new IntentFilter("activity"));
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
        EasyLog.log("Easy163 VPN 开始运行");
        EasyLog.log("版本更新关注 Github Release");
        Cache.init();
        Local.load();
        isRunning = true;
        Log.i(TAG, "Easy163 VPN 开始运行");
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
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.icon);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 100, intent, 0);
        Notification.Builder builder = new Notification.Builder(this)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.icon)
                .setLargeIcon(icon)
                .setContentTitle("Easy163")
                .setContentText("Easy163 正在运行...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            builder.setChannelId(notificationId);
        }
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
        sendState();
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        executorService.shutdownNow();
        cleanup();
        isRunning = false;
        EasyLog.log("Easy163 VPN 停止运行");
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
        Intent replyIntent=  new Intent("service");
        replyIntent.putExtra("isRunning", isRunning);
        LocalBroadcastManager.getInstance(this).sendBroadcast(replyIntent);
    }

    // TODO: Move this to a "utils" class for reuse
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
                while (true)
                {
                    try
                    {
                        ByteBuffer bufferFromNetwork = networkToDeviceQueue.take();
                        bufferFromNetwork.flip();

                        while (bufferFromNetwork.hasRemaining())
                        {
                            int w = vpnOutput.write(bufferFromNetwork);
                            if (w > 0)
                            {
                                //MainActivity.downByte.addAndGet(w);
                            }

                            if (Config.logRW)
                            {
                                Log.d(TAG, "vpn write " + w);
                            }
                        }
                    } catch (Exception e)
                    {
                        Log.i(TAG, "WriteVpnThread fail", e);
                    }

                }

            }
        }

        @Override
        public void run()
        {
            //Log.i(TAG, "Started");

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
                        try
                        {
                            Thread.sleep(50);
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e)
            {
                Log.w(TAG, e.toString(), e);
            } finally
            {
                closeResources(vpnInput, vpnOutput);
            }
        }
    }
}

