import Controller from './Controller';

import RequestsView from '../views/requests';

import { fetchRequests } from '../actions/api/requests';

class RequestsController extends Controller {

  initialize({store}) {
    this.store = store;
    this.title('Requests');

    this.store.dispatch(fetchRequests());

    this.view = new RequestsView(this.store);

    this.setView(this.view);
    return app.showView(this.view);
  }
}

export default RequestsController;
