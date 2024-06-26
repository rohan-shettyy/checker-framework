// A test for a bug that came up while porting the Resource Leak Checker into the
// Checker Framework proper.

import org.checkerframework.checker.mustcall.qual.*;

import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.*;

class SSLSocketFactoryTest {
    public SSLSocket createSSLSocket(@Owning Socket socket, SSLContext sslContext)
            throws IOException {
        SSLSocket sslSocket =
                (SSLSocket)
                        sslContext
                                .getSocketFactory()
                                .createSocket(socket, null, socket.getPort(), true);
        return sslSocket;
    }
}
