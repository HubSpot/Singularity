package com.hubspot.singularity.logwatcher.tailer;

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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.logwatcher.SimpleStore;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration;
import com.hubspot.singularity.logwatcher.impl.WatchServiceHelper;
import com.hubspot.singularity.logwatcher.logrotate.LogrotateTemplateContext;
import com.hubspot.singularity.logwatcher.logrotate.LogrotateTemplateManager;
import com.hubspot.singularity.runner.base.config.TailMetadata;

public class SingularityLogWatcherTailer extends WatchServiceHelper implements Closeable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityLogWatcherTailer.class);

  private static final char END_OF_LINE_CHAR = '\n';

  private final TailMetadata tailMetadata;
  private final Path logfile;
  private final SeekableByteChannel byteChannel;
  private final ByteBuffer byteBuffer;
  private final LogForwarder logForwarder;
  private final SimpleStore store;
  private final LogrotateTemplateManager logrotateTemplateManager;;
  
  public SingularityLogWatcherTailer(TailMetadata tailMetadata, SingularityLogWatcherConfiguration configuration, LogrotateTemplateManager logrotateTemplateManager, SimpleStore simpleStore, LogForwarder logForwarder) {
    super(configuration, Paths.get(tailMetadata.getFilename()).toAbsolutePath().getParent(), Collections.singletonList(StandardWatchEventKinds.ENTRY_MODIFY));
    this.tailMetadata = tailMetadata;
    this.logrotateTemplateManager = logrotateTemplateManager;
    this.logfile = Paths.get(tailMetadata.getFilename());
    this.store = simpleStore;
    this.logForwarder = logForwarder;
    this.byteBuffer = ByteBuffer.allocate(configuration.getByteBufferCapacity());
    this.byteChannel = openByteChannelAtCurrentPosition();
  }

  public void consumeStream() throws IOException {
    checkRead(true);
  }

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
        channel.position(previousPosition.get());
      }
      return channel;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

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

  private void checkRead(boolean readAllBytes) throws IOException {
    int bytesRead = 0;
    int bytesLeft = 0;

    while ((bytesRead = read()) > 0) {
      bytesLeft = processByteBufferAndReturnRemainingBytes();

      LOG.trace("{} read {} bytes ({} left)", logfile, bytesRead, bytesLeft);

      if (bytesLeft != bytesRead) {
        updatePosition(bytesLeft);
      }
    }
    
    if (readAllBytes && bytesLeft > 0) {
      String string = new String(byteBuffer.array(), byteBuffer.position() - bytesLeft, byteBuffer.position(), JavaUtils.CHARSET_UTF8);
      logForwarder.forwardMessage(tailMetadata, string);
    }
    
    if (!readAllBytes) {
      checkLogrotate();
    } else {
      logrotate();
    }
  }

  private void checkLogrotate() throws IOException {
    if (byteChannel.position() > getConfiguration().getLogrotateAfterBytes()) {
      logrotate();
    }
  }
  
  private void logrotate() throws IOException {
    Path tempFilePath = Files.createTempFile(null, ".logrotate");
    
    logrotateTemplateManager.writeRunnerScript(tempFilePath.toAbsolutePath(), 
        new LogrotateTemplateContext(getConfiguration().getS3QueueDirectory().toAbsolutePath().toString(), logfile.toAbsolutePath().toString()));
    List<String> command = ImmutableList.of("logrotate", "-f", "-v", tempFilePath.toAbsolutePath().toString());
    
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.inheritIO();
    
    LOG.debug("Logrotating {} by calling {}", logfile, command);
    
    try {
      int exitCode = processBuilder.start().waitFor();
      
      if (exitCode != 0) {
        // TODO hadnle failed log rotate
        
      }
    } catch (InterruptedException ie) {
      throw Throwables.propagate(ie);
    }
    // TODO handle this.
    byteChannel.position(0);
    updatePosition(0);
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
    String string = new String(byteBuffer.array(), 0, byteBuffer.position(), JavaUtils.CHARSET_UTF8);

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
