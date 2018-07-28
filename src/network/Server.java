package network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import javafx.application.Platform;
import logic.MasterLogicController;
import org.json.JSONObject;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Server {
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private MasterLogicController logicController;

    public Server(MasterLogicController logicController) {
        this.logicController = logicController;
    }

    public ChannelFuture listen(int port) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(1);
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel socketChannel) {
                        ChannelPipeline pipeline = socketChannel.pipeline();

                        bossGroup.shutdownGracefully();//只接受一个连接

                        pipeline.addLast("json", new JsonObjectDecoder());
                        pipeline.addLast("decoder", new StringDecoder());
                        pipeline.addLast("encoder", new StringEncoder());
                        pipeline.addLast("handler", new ServerHandler(logicController));
                    }
                });

        ChannelFuture bindFuture = bootstrap.bind(port);
        return bindFuture;
    }

    public String getLocalIP() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if(iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if(addr instanceof Inet6Address) {
                        continue;
                    }
                    ip = addr.getHostAddress();
                }
            }
        } catch(SocketException e) {
            e.printStackTrace();
        }
        return ip;
    }

    public void close() {
        if(!bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully();
        }
        if(!workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully();
        }
    }
}

class ServerHandler extends ChannelInboundHandlerAdapter {
    private  MasterLogicController logicController;

    public ServerHandler(MasterLogicController logicController) {
        this.logicController = logicController;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Platform.runLater(() -> {
            logicController.setChannel(ctx.channel());
            logicController.startGame();
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println(ctx.channel().remoteAddress() + "left");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        JSONObject JSONMsg = new JSONObject((String)msg);
        Platform.runLater(() -> {
            logicController.onMessageReceived(JSONMsg);
        });
    }
}