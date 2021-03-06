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
package com.alibaba.dubbo.rpc.proxy.javassist;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.bytecode.Proxy;
import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyFactory;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker;
import com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler;

/**
 * JavaassistRpcProxyFactory 

 * @author william.liangf
 */
public class JavassistProxyFactory extends AbstractProxyFactory {

    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }
    //proxy是具体实例对象com.alibaba.dubbo.demo.user.facade.AnnotationDrivenUserRestServiceImpl@6d176900,type是interface com.alibaba.dubbo.demo.user.facade.UserRestService,url是最终发布的服务registry://slavenode1:2181/com.alibaba.dubbo.registry.RegistryService?application=demo-provider&dubbo=2.0.0&export=rest%3A%2F%2F10.107.22.89%3A8888%2Fservices%2Fcom.alibaba.dubbo.demo.user.facade.UserRestService%3Faccepts%3D500%26anyhost%3Dtrue%26application%3Ddemo-provider%26dubbo%3D2.0.0%26extension%3Dcom.alibaba.dubbo.demo.extension.TraceInterceptor%2C+++++++++++++++++++++com.alibaba.dubbo.demo.extension.TraceFilter%2C+++++++++++++++++++++com.alibaba.dubbo.demo.extension.ClientTraceFilter%2C+++++++++++++++++++++com.alibaba.dubbo.demo.extension.DynamicTraceBinding%2C+++++++++++++++++++++com.alibaba.dubbo.demo.extension.CustomExceptionMapper%2C+++++++++++++++++++++com.alibaba.dubbo.rpc.protocol.rest.support.LoggingFilter%26generic%3Dfalse%26group%3DannotationConfig%26interface%3Dcom.alibaba.dubbo.demo.user.facade.UserRestService%26methods%3DgetUser%2CregisterUser%26organization%3Ddubbox%26owner%3Dprogrammer%26pid%3D13248%26server%3Dtomcat%26side%3Dprovider%26threads%3D500%26timestamp%3D1513683433191%26validation%3Dtrue&organization=dubbox&owner=programmer&pid=13248&registry=zookeeper&timestamp=1513683113665
    //返回值是AbstractProxyInvoker,而AbstractProxyInvoker就是继承自Invoker,因此可以解释成返回值就是Invoker
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
        // TODO Wrapper类不能正确处理带$的类名
        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);//debug com.alibaba.dubbo.common.bytecode.Wrapper1@3b59640d
        return new AbstractProxyInvoker<T>(proxy, type, url) {
            /**
             * @param proxy //debug  com.alibaba.dubbo.demo.user.facade.AnnotationDrivenUserRestServiceImpl@48931b44
             * @param methodName 要调用的接口的某个方法
             * @param parameterTypes 方法参数类型
             * @param arguments 方法的参数值
             * @return 返回调用结果
             * 此时的目的就是无论该实例对象proxy有哪些方法,都会被转移到invoke方法的形式去调用
             *
             * 注意:该方法是在被Invoker调用Result invoke(Invocation invocation)方法的时候才会被执行的
             *
             * 注意:该方法需要的所有参数都来自于invocation对象,即invoke(Invocation invocation)的时候会从invocation参数中抽取出方法和方法的参数值以及参数类型信息
             */
            @Override
            protected Object doInvoke(T proxy, String methodName, 
                                      Class<?>[] parameterTypes, 
                                      Object[] arguments) throws Throwable {
                return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
            }
        };
    }

}