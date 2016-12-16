package com.mizofumi.androidserialsample2;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by mizofumi on 2016/11/18.
 */

public class Serial {
    SerialListener serialListener;
    boolean StopFlag = false;
    boolean Connected = false;
    boolean Runnable = false;
    BluetoothAdapter mAdapter;
    BluetoothDevice mDevice;
    BluetoothSocket mSocket;

    private InputStream mmInStream;
    private OutputStream mmOutputStream;



    public Serial() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void connectDevice(final String DeviceName, final UUID uuid){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Set< BluetoothDevice > devices = mAdapter.getBondedDevices();
                for ( BluetoothDevice device : devices){
                    if(device.getName().equals(DeviceName)){
                        mDevice = device;
                    }
                }
                try {
                    if (mDevice != null){
                        mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mSocket.connect();
                        if (!mSocket.isConnected()){
                            throw new IOException("デバイスの接続に失敗しました");
                        }else {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    serialListener.device_connect_success();
                                }
                            });
                        }
                    }else {
                        throw new IOException("デバイスが見つかりませんでした");
                    }
                }catch (final IOException e){
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            serialListener.device_connect_faild(e.getMessage());
                        }
                    });
                }
            }
        }).start();

    }

    public Set<BluetoothDevice> getDevices(){
        return mAdapter.getBondedDevices();
    }

    public void setDevice(BluetoothDevice mDevice) {
        this.mDevice = mDevice;
    }

    public void setSerialListener(SerialListener serialListener) {
        this.serialListener = serialListener;
    }

    public void open() throws NullPointerException{
        if (serialListener == null){
            throw new NullPointerException("SerialListenerが定義されていません");
        }
        try {
            mmInStream = mSocket.getInputStream();
            mmOutputStream = mSocket.getOutputStream();
            serialListener.opened();
            Connected = true;
        } catch (IOException e) {
            e.printStackTrace();
            serialListener.open_failed("IOエラー:"+e.getMessage());
            Connected = false;
        }
    }

    public void run() throws NullPointerException{
        if (serialListener == null){
            throw new NullPointerException("SerialListenerが定義されていません");
        }
        new Thread(new Runnable() {
            String readMsg;

            @Override
            public void run() {
                while (!StopFlag){
                    Runnable = true;
                    final byte buf[] = new byte[1024];
                    try {
                        final int num = mmInStream.read(buf);
                        readMsg = new String(buf, 0, num);
                        if(readMsg.trim() != null && !readMsg.trim().equals("")){
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    serialListener.read(readMsg);
                                }
                            });
                        }
                        else{
                            serialListener.read_failed("NoData");
                            // Log.i(TAG,"value=nodata");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        serialListener.read_failed("IOエラー:"+e.getMessage());
                    }
                }
                Runnable = false;
            }
        }).start();
    }

    public void stop() throws NullPointerException{
        if (serialListener == null){
            throw new NullPointerException("SerialListenerが定義されていません");
        }
        Runnable = false;
        StopFlag = true;
        serialListener.stoped();
    }

    public void close() throws NullPointerException{
        if (serialListener == null){
            throw new NullPointerException("SerialListenerが定義されていません");
        }
        try {
            mSocket.close();
            serialListener.closed();
            Connected = false;
        } catch (IOException e) {
            e.printStackTrace();
            serialListener.close_failed("IOエラー:"+e.getMessage());
        }
    }

    public void write(byte[] bytes) throws NullPointerException{
        if (serialListener == null){
            throw new NullPointerException("SerialListenerが定義されていません");
        }
        try {
            mmOutputStream.write(bytes);
            serialListener.write_success();
        } catch (IOException e) {
            e.printStackTrace();
            serialListener.write_failed("IOエラー:"+e.getMessage());
        }
    }

    public void write(byte[] bytes,int offset,int length) throws NullPointerException{
        if (serialListener == null){
            throw new NullPointerException("SerialListenerが定義されていません");
        }
        try {
            mmOutputStream.write(bytes,offset,length);
            serialListener.write_success();
        } catch (IOException e) {
            e.printStackTrace();
            serialListener.write_failed("IOエラー:"+e.getMessage());
        }
    }

    public void write(int b) throws NullPointerException{
        if (serialListener == null){
            throw new NullPointerException("SerialListenerが定義されていません");
        }
        try {
            mmOutputStream.write(b);
            serialListener.write_success();
        } catch (IOException e) {
            e.printStackTrace();
            serialListener.write_failed("IOエラー:"+e.getMessage());
        }
    }


    public boolean isConnected() {
        return Connected;
    }

    public boolean isRunnable() {
        return Runnable;
    }
}
