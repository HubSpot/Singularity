import Controller from './Controller';
import DeployView from '../views/deploy';
import { FetchAction as DeployFetchAction} from '../actions/api/deploy';
import { fetchTask as TaskFetchAction} from '../actions/api/task';
import { FetchForDeployAction } from '../actions/api/tasks';

class DeployDetailController extends Controller {

    initialize({store, requestId, deployId}) {
        app.showPageLoader()
        this.title('Status');
        this.store = store;
        this.requestId = requestId;
        this.deployId = deployId;

        let initPromise = this.store.dispatch(DeployFetchAction.trigger(requestId, deployId));
        initPromise.then(() => {
          this.setView(new DeployView(store));
          app.showView(this.view);
        });

        let tasksPromise = this.store.dispatch(FetchForDeployAction.trigger(requestId, deployId));
        tasksPromise.then(() => {
          for (let t of store.getState().api.activeTasksForDeploy.data) {
            this.store.dispatch(TaskFetchAction(t.taskId.id));
          }
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
