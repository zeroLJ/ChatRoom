package com.example.chatapp;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ServerActivity extends AppCompatActivity {
    public List<String> hostList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        ((TextView) findViewById(R.id.ip_tv)).setText(getHostIP());
        new hostReceiveThread().start();
        new msgReceiveThread().start();
        new photoReceiveThread().start();
    }

    class hostReceiveThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                Boolean endFlag = false;
                ServerSocket ss = new ServerSocket(2000);
                while (!endFlag) {
                    // 等待客户端连接
                    Socket s = ss.accept();
                    BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    final String message = input.readLine();
                    if (!hostList.contains(message)) {
                        hostList.add(message);
                    }
                    Log.i("ljl", "ip:" + message);
                    Log.d("Tcp Demo", "message from Client:" + message);
                    if ("shutDown".equals(message)) {
                        endFlag = true;
                    }
                    s.close();
                }
                ss.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class msgReceiveThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                Boolean endFlag = false;
                ServerSocket ss = new ServerSocket(3000);
                while (!endFlag) {
                    // 等待客户端连接
                    Socket s = ss.accept();
                    BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    final String message = input.readLine();
                    for (final String host : hostList) {
                        if (!host.equals(s.getInetAddress().getHostAddress())) {
                            sendMsg(host, message);
                        }
                    }
//                    sendMsg(s.getInetAddress().getHostAddress(), message);
                    if ("shutDown".equals(message)) {
                        endFlag = true;
                    }
                    s.close();
                }
                ss.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    void sendMsg(final String host, final String str) {
        Log.i("ljl", "host:" + host);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//                    Socket s = new Socket("192.168.0.111", 12345);
                    Socket s = new Socket(host, 4000);
                    Log.i("ljl", "send");
                    // outgoing stream redirect to socket
                    OutputStream out = s.getOutputStream();
                    // 注意第二个参数据为true将会自动flush，否则需要需要手动操作out.flush()
                    PrintWriter output = new PrintWriter(out, true);
                    output.println(str);

                    s.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    /**
     * 获取ip地址
     *
     * @return
     */
    public static String getHostIP() {

        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return hostIp;
    }

    class photoReceiveThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                final ServerSocket server = new ServerSocket(1234);
                while (true) {
                    try {
                        Log.i("ljl", "开始监听......");
                        Socket socket = server.accept();
                        Log.i("ljl", "有链接......");
                        byte[] inputByte = null;
                        int length = 0;
                        DataInputStream dis = null;
                        FileOutputStream fos = null;
                        try {
                            try {
                                dis = new DataInputStream(socket.getInputStream());
                                final File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/a.jpg");
                                fos = new FileOutputStream(file);
                                inputByte = new byte[1024 * 4];
                                Log.i("ljl", "开始接收数据...");
                                while ((length = dis.read(inputByte, 0, inputByte.length)) > 0) {
                                    fos.write(inputByte, 0, length);
                                    fos.flush();
                                }
                                for (final String host : hostList) {
                                    if (!host.equals(socket.getInetAddress().getHostAddress())) {
                                        sendPhoto(host, file.getAbsolutePath());
                                    }
                                }
                                Log.i("ljl", "完成接收");
                            } finally {
                                if (fos != null)
                                    fos.close();
                                if (dis != null)
                                    dis.close();
                                if (socket != null)
                                    socket.close();
                            }
                        } catch (Exception e) {

                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendPhoto(final String host, final String path){
        new Thread(new Runnable() {
            @Override
            public void run() {
                int length = 0;
                byte[] sendBytes = null;
                Socket socket = null;
                DataOutputStream dos = null;
                FileInputStream fis = null;
                try {
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(host, 5000),30 * 1000);
                        dos = new DataOutputStream(socket.getOutputStream());
                        final File file = new File(path);
//                        final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/a.jpg");
                        Log.i("ljl",new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/a.jpg").getAbsolutePath());
                        fis = new FileInputStream(file);
                        sendBytes = new byte[1024*4];
                        while ((length = fis.read(sendBytes, 0, sendBytes.length)) > 0) {
                            dos.write(sendBytes, 0, length);
                            dos.flush();
                        }
                        Log.i("ljl","发送完成");
                    } finally {
                        if (dos != null)
                            dos.close();
                        if (fis != null)
                            fis.close();
                        if (socket != null)
                            socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
