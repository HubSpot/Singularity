import Controller from './Controller';

import NewDeployFormView from 'views/newDeployForm';

import { FetchAction as RequestFetchAction, SaveAction } from '../actions/api/request';

import { clearForm } from '../actions/form';

class NewDeployForm extends Controller {

  showView () {
    this.setView(new NewDeployFormView({store: this.store}));
    app.showView(this.view);
  }

  initialize ({store, requestId}) {
    app.showPageLoader();
    this.title('New Deploy');
    this.store = store;

    const requestFetchPromise = this.store.dispatch(RequestFetchAction.trigger(requestId));
    const formClearPromise = this.store.dispatch(clearForm('newDeployForm'));
    const clearSaveDeployDataPromise = this.store.dispatch(SaveAction.clearData());
    Promise.all([requestFetchPromise, formClearPromise, clearSaveDeployDataPromise]).then(() => this.showView());
  }
}

export default NewDeployForm;
