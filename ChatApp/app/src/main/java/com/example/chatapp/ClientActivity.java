package com.example.chatapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.yuyh.library.imgsel.ISNav;
import com.yuyh.library.imgsel.common.ImageLoader;
import com.yuyh.library.imgsel.config.ISListConfig;

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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class ClientActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MsgAdapter adapter;
    private List<Msg> list;
    private EditText msg_et;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        msg_et = (EditText) findViewById(R.id.msgSend_et);
        // 自定义图片加载器
        ISNav.getInstance().init(new ImageLoader() {
            @Override
            public void displayImage(Context context, String path, ImageView imageView) {
                Glide.with(context).load(path).into(imageView);
            }
        });

        findViewById(R.id.send_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!msg_et.getText().toString().equals("")){
                    sendMsg(msg_et.getText().toString());
                }
            }
        });
        findViewById(R.id.photo_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                sendPhoto();
//                sendPhoto(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath()+"/a.jpg");
//                if (true)return;
                // 自由配置选项
                ISListConfig config = new ISListConfig.Builder()
                        // 是否多选, 默认true
                        .multiSelect(false)
                        // 是否记住上次选中记录, 仅当multiSelect为true的时候配置，默认为true
                        .rememberSelected(false)
                        // “确定”按钮背景色
                        .btnBgColor(Color.GRAY)
                        // “确定”按钮文字颜色
                        .btnTextColor(Color.BLUE)
                        // 使用沉浸式状态栏
                        .statusBarColor(Color.parseColor("#3F51B5"))
                        // 返回图标ResId
//                        .backResId(R.mipmap.ic_launcher)
                        // 标题
                        .title("图片")
                        // 标题文字颜色
                        .titleColor(Color.WHITE)
                        // TitleBar背景色
                        .titleBgColor(Color.parseColor("#3F51B5"))
                        // 裁剪大小。needCrop为true的时候配置
                        .cropSize(1, 1, 200, 200)
                        .needCrop(false)
                        // 第一个是否显示相机，默认true
                        .needCamera(false)
                        // 最大选择图片数量，默认9
                        .maxNum(9)
                        .build();
                // 跳转到图片选择器
                ISNav.getInstance().toListActivity(ClientActivity.this, config, 1000);
            }
        });
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        list = new ArrayList<>();
        adapter = new MsgAdapter(this, list);
        recyclerView.setAdapter(adapter);
        new photoReceiveThread().start();
        new msgReceiveThread().start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 图片选择结果回调
        if (requestCode == 1000 && resultCode == RESULT_OK && data != null) {
            List<String> pathList = data.getStringArrayListExtra("result");
            for (final String path : pathList) {
                Log.i("ljl","ssss"+ path);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sendPhoto(path);
                    }
                });
            }
        }
    }

    void sendMsg(final String str){
        msg_et.setText("");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//                    Socket s = new Socket("192.168.0.111", 12345);
                    Socket s = new Socket(MainActivity.host, 3000);
                    // outgoing stream redirect to socket
                    OutputStream out = s.getOutputStream();
                    // 注意第二个参数据为true将会自动flush，否则需要需要手动操作out.flush()
                    PrintWriter output = new PrintWriter(out, true);
                    output.println(MainActivity.name+"!@#"+str);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Msg msg = new Msg();
                            msg.type = 0;
                            msg.msg = str;
                            list.add(msg);
                            adapter.notifyItemInserted(list.size()-1);
                            recyclerView.scrollToPosition(list.size()-1);
                        }
                    });
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

    class Msg{
        String msg;
        int type;
        String photoPath;
    }

    class MsgAdapter extends RecyclerView.Adapter {
        private Context context;
        /**
         * 存放消息内容的列表
         */
        private List<Msg> msgList;

        public MsgAdapter(Context context, List<Msg> msgList){
            this.context = context;
            this.msgList = msgList;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType){
                case 0:
                    return new MsgSendViewHolder(LayoutInflater.from(context).inflate(R.layout.item_send, parent,
                            false));
                case 1:
                    return new MsgReceiveViewHolder(LayoutInflater.from(context).inflate(R.layout.item_receive, parent,
                            false));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            if (holder instanceof MsgSendViewHolder){
                MsgSendViewHolder mHolder = (MsgSendViewHolder) holder;
                mHolder.msgSend.setText(msgList.get(position).msg);
                if (msgList.get(position).photoPath!=null){
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 3;
                    Bitmap bm = BitmapFactory.decodeFile(msgList.get(position).photoPath, options);
                    mHolder.photoSend.setImageBitmap(bm);
                }else {
                    mHolder.photoSend.setVisibility(View.GONE);
                }
            }else if (holder instanceof  MsgReceiveViewHolder){
                MsgReceiveViewHolder mHolder = (MsgReceiveViewHolder) holder;
                if (msgList.get(position).msg!=null){
                    int i = msgList.get(position).msg.indexOf("!@#");
                    if (!msgList.get(position).msg.endsWith("!@#")){
                        mHolder.msgReceive.setText(msgList.get(position).msg.substring(i+3));
                    }else {
                        mHolder.msgReceive.setText("");
                    }
                    mHolder.nameRecerive.setText(msgList.get(position).msg.substring(0,i));
                }else {
                    mHolder.msgReceive.setText("");
                    mHolder.nameRecerive.setText("");
                }
                if (msgList.get(position).photoPath!=null){
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 3;
                    Bitmap bm = BitmapFactory.decodeFile(msgList.get(position).photoPath, options);
                    mHolder.photoReceive.setImageBitmap(bm);
                }else {
                    mHolder.photoReceive.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            return msgList.get(position).type;
        }

        @Override
        public int getItemCount() {
            return msgList.size();
        }

        class MsgSendViewHolder extends RecyclerView.ViewHolder{
            TextView msgSend;
            ImageView photoSend;
            public MsgSendViewHolder(View itemView) {
                super(itemView);
                msgSend = (TextView) itemView.findViewById(R.id.msg_item_send);
                photoSend = (ImageView) itemView.findViewById(R.id.photo_item_send);
            }
        }

        class MsgReceiveViewHolder extends RecyclerView.ViewHolder{
            TextView msgReceive;
            ImageView photoReceive;
            TextView nameRecerive;
            public MsgReceiveViewHolder(View itemView) {
                super(itemView);
                msgReceive = (TextView) itemView.findViewById(R.id.msg_item_receive);
                photoReceive = (ImageView) itemView.findViewById(R.id.photo_item_receive);
                nameRecerive = (TextView) itemView.findViewById(R.id.name_item_receive);
//                photoReceive.setMaxHeight(manager.getDefaultDisplay().getHeight());
            }
        }
    }


    private void sendPhoto(final String path){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//                    Socket s = new Socket("192.168.0.111", 12345);
                    Socket s = new Socket(MainActivity.host, 3000);
                    // outgoing stream redirect to socket
                    OutputStream out = s.getOutputStream();
                    // 注意第二个参数据为true将会自动flush，否则需要需要手动操作out.flush()
                    PrintWriter output = new PrintWriter(out, true);
                    output.println(MainActivity.name+"!@#");
                    s.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
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
                        socket.connect(new InetSocketAddress(MainActivity.host, 1234),30 * 1000);
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Msg msg = new Msg();
                                msg.type = 0;
                                msg.photoPath = path;
                                list.add(msg);
                                adapter.notifyItemInserted(list.size()-1);
                                recyclerView.scrollToPosition(list.size()-1);
                            }
                        });
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
    class msgReceiveThread extends Thread{
        @Override
        public void run() {
            super.run();
            try {
                Boolean endFlag = false;
//                ServerSocket ss = new ServerSocket(4000);
                while (!endFlag) {
                    // 等待客户端连接
                    ServerSocket ss = new ServerSocket(4000);
                    Socket s = ss.accept();
                    Log.i("ljl",s.getInetAddress().getHostAddress());
                    Log.i("ljl",s.getInetAddress().getHostName());
                    BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    final String message = input.readLine();
                    Log.d("Tcp Demo", "message from Client:"+message);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Msg msg = new Msg();
                            msg.type = 1;
                            msg.msg = message;
                            list.add(msg);
                            adapter.notifyItemInserted(list.size()-1);
                            recyclerView.scrollToPosition(list.size()-1);
                        }
                    });
                    if("shutDown".equals(message)){
                        endFlag=true;
                    }
                    s.close();
                    ss.close();
                }
