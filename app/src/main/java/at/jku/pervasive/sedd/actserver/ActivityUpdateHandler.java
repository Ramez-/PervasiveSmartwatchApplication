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

import at.jku.pervasive.sedd.actclient.ClassLabel;
import at.jku.pervasive.sedd.actclient.RoomState;
import at.jku.pervasive.sedd.actclient.UserRole;
import at.jku.pervasive.sedd.util.OptionParser;

public class ActivityUpdateHandler implements Runnable {

	private final CharsetEncoder encoder = ActivityServer.NET_CHARSET.newEncoder();
	private final CharsetDecoder decoder = ActivityServer.NET_CHARSET.newDecoder();

	private SocketChannel channel;
	private String userId;

	private ByteBuffer inbuf = ByteBuffer.allocate(1024);
	private ByteBuffer swapbuf = ByteBuffer.allocate(1024);

	public ActivityUpdateHandler(SocketChannel channel) {
		this.channel = channel;
		userId = null;
	}

	private String readLine() throws IOException {
		// read till we find a newline
		int nlpos = 0;
		readloop: do {
			while (nlpos < inbuf.position()) {
				if (inbuf.get(nlpos) == '\n')
					break readloop;
				else
					nlpos++;
			}
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
		final String remote = " " + channel.socket().getRemoteSocketAddress() + " user: ";
		final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println(dateFormat.format(new Date()) + remote + "connect");
		Thread outputThread = null;
		try {
			userId = readLine();
			if (!userId.matches("\\d{8}")) {
				writeLine("not a valid client id");
				throw new IOException("invalid user id \"" + userId + "\"");
			}
			writeLine("accepted");
			System.out.println(dateFormat.format(new Date()) + remote + userId);
			// set up output thread
			outputThread = new Thread() {

				@Override
				public void run() {
					try {
						// submit group state to client whenever it changes, but
						// at most every 100 ms and at least every 5000 ms
						long lastSubmit = 0;
						int lastGroupUpdateId = -1;
						while (true) {
							long now = System.currentTimeMillis();
							long deltaSubmit = now - lastSubmit;
							int groupUpdateId = ActivityServer.getGroupUpdateId();
							if (deltaSubmit > 5000 || lastGroupUpdateId != groupUpdateId) {
								lastGroupUpdateId = groupUpdateId;
								lastSubmit = now;
								writeLine(ActivityServer.getGroupStateString());
							}
							Thread.sleep(100);
						}
					} catch (InterruptedException e) {
						// thread interrupted, thats okay, do nothing
					} catch (ClosedByInterruptException e) {
						// thread interrupted, thats okay, do nothing
					} catch (IOException e) {
						System.out.println(dateFormat.format(new Date()) + remote + e.getMessage());
					}
					try {
						if (channel.isConnected()) channel.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			};
			outputThread.start();
			// input thread loop
			int nln = 1;
			while (true) {
				String line = readLine();
				if (line.equals("")) break;
				if (line.startsWith("l")) {
					int ln = Integer.parseInt(line.substring(1, 6).trim());
					if (ln != nln) System.out.println("warning skipped " + (ln - nln) + " lines");
					line = line.substring(6);
					nln = ln + 1;
				}
				if (line.startsWith("R:")) {
					String[] lp = OptionParser.split(line, ":");
					if (lp.length != 2) {
						writeLine("invalid room state format");
						throw new IOException("invalid room state format");
					}
					RoomState rr = RoomState.parse(lp[1]);
					ActivityServer.setRoomState(userId, rr);
					//System.out.println(userId + " says room is " + rr);
				} else if (line.startsWith("U:")) {
					String[] lp = OptionParser.split(line, ":");
					if (lp.length != 3) {
						writeLine("invalid user role format");
						throw new IOException("invalid user role format");
					}
					String uid = lp[1];
					UserRole rr = UserRole.parse(lp[2]);
					ActivityServer.setUserRole(userId, uid, rr);
					//System.out.println(userId + " says user " +uid+ " is " + rr);
				} else {
					ClassLabel activity = ClassLabel.parse(line);
					ActivityServer.setUserActivity(userId, activity);
					//System.out.println(userId + " says activity is " + activity);
				}
			}

		} catch (ClosedByInterruptException e) {
			// thread interrupted, thats okay, do nothing
		} catch (IOException e) {
			System.out.println(dateFormat.format(new Date()) + remote + e.getMessage());
		}
		try {
			if (channel.isConnected()) channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (outputThread != null) outputThread.interrupt();
		System.out.println(dateFormat.format(new Date()) + remote + "disconnect");
	}

}
