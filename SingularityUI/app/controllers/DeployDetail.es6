import Controller from './Controller';
import DeployView from '../views/deploy';
import { FetchAction } from '../actions/api/deploy';
import { FetchForDeployAction } from '../actions/api/tasks';

class DeployDetailController extends Controller {

    initialize({store, requestId, deployId}) {
        app.showPageLoader()
        this.title('Status');
        this.store = store;
        this.requestId = requestId;
        this.deployId = deployId;

        let initPromise = this.store.dispatch(FetchAction.trigger(requestId, deployId));
        let tasksPromise = this.store.dispatch(FetchForDeployAction.trigger(requestId, deployId));
        initPromise.then(() => {
          this.setView(new DeployView(store));
          app.showView(this.view);
        });
    }

    refresh() {
        this.store.dispatch(FetchAction.trigger(this.requestId, this.deployId));
        this.store.dispatch(FetchForDeployAction.trigger(this.requestId, this.deployId));
    }
}


export default DeployDetailController;
