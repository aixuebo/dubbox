package com.alibaba.dubbo.rpc.filter.trace;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.remoting.TimeoutException;
import com.jthink.skyeye.base.constant.Constants;
import com.jthink.skyeye.base.dapper.BinaryAnnotation;
import com.jthink.skyeye.base.dapper.EndPoint;
import com.jthink.skyeye.base.dapper.ExceptionType;
import com.jthink.skyeye.base.dapper.Span;
import com.jthink.skyeye.trace.core.generater.IncrementIdGen;
import com.jthink.skyeye.trace.core.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc dubbo rpc trace filter
 * @date 2016-11-03 17:57:54
 */
@Activate(group = {com.alibaba.dubbo.common.Constants.PROVIDER, com.alibaba.dubbo.common.Constants.CONSUMER})
public class RpcTraceFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcTraceFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (IncrementIdGen.getId() == null) {
            // 如果分配的id未生成
            return invoker.invoke(invocation);//直接继续执行代码,因为该节点没有分配rpc分布式ID的唯一码
        }

        long start = System.currentTimeMillis();
        RpcContext context = RpcContext.getContext();
        boolean isConsumerSide = context.isConsumerSide();//是否是消费者---true表示此时是client主动去发送数据一端
        boolean isProviderSide = context.isProviderSide();//是否是生产者---true表示此时是server端主动接收数据的一端
        String methodName = context.getMethodName();//服务调用的方法
        RpcInvocation rpcInvocation = (RpcInvocation) invocation;

        //　设计的serviceId:服务的接口+method方法名
        String serviceId = context.getUrl().getServiceInterface() + Constants.UNDER_LINE + methodName;

        Tracer tracer = Tracer.getInstance();//创建一个trace实例

        EndPoint endPoint = tracer.buildEndPoint(context.getLocalAddressString(), context.getLocalPort());

        Span span = null;
        try {
            if (isConsumerSide) {//比如此时说明是客户端发送到服务的第一个接口,从该接口开始向其他接口服务发送信息
                // 如果是消费者
                Span parentSpan = tracer.getParentSpan();
                if (null == parentSpan) {//说明是第一个根节点
                    // 如果parentSpan为null, 表示该Span为root span
                    span = tracer.newSpan(methodName, serviceId);//创建新的spanId,转换成span对象,同时里面创建对应的traceId等信息
                } else {
                    // 叶子span----表示比如A调用B,B调用C,此时表示B要调用c,因此作为新的客户端,创建一个新的span对象,依赖的父span就是B的spanId
                    span = tracer.buildSpan(parentSpan.getTraceId(), parentSpan.getId(), tracer.generateSpanId(), methodName, parentSpan.getSample(), serviceId);
                }
            } else if (isProviderSide) {//表示服务器端接收的数据情况
                // 如果是生产者
                //以下内容都是客户端将参数传递到request中,发送过来的,因此服务端是可以类似request中get到的属性值
                String traceId = AttachmentUtil.getAttachment(rpcInvocation, Constants.TRACE_ID);
                String parentId = AttachmentUtil.getAttachment(rpcInvocation, Constants.PARENT_ID);
                String spanId = AttachmentUtil.getAttachment(rpcInvocation, Constants.SPAN_ID);
                boolean isSample = traceId != null && AttachmentUtil.getAttachmentBoolean(rpcInvocation, Constants.SAMPLE);
                //生产者的span都是已知的,都是从客户端创建的,因此span只有客户端才会被创建新的
                span = tracer.buildSpan(traceId, parentId, spanId, methodName, isSample, serviceId);
            }

            // 调用具体业务逻辑之前处理
            this.invokeBefore(span, endPoint, start, isConsumerSide, isProviderSide);
            // 传递附件到RpcInvocation, 传递到下游，确保下游能够收到traceId等相关信息，保证请求能够标记到正确的traceId
            this.setAttachment(rpcInvocation, span);
            // 执行具体的业务或者下游的filter
            Result result = invoker.invoke(invocation);

            if (result.hasException()) {
                // 如果在请求过程中发生了异常, 需要进行异常的处理和相关annotation的记录
                this.processException(endPoint, result.getException().getMessage(), ExceptionType.EXCEPTION);
            }

            return result;
        } catch (RpcException e) {
            if (null != e.getCause() && e.getCause() instanceof TimeoutException) {
                // 执行该filter发生异常，如果异常是com.alibaba.dubbo.remoting.TimeoutException
                this.processException(endPoint, e.getMessage(), ExceptionType.TIMEOUTEXCEPTION);
            } else {
                // 其他异常
                this.processException(endPoint, e.getMessage(), ExceptionType.EXCEPTION);
            }
            // 将异常抛出去
            throw e;
        } finally {
            // 调用具体业务逻辑之后处理
            if (null != span) {
                long end = System.currentTimeMillis();
                this.invokeAfter(span, endPoint, end, isConsumerSide, isProviderSide);
            }
        }

    }

    /**
     * 处理异常，构造Span的BinaryAnnotation
     * @param endPoint
     * @param message
     * @param type
     */
    private void processException(EndPoint endPoint, String message, ExceptionType type) {
        BinaryAnnotation exAnnotation = new BinaryAnnotation();
        exAnnotation.setKey(type.label());
        exAnnotation.setValue(message);
        exAnnotation.setType(type.symbol());
        exAnnotation.setEndPoint(endPoint);
        // add到span
        Tracer tracer = Tracer.getInstance();
        tracer.addBinaryAnntation(exAnnotation);//向父span追加一个自定义的异常内容
    }

    /**
     * 将Span的相关值设置到RpcInvocation的attachment中, 然后传递到下游
     相当于向request中set各种属性,让server端可以接收到这些属性
     * @param invocation
     * @param span
     */
    private void setAttachment(RpcInvocation invocation, Span span) {
        if (span.getSample() && null != span) {
            // 如果进行采样
            invocation.setAttachment(Constants.TRACE_ID, span.getTraceId() == null ? null : String.valueOf(span.getTraceId()));
            invocation.setAttachment(Constants.SPAN_ID, span.getId() == null ? null : String.valueOf(span.getId()));
            invocation.setAttachment(Constants.PARENT_ID, span.getParentId() == null ? null : String.valueOf(span.getParentId()));
            invocation.setAttachment(Constants.SAMPLE, span.getSample() == null ? null : String.valueOf(span.getSample()));
        }

    }

    /**
     * 调用具体逻辑之前，记录相关的annotation、设置对应的parentSpan
     * @param span
     * @param endPoint
     * @param isConsumerSide
     * @param isProviderSide
     */
    private void invokeBefore(Span span, EndPoint endPoint, long start, boolean isConsumerSide, boolean isProviderSide) {
        Tracer tracer = Tracer.getInstance();
        if (isConsumerSide && span.getSample()) {//说明该span是可以支持抽样的,因此要对其发送数据
            // 如果是消费者, ClientSend
            tracer.clientSend(span, endPoint, start);
        } else if (isProviderSide) {
            // 如果是提供者
            if (span.getSample()) {
                // ServerReceive
                tracer.serverReceive(span, endPoint, start);
            }
            // 将该span作为parentSpan设置到ThreadLocal中
            tracer.setParentSpan(span);
        }
    }

    /**
     * 调用具体逻辑之后，记录相关的annotation、去除对应的parentSpan
     * @param span
     * @param endPoint
     * @param end
     * @param isConsumerSide
     * @param isProviderSide
     */
    private void invokeAfter(Span span, EndPoint endPoint, long end, boolean isConsumerSide, boolean isProviderSide) {
        Tracer tracer = Tracer.getInstance();
        if (isConsumerSide && span.getSample()) {
            // 如果是消费者, ClientReceive
            tracer.clientReceive(span, endPoint, end);
        } else if (isProviderSide) {
            // 如果是提供者
            if (span.getSample()) {
                // ServerSend
                tracer.serverSend(span, endPoint, end);
            }
            tracer.removeParentSpan();
        }
    }
}
