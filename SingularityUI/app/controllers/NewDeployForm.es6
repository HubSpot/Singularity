import Controller from './Controller';

import NewDeployFormView from 'views/newDeployForm';

import { FetchAction as RequestFetchAction } from '../actions/api/request';

class NewDeployForm extends Controller {

  showView () {
    this.setView(new NewDeployFormView({store: this.store}));
    app.showView(this.view);
  }

  initialize ({store, requestId}) {
    app.showPageLoader();
    this.title('New Deploy');
    this.store = store;

    this.store.dispatch(RequestFetchAction.trigger(requestId)).then(() => this.showView());
  }
}

export default NewDeployForm;
