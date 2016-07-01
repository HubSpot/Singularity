import Controller from './Controller';
import StatusView from '../views/status';
import { FetchSingularityStatus } from '../actions/api/state';

class StatusController extends Controller {

  initialize({store}) {
    app.showPageLoader();
    this.title('Status');
    this.store = store;

    const initPromise = this.store.dispatch(FetchSingularityStatus.trigger());
    initPromise.then(() => {
      this.setView(new StatusView(store));
      app.showView(this.view);
    });
  }

  refresh() {
    this.store.dispatch(FetchSingularityStatus.trigger());
  }
}


export default StatusController;
