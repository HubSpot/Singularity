import Controller from './Controller';
import SlavesView from '../views/slaves';
import { fetchSlaves } from '../actions/api/slaves';

class SlavesController extends Controller {

  initialize({store}) {
      this.title('Slaves');
      this.setView(new SlavesView(store));
      app.showView(this.view);

      this.store = store;

      this.refresh();
  }

  refresh() {
      this.store.dispatch(fetchSlaves());
  }
}

export default SlavesController
