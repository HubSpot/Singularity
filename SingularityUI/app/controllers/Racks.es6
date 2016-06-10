import Controller from './Controller';
import RacksView from '../views/racks';
import { fetchRacks } from '../actions/api/racks';

class RacksController extends Controller {
  initialize({store}) {
    console.log("store:", store);
    this.title('Racks');
    this.setView(new RacksView(store));
    app.showView(this.view);
    this.store = store;
    this.refresh();
  }

  refresh() {
    this.store.dispatch(fetchRacks());
  }
}

export default RacksController;
