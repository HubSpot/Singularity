import ReactView from './reactView';
import Navigation from '../components/common/Navigation';

import React from 'react';
import ReactDOM from 'react-dom';

class NavView extends ReactView {

  initialize() {
    // Won't be necessary once we switch to react (redux) router
    window.addEventListener('viewChange', () => {
      this.render();
    });
  }

  render() {
    ReactDOM.render(<Navigation path={Backbone.history.fragment} />, this.el);
  }
}

export default NavView;
