import Controller from './Controller';
import DeployView from '../views/deploy';
import { FetchAction } from '../actions/api/deploy';

class DeployDetailController extends Controller {

    initialize({store, requestId, deployId}) {
        app.showPageLoader()
        this.title('Status');
        this.store = store;
        this.requestId = requestId;
        this.deployId = deployId;

        let initPromise = this.store.dispatch(FetchAction.trigger(requestId, deployId));
        initPromise.then(() => {
          this.setView(new DeployView(store));
          app.showView(this.view);
        });
    }

    refresh() {
        this.store.dispatch(FetchAction.trigger(this.requestId, this.deployId));
    }
}


export default DeployDetailController;
