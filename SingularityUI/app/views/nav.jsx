// _ = require 'underscore'
// View = require './view'
//
// class NavView extends View
//
//     template: require '../templates/nav'
//
//     initialize: ->
//         Backbone.history.on 'route', =>
//             @render()
//
//     events: ->
//         _.extend super,
//             'click .global-search-button': 'showSearch'
//
//     render: ->
//         fragment = Backbone.history.fragment?.split("/")[0]
//
//         # So the nav things are highlighted on the detail pages
//         if fragment is 'request'
//             fragment = 'requests'
//         else if fragment is 'task'
//             fragment = 'tasks'
//
//         @$el.html @template {fragment, title: config.title}
//
//     showSearch: ->
//         app.views.globalSearch.show()
//
// module.exports = NavView

import ReactView from './reactView';
import Navigation from '../components/common/Navigation';

import React from 'react';
import ReactDOM from 'react-dom';

class NavView extends ReactView {
  render() {
    ReactDOM.render(<Navigation path={Backbone.history.fragment} />, this.el);
  }
}

export default NavView;
