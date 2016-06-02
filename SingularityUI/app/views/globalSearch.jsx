import View from './view';

import Requests from '../collections/Requests';

import GlobalSearch from '../components/globalSearch/GlobalSearch';

import React from 'react';
import ReactDOM from 'react-dom';

class GlobalSearchView extends View {
  constructor(...args) {
    super(...args);
    this.show = this.show.bind(this);
    this.hide = this.hide.bind(this);
    this.focusSearch = this.focusSearch.bind(this);

    this.searchActive = false;
  }

  show() {
    this.searchActive = true;
    this.collection.fetch().done(() => {
      return this.render();
    });

    this.render();
    return this.focusSearch();
  }

  hide() {
    this.searchActive = false;
    return this.render();
  }

  focusSearch() {
    return this.ref.focus();
  }

  initialize({state}) {
    this.state = state;
    this.collection = new Requests([], {state: 'all'});

    return $(window).on('keydown', event => {
      let focusBody = $(event.target).is('body');
      let focusInput = $(event.target).is(this.$('input.big-search-box'));

      let modifierKey = event.metaKey || event.shiftKey || event.ctrlKey;
      // s and t
      let loadSearchKeysPressed = [83, 84].indexOf(event.keyCode) >= 0 && !modifierKey;
      let escPressed = event.keyCode === 27;

      if (escPressed && (focusBody || focusInput)) {
        return this.hide();
      } else if (loadSearchKeysPressed && focusBody) {
        this.show();
        return event.preventDefault();
      }
    });
  }

  render() {
    return this.ref = ReactDOM.render(
      <GlobalSearch
        requests={this.collection}
        visible={this.searchActive}
        onHide={this.hide}
      />,
      this.el
    );
  }
}

export default GlobalSearchView;
