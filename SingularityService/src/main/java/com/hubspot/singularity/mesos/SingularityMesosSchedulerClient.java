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
import org.apache.mesos.v1.Protos.InverseOffer;
import org.apache.mesos.v1.Protos.KillPolicy;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.scheduler.Protos;
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

import com.google.protobuf.ByteString;
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
public abstract class SingularityMesosSchedulerClient {
  private static final Logger log = LoggerFactory.getLogger(SingularityMesosSchedulerClient.class);

  private SerializedSubject<Optional<SinkOperation<Call>>, Optional<SinkOperation<Call>>> publisher;

  private FrameworkID frameworkId;

  private AwaitableSubscription openStream;

  private Thread subscriberThread;

  /**
   * The first call to mesos, needed to setup connection properly and identify
   * a framework.
   *
   * @throws URISyntaxException if the URL provided was not a syntactically correct URL.
   */
  public void subscribe(URI mesosMasterURI, FrameworkInfo frameworkInfo) throws URISyntaxException {

    if (openStream == null || openStream.isUnsubscribed()) {

      // Do we get here ever?
      if (subscriberThread != null) {
        subscriberThread.interrupt();
      }

      subscriberThread = new Thread() {
        public void run() {
          try {
            connect(mesosMasterURI, frameworkInfo);
          } catch (URISyntaxException e) {
            log.error("Could not connect: ", e);
          }
        }

      };
      subscriberThread.start();
    }
  }

  /**
   * Sets up the connection and is blocking in wait for calls from mesos
   * master.
   */
  private void connect(URI mesosMasterURI, FrameworkInfo frameworkInfo) throws URISyntaxException {

    MesosClientBuilder<Call, Event> clientBuilder = ProtobufMesosClientBuilder.schedulerUsingProtos()
        .mesosUri(mesosMasterURI)
        .applicationUserAgentEntry(UserAgentEntries.userAgentEntryForMavenArtifact("com.hubspot.singularity", "Singularity Scheduler"));

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
          .subscribe(e -> error(e.getError().getMessage()));

      events.filter(event -> event.getType() == Event.Type.FAILURE)
          .subscribe(e -> failure(e.getFailure()));

      events.filter(event -> event.getType() == Event.Type.HEARTBEAT).subscribe(e -> heartbeat());

      events.filter(event -> event.getType() == Event.Type.INVERSE_OFFERS)
          .subscribe(e -> inverseOffers(e.getInverseOffers().getInverseOffersList()));

      events.filter(event -> event.getType() == Event.Type.MESSAGE)
          .subscribe(e -> message(e.getMessage()));

      events.filter(event -> event.getType() == Event.Type.OFFERS)
          .subscribe(e -> resourceOffers(e.getOffers().getOffersList()));

      events.filter(event -> event.getType() == Event.Type.RESCIND)
          .subscribe(e -> rescind(e.getRescind().getOfferId()));

      events.filter(event -> event.getType() == Event.Type.RESCIND_INVERSE_OFFER)
          .subscribe(e -> rescindInverseOffer(e.getRescindInverseOffer().getInverseOfferId()));

      events.filter(event -> event.getType() == Event.Type.SUBSCRIBED).subscribe(e -> {
        this.frameworkId = e.getSubscribed().getFrameworkId();
        subscribed(e.getSubscribed());
      });

      events.filter(event -> event.getType() == Event.Type.UPDATE).subscribe(e -> {
        // Ack is done in the statusUpdate method
        statusUpdate(e.getUpdate().getStatus());
      });

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
    } catch (Throwable e) {
      e.printStackTrace();
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

  /**
   * First event received when the scheduler subscribes. This contains the
   * frameworkId generated by mesos that should be used if the scheduler needs
   * to reconnect in the future.
   *
   * @param subscribed Data regarding your subscription
   */
  public abstract void subscribed(Protos.Event.Subscribed subscribed);

  /**
   * Received whenever there are new resources that are offered to the
   * scheduler. Each offer corresponds to a set of resources on an agent.
   * Until the scheduler accepts or declines an offer the resources are
   * considered allocated to the scheduler.
   *
   * @param offers A list of offers from mesos
   */
  public abstract void resourceOffers(List<Offer> offers);

  /**
   * Received whenever there are resources requested back from the scheduler.
   * Each inverse offer specifies the agent, and optionally specific
   * resources. Accepting or Declining an inverse offer informs the allocator
   * of the scheduler's ability to release the specified resources without
   * violating an SLA. If no resources are specified then all resources on the
   * agent are requested to be released.
   *
   * @param offers A list of reverse offers from mesos
   */
  public abstract void inverseOffers(List<InverseOffer> offers);

  /**
   * Received when a particular offer is no longer valid (e.g., the agent
   * corresponding to the offer has been removed) and hence needs to be
   * rescinded. Any future calls ('Accept' / 'Decline') made by the scheduler
   * regarding this offer will be invalid.
   *
   * @param offerId the recinded offer
   */
  public abstract void rescind(OfferID offerId);

  /**
   * Received when a particular inverse offer is no longer valid (e.g., the
   * agent corresponding to the offer has been removed) and hence needs to be
   * rescinded. Any future calls ('Accept' / 'Decline') made by the scheduler
   * regarding this inverse offer will be invalid.
   *
   * @param offerId The rescind inverse offer id
   */
  public abstract void rescindInverseOffer(OfferID offerId);

  /**
   * Received whenever there is a status update that is generated by the
   * executor or agent or master. Status updates should be used by executors
   * to reliably communicate the status of the tasks that they manage. It is
   * crucial that a terminal update (see TaskState in v1/mesos.proto) is sent
   * by the executor as soon as the task terminates, in order for Mesos to
   * release the resources allocated to the task. It is also the
   * responsibility of the scheduler to explicitly acknowledge the receipt of
   * a status update. See 'Acknowledge' in the 'Call' section below for the
   * semantics.
   *
   * @param update Contains info about the current tasks status
   */
  public abstract void statusUpdate(TaskStatus update);

  /**
   * Received when a custom message generated by the executor is forwarded by
   * the master. Note that this message is not interpreted by Mesos and is
   * only forwarded (without reliability guarantees) to the scheduler. It is
   * up to the executor to retry if the message is dropped for any reason.
   *
   * @param message Message sent from executor
   */
  public abstract void message(Protos.Event.Message message);

  /**
   * Received when an agent is removed from the cluster (e.g., failed health
   * checks) or when an executor is terminated. Note that, this event
   * coincides with receipt of terminal UPDATE events for any active tasks
   * belonging to the agent or executor and receipt of 'Rescind' events for
   * any outstanding offers belonging to the agent. Note that there is no
   * guaranteed order between the 'Failure', 'Update' and 'Rescind' events
   * when an agent or executor is removed.
   *
   * @param failure Information regarding the current failure
   */
  public abstract void failure(Protos.Event.Failure failure);

  /**
   * Received when there is an unrecoverable error in the scheduler (e.g.,
   * scheduler failed over, rate limiting, authorization errors etc.). The
   * scheduler should abort on receiving this event.
   *
   * @param message Error message
   */
  public abstract void error(String message);

  /**
   * Periodic message sent by the Mesos master according to
   * 'Subscribed.heartbeat_interval_seconds'. If the scheduler does not
   * receive any events (including heartbeats) for an extended period of time
   * (e.g., 5 x heartbeat_interval_seconds), there is likely a network
   * partition. In such a case the scheduler should close the existing
   * subscription connection and resubscribe using a backoff strategy.
   */
  public abstract void heartbeat();

}
