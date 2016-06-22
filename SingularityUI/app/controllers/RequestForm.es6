import Controller from './Controller';
import RequestFormView from '../views/requestForm';
import Racks from '../collections/Racks';
import Request from '../models/Request';
import { FetchAction as RequestFetchAction } from '../actions/api/request';
import { FetchAction as RacksFetchAction } from '../actions/api/racks';

class ReqeustFormController extends Controller {

  initialize({store, requestId = ''}) {
    app.showPageLoader();
    this.title(`${requestId ? 'Edit' : 'New'} Request`);
    this.store = store;
    this.requestId = requestId;

    const racksPromise = this.store.dispatch(RacksFetchAction.trigger());
    const requestPromise = this.requestId ? this.store.dispatch(RequestFetchAction.trigger(this.requestId)) : Promise.resolve();

    Promise.all([racksPromise, requestPromise]).then(() => {
      this.setView(new RequestFormView({store: this.store, editing: this.requestId}));
      app.showView(this.view);
    });
  }
}

export default ReqeustFormController;
