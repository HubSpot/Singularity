import Controller from './Controller';
import DashboardView from '../views/dashboard';
import { FetchRequests } from '../actions/api/requests';

class DashboardController extends Controller {

  initialize({store}) {
    app.showPageLoader();
    this.title('Dashboard');
    this.store = store;

    const initPromise = this.store.dispatch(FetchRequests.trigger());
    initPromise.then(() => {
      this.setView(new DashboardView(store));
      app.showView(this.view);
    });
  }

  refresh() {
    this.store.dispatch(FetchRequests.trigger());
  }
}


export default DashboardController;
