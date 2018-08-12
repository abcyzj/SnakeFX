package network;

/*
 * 客户端网络模块
 */

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import javafx.application.Platform;
import logic.SlaveLogicController;
import org.json.JSONException;
import org.json.JSONObject;

public class Client {
    private SlaveLogicController logicController;
    private String host;
    private int port;
    private EventLoopGroup group;

    public Client(SlaveLogicController logicController, String host, int port) {
        this.logicController = logicController;
        this.host = host;
        this.port = port;
    }

    public ChannelFuture connect() {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("json", new JsonObjectDecoder());
                        pipeline.addLast("decoder", new StringDecoder());
                        pipeline.addLast("encoder", new StringEncoder());
                        pipeline.addLast("handler", new ClientHandler(logicController));
                    }
                });

        return bootstrap.connect(host, port);
    }

    public void close() {
        if(!group.isShutdown()) {
            group.shutdownGracefully();
        }
    }
}

class ClientHandler extends ChannelInboundHandlerAdapter {

    private SlaveLogicController logicController;

    public ClientHandler(SlaveLogicController logicController) {
        this.logicController = logicController;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Platform.runLater(() -> {
            logicController.setChannel(ctx.channel());
            logicController.onChannelActive();
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Platform.runLater(logicController::onConnectionInactive);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        JSONObject JSONMsg;
        try {
            JSONMsg = new JSONObject((String)msg);
        } catch(JSONException e) {
            e.printStackTrace();
            return;
        }

        Platform.runLater(() -> {
            logicController.onMsgReceived(JSONMsg);
        });
    }
}
