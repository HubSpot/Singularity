import Controller from './Controller';
import RequestFormView from '../views/requestForm';
import Racks from '../collections/Racks';
import Request from '../models/Request';
import { FetchAction as RequestFetchAction } from '../actions/api/request';
import { FetchAction as RacksFetchAction } from '../actions/api/racks';

class ReqeustFormController extends Controller {

  showViewIfReady() {
    if (this.racksPromiseDone && (this.requestPromiseDone || !this.requestId)) {
      this.setView(new RequestFormView({store: this.store}));
      app.showView(this.view);
    }
  }

  initialize({store, requestId = ''}) {
    app.showPageLoader();
    this.title(`${requestId ? 'Edit' : 'New'} Request`);
    this.store = store;
    this.requestId = requestId;

    this.requestPromiseDone = false;
    this.racksPromiseDone = false;

    const racksPromise = this.store.dispatch(RacksFetchAction.trigger()).then(() => {
      this.racksPromiseDone = true;
      this.showViewIfReady();
    })
    if (requestId) {
      const requestPromise = this.store.dispatch(RequestFetchAction.trigger(this.requestId));
      requestPromise.then(() => {
        this.requestPromiseDone = true;
        this.showViewIfReady();
      });
    }
  }
}

export default ReqeustFormController;
