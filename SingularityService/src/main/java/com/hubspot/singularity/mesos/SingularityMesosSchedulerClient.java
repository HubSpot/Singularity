package com.hubspot.singularity.mesos;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.ExecutorID;
import org.apache.mesos.v1.Protos.Filters;
import org.apache.mesos.v1.Protos.FrameworkID;
import org.apache.mesos.v1.Protos.FrameworkInfo;
import org.apache.mesos.v1.Protos.KillPolicy;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.scheduler.Protos.Call;
import org.apache.mesos.v1.scheduler.Protos.Call.Accept;
import org.apache.mesos.v1.scheduler.Protos.Call.Acknowledge;
import org.apache.mesos.v1.scheduler.Protos.Call.Builder;
import org.apache.mesos.v1.scheduler.Protos.Call.Decline;
import org.apache.mesos.v1.scheduler.Protos.Call.Kill;
import org.apache.mesos.v1.scheduler.Protos.Call.Message;
import org.apache.mesos.v1.scheduler.Protos.Call.Reconcile;
import org.apache.mesos.v1.scheduler.Protos.Call.Request;
import org.apache.mesos.v1.scheduler.Protos.Call.Shutdown;
import org.apache.mesos.v1.scheduler.Protos.Call.Type;
import org.apache.mesos.v1.scheduler.Protos.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.UIConfiguration;
import com.hubspot.singularity.resources.UiResource;
import com.mesosphere.mesos.rx.java.AwaitableSubscription;
import com.mesosphere.mesos.rx.java.MesosClientBuilder;
import com.mesosphere.mesos.rx.java.SinkOperation;
import com.mesosphere.mesos.rx.java.SinkOperations;
import com.mesosphere.mesos.rx.java.protobuf.ProtobufMesosClientBuilder;
import com.mesosphere.mesos.rx.java.util.UserAgentEntries;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

/**
 * Implementation of communication with the Mesos Master. To correctly close the
 * connection when done you need to call {@link #close()}.
 * <p>
 * http://mesos.apache.org/documentation/latest/scheduler-http-api/
 */
