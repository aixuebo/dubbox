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
package com.alibaba.dubbo.common.bytecode;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.dubbo.common.utils.ClassHelper;
import com.alibaba.dubbo.common.utils.ReflectUtils;

/**
 * Proxy.
 * 
 * @author qian.lei
 */

public abstract class Proxy
{
	private static final AtomicLong PROXY_CLASS_COUNTER = new AtomicLong(0);//代理类计数器---即有多少个代理类

	private static final String PACKAGE_NAME = Proxy.class.getPackage().getName();

	public static final InvocationHandler RETURN_NULL_INVOKER = new InvocationHandler(){
		public Object invoke(Object proxy, Method method, Object[] args){ return null; }
	};

	public static final InvocationHandler THROW_UNSUPPORTED_INVOKER = new InvocationHandler(){
		public Object invoke(Object proxy, Method method, Object[] args){ throw new UnsupportedOperationException("Method [" + ReflectUtils.getName(method) + "] unimplemented."); }
	};
    //每一个classLoad一个缓存空间,value的key是接口集合,用分号拆分,value是可以代表该接口的代理对象WeakReference<Proxy>(proxy),而且被WeakReference弱引用了一下
	private static final Map<ClassLoader, Map<String, Object>> ProxyCacheMap = new WeakHashMap<ClassLoader, Map<String, Object>>();

	private static final Object PendingGenerationMarker = new Object();

	/**
	 * Get proxy.
	 * 创建一个代理类
	 * @param ics interface class array.
	 * @return Proxy instance.
	 */
	public static Proxy getProxy(Class<?>... ics)
	{
		return getProxy(ClassHelper.getCallerClassLoader(Proxy.class), ics);
	}

	/**
	 * Get proxy.
	 * @param cl class loader.
	 * @param ics interface class array.
	 * 
	 * @return Proxy instance.
	 */
	public static Proxy getProxy(ClassLoader cl, Class<?>... ics)
	{
		if( ics.length > 65535 )
			throw new IllegalArgumentException("interface limit exceeded");
		//多个接口用分号拆分,比如com.alibaba.dubbo.demo.user.facade.UserRestService;com.alibaba.dubbo.rpc.service.EchoService;
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<ics.length;i++)
		{
			String itf = ics[i].getName();
			if( !ics[i].isInterface() )
				throw new RuntimeException(itf + " is not a interface.");

			Class<?> tmp = null;
			try
			{
				tmp = Class.forName(itf, false, cl);
			}
			catch(ClassNotFoundException e)
			{}
            //目的就是证明是否是在同一个classLoader下能否加载成功
			if( tmp != ics[i] )
				throw new IllegalArgumentException(ics[i] + " is not visible from class loader");

		    sb.append(itf).append(';');
		}

		// use interface class name list as key.
		String key = sb.toString();

		// get cache by class loader.
		Map<String, Object> cache;//每一个classLoad一个缓存空间
		synchronized( ProxyCacheMap )
		{
			cache = ProxyCacheMap.get(cl);
			if( cache == null )
		    {
				cache = new HashMap<String, Object>();
				ProxyCacheMap.put(cl, cache);
		    }
		}

		Proxy proxy = null;
		synchronized( cache )
		{
			do
			{
				Object value = cache.get(key);
				if( value instanceof Reference<?> )
				{
					proxy = (Proxy)((Reference<?>)value).get();
					if( proxy != null )
						return proxy;
				}

				if( value == PendingGenerationMarker )
				{
					try{ cache.wait(); }catch(InterruptedException e){}
				}
				else
				{
					cache.put(key, PendingGenerationMarker);
					break;
				}
			}
			while( true );
		}

