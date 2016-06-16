import Controller from './Controller';
import DeployView from '../views/deploy';
import { FetchAction as DeployFetchAction} from '../actions/api/deploy';
import { fetchTask as TaskFetchAction} from '../actions/api/task';
import { FetchForDeployAction } from '../actions/api/tasks';
import { FetchForDeploy as TaskHistoryFetchForDeploy } from '../actions/api/taskHistory';

class DeployDetailController extends Controller {

    initialize({store, requestId, deployId}) {
        app.showPageLoader()
        this.title(`${requestId} deploy ${deployId}`);
        this.store = store;
        this.requestId = requestId;
        this.deployId = deployId;

        let promises = [];
        promises.push(this.store.dispatch(DeployFetchAction.trigger(requestId, deployId)));
        let tasksPromise = this.store.dispatch(FetchForDeployAction.trigger(requestId, deployId));
        promises.push(this.store.dispatch(TaskHistoryFetchForDeploy.clear()));
        tasksPromise.then(() => {
          for (let t of store.getState().api.activeTasksForDeploy.data) {
            this.store.dispatch(TaskFetchAction(t.taskId.id));
          }
        });
        promises.push(tasksPromise);
        promises.push(this.store.dispatch(TaskHistoryFetchForDeploy.trigger(requestId, deployId, 5, 1)));

        Promise.all(promises).then(() => {
          this.setView(new DeployView(store));
          app.showView(this.view);
        });
    }

    refresh() {
        this.store.dispatch(DeployFetchAction.trigger(this.requestId, this.deployId));
        let tasksPromise = this.store.dispatch(FetchForDeployAction.trigger(this.requestId, this.deployId));
        tasksPromise.then(() => {
          for (let t of this.store.getState().api.activeTasksForDeploy.data) {
            this.store.dispatch(TaskFetchAction(t.taskId.id));
          }
        });
    }
}


export default DeployDetailController;
