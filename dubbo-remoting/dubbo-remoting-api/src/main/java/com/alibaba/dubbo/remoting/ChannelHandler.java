/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.common.extension.SPI;


/**
 * ChannelHandler. (API, Prototype, ThreadSafe)
 * 
 * @see com.alibaba.dubbo.remoting.Transporter#bind(com.alibaba.dubbo.common.URL, ChannelHandler)
 * @see com.alibaba.dubbo.remoting.Transporter#connect(com.alibaba.dubbo.common.URL, ChannelHandler)
 * @author qian.lei
 * @author william.liangf
 */
@SPI
public interface ChannelHandler {

    /**
     * on channel connected.
     * 建立一个通道
     * @param channel channel.
     */
    void connected(Channel channel) throws RemotingException;

    /**
     * on channel disconnected.
     * 关闭一个通道
     * @param channel channel.
     */
    void disconnected(Channel channel) throws RemotingException;

    /**
     * on message sent.
     * 向某一个通道发送信息
     * @param channel channel.
     * @param message message.
     */
    void sent(Channel channel, Object message) throws RemotingException;

    /**
     * on message received.
     * 从某个通道收到信息
     * @param channel channel.
     * @param message message.
     */
    void received(Channel channel, Object message) throws RemotingException;

    /**
     * on exception caught.
     * 某个通道产生异常
     * @param channel channel.
     * @param exception exception.
     */
    void caught(Channel channel, Throwable exception) throws RemotingException;

}