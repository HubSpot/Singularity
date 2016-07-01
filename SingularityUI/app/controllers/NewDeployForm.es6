import Controller from './Controller';

import NewDeployFormView from 'views/newDeployForm';

import { FetchRequest, SaveRequest } from '../actions/api/requests';

import { ClearForm } from '../actions/ui/form';

class NewDeployForm extends Controller {

  showView () {
    this.setView(new NewDeployFormView({store: this.store, requestId: this.requestId}));
    app.showView(this.view);
  }

  initialize ({store, requestId}) {
    app.showPageLoader();
    this.title('New Deploy');
    this.store = store;
    this.requestId = requestId;

    const requestFetchPromise = this.store.dispatch(FetchRequest.trigger(requestId));
    const formClearPromise = this.store.dispatch(ClearForm('newDeployForm'));
    const clearSaveDeployDataPromise = this.store.dispatch(SaveRequest.clearData());
    Promise.all([requestFetchPromise, formClearPromise, clearSaveDeployDataPromise]).then(() => this.showView());
  }
}

export default NewDeployForm;
