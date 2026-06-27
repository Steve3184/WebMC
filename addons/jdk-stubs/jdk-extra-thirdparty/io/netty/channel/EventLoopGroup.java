package io.netty.channel;

public interface EventLoopGroup {
    EventLoop next();
    void shutdownGracefully();
}
