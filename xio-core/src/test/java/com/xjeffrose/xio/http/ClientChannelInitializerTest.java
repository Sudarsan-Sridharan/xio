package com.xjeffrose.xio.http;

import static org.powermock.api.mockito.PowerMockito.when;

import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import com.xjeffrose.xio.tracing.HttpClientTracingHandler;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslHandler;
import lombok.val;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ClientChannelInitializerTest extends Assert {

  private ClientChannelInitializer subject;
  @Mock private XioTracing tracing;
  @Mock private HttpClientTracingHandler tracingHandler;

  private MockResponse buildResponse() {
    return new MockResponse().setBody("hello, world").setSocketPolicy(SocketPolicy.KEEP_OPEN);
  }

  private ChannelHandler appHandler =
      new ChannelHandler() {
        @Override
        public void handlerAdded(ChannelHandlerContext channelHandlerContext) throws Exception {}

        @Override
        public void handlerRemoved(ChannelHandlerContext channelHandlerContext) throws Exception {}

        @Override
        public void exceptionCaught(
            ChannelHandlerContext channelHandlerContext, Throwable throwable) throws Exception {}
      };

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testDisabledTracing() {
    val channelConfig = ChannelConfiguration.clientConfig(1, "worker");
    val clientConfig = ClientConfig.from(ConfigFactory.load().getConfig("xio.basicClient"));
    val clientState = new ClientState(channelConfig, clientConfig);
    // when we have disabled Tracing the tracing returns null for newClientHandler
    when(tracing.newClientHandler()).thenReturn(null);

    subject = new ClientChannelInitializer(clientState, () -> appHandler, tracing);

    // Assert that we did not add a HttpClientTracingHandler to the pipeline
    val testChannel = new EmbeddedChannel(subject);
    val result = testChannel.pipeline().get(HttpClientTracingHandler.class);
    assertEquals(result, null);
  }

  @Test
  public void testEnabledTracing() throws Exception {
    val channelConfig = ChannelConfiguration.clientConfig(1, "worker");
    val clientConfig = ClientConfig.from(ConfigFactory.load().getConfig("xio.basicClient"));
    val clientState = new ClientState(channelConfig, clientConfig);
    // when we have enabled Tracing the tracing returns a non-null newClientHandler
    when(tracing.newClientHandler()).thenReturn(tracingHandler);

    subject = new ClientChannelInitializer(clientState, () -> appHandler, tracing);

    // Assert that we did not add a HttpClientTracingHandler to the pipeline
    val testChannel = new EmbeddedChannel(subject);
    val result = testChannel.pipeline().get(HttpClientTracingHandler.class);
    assertEquals(result, tracingHandler);
  }

  @Test
  public void testEnableSsl() {
    val channelConfig = ChannelConfiguration.clientConfig(1, "worker");
    val clientConfig = ClientConfig.from(ConfigFactory.load().getConfig("xio.sslClient"));
    val clientState = new ClientState(channelConfig, clientConfig);
    // when we have enabled Tracing the tracing returns a non-null newClientHandler
    when(tracing.newClientHandler()).thenReturn(tracingHandler);

    subject = new ClientChannelInitializer(clientState, () -> appHandler, tracing);

    // Assert that we did not add a HttpClientTracingHandler to the pipeline
    val testChannel = new EmbeddedChannel(subject);
    val result = testChannel.pipeline().get(SslHandler.class);
    assertNotNull(result);
  }

  @Test
  public void testDisableSsl() {
    val channelConfig = ChannelConfiguration.clientConfig(1, "worker");
    val clientConfig = ClientConfig.from(ConfigFactory.load().getConfig("xio.basicClient"));
    val clientState = new ClientState(channelConfig, clientConfig);
    // when we have enabled Tracing the tracing returns a non-null newClientHandler
    when(tracing.newClientHandler()).thenReturn(tracingHandler);

    subject = new ClientChannelInitializer(clientState, () -> appHandler, tracing);

    // Assert that we did not add a HttpClientTracingHandler to the pipeline
    val testChannel = new EmbeddedChannel(subject);
    val result = testChannel.pipeline().get(SslHandler.class);
    assertNull(result);
  }

  @Test
  public void testDisableIdleTimeout() {
    val channelConfig = ChannelConfiguration.clientConfig(1, "worker");
    val clientConfig = ClientConfig.from(ConfigFactory.load().getConfig("xio.idleDisabledClient"));
    val clientState = new ClientState(channelConfig, clientConfig);
    // when we have enabled Tracing the tracing returns a non-null newClientHandler
    when(tracing.newClientHandler()).thenReturn(tracingHandler);

    subject = new ClientChannelInitializer(clientState, () -> appHandler, tracing);

    // Assert that we did not add a HttpClientTracingHandler to the pipeline
    val testChannel = new EmbeddedChannel(subject);
    val result = testChannel.pipeline().get(XioIdleDisconnectHandler.class);
    assertNull(result);
  }

  @Test
  public void testEnableIdleTimeout() {
    val channelConfig = ChannelConfiguration.clientConfig(1, "worker");
    val clientConfig = ClientConfig.from(ConfigFactory.load().getConfig("xio.idleEnabledClient"));
    val clientState = new ClientState(channelConfig, clientConfig);
    // when we have enabled Tracing the tracing returns a non-null newClientHandler
    when(tracing.newClientHandler()).thenReturn(tracingHandler);

    subject = new ClientChannelInitializer(clientState, () -> appHandler, tracing);

    // Assert that we did not add a HttpClientTracingHandler to the pipeline
    val testChannel = new EmbeddedChannel(subject);
    val result = testChannel.pipeline().get(XioIdleDisconnectHandler.class);
    assertNotNull(result);
  }
}
