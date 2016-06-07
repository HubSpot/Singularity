import View from './view';

import Slave from '../models/Slave';
import Slaves from '../collections/Slaves';
import SlavesPage from '../components/machines/Slaves';

import React from 'react';
import ReactDOM from 'react-dom';

class SlavesView extends View {

    constructor(...args) {
      super(...args);
    }

    initialize({state}) {
        this.state = state;
        this.initialPageLoad = true;
    }

    render() {
        if (!this.collection.synced && this.collection.isEmpty()) {
          return;
        }
        ReactDOM.render(<SlavesPage slaves={this.collection} />, this.el)

        if (this.state && this.initialPageLoad) {
            if (this.state === 'all') { return; }
            utils.scrollTo(`#${this.state}`);
            this.initialPageLoad = false;
        }
    }
}

export default SlavesView;
