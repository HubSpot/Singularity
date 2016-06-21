import Controller from './Controller';
import DashboardView from '../views/dashboard';
import { FetchAction } from '../actions/api/requests';
import { getStarredRequests } from '../actions/ui/starred';

class DashboardController extends Controller {

  initialize({store}) {
    app.showPageLoader();
    this.title('Dashboard');
    this.store = store;

    // load from localStorage
    this.store.dispatch(getStarredRequests());

    const initPromise = this.store.dispatch(FetchAction.trigger());
    initPromise.then(() => {
      this.setView(new DashboardView(store));
      app.showView(this.view);
    });
  }

  refresh() {
    this.store.dispatch(FetchAction.trigger());
  }
}


export default DashboardController;
