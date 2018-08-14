package com.example.administrator.threadtes;

import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {
    private Socket socket;
    private EditText port;
    private Button send;
    private Button connecte;
    private EditText sendText;
    private TextView receive;
    private ExecutorService executorService;
    private int n = 1;
    private Button cancel;
    private InputStream inputStream;
    private OutputStream outputStream;
    private String TAG=this.getClass().getSimpleName();
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                receive.setText(msg.obj + "   " + n);
                n++;
            }
            if (msg.what==2){
                cancel.performClick();
                connecte.performClick();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println(">>>>>>>>>>>>"+TAG);
        inti();
        executorService = Executors.newCachedThreadPool();

        connecte.setOnClickListener((View v) -> {
//            SocketConnect connect = new SocketConnect(socketWeakReference, Integer.parseInt(port.getText().toString().trim()));
            SocketConnect connect=new SocketConnect("192.168.43.10",9999);
            Future<Map<String,Object>> future = executorService.submit(connect);
            try {
                Map<String,Object> result=future.get();
                if (!result.get("isSuccess").equals(false)&& !result.get("isSuccess").toString().equals("false")){
                    socket= (Socket) result.get("isSuccess");
                }else {connecte.setClickable(false);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        send.setOnClickListener((v) -> {
            Future<Boolean> future = executorService.submit(() -> {
                String sendData = sendText.getText().toString().trim();
                outputStream = socket.getOutputStream();
                if (outputStream != null) {
                    DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                    dataOutputStream.writeUTF(sendData);
                    dataOutputStream.flush();
//                dataOutputStream.close();
//                socketWeakReference.get().close();
                    System.out.println("发送了");
                    return true;
                } else {
                    return false;
                }

            });
            try {
                Toast.makeText(MainActivity.this, future.get().toString(), Toast.LENGTH_SHORT).show();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        executorService.execute(() -> {

            while (true) {
                if (socket!= null&&!socket.isClosed()&&!socket.isConnected()) {
//                    System.out.println("不为空");
//                    System.out.println("socketWeakReference.get().isConnected()"+socketWeakReference.get().isConnected());
                        try {
                            inputStream = socket.getInputStream();
                            if (inputStream != null && inputStream.available() != 0) {
                                DataInputStream dataInputStream = new DataInputStream(inputStream);
                                String date = dataInputStream.readUTF();
                                Message message = Message.obtain();
                                message.obj = date;
                                message.what = 1;
                                handler.sendMessage(message);
                                System.out.println("服务器数据" + date);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                }
            }
        });

        //心跳
        executorService.execute(() -> {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (socket!=null&&!socket.isClosed()) {
                        try {
                            outputStream = socket.getOutputStream();
                            if (outputStream != null) {
                                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                                dataOutputStream.writeUTF("xintiao");
                                dataOutputStream.flush();
                                System.out.println("心跳包发送"+System.currentTimeMillis());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Message message=Message.obtain();
                            message.what=2;
                            handler.sendMessage(message);
                            System.out.println("重新连接");
                            timer.cancel();
                        }
                    }
                }
            }, 0, 20000);
        });

        cancel.setOnClickListener((view) -> {

            if (socket != null && !socket.isClosed()&&!socket.isConnected()) {
                    try {
                        if (inputStream != null && outputStream != null) {
                            inputStream.close();
                            outputStream.close();
                        }
                        socket.close();
                        socket = null;
                        System.gc();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

            }

        });
    }


    public void inti() {
        port = findViewById(R.id.port);
        send = findViewById(R.id.send);
        sendText = findViewById(R.id.send_text);
        receive = findViewById(R.id.receive);
        connecte = findViewById(R.id.connect);
        cancel = findViewById(R.id.canel);
    }

}



