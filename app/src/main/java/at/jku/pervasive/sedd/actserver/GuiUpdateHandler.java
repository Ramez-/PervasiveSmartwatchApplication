package at.jku.pervasive.sedd.actserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GuiUpdateHandler implements Runnable {

	private final CharsetEncoder encoder = ActivityServer.NET_CHARSET.newEncoder();
	private final CharsetDecoder decoder = ActivityServer.NET_CHARSET.newDecoder();

	private SocketChannel channel;
	private long lastSubmit;
	private int lastGroupUpdateId;
	private String userId;
	private boolean forceUpdate;
	
	private ByteBuffer inbuf = ByteBuffer.allocate(1024);
	private ByteBuffer swapbuf = ByteBuffer.allocate(1024);

	public GuiUpdateHandler(SocketChannel channel) {
		this.channel = channel;
		lastSubmit = 0;
		lastGroupUpdateId = -1;
		forceUpdate = false;
	}

	private String readLine() throws IOException {
		// read till we find a newline
		int nlpos = 0;
		readloop: do {
			while (nlpos < inbuf.position())
				if (inbuf.get(nlpos) == '\n')
					break readloop;
				else
					nlpos++;
			if (channel.read(inbuf) < 0) break readloop;
		} while (true);
		// put remainder of buffer in swap buffer
		inbuf.limit(inbuf.position()).position(Math.min(nlpos+1, inbuf.limit()));
		swapbuf.clear();
		swapbuf.put(inbuf);
		// extract line from buffer
		inbuf.position(0).limit(nlpos);
		String line = decoder.decode(inbuf).toString();
		inbuf.clear();
		// swap buffers
		ByteBuffer tmpbuf = inbuf;
		inbuf = swapbuf;
		swapbuf = tmpbuf;
		return line.trim();
	}

	private void writeLine(String line) throws IOException {
		channel.write(encoder.encode(CharBuffer.wrap(line + "\n")));
	}

	@Override
	public void run() {
		String remote = " " + channel.socket().getRemoteSocketAddress() + " gui: ";
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println(dateFormat.format(new Date()) + remote + "connect");
		// create input thread
		final Thread inputThread = new Thread() {
			
			@Override
			public void run() {
				String line;
				while (!interrupted()) {
					try {
						line = readLine();
						if (line.equals("add_emulated")) {
							ActivityServer.addEmulatedClient();
						} else if (line.equals("clear_emulated")) {
							ActivityServer.clearEmulatedClients();
						} else if (line.equals("clear_data")) {
							ActivityServer.clearDataStore();
						} else {
							if (line.length() == 0) line = null;
							if (line != null && !line.matches("\\d{8}")) {
								writeLine("not a valid client id");
								throw new IOException("invalid user id");
							}
							userId = line;
						}
						forceUpdate = true;
					} catch (Exception e) {
						System.out.println(e.getMessage());
						interrupt();
					}
				}
			}

		};
		inputThread.start();
		// output loop
		try {
			// submit group state to client whenever it changes, but at most
			// every 200 ms and at least every 5000 ms
			while (true) {
				long now = System.currentTimeMillis();
				long deltaSubmit = now - lastSubmit;
				int groupUpdateId = ActivityServer.getGroupUpdateId();
				if (forceUpdate || deltaSubmit > 5000 || lastGroupUpdateId != groupUpdateId) {
					lastGroupUpdateId = groupUpdateId;
					lastSubmit = now;
					forceUpdate = false;
					String s = ActivityServer.getGuiStateString(userId);
					writeLine(s);
					// System.out.println("g>" + s);
				}
				Thread.sleep(200);
			}

		} catch (InterruptedException e) {
			// thread interrupted, thats okay, do nothing
		} catch (ClosedByInterruptException e) {
			// thread interrupted, thats okay, do nothing
		} catch (IOException e) {
			System.out.println(dateFormat.format(new Date()) + remote + e.getMessage());
		}
		inputThread.interrupt();
		try {
			if (channel.isConnected()) channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(dateFormat.format(new Date()) + remote + "disconnect");
	}

}
