package com.example.demo.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * 后端生成随机数
 *
 * @ServerEndpoint(value = "/oneToMany") 前端通过此URI 和后端交互，建立连接
 */
@Slf4j
@ServerEndpoint(value = "/random")
@Component
public class RandomWebSocket {
    /** 记录当前在线连接数 */
    private static AtomicInteger onlineCount = new AtomicInteger(0);

    /** 存放所有在线的客户端 */
    private static Map<String, Session> clients = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        onlineCount.incrementAndGet(); // 在线数加1
        clients.put(session.getId(), session);
        this.sendMessage("当前ID："+session.getId());
        log.info("有新连接加入：{}，当前在线人数为：{}", session.getId(), onlineCount.get());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session) {
        onlineCount.decrementAndGet(); // 在线数减1
        clients.remove(session.getId());
        log.info("有一连接关闭：{}，当前在线人数为：{}", session.getId(), onlineCount.get());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("服务端收到客户端[{}]的消息:{}", session.getId(), message);
        this.sendMessage(message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误");
        error.printStackTrace();
    }

    /**
     * 群发消息
     *
     * @param message 消息内容
     */
    private void sendMessage(String message)  {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                callback();
            }
        });
        thread.start();
    }

    public void callback()  {
        try {
            while (true) {
                int m = (int) (20 + Math.floor(Math.random()*100));
                String str = Integer.toString(m);
                for (Map.Entry<String, Session> sessionEntry : clients.entrySet()) {
                    Session toSession = sessionEntry.getValue();
                    // 排除掉自己
                    //if (fromSession.getId().equals(toSession.getId()))  continue;
                    log.info("发送随机数"+str, toSession.getId(), str);
                    toSession.getAsyncRemote().sendText("随机数"+str);
                }
                Thread.sleep(3000);
            }
        }
        catch (Exception ex){
            log.error("发生错误");
        }
    }

}
