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
    private HeartBeat heartBeat;
    public MainService() {
        this.executorService = Executors.newFixedThreadPool(10);
        this.playerBinder = new PlayerBinder();
        this.timer = new Timer();
        System.out.println(TAG+"MainService   我实例了");
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
            if(executorService==null||!executorService.isShutdown()){
                //重新建立一个线程池 以便用于提交数据
                executorService=Executors.newFixedThreadPool(10);
            }
            return startThread();
        }
        void exitThread() {
            try {
                //关闭socket 防止pos端口占用
                if (socket!=null){
                    socket.close();
                }

                //退出心跳线程 因为持有socke的引用 线程池也无法关闭，只能自己本身退出
                heartBeat.isExit=true;
                //关闭线程池
                executorService.shutdownNow();

                //因为线程池关闭后 无法再提交Task  所以 回收
                executorService=null;

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

    //开启线程，提交到线程池 返回结果  连接结果
    Boolean startThread() {
        Future<Map<String, Object>> future = executorService.submit(new SocketConnect("10.132.255.170", 2000));
        try {
            //线程结束运行时，返回的结果
            Map<String, Object> result = future.get();
            if (!result.get("isSuccess").equals(false) && !result.get("isSuccess").toString().equals("false")) {
                 socket=(Socket)result.get("isSuccess");
                 //心跳线程持有socket引用 所以需要本身自己关闭自己
                 heartBeat=new HeartBeat(socket);
                 //提交到线程池
                executorService.execute(heartBeat);
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
            long first=0;//记录上一次成功发送心跳包的时间
            long second;//记录现在的时间
            OutputStream outputStream = null;
            DataOutputStream dataOutputStream;
            while (!isExit) {
                if (socket != null && !socket.isClosed()) {
                     //记录当前时间 赋值
                    second=System.currentTimeMillis();
//                    System.out.println("时间间隔"+(second-first));
                    //当前时间和上一次时间之差大于5秒 则发送
                    if((second-first)>5000){
                        try {
                            if (outputStream==null){
                                outputStream=socket.getOutputStream();
                            }
                            dataOutputStream = new DataOutputStream(outputStream);
                            dataOutputStream.writeUTF("xintiao");
                            dataOutputStream.flush();
                            System.out.println("HeartBeat   心跳包发送" + System.currentTimeMillis());
                            first=second;
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("HeartBeat  心跳包异常");
                            anew(socket);
                        }
                    }
                } else {
                    System.out.println("HeartBeat  socket关闭");
                    anew(socket);
                }
            }
        }
    }

    //重新启动线程连接Socket
    public void anew(Socket socket){
//        System.out.println(executorService.isShutdown()+" "+executorService.isTerminated());
        if (executorService!=null&&!executorService.isShutdown()) {
            if (socket!=null) {

                try {
                    socket.close();
                    socket=null;
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(TAG+" anew "+"关闭Socket出错");
                }
            }
            System.out.println(TAG+"  anew "+ "重新启动线程连接Socket");
            if (!startThread()) {
                anew(socket);
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

                       if (inputStream.available()!=-1) {
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
                           StringBuilder stringBuffer=new StringBuilder();
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
                           System.out.println(TAG+"  ReadTread "+"获得的数据"+stringBuffer.toString());

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
                       }


                   } catch (IOException e) {
                       System.out.println(TAG+"    ReadTread   数据读取线程出现异常");
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
                System.out.println(TAG+" sendDataImp  sendData 出错");
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
