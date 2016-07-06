import ReactView from './reactView';

import GlobalSearch from '../components/globalSearch/GlobalSearch';
import { SetVisibility } from '../actions/ui/globalSearch';

import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

class GlobalSearchView extends ReactView {

  initialize({store}) {
    this.store = store;
  }

  show() {
    this.store.dispatch(SetVisibility(true));
  }

  hide() {
    this.store.dispatch(SetVisibility(false));
  }

  render() {
    ReactDOM.render(
      <Provider store={this.store}>
        <GlobalSearch
          requests={this.collection}
        />
      </Provider>,
      this.el
    );
  }
}

export default GlobalSearchView;
