import Controller from './Controller';
import RequestDetailView from '../views/request';
import { FetchRequest } from '../actions/api/requests';
import { FetchActiveTasksForRequest } from '../actions/api/history';
import { FetchTaskCleanups } from '../actions/api/tasks';

class RequestDetailController extends Controller {

  initialize({requestId, store}) {
    app.showPageLoader();
    this.requestId = requestId;
    this.store = store;

    this.title(this.requestId);

    const initPromise = this.store.dispatch(FetchRequest.trigger(this.requestId));
    initPromise.then(() => {
      this.setView(new RequestDetailView({store, requestId}));
      app.showView(this.view);
    });
  }

  refresh() {
    this.store.dispatch(FetchRequest.trigger(this.requestId));
    // FetchActiveForRequest

    // const promises = [];
    // const requestPromise = this.store.dispatch(RequestFetchAction.trigger(this.requestId));
    // requestPromise.then(() => {
    //   const task = this.store.getState().api.task[this.taskId].data;
    //   promises.push(this.store.dispatch(DeployFetchAction.trigger(task.task.taskId.requestId, task.task.taskId.deployId)));
    //   if (task.isStillRunning) {
    //     promises.push(this.store.dispatch(TaskResourceUsageFetchAction.trigger(this.taskId)));
    //   }
    // });
    // promises.push(requestPromise);
    // promises.push(this.store.dispatch(TaskCleanupsFetchAction.trigger()));
    // promises.push(this.store.dispatch(DeploysFetchAction.trigger('pending')));
    // promises.push(this.store.dispatch(TaskS3LogsFetchAction.trigger(this.taskId)));
    // return Promise.all(promises);
  }
}


export default RequestDetailController;
