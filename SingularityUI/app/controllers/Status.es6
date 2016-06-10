import Controller from './Controller';
import StatusView from '../views/status';
import { fetchStatus } from '../actions/api/status';

class StatusController extends Controller {

    initialize({store}) {
        app.showPageLoader()
        this.title('Status');
        this.store = store;

        let initPromise = this.store.dispatch(fetchStatus());
        initPromise.then(() => {
          this.setView(new StatusView(store));
          app.showView(this.view);
        });
    }

    refresh() {
        this.store.dispatch(fetchStatus());
    }
}


export default StatusController;
