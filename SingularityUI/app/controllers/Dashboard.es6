import Controller from './Controller';
import DashboardView from '../views/dashboard';
import { FetchAction } from '../actions/api/requests';

class DashboardController extends Controller {

  initialize({store}) {
    app.showPageLoader()
    this.title('Dashboard');
    this.store = store;

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