		long id = PROXY_CLASS_COUNTER.getAndIncrement();//为代理类匹配一个自增长的ID
		String pkg = null;
		ClassGenerator ccp = null, ccm = null;
		try
		{
			ccp = ClassGenerator.newInstance(cl);

			Set<String> worked = new HashSet<String>();//方法和参数还有返回值表示一个元素
			List<Method> methods = new ArrayList<Method>();//追加的方法

			for(int i=0;i<ics.length;i++)
			{
				if( !Modifier.isPublic(ics[i].getModifiers()) )
				{//不是public的应该也不是很经常存在,因此忽略该情况
					String npkg = ics[i].getPackage().getName();
					if( pkg == null )
					{
						pkg = npkg;
					}
					else
					{
						if( !pkg.equals(npkg)  )
							throw new IllegalArgumentException("non-public interfaces from different packages");
					}
				}
				ccp.addInterface(ics[i]);//添加一个接口

				for( Method method : ics[i].getMethods() )
				{
					String desc = ReflectUtils.getDesc(method);//registerUser(Lcom/alibaba/dubbo/demo/user/User;)Lcom/alibaba/dubbo/demo/user/facade/RegistrationResult;描述方法以及参数和返回值
					if( worked.contains(desc) )
						continue;
					worked.add(desc);

					int ix = methods.size();
					Class<?> rt = method.getReturnType();
					Class<?>[] pts = method.getParameterTypes();
                    //生成的代码:Object[] args = new Object[1]; args[0] = ($w)$1; Object ret = handler.invoke(this, methods[0], args); return (com.alibaba.dubbo.demo.user.facade.RegistrationResult)ret;
					StringBuilder code = new StringBuilder("Object[] args = new Object[").append(pts.length).append("];");
					for(int j=0;j<pts.length;j++)
						code.append(" args[").append(j).append("] = ($w)$").append(j+1).append(";");
					code.append(" Object ret = handler.invoke(this, methods[" + ix + "], args);");
					if( !Void.TYPE.equals(rt) )
						code.append(" return ").append(asArgument(rt, "ret")).append(";");

					methods.add(method);//添加一个方法
					ccp.addMethod(method.getName(), method.getModifiers(), rt, pts, method.getExceptionTypes(), code.toString());//向方法中添加代码的实现
				}
			}

			if( pkg == null )
				pkg = PACKAGE_NAME;

			// create ProxyInstance class.
			String pcn = pkg + ".proxy" + id;//com.alibaba.dubbo.common.bytecode.proxy0
			ccp.setClassName(pcn);
			ccp.addField("public static java.lang.reflect.Method[] methods;");//定义该代理包含的所有方法集合
			ccp.addField("private " + InvocationHandler.class.getName() + " handler;");//定义一个handler
			ccp.addConstructor(Modifier.PUBLIC, new Class<?>[]{ InvocationHandler.class }, new Class<?>[0], "handler=$1;");//定义构造函数
            ccp.addDefaultConstructor();
			Class<?> clazz = ccp.toClass();
			clazz.getField("methods").set(null, methods.toArray(new Method[0]));//为class设置属性

			// create Proxy class.
			String fcn = Proxy.class.getName() + id;//com.alibaba.dubbo.common.bytecode.Proxy0
			ccm = ClassGenerator.newInstance(cl);//创建一个实例对象
			ccm.setClassName(fcn);
			ccm.addDefaultConstructor();
			ccm.setSuperClass(Proxy.class);
			ccm.addMethod("public Object newInstance(" + InvocationHandler.class.getName() + " h){ return new " + pcn + "($1); }");
			Class<?> pc = ccm.toClass();
			proxy = (Proxy)pc.newInstance();
		}
		catch(RuntimeException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}
		finally
		{
			// release ClassGenerator
			if( ccp != null )
				ccp.release();
			if( ccm != null )
				ccm.release();
			synchronized( cache )
			{
				if( proxy == null )
					cache.remove(key);
				else
					cache.put(key, new WeakReference<Proxy>(proxy));
				cache.notifyAll();
			}
		}
		return proxy;
	}

	/**
	 * get instance with default handler.
	 * 
	 * @return instance.
	 */
	public Object newInstance()
	{
		return newInstance(THROW_UNSUPPORTED_INVOKER);
	}

	/**
	 * get instance with special handler.
	 * 
	 * @return instance.
	 */
	abstract public Object newInstance(InvocationHandler handler);

	protected Proxy(){}

	private static String asArgument(Class<?> cl, String name)
	{
		if( cl.isPrimitive() )
		{
			if( Boolean.TYPE == cl )
				return name + "==null?false:((Boolean)" + name + ").booleanValue()";
			if( Byte.TYPE == cl )
				return name + "==null?(byte)0:((Byte)" + name + ").byteValue()";
			if( Character.TYPE == cl )
				return name + "==null?(char)0:((Character)" + name + ").charValue()";
			if( Double.TYPE == cl )
				return name + "==null?(double)0:((Double)" + name + ").doubleValue()";
			if( Float.TYPE == cl )
				return name + "==null?(float)0:((Float)" + name + ").floatValue()";
			if( Integer.TYPE == cl )
				return name + "==null?(int)0:((Integer)" + name + ").intValue()";
			if( Long.TYPE == cl )
				return name + "==null?(long)0:((Long)" + name + ").longValue()";
			if( Short.TYPE == cl )
				return name + "==null?(short)0:((Short)" + name + ").shortValue()";
			throw new RuntimeException(name+" is unknown primitive type."); 
		}
		return "(" + ReflectUtils.getName(cl) + ")"+name;
	}
}