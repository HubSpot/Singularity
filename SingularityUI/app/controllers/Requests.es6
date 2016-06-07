import Controller from './Controller';

import Requests from '../collections/Requests';

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

    let initPromise = this.store.dispatch(fetchRequests());

    this.view = new RequestsView(this.store);

    this.setView(this.view);
    return app.showView(this.view);
  }
}

export default RequestsController;
