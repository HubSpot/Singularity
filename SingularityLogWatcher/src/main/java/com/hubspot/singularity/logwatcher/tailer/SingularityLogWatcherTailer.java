package com.hubspot.singularity.logwatcher.tailer;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.logwatcher.SimpleStore;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration;
import com.hubspot.singularity.runner.base.shared.TailMetadata;
import com.hubspot.singularity.runner.base.shared.WatchServiceHelper;

public class SingularityLogWatcherTailer extends WatchServiceHelper implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityLogWatcherTailer.class);

  private static final char END_OF_LINE_CHAR = '\n';

  private final TailMetadata tailMetadata;
  private final Path logfile;
  private final SeekableByteChannel byteChannel;
  private final ByteBuffer byteBuffer;
  private final LogForwarder logForwarder;
  private final SimpleStore store;

  public SingularityLogWatcherTailer(TailMetadata tailMetadata, SingularityLogWatcherConfiguration configuration, SimpleStore simpleStore, LogForwarder logForwarder) {
    super(configuration.getPollMillis(), Paths.get(tailMetadata.getFilename()).getParent(), Collections.singletonList(StandardWatchEventKinds.ENTRY_MODIFY));

    this.tailMetadata = tailMetadata;
    this.logfile = Paths.get(tailMetadata.getFilename());
    this.store = simpleStore;
    this.logForwarder = logForwarder;
    this.byteBuffer = ByteBuffer.allocate(configuration.getByteBufferCapacity());
    this.byteChannel = openByteChannelAtCurrentPosition();
  }

  public void consumeStream() throws IOException {
    checkRead(true);
  }

  @Override
  public void close() {
    try {
      Closeables.close(byteChannel, true);
    } catch (IOException ioe) {
      // impossible!
    }

    super.close();
  }

  private SeekableByteChannel openByteChannelAtCurrentPosition() {
    try {
      SeekableByteChannel channel = Files.newByteChannel(logfile, StandardOpenOption.READ);
      Optional<Long> previousPosition = store.getPosition(tailMetadata);
      if (previousPosition.isPresent()) {
        long storePosition = previousPosition.get();

        if (storePosition < channel.size()) {
          LOG.warn("Found {} with size {} and position {}, resetting to 0", logfile, channel.size(), storePosition);
          savePosition(0);
        } else {
          channel.position(previousPosition.get());
        }
      }
      return channel;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public void watch() throws IOException, InterruptedException {
    LOG.info("Watching file {} at position {} with a {} byte buffer", logfile, byteChannel.position(), byteBuffer.capacity());

    checkRead(tailMetadata.isFinished());

    if (!tailMetadata.isFinished()) {
      super.watch();
    }
  }

  @Override
  protected boolean processEvent(Kind<?> kind, Path filename) throws IOException {
    if (!filename.equals(logfile.getFileName())) {
      LOG.trace("Ignoring a modification to {} (only care about {})", filename, logfile.getFileName());
      return true;
    }

    checkRead(false);

    return true;
  }

  // TODO TEST handling resize
  private void checkRead(boolean readAllBytes) throws IOException {
    checkPosition();

    int bytesRead = 0;
    int bytesLeft = 0;

    while ((bytesRead = read()) > 0) {
      bytesLeft = processByteBufferAndReturnRemainingBytes();

      LOG.trace("{} read {} bytes ({} left)", logfile, bytesRead, bytesLeft);

      if (bytesLeft != bytesRead) {
        updatePosition(bytesLeft);
      }
    }

    if (readAllBytes && (bytesLeft > 0)) {
      String string = new String(byteBuffer.array(), byteBuffer.position() - bytesLeft, byteBuffer.position(), UTF_8);
      logForwarder.forwardMessage(tailMetadata, string);
    }
  }

  private void checkPosition() throws IOException {
    if (byteChannel.position() > byteChannel.size()) {
      resetPosition();
    }
  }

  private void resetPosition() throws IOException {
    byteChannel.position(0);
    savePosition(0);
  }

  private int read() throws IOException {
    byteBuffer.clear();
    return byteChannel.read(byteBuffer);
  }

  private void savePosition(long newPosition) {
    store.savePosition(tailMetadata, newPosition);
  }

  private void updatePosition(int bytesLeft) throws IOException {
    long newPosition = byteChannel.position() - bytesLeft;

    savePosition(newPosition);

    byteChannel.position(newPosition);
  }

  private int processByteBufferAndReturnRemainingBytes() {
    String string = new String(byteBuffer.array(), 0, byteBuffer.position(), UTF_8);

    LOG.trace("{} had a string with size {}", logfile, string.length());

    int lastNewLineIndex = 0;
    int nextNewLineIndex = string.indexOf(END_OF_LINE_CHAR);

    while (nextNewLineIndex >= 0) {
      logForwarder.forwardMessage(tailMetadata, string.substring(lastNewLineIndex, nextNewLineIndex));

      lastNewLineIndex = nextNewLineIndex + 1;
      nextNewLineIndex = string.indexOf(END_OF_LINE_CHAR, lastNewLineIndex);
    }

    int remainingBytes = string.substring(lastNewLineIndex).getBytes(UTF_8).length;

    return remainingBytes;
  }

}
