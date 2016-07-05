import ReactView from './reactView';

import GlobalSearch from '../components/globalSearch/GlobalSearch';

import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

class GlobalSearchView extends ReactView {

  initialize({store}) {
    this.store = store;
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
