package com.example.administrator.threadtes;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class SocketConnect  implements Callable<Map<String,Object>> {
    private Socket socket;
    private int port;
    private String host;
    private String TAG="SocketConnect ";
    public SocketConnect( String host,  int port) {
        this.port = port;
        this.host=host;
    }

    @Override
    public Map<String, Object> call() throws Exception {
        Map<String, Object> resultCall = new HashMap<>();
        Process process = Runtime.getRuntime().exec("ping -w 2" + " " + host);//判断是否连接的IP;
        int status = process.waitFor();
        System.out.println(TAG + "IP地址是否ping通" + status);
        if (status == 0) {
            try {
                if (socket!=null){
                    socket=null;
                }
                socket = new Socket();
                socket.setKeepAlive(true);
                socket.setSoTimeout(1000000);
                SocketAddress socketAddress = new InetSocketAddress(host, port);
                socket.connect(socketAddress, 1000);
                System.out.println("是否连接成功" + socket.isConnected());
                if (socket.isConnected()) {
                    //
                    resultCall.put("isSuccess", socket);
                    return resultCall;
                } else {
                    resultCall.put("isSuccess", false);
                    return resultCall;
                }
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                System.out.println(TAG + "  Socket端口被占用 建议重启pos端");
                resultCall.put("isSuccess", false);
                return resultCall;
            } catch (IOException e) {
                e.printStackTrace();
                resultCall.put("isSuccess", false);
                return resultCall;
            }
        } else {
            resultCall.put("isSuccess", false);
            return resultCall;
        }
    }
}
