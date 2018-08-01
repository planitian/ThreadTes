package com.example.administrator.threadtes;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainService extends Service {
    private ExecutorService executorService;
    private PlayerBinder playerBinder;
    private Timer timer;
    private String TAG=this.getClass().getSimpleName();
    private Socket socket;

    public MainService() {
        this.executorService = Executors.newFixedThreadPool(10);
        this.playerBinder = new PlayerBinder();
        this.timer = new Timer();
        System.out.println(TAG+"我实例了");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return playerBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public class PlayerBinder extends Binder {
        boolean start() {
            return startThread();
        }
        void exitThread() {
            try {
                socket.close();
                executorService.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        boolean sendData(String json) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendDataImp(json);
                }
            }).start();
            return true;
        }

    }

    Boolean startThread() {
        Future<Map<String, Object>> future = executorService.submit(new SocketConnect("10.132.255.170", 2000));
        try {
            Map<String, Object> result = future.get();
            if (!result.get("isSuccess").equals(false) && !result.get("isSuccess").toString().equals("false")) {
                 socket=(Socket)result.get("isSuccess");
                executorService.execute( new HeartBeat(socket));
                executorService.execute(new ReadTread(socket));
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    //心跳包线程
    public class HeartBeat implements Runnable {
        private Socket socket;
        volatile boolean isExit = false;

        HeartBeat(Socket socket) {
            this.socket = socket;
        }
        @Override
        public void run() {
            long first=0;
            long second;
            OutputStream outputStream = null;
            DataOutputStream dataOutputStream;
            while (!isExit) {
                if (socket != null && !socket.isClosed()) {

                    second=System.currentTimeMillis();
//                    System.out.println("时间间隔"+(second-first));
                    if((second-first)>5000){
                        try {
                            if (outputStream==null){
                                outputStream=socket.getOutputStream();
                            }
                            dataOutputStream = new DataOutputStream(outputStream);
                            dataOutputStream.writeUTF("xintiao");
                            dataOutputStream.flush();
                            System.out.println("心跳包发送" + System.currentTimeMillis());
                            first=second;

                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("心跳包异常");
                            anew(socket);
                        }
                    }
                } else {
                    System.out.println("socket关闭");
                    anew(socket);
                }
            }
        }
    }

    //重新启动线程连接Socket
    public void anew(Socket socket){
        if (socket!=null) {
            try {
                socket.close();
                System.out.println(TAG+" "+ "重新启动线程连接Socket");
                if (!startThread()) {
                    anew(socket);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(TAG+"  "+"重新启动Socket线程出错");
            }
        }
    }

   //读取数据线程
    class ReadTread implements Runnable {
        volatile boolean isExit=false;
        private Socket socket;

       public ReadTread(Socket socket) {
           this.socket = socket;
       }

       @Override
        public void run() {
           InputStream inputStream;
           while (!isExit){
               if (socket!=null&&!socket.isClosed()){
                   try {
                       int n=1;
                       int size=10;
                       inputStream=socket.getInputStream();
                       byte[] temp=new byte[size];
//                       ArrayList<byte> shhh=new ArrayList<byte>();
                       BufferedInputStream bufferedInputStream=new BufferedInputStream(inputStream);

                       ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
                       int len=0;
//                       while ((len=bufferedInputStream.read(temp))!=-1){
//                           System.out.println("读取数据线程"+len);
//                           byteArrayOutputStream.write(temp,0,len);
//                           byteArrayOutputStream.flush();
//                       }
                       StringBuffer stringBuffer=new StringBuffer();
                       while ((len=bufferedInputStream.read(temp))>=size){
                           String tempString=new String(Arrays.copyOf(temp,
                                   len)).trim();
                           stringBuffer.append(tempString);
                           temp=null;
                           temp=new byte[(n+1)*len];
                           size=size*2;
                       }
                       String gainData=new String(Arrays.copyOf(temp,
                               len)).trim();
                       stringBuffer.append(gainData);
                       System.out.println(TAG+" "+"获得的数据"+stringBuffer.toString());

//                       if ((len=bufferedInputStream.read(temp))>0){
//                            String gainData=new String(Arrays.copyOf(temp,
//                                    len)).trim();
//                           System.out.println(TAG+" "+"获得的数据"+gainData+len);
//                       }
//                       byte[] temp1=new byte[10];
//                       if (len==10){
//                           if ((len=bufferedInputStream.read(temp1))>0){
//                               String gainData=new String(Arrays.copyOf(temp,
//                                       len)).trim();
//                               System.out.println(TAG+" "+"获得的数据22"+gainData+len);
//                           }
//                       }
//                       String gainData= byteArrayOutputStream.toString();

//                       DataInputStream  dataInputStream=new DataInputStream(inputStream);
//                       String gainData=dataInputStream.readUTF();
//                       System.out.println(TAG+" 获得的数据 "+gainData );
//                       eventbus 发送


                   } catch (IOException e) {
                       System.out.println(TAG+"数据读取线程出现异常");
                       e.printStackTrace();
                       anew(socket);
                   }
               }
           }
        }
    }

   Boolean sendDataImp(String data){
        if (socket!=null&&!socket.isClosed()){

            try {
                OutputStream outputStream=socket.getOutputStream();
                DataOutputStream dataOutputStream=new DataOutputStream(outputStream);
                dataOutputStream.writeUTF(data);
                dataOutputStream.flush();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(TAG+"sendData 出错");
                return false;
            }
        }else {
            boolean  success;
            if (executorService.isShutdown()){
                executorService.shutdownNow();
                 success= startThread();
            }else {
                success= startThread();
            }
            return success;
        }
   }

}