public class SingularityMesosSchedulerClient {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosSchedulerClient.class);

  private final SingularityConfiguration configuration;
  private final MesosConfiguration mesosConfiguration;
  private final String singularityUriBase;

  private SerializedSubject<Optional<SinkOperation<Call>>, Optional<SinkOperation<Call>>> publisher;
  private FrameworkID frameworkId;
  private AwaitableSubscription openStream;
  private Thread subscriberThread;

  @Inject
  public SingularityMesosSchedulerClient(SingularityConfiguration configuration, @Named(SingularityMainModule.SINGULARITY_URI_BASE) final String singularityUriBase) {
    this.configuration = configuration;
    this.mesosConfiguration = configuration.getMesosConfiguration();
    this.singularityUriBase = singularityUriBase;
  }

  /**
   * The first call to mesos, needed to setup connection properly and identify
   * a framework.
   *
   * @throws URISyntaxException if the URL provided was not a syntactically correct URL.
   */
  public void subscribe(String mesosMasterURI, SingularityMesosScheduler scheduler) throws URISyntaxException {

    FrameworkInfo frameworkInfo = buildFrameworkInfo();

    if (mesosMasterURI == null || mesosMasterURI.contains("zk:")) {
      throw new IllegalArgumentException(String.format("Must use master address for http api (e.g. http://localhost:5050/api/v1/scheduler) was %s", mesosMasterURI));
    }

    if (openStream == null || openStream.isUnsubscribed()) {

      // Do we get here ever?
      if (subscriberThread != null) {
        subscriberThread.interrupt();
      }

      subscriberThread = new Thread() {
        public void run() {
          try {
            connect(URI.create(mesosMasterURI), frameworkInfo, scheduler);
          } catch (URISyntaxException e) {
            LOG.error("Could not connect: ", e);
          }
        }

      };
      subscriberThread.start();
    }
  }

  private FrameworkInfo buildFrameworkInfo() {
    final FrameworkInfo.Builder frameworkInfoBuilder = FrameworkInfo.newBuilder()
        .setCheckpoint(mesosConfiguration.isCheckpoint())
        .setFailoverTimeout(mesosConfiguration.getFrameworkFailoverTimeout())
        .setName(mesosConfiguration.getFrameworkName())
        .setId(FrameworkID.newBuilder().setValue(mesosConfiguration.getFrameworkId()))
        .setUser(mesosConfiguration.getFrameworkUser()); // https://issues.apache.org/jira/browse/MESOS-3747

    if (configuration.getHostname().isPresent()) {
      frameworkInfoBuilder.setHostname(configuration.getHostname().get());
    }

    // only set the web UI URL if it's fully qualified
    if (singularityUriBase.startsWith("http://") || singularityUriBase.startsWith("https://")) {
      if (configuration.getUiConfiguration().getRootUrlMode() == UIConfiguration.RootUrlMode.INDEX_CATCHALL) {
        frameworkInfoBuilder.setWebuiUrl(singularityUriBase);
      } else {
        frameworkInfoBuilder.setWebuiUrl(singularityUriBase + UiResource.UI_RESOURCE_LOCATION);
      }
    }

    if (mesosConfiguration.getFrameworkRole().isPresent()) {
      frameworkInfoBuilder.setRole(mesosConfiguration.getFrameworkRole().get());
    }

    return frameworkInfoBuilder.build();
  }

  /**
   * Sets up the connection and is blocking in wait for calls from mesos
   * master.
   */
  private void connect(URI mesosMasterURI, FrameworkInfo frameworkInfo, SingularityMesosScheduler scheduler) throws URISyntaxException {

    MesosClientBuilder<Call, Event> clientBuilder = ProtobufMesosClientBuilder.schedulerUsingProtos()
        .mesosUri(mesosMasterURI)
        .applicationUserAgentEntry(UserAgentEntries.userAgentEntryForMavenArtifact("com.hubspot.singularity", "SingularityService"));

    Call subscribeCall = Call.newBuilder()
        .setType(Call.Type.SUBSCRIBE)
        .setFrameworkId(frameworkInfo.getId())
        .setSubscribe(Call.Subscribe.newBuilder()
            .setFrameworkInfo(frameworkInfo)
            .build())
        .build();

    MesosClientBuilder<Call, Event> subscribe = clientBuilder.subscribe(subscribeCall);

    subscribe.processStream(unicastEvents -> {

      final Observable<Event> events = unicastEvents.share();

      events.filter(event -> event.getType() == Event.Type.ERROR)
          .map(event -> event.getError().getMessage())
          .subscribe(scheduler::error, scheduler::onUncaughtException);

      events.filter(event -> event.getType() == Event.Type.FAILURE)
          .map(Event::getFailure)
          .subscribe(scheduler::failure, scheduler::onUncaughtException);

      events.filter(event -> event.getType() == Event.Type.HEARTBEAT)
          .subscribe(scheduler::heartbeat, scheduler::onUncaughtException);

      events.filter(event -> event.getType() == Event.Type.INVERSE_OFFERS)
          .map(event -> event.getInverseOffers().getInverseOffersList())
          .subscribe(scheduler::inverseOffers, scheduler::onUncaughtException);

      events.filter(event -> event.getType() == Event.Type.MESSAGE)
          .map(Event::getMessage)
          .subscribe(scheduler::message, scheduler::onUncaughtException);

      events.filter(event -> event.getType() == Event.Type.OFFERS)
          .map(event -> event.getOffers().getOffersList())
          .subscribe(scheduler::resourceOffers, scheduler::onUncaughtException);

      events.filter(event -> event.getType() == Event.Type.RESCIND)
          .map(event -> event.getRescind().getOfferId())
          .subscribe(
              event -> scheduler.rescind(event.getRescind().getOfferId()),
              scheduler::onUncaughtException
          );

      events.filter(event -> event.getType() == Event.Type.RESCIND_INVERSE_OFFER)
          .subscribe(
              event -> scheduler.rescindInverseOffer(event.getRescindInverseOffer().getInverseOfferId()),
              scheduler::onUncaughtException
          );

      events.filter(event -> event.getType() == Event.Type.SUBSCRIBED).subscribe(e -> {
        this.frameworkId = e.getSubscribed().getFrameworkId();
        scheduler.subscribed(e.getSubscribed());
      }, scheduler::onUncaughtException);

      events.filter(event -> event.getType() == Event.Type.UPDATE).subscribe(e -> {
        TaskStatus status = e.getUpdate().getStatus();
        acknowledge(status.getAgentId(), status.getTaskId(), status.getUuid());
        scheduler.statusUpdate(status);
      }, scheduler::onUncaughtException);

      // This is the observable that is responsible for sending calls to mesos master.
      PublishSubject<Optional<SinkOperation<Call>>> p = PublishSubject.create();

      // toSerialised handles the fact that we can add calls on different threads.
      publisher = p.toSerialized();
      return publisher;
    });

    com.mesosphere.mesos.rx.java.MesosClient<Call, Event> client = clientBuilder.build();
    openStream = client.openStream();
    try {
      openStream.await();
    } catch (Throwable t) {
      LOG.error("Observable was unexpectedly closed", t);
      scheduler.onUncaughtException(t);
    }
  }

  /**
   * Closes the connection to mesos.
   */
  public void close() {
    if (openStream != null) {
      if (!openStream.isUnsubscribed()) {
        openStream.unsubscribe();
      }
    }
  }

  public void sendCall(Call call) {
    if (publisher == null) {
      throw new RuntimeException("No publisher found, please call subscribe before sending anything.");
    }
    publisher.onNext(Optional.of(SinkOperations.create(call)));
  }

  private void sendCall(Call.Builder b, Type t) {
    Call call = b.setType(t).setFrameworkId(frameworkId).build();
    sendCall(call);
  }

  // This is a dangerous call, removes all tasks associated with the framework :donotwant:
  public void teardown() {
    sendCall(build(), Type.TEARDOWN);
  }

  public void accept(List<OfferID> offerIds, List<Offer.Operation> offerOperations) {
    Builder accept = build()
        .setAccept(Accept.newBuilder().addAllOfferIds(offerIds).addAllOperations(offerOperations));
    sendCall(accept, Type.ACCEPT);
  }

  public void accept(List<OfferID> offerIds, List<Offer.Operation> offerOperations, Filters filters) {
    Builder accept = build().setAccept(
        Accept.newBuilder().addAllOfferIds(offerIds).addAllOperations(offerOperations).setFilters(filters));
    sendCall(accept, Type.ACCEPT);
  }

  public void decline(List<OfferID> offerIds) {
    Builder decline = build().setDecline(Decline.newBuilder().addAllOfferIds(offerIds));
    sendCall(decline, Type.DECLINE);
  }

  public void decline(List<OfferID> offerIds, Filters filters) {
    Builder decline = build().setDecline(Decline.newBuilder().addAllOfferIds(offerIds).setFilters(filters));
    sendCall(decline, Type.DECLINE);
  }

  public void kill(TaskID taskId) {
    Builder kill = build().setKill(Kill.newBuilder().setTaskId(taskId));
    sendCall(kill, Type.KILL);
  }

  public void kill(TaskID taskId, AgentID agentId, KillPolicy killPolicy) {
    Builder kill = build()
        .setKill(Kill.newBuilder().setTaskId(taskId).setAgentId(agentId).setKillPolicy(killPolicy));
    sendCall(kill, Type.KILL);
  }

  public void kill(TaskID taskId, KillPolicy killPolicy) {
    Builder kill = build().setKill(Kill.newBuilder().setTaskId(taskId).setKillPolicy(killPolicy));
    sendCall(kill, Type.KILL);
  }

  public void kill(TaskID taskId, AgentID agentId) {
    Builder kill = build().setKill(Kill.newBuilder().setTaskId(taskId).setAgentId(agentId));
    sendCall(kill, Type.KILL);
  }

  public void revive() {
    Builder revive = build();
    sendCall(revive, Type.REVIVE);
  }

  public void shutdown(ExecutorID executorId) {
    Builder shutdown = build().setShutdown(Shutdown.newBuilder().setExecutorId(executorId));
    sendCall(shutdown, Type.SHUTDOWN);
  }

  public void shutdown(ExecutorID executorId, AgentID agentId) {
    Builder shutdown = build().setShutdown(Shutdown.newBuilder().setExecutorId(executorId).setAgentId(agentId));
    sendCall(shutdown, Type.SHUTDOWN);
  }

  public void acknowledge(AgentID agentId, TaskID taskId, ByteString uuid) {
    Builder acknowledge = build()
        .setAcknowledge(Acknowledge.newBuilder().setAgentId(agentId).setTaskId(taskId).setUuid(uuid));
    sendCall(acknowledge, Type.ACKNOWLEDGE);
  }

  public void reconcile(List<Reconcile.Task> tasks) {
    Builder reconsile = build().setReconcile(Reconcile.newBuilder().addAllTasks(tasks));
    sendCall(reconsile, Type.RECONCILE);
  }

  public void frameworkMessage(ExecutorID executorId, AgentID agentId, byte[] data) {
    Builder message = build()
        .setMessage(Message.newBuilder().setAgentId(agentId).setExecutorId(executorId).setData(ByteString.copyFrom(data)));
    sendCall(message, Type.MESSAGE);
  }

  public void request(List<org.apache.mesos.v1.Protos.Request> requests) {
    Builder request = build().setRequest(Request.newBuilder().addAllRequests(requests));
    sendCall(request, Type.REQUEST);
  }

  private static Builder build() {
    return Call.newBuilder();
  }

  public boolean isRunning() {
    return publisher != null;
  }
}
