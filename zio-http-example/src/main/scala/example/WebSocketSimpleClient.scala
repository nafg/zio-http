package example

import zio._
import zio.http.ChannelEvent.{ChannelRead, UserEvent, UserEventTriggered}
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.http.{ChannelEvent, Client, Handler, Response, Route}

object WebSocketSimpleClient extends ZIOAppDefault {

  val url = "ws://ws.vi-server.org/mirror"

  val httpSocket: Route[Any, Throwable, WebSocketChannelEvent, Unit] =
    Route

      // Listen for all websocket channel events
      .collectZIO[WebSocketChannelEvent] {

        // Send a "foo" message to the server once the connection is established
        case ChannelEvent(ch, UserEventTriggered(UserEvent.HandshakeComplete)) =>
          ch.writeAndFlush(WebSocketFrame.text("foo"))

        // Send a "bar" if the server sends a "foo"
        case ChannelEvent(ch, ChannelRead(WebSocketFrame.Text("foo")))         =>
          ch.writeAndFlush(WebSocketFrame.text("bar"))

        // Close the connection if the server sends a "bar"
        case ChannelEvent(ch, ChannelRead(WebSocketFrame.Text("bar")))         =>
          ZIO.succeed(println("Goodbye!")) *> ch.writeAndFlush(WebSocketFrame.close(1000))
      }

  val app: ZIO[Any with Client with Scope, Throwable, Response] =
    httpSocket.toHandler(Handler.notFound).toSocketApp.connect(url) *> ZIO.never

  val run = app.provide(Client.default, Scope.default)

}
