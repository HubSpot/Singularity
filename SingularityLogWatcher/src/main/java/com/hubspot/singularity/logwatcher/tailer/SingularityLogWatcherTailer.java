package com.hubspot.singularity.logwatcher.tailer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.logwatcher.SimpleStore;
import com.hubspot.singularity.logwatcher.TailMetadata;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration;

public class SingularityLogWatcherTailer implements Closeable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityLogWatcherTailer.class);

  private static final char END_OF_LINE_CHAR = '\n';
  
  private final TailMetadata tailMetadata;
  private final Path logfile;
  private final SeekableByteChannel byteChannel;
  private final ByteBuffer byteBuffer;
  private final WatchService watchService;
  private final LogForwarder logForwarder;
  private final SimpleStore store;
  private final SingularityLogWatcherConfiguration configuration;
  
  private volatile boolean stopped;
  
  public SingularityLogWatcherTailer(TailMetadata tailMetadata, SingularityLogWatcherConfiguration configuration, SimpleStore simpleStore, LogForwarder logForwarder) {
    this.tailMetadata = tailMetadata;
    this.logfile = Paths.get(tailMetadata.getFilename());
    this.configuration = configuration;
    this.store = simpleStore;
    this.logForwarder = logForwarder;
    this.byteBuffer = ByteBuffer.allocate(configuration.getByteBufferCapacity());
    this.byteChannel = openByteChannelAtCurrentPosition();
    this.watchService = createWatchService();

    this.stopped = false;
  }
  
  public void stop() {
    this.stopped = true;
  }
  
  public void consumeStream() throws IOException {
    checkRead(true);
  }
  
  public void close() {
    try {
      Closeables.close(byteChannel, true);
      Closeables.close(watchService, true);
    } catch (IOException ioe) {
      // impossible!
    }
  }
  
  private SeekableByteChannel openByteChannelAtCurrentPosition() {
    try {
      SeekableByteChannel channel = Files.newByteChannel(logfile, StandardOpenOption.READ);
      Optional<Long> previousPosition = store.getPosition(tailMetadata);
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

  public void watch() throws IOException, InterruptedException {
    LOG.info("Watching file {} at position {} with a {} byte buffer and minimum read size {}", logfile, byteChannel.position(), byteBuffer.capacity(), configuration.getMinimimReadSizeBytes());
    
    checkRead(false);
    
    WatchKey watchKey = logfile.toAbsolutePath().getParent().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
    
    while (!stopped) {
      if (watchKey != null) {
        processWatchKey(watchKey, byteChannel);
        
        if (!watchKey.reset()) {
          LOG.warn("WatchKey for {} no longer valid", logfile);
          break;
        }
      }
      
      watchKey = watchService.poll(configuration.getPollMillis(), TimeUnit.MILLISECONDS);
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

      if (kind != StandardWatchEventKinds.ENTRY_MODIFY) {
        LOG.trace("Ignoring an {} event to {}", event.context());
        continue;
      }
      
      WatchEvent<Path> ev = cast(event);
      Path filename = ev.context();
      
      if (!filename.equals(logfile.getFileName())) {
        LOG.trace("Ignoring a modification to {} (only care about {})", filename, logfile.getFileName());
        continue;
      }
      
      checkRead(false);
    }
    
    LOG.trace("Handled {} events in {}ms", events.size(), System.currentTimeMillis() - now);
  }
  
  private void checkRead(boolean force) throws IOException {
    if (!force && !shouldRead()) {
      LOG.trace("Ignoring update to {}, not enough bytes available (minimum {})", logfile, configuration.getMinimimReadSizeBytes());
      return;
    }
    
    int bytesRead = 0;
    int bytesLeft = 0;
    
    while ((bytesRead = read()) > 0) {
      bytesLeft = processByteBufferAndReturnRemainingBytes();
    
      LOG.trace("{} read {} bytes ({} left)", logfile, bytesRead, bytesLeft);
      
      if (bytesLeft != bytesRead) {
        updatePosition(bytesLeft);
      }
    }
    
    if (force && bytesLeft > 0) {
      String string = new String(byteBuffer.array(), byteBuffer.position() - bytesLeft, byteBuffer.position(), Charset.forName("UTF-8"));
      logForwarder.forwardMessage(tailMetadata, string);
    }
  }
  
  private boolean shouldRead() throws IOException {
    final long newBytesAvailable = byteChannel.size() - byteChannel.position();
    
    return newBytesAvailable >= configuration.getMinimimReadSizeBytes();
  }
  
  private int read() throws IOException {
    byteBuffer.clear();
    return byteChannel.read(byteBuffer);
  }
    
  private void updatePosition(int bytesLeft) throws IOException {
    long newPosition = byteChannel.position() - bytesLeft;
    
    store.savePosition(tailMetadata, newPosition);
    
    byteChannel.position(newPosition);
  }
  
  private int processByteBufferAndReturnRemainingBytes() {
    String string = new String(byteBuffer.array(), 0, byteBuffer.position(), Charset.forName("UTF-8"));
    
    LOG.trace("{} had a string with size {}", logfile, string.length());
    
    int lastNewLineIndex = 0;
    int nextNewLineIndex = string.indexOf(END_OF_LINE_CHAR);
    
    while (nextNewLineIndex >= 0) {
      logForwarder.forwardMessage(tailMetadata, string.substring(lastNewLineIndex, nextNewLineIndex));
      
      lastNewLineIndex = nextNewLineIndex + 1;
      nextNewLineIndex = string.indexOf(END_OF_LINE_CHAR, lastNewLineIndex);
    }
    
    int remainingBytes = string.substring(lastNewLineIndex).getBytes().length;
    
    return remainingBytes;
  }

}
