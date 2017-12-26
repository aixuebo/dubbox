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
package com.alibaba.dubbo.rpc.proxy;

import java.lang.reflect.InvocationTargetException;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcResult;

/**
 * InvokerWrapper
 * 
 * @author william.liangf
 */
public abstract class AbstractProxyInvoker<T> implements Invoker<T> {
    
    private final T proxy;//debug  com.alibaba.dubbo.demo.user.facade.AnnotationDrivenUserRestServiceImpl@48931b44
    
    private final Class<T> type;//debug interface com.alibaba.dubbo.demo.user.facade.UserRestService
    
    private final URL url;//debug injvm://127.0.0.1/services/com.alibaba.dubbo.demo.user.facade.UserRestService?accepts=500&anyhost=true&application=demo-provider&dubbo=2.0.0&extension=com.alibaba.dubbo.demo.extension.TraceInterceptor,                     com.alibaba.dubbo.demo.extension.TraceFilter,                     com.alibaba.dubbo.demo.extension.ClientTraceFilter,                     com.alibaba.dubbo.demo.extension.DynamicTraceBinding,                     com.alibaba.dubbo.demo.extension.CustomExceptionMapper,                     com.alibaba.dubbo.rpc.protocol.rest.support.LoggingFilter&generic=false&group=annotationConfig&interface=com.alibaba.dubbo.demo.user.facade.UserRestService&methods=getUser,registerUser&organization=dubbox&owner=programmer&pid=7340&server=tomcat&side=provider&threads=500&timestamp=1514256952105&validation=true

    public AbstractProxyInvoker(T proxy, Class<T> type, URL url){
        if (proxy == null) {
            throw new IllegalArgumentException("proxy == null");
        }
        if (type == null) {
            throw new IllegalArgumentException("interface == null");
        }
        if (! type.isInstance(proxy)) {
            throw new IllegalArgumentException(proxy.getClass().getName() + " not implement interface " + type);
        }
        this.proxy = proxy;
        this.type = type;
        this.url = url;
    }

    public Class<T> getInterface() {
        return type;
    }

    public URL getUrl() {
        return url;
    }

    public boolean isAvailable() {
        return true;
    }

    public void destroy() {
    }

    public Result invoke(Invocation invocation) throws RpcException {
        try {
            return new RpcResult(doInvoke(proxy, invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArguments()));
        } catch (InvocationTargetException e) {
            return new RpcResult(e.getTargetException());
        } catch (Throwable e) {
            throw new RpcException("Failed to invoke remote proxy method " + invocation.getMethodName() + " to " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }
    
    protected abstract Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable;

    @Override
    public String toString() {
        return getInterface() + " -> " + getUrl()==null?" ":getUrl().toString();
    }

    
}