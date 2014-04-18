package com.hubspot.singularity.logwatcher.tailer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.logwatcher.SimpleStore;

public class SingularityLogWatcherTailer {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityLogWatcherTailer.class);

  private static final char END_OF_LINE_CHAR = '\n';
  
  private final String tag;
  private final Path logfile;
  private final SeekableByteChannel byteChannel;
  private final ByteBuffer byteBuffer;
  private final WatchService watchService;
  private final LogForwarder logForwarder;
  private final SimpleStore store;
  private final long minimumReadSizeBytes;
  
  public SingularityLogWatcherTailer(String tag, Path logfile, int byteBufferCapacity, long minimumReadSizeBytes, SimpleStore simpleStore, LogForwarder logForwarder) {
    this.tag = tag;
    this.logfile = logfile;
    this.minimumReadSizeBytes = minimumReadSizeBytes;
    this.store = simpleStore;
    this.logForwarder = logForwarder;
    this.byteBuffer = ByteBuffer.allocate(byteBufferCapacity);
    this.byteChannel = openByteChannelAtCurrentPosition();
    this.watchService = createWatchService();
  }
  
  private SeekableByteChannel openByteChannelAtCurrentPosition() {
    try {
      SeekableByteChannel channel = Files.newByteChannel(logfile, StandardOpenOption.READ);
      Optional<Long> previousPosition = store.getPosition(logfile);
      if (previousPosition.isPresent()) {
        channel.position(previousPosition.get());
      }
      return channel;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
  
  private WatchService createWatchService() {
    try {
      return FileSystems.getDefault().newWatchService();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public void watch() throws Exception { // TODO
    LOG.info("Watching file {} at position {} with a {} byte buffer and minimum read size {}", logfile, byteChannel.position(), byteBuffer.capacity(), minimumReadSizeBytes);
    
    checkRead();
    
    WatchKey watchKey = logfile.toAbsolutePath().getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
    
    while (true) {
      processWatchKey(watchKey, byteChannel);
      
      if (!watchKey.reset()) {
        LOG.warn("WatchKey for {} no longer valid", logfile);
        break;
      }
      
      watchKey = watchService.take(); // handle IE exception
    }
  }

  @SuppressWarnings("unchecked")
  private WatchEvent<Path> cast(WatchEvent<?> event) {
    return (WatchEvent<Path>) event;
  }
  
  private void processWatchKey(WatchKey watchKey, SeekableByteChannel byteChannel) throws IOException {
    final long now = System.currentTimeMillis();
    final List<WatchEvent<?>> events = watchKey.pollEvents();
    
    for (WatchEvent<?> event : events) {
      WatchEvent.Kind<?> kind = event.kind();

      if (kind == StandardWatchEventKinds.OVERFLOW) {
        LOG.trace("Ignoring an overflow event");
        continue;
      }

      WatchEvent<Path> ev = cast(event);
      Path filename = ev.context();
      
      if (!filename.equals(logfile.getFileName())) {
        LOG.trace("Ignoring a modification to {} (only care about {})", filename, logfile.getFileName());
        continue;
      }
      
      checkRead();
      
      checkForRotate();
    }
    
    LOG.trace("Handled {} events in {}ms", events.size(), System.currentTimeMillis() - now);
  }
  
  private void checkRead() throws IOException {
    if (!shouldRead()) {
      LOG.trace("Ignoring update to {}, not enough bytes available (minimum: {}", logfile, minimumReadSizeBytes);
      return;
    }
    
    int bytesRead = 0;
    
    while ((bytesRead = read()) > 0) {
      int bytesLeft = processByteBufferAndReturnRemainingBytes();
    
      LOG.trace("{} read {} bytes ({} left)", logfile, bytesRead, bytesLeft);
      
      if (bytesLeft != bytesRead) {
        updatePosition(bytesLeft);
      }
    }
  }
  
  private boolean shouldRead() throws IOException {
    final long newBytesAvailable = byteChannel.size() - byteChannel.position();
    
    return newBytesAvailable >= minimumReadSizeBytes;
  }
  
  private int read() throws IOException {
    byteBuffer.clear();
    return byteChannel.read(byteBuffer);
  }
  
  private void checkForRotate() {
    
  }
  
  private void updatePosition(int bytesLeft) throws IOException {
    long newPosition = byteChannel.position() - bytesLeft;
    
    store.savePosition(logfile, newPosition);
    
    byteChannel.position(newPosition);
  }
  
  private int processByteBufferAndReturnRemainingBytes() {
    String string = new String(byteBuffer.array(), 0, byteBuffer.position(), Charset.forName("UTF-8"));
    
    LOG.trace("{} had a string with size {}", logfile, string.length());
    
    int lastNewLineIndex = 0;
    int nextNewLineIndex = string.indexOf(END_OF_LINE_CHAR);
    
    while (nextNewLineIndex >= 0) {
      logForwarder.forwardMessage(tag, string.substring(lastNewLineIndex, nextNewLineIndex));
      
      lastNewLineIndex = nextNewLineIndex + 1;
      nextNewLineIndex = string.indexOf(END_OF_LINE_CHAR, lastNewLineIndex);
    }
    
    int remainingBytes = string.substring(lastNewLineIndex).getBytes().length;
    
    return remainingBytes;
  }

}
