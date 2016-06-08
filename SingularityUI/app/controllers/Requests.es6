import Controller from './Controller';

import RequestsView from '../views/requests';

import configureStore from '../store/configureStore';

import { fetchRequests } from '../actions/api/requests';

class RequestsController extends Controller {

  initialize({state, subFilter, searchFilter}) {
    this.state = state;
    this.subFilter = subFilter;
    this.searchFilter = searchFilter;
    this.title('Requests');

    this.store = configureStore();

    this.store.dispatch(fetchRequests());

    this.view = new RequestsView(this.store);

    this.setView(this.view);
    return app.showView(this.view);
  }
}

export default RequestsController;
