我猜测该模式是在每一个客户端使用的,如果客户端使用了集群模式,则就会在客户端使用该cluster包代码

至于服务的url怎么动态的加载过来的,这个应该是有监听zookeeper等服务

配置的demo
<dubbo:service interface="org.shirdrn.dubbo.api.ChatRoomOnlineUserCounterService" version="1.0.0"
     cluster="failover" retries="2" timeout="100" ref="chatRoomOnlineUserCounterService" protocol="dubbo" >
     <dubbo:method name="queryRoomUserCount" timeout="80" retries="2" />
</dubbo:service>

