import Controller from './Controller';
import DeployView from '../views/deploy';

import {
  FetchTaskHistory,
  FetchActiveTasksForDeploy,
  FetchTaskHistoryForDeploy,
  FetchDeployForRequest
} from '../actions/api/history';

class DeployDetailController extends Controller {

  initialize({store, requestId, deployId}) {
    app.showPageLoader();
    this.title(`${requestId} deploy ${deployId}`);
    this.store = store;
    this.requestId = requestId;
    this.deployId = deployId;

    const promises = [];
    promises.push(this.store.dispatch(FetchDeployForRequest.trigger(requestId, deployId)));
    promises.push(this.store.dispatch(FetchActiveTasksForDeploy.trigger(requestId, deployId)));
    promises.push(this.store.dispatch(FetchTaskHistoryForDeploy.clearData()));
    promises.push(this.store.dispatch(FetchTaskHistoryForDeploy.trigger(requestId, deployId, 5, 1)));

    Promise.all(promises).then(() => {
      const readyPromise = [];
      for (const t of store.getState().api.activeTasksForDeploy.data) {
        readyPromise.push(this.store.dispatch(FetchTaskHistory.trigger(t.taskId.id)));
      }
      Promise.all(readyPromise).then(() => {
        this.setView(new DeployView(store));
        app.showView(this.view);
      });
    });
  }

  refresh() {
    this.store.dispatch(FetchDeployForRequest.trigger(this.requestId, this.deployId));
    const tasksPromise = this.store.dispatch(FetchActiveTasksForDeploy.trigger(this.requestId, this.deployId));
    tasksPromise.then(() => {
      for (const t of this.store.getState().api.activeTasksForDeploy.data) {
        this.store.dispatch(FetchTaskHistory.trigger(t.taskId.id));
      }
    });
  }
}


export default DeployDetailController;
