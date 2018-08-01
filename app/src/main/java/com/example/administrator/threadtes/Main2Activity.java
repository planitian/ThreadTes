package com.example.administrator.threadtes;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class Main2Activity extends AppCompatActivity {
    private MainService.PlayerBinder playerBinder;
    @BindView(R.id.connect)
    Button connect;
    @BindView(R.id.send_text)
    EditText sendText;
    @BindView(R.id.send)
    Button send;
    @BindView(R.id.receive)
    TextView receive;
    @BindView(R.id.port)
    EditText port;
    @BindView(R.id.canel)
    Button canel;

    private ServiceConnection serviceConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            playerBinder= (MainService.PlayerBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            System.out.println("onServiceDisconnected");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Intent intent=new Intent(Main2Activity.this,MainService.class);
        bindService(intent,serviceConnection,BIND_AUTO_CREATE);
        EventBus.getDefault().register(this);

//        connect.setOnClickListener((view)->{
//            System.out.println("dianji l ");
//            boolean isSuccess=playerBinder.start();
//            Toast.makeText(Main2Activity.this,String.valueOf(isSuccess),Toast.LENGTH_SHORT).show();
//        });
//        send.setOnClickListener((v)->{
//            boolean isSuccess=playerBinder.sendData(sendText.getText().toString().trim());
//            Toast.makeText(Main2Activity.this,String.valueOf(isSuccess),Toast.LENGTH_SHORT).show();
//        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        EventBus.getDefault().unregister(this);
    }

    @OnClick(R.id.connect)
    public void setConnect(){
        System.out.println("dianji l ");
        boolean isSuccess=playerBinder.start();
        Toast.makeText(Main2Activity.this,String.valueOf(isSuccess),Toast.LENGTH_SHORT).show();
    }
    @OnClick(R.id.send)
    public void setSend(){
        boolean isSuccess=playerBinder.sendData("{\"shopno\":\"001\",\"posno\":\"91\",\"operators\":\"\",\"password\":\"\",\"cashier\":\"1018\",\"cashierpwd\":\"1234\",\"INTERFACETYPE\":\"LOGINCERTIFY\",\"flag\":\"0\"}");
        Toast.makeText(Main2Activity.this,String.valueOf(isSuccess),Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.canel)
    public void setCanel(){
        playerBinder.exitThread();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showData(String data){
         receive.setText(data);
    }

}
