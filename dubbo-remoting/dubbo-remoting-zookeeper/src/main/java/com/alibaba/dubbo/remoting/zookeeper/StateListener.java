package com.alibaba.dubbo.remoting.zookeeper;

//当发生zookeeper的客户端状态变化的时候,如何处理
public interface StateListener {

	int DISCONNECTED = 0;

	int CONNECTED = 1;

	int RECONNECTED = 2;

	void stateChanged(int connected);

}
