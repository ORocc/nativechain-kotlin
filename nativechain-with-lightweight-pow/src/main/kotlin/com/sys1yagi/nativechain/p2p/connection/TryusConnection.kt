package com.sys1yagi.nativechain.p2p.connection

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.sendBlocking
import org.glassfish.tyrus.client.ClientManager
import org.slf4j.LoggerFactory
import java.net.URI
import javax.websocket.*


class TryusConnection(val uri: URI) : Connection {
    private val logger = LoggerFactory.getLogger("TryusConnection")
    private val channel = Channel<String>()
    private var session: Session? = null

    fun connect(onOpen: (Channel<String>) -> Unit) {
        logger.debug("connect ${uri.host}:${uri.port}")
        val config = ClientEndpointConfig.Builder.create().build()
        val client = ClientManager.createClient()
        client.connectToServer(
            object : Endpoint() {
                override fun onOpen(session: Session, config: EndpointConfig?) {
                    logger.debug("onOpen ${uri.scheme}://${uri.host}")
                    this@TryusConnection.session = session
                    onOpen(channel)
                    session.addMessageHandler(MessageHandler.Whole<String> { message ->
                        channel.sendBlocking(message)
                    })
                }

                override fun onClose(session: Session?, closeReason: CloseReason?) {
                    logger.debug("onClose ${uri.host} ${closeReason}")
                    super.onClose(session, closeReason)
                    channel.close()
                }

                override fun onError(session: Session?, thr: Throwable?) {
                    logger.debug("onError ${uri.host}")
                    thr?.printStackTrace()
                    channel.close()
                }
            },
            config,
            uri
        )
    }

    override fun receiveChannel() = channel

    override fun send(message: String) {
        session?.basicRemote?.sendText(message)
    }

    override fun peer() = uri.toString()
}