//                ss.close();


            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class photoReceiveThread extends Thread{
        @Override
        public void run() {
            super.run();
            try {
                final ServerSocket server = new ServerSocket(5000);
                while (true) {
                    try {
                        Log.i("ljl","开始监听......");
                        Socket socket = server.accept();
                        Log.i("ljl","有链接......");
                        receiveFile(socket);
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void receiveFile(Socket socket) {
        byte[] inputByte = null;
        int length = 0;
        DataInputStream dis = null;
        FileOutputStream fos = null;
        try {
            try {
                dis = new DataInputStream(socket.getInputStream());
                final File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath()+"/a.jpg");
                fos = new FileOutputStream(file);
                inputByte = new byte[1024*4];
                Log.i("ljl","开始接收数据...");
                while ((length = dis.read(inputByte, 0, inputByte.length)) > 0) {
                    fos.write(inputByte, 0, length);
                    fos.flush();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (list.size()>0){
                            if (list.get(list.size()-1).msg.endsWith("!@#")){
                                list.get(list.size()-1).photoPath = file.getAbsolutePath();
                                adapter.notifyItemChanged(list.size()-1);
                            }else {
                                Msg msg = new Msg();
                                msg.type = 1;
                                msg.photoPath = file.getAbsolutePath();
                                list.add(msg);
                                adapter.notifyItemInserted(list.size()-1);
                            }
                        }
//                        if (list.get(list.size()-1).msg.endsWith("!@#")){
//                            list.get(list.size()-1).photoPath = file.getAbsolutePath();
//                        }else {
//                            Msg msg = new Msg();
//                            msg.type = 1;
//                            msg.photoPath = file.getAbsolutePath();
//                            list.add(msg);
//                        }
//                        Msg msg = new Msg();
//                        msg.type = 1;
//                        msg.photoPath = file.getAbsolutePath();
//                        list.add(msg);
//                        adapter.notifyItemInserted(list.size()-1);
                        recyclerView.scrollToPosition(list.size()-1);
                    }
                });
                Log.i("ljl","完成接收");
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

    }


}
