import View from './view';

import Rack from '../models/Rack';
import Racks from '../collections/Racks';
import RacksPage from '../components/machines/Racks';

import React from 'react';
import ReactDOM from 'react-dom';

class RacksView extends View {

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

        ReactDOM.render(<RacksPage racks={this.collection} />, this.el);

        if (this.state && this.initialPageLoad) {
            if (this.state === 'all') { return; }
            utils.scrollTo(`#${this.state}`);
            this.initialPageLoad = false;
        }
    }
}


export default RacksView;
