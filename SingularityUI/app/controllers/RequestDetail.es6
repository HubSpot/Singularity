import Controller from './Controller';
import RequestDetailView from '../views/request';
import { FetchAction } from '../actions/api/request';

class RequestDetailController extends Controller {

  initialize({requestId, store}) {
    app.showPageLoader();
    this.requestId = requestId;
    this.store = store;

    this.title(this.requestId);

    const initPromise = this.store.dispatch(FetchAction.trigger(this.requestId));
    initPromise.then(() => {
      this.setView(new RequestDetailView({store, requestId}));
      app.showView(this.view);
    });
  }

  refresh() {
    this.store.dispatch(FetchAction.trigger(this.requestId));
  }
}


export default RequestDetailController;
